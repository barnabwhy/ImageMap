package cc.barnab.core.maps;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageSources;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypeFilter;
import net.minecraft.util.math.*;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class PosterMap {
    private static final HashMap<UUID, Long> LAST_PLACED_POSTER = new HashMap<>();

    public static boolean place(PlayerEntity player, Hand hand, ItemFrameEntity entity) {
        if (LAST_PLACED_POSTER.containsKey(player.getUuid()) && System.currentTimeMillis() - LAST_PLACED_POSTER.get(player.getUuid()) < 100)
            return false;

        LAST_PLACED_POSTER.put(player.getUuid(), System.currentTimeMillis());

        ItemStack heldItem = player.getStackInHand(hand);

        if(entity.containsMap())
            return false;

        if (!heldItem.isOf(Items.FILLED_MAP) || !heldItem.contains(DataComponentTypes.CUSTOM_DATA))
            return false;

        NbtCompound nbt = heldItem.get(DataComponentTypes.CUSTOM_DATA).getNbt();
        if (!nbt.contains("image_map_id") || !nbt.contains("image_map_owner"))
            return false;

        String mapId = nbt.getString("image_map_id");
        String uuid = nbt.getString("image_map_owner");

        Optional<MapImage> mapImage = MapLoader.getPlayerMaps(UUID.fromString(uuid)).mapList.stream().filter(m -> m.getId().equals(mapId)).findFirst();

        if (mapImage.isEmpty())
            return false;

        Direction playerDir = Direction.getFacing(player.getRotationVector(0.0f, player.getYaw(1.0f)));
        List<ItemFrameEntity> frames = getFrames(entity, playerDir, mapImage.get().getWidth(), mapImage.get().getHeight());
        if (frames.size() < mapImage.get().getWidth() * mapImage.get().getHeight()) {
            player.sendMessage(Text.literal("Failed to place poster map. Not enough item frames were available.").formatted(Formatting.RED), false);
            return false;
        }

        BlockPos origin = entity.getBlockPos();

        // Async to prevent bug where bottom left map gets rotated
        CompletableFuture.supplyAsync(() -> {
            int i = 0;
            for (ItemFrameEntity frame : frames) {
                int rotation = getFrameRotation(entity, player);
                ItemStack mapItem = MapItem.fromMapImageForFrame(mapImage.get(), i, origin, rotation);
                frame.setHeldItemStack(mapItem);
                frame.setRotation(rotation);
                i++;
            }
            return true;
        });

        return true;
    }

    public static boolean destroy(PlayerEntity player, ItemFrameEntity entity) {
        if (LAST_PLACED_POSTER.containsKey(player.getUuid()) && System.currentTimeMillis() - LAST_PLACED_POSTER.get(player.getUuid()) < 100)
            return false;

        LAST_PLACED_POSTER.put(player.getUuid(), System.currentTimeMillis());

        if(!entity.containsMap())
            return false;

        ItemStack framedMap = entity.getHeldItemStack();
        if (!framedMap.contains(DataComponentTypes.CUSTOM_DATA))
            return false;

        NbtCompound nbt = framedMap.get(DataComponentTypes.CUSTOM_DATA).getNbt();
        if (
                !nbt.contains("image_map_id")
                || !nbt.contains("image_map_origin_x") || !nbt.contains("image_map_origin_y") || !nbt.contains("image_map_origin_z")
                || !nbt.contains("image_map_width") || !nbt.contains("image_map_height")
        )
            return false;

        String mapId = nbt.getString("image_map_id");
        int originX = nbt.getInt("image_map_origin_x");
        int originY = nbt.getInt("image_map_origin_y");
        int originZ = nbt.getInt("image_map_origin_z");
        int rotation = nbt.getInt("image_map_rotation");
        int width = nbt.getInt("image_map_width");
        int height = nbt.getInt("image_map_height");

        Direction playerDir = Direction.UP;
        if (entity.getFacing() == Direction.UP) {
            playerDir = switch (rotation) {
                case 0 -> Direction.NORTH;
                case 1 -> Direction.EAST;
                case 2 -> Direction.SOUTH;
                case 3 -> Direction.WEST;
                default -> Direction.UP;
            };
        } else if (entity.getFacing() == Direction.DOWN) {
            playerDir = switch (rotation) {
                case 0 -> Direction.SOUTH;
                case 1 -> Direction.WEST;
                case 2 -> Direction.NORTH;
                case 3 -> Direction.EAST;
                default -> Direction.UP;
            };
        }

        BlockPos origin = new BlockPos(originX, originY, originZ);

        List<ItemFrameEntity> frames = getFrames(entity, playerDir, width, height, origin);

        // If the item frame isn't actually in the area don't delete them
        // Prevents bug causing remote map deletion to be possible (kind of funny)
        if (!frames.contains(entity)) {
            return false;
        }

        for (ItemFrameEntity frame : frames) {
            if (!frame.containsMap())
                continue;

            ItemStack mapItem = frame.getHeldItemStack();
            NbtCompound framedNbt = mapItem.get(DataComponentTypes.CUSTOM_DATA).getNbt();
            if (
                    !framedNbt.contains("image_map_id")
                    || !framedNbt.contains("image_map_origin_x") || !framedNbt.contains("image_map_origin_y") || !framedNbt.contains("image_map_origin_z")
                    || !framedNbt.contains("image_map_width") || !framedNbt.contains("image_map_height")
            )
                continue;

            String framedMapId = nbt.getString("image_map_id");
            int framedOriginX = nbt.getInt("image_map_origin_x");
            int framedOriginY = nbt.getInt("image_map_origin_y");
            int framedOriginZ = nbt.getInt("image_map_origin_z");

            if (mapId.equals(framedMapId) && originX == framedOriginX && originY == framedOriginY && originZ == framedOriginZ)
                frame.damage((ServerWorld) frame.getWorld(), new DamageSource(new DamageSources(entity.getRegistryManager()).create(DamageTypes.PLAYER_ATTACK).getTypeRegistryEntry(), player), 0.0f);
        }

        return true;
    }

    private static int getFrameRotation(ItemFrameEntity entity, PlayerEntity player) {
        if (entity.getFacing() == Direction.UP) {
            // Floor
            Direction playerYawDir = Direction.getFacing(player.getRotationVector(0.0f, player.getYaw(1.0f)));
            return switch (playerYawDir) {
                case NORTH -> 0;
                case EAST -> 1;
                case SOUTH -> 2;
                case WEST -> 3;
                default -> 0; // :3
            };
        } else if (entity.getFacing() == Direction.DOWN) {
            // Ceiling
            Direction playerYawDir = Direction.getFacing(player.getRotationVector(0.0f, player.getYaw(1.0f)));
            return switch (playerYawDir) {
                case NORTH -> 2;
                case EAST -> 3;
                case SOUTH -> 0;
                case WEST -> 1;
                default -> 0; // :3
            };
        }

        // Wall
        return 0;
    }

    private static List<ItemFrameEntity> getFrames(ItemFrameEntity entity, Direction playerDir, int width, int height) {
        return getFrames(entity, playerDir, width, height, entity.getBlockPos());
    }

    private static List<ItemFrameEntity> getFrames(ItemFrameEntity entity, Direction playerDir, int width, int height, BlockPos origin) {
        if (entity.getFacing() == Direction.UP) {
            // Floor
            Direction rightDir = switch (playerDir) {
                case NORTH -> Direction.EAST;
                case EAST -> Direction.SOUTH;
                case SOUTH -> Direction.WEST;
                case WEST -> Direction.NORTH;
                default -> Direction.UP; // :3
            };

            World world = entity.getWorld();

            int xSize = (width - 1) * rightDir.getOffsetX() + (height - 1) * playerDir.getOffsetX();
            int zSize = (width - 1) * rightDir.getOffsetZ() + (height - 1) * playerDir.getOffsetZ();

            BlockPos endPos = origin.add(new Vec3i(xSize, 0, zSize));

            List<ItemFrameEntity> allFrames = world.getEntitiesByType(TypeFilter.instanceOf(ItemFrameEntity.class), Box.enclosing(origin, endPos), e -> {
                if (e instanceof ItemFrameEntity i) {
                    return i.isAlive() && (!i.containsMap() || i.getHeldItemStack().contains(DataComponentTypes.CUSTOM_DATA)) && i.getFacing().equals(entity.getFacing());
                }
                return false;
            });

            allFrames.sort((a, b) -> {
                BlockPos aPos = a.getBlockPos();
                BlockPos bPos = b.getBlockPos();

                int xUpDiff = (aPos.getX() - bPos.getX()) * playerDir.getOffsetX();
                int zUpDiff = (aPos.getZ() - bPos.getZ()) * playerDir.getOffsetZ();
                if (xUpDiff + zUpDiff != 0) {
                    return xUpDiff + zUpDiff;
                }

                int xRightDiff = (aPos.getX() - bPos.getX()) * rightDir.getOffsetX();
                int zRightDiff = (aPos.getZ() - bPos.getZ()) * rightDir.getOffsetZ();
                return xRightDiff + zRightDiff;
            });

            return allFrames;
        } else if (entity.getFacing() == Direction.DOWN) {
            // Ceiling
            Direction rightDir = switch (playerDir) {
                case NORTH -> Direction.EAST;
                case EAST -> Direction.SOUTH;
                case SOUTH -> Direction.WEST;
                case WEST -> Direction.NORTH;
                default -> Direction.UP; // :3
            };

            World world = entity.getWorld();

            int xSize = (width - 1) * rightDir.getOffsetX() + (height - 1) * -playerDir.getOffsetX();
            int zSize = (width - 1) * rightDir.getOffsetZ() + (height - 1) * -playerDir.getOffsetZ();

            BlockPos endPos = origin.add(new Vec3i(xSize, 0, zSize));

            List<ItemFrameEntity> allFrames = world.getEntitiesByType(TypeFilter.instanceOf(ItemFrameEntity.class), Box.enclosing(origin, endPos), e -> {
                if (e instanceof ItemFrameEntity i) {
                    return i.isAlive() && (!i.containsMap() || i.getHeldItemStack().contains(DataComponentTypes.CUSTOM_DATA)) && i.getFacing().equals(entity.getFacing());
                }
                return false;
            });

            allFrames.sort((a, b) -> {
                BlockPos aPos = a.getBlockPos();
                BlockPos bPos = b.getBlockPos();

                int xUpDiff = (aPos.getX() - bPos.getX()) * -playerDir.getOffsetX();
                int zUpDiff = (aPos.getZ() - bPos.getZ()) * -playerDir.getOffsetZ();
                if (xUpDiff + zUpDiff != 0) {
                    return xUpDiff + zUpDiff;
                }

                int xRightDiff = (aPos.getX() - bPos.getX()) * rightDir.getOffsetX();
                int zRightDiff = (aPos.getZ() - bPos.getZ()) * rightDir.getOffsetZ();
                return xRightDiff + zRightDiff;
            });

            return allFrames;
        } else {
            // Wall
            Direction rightDir = switch (entity.getFacing()) {
                case NORTH -> Direction.WEST;
                case EAST -> Direction.NORTH;
                case SOUTH -> Direction.EAST;
                case WEST -> Direction.SOUTH;
                default -> Direction.UP; // :3
            };

            World world = entity.getWorld();

            int xSize = (width - 1) * rightDir.getOffsetX();
            int zSize = (width - 1) * rightDir.getOffsetZ();

            BlockPos endPos = origin.add(new Vec3i(xSize, height - 1, zSize));

            List<ItemFrameEntity> allFrames = world.getEntitiesByType(TypeFilter.instanceOf(ItemFrameEntity.class), Box.enclosing(origin, endPos), e -> {
                if (e instanceof ItemFrameEntity i) {
                    return i.isAlive() && (!i.containsMap() || i.getHeldItemStack().contains(DataComponentTypes.CUSTOM_DATA)) && i.getFacing().equals(entity.getFacing());
                }
                return false;
            });

            allFrames.sort((a, b) -> {
                BlockPos aPos = a.getBlockPos();
                BlockPos bPos = b.getBlockPos();
                if (aPos.getY() == bPos.getY()) {
                    int xDiff = (aPos.getX() - bPos.getX()) * rightDir.getOffsetX();
                    int zDiff = (aPos.getZ() - bPos.getZ()) * rightDir.getOffsetZ();
                    return xDiff + zDiff;
                } else {
                    return aPos.getY() - bPos.getY();
                }
            });

            return allFrames;
        }
    }
}

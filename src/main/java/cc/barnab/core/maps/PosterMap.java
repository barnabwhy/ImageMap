package cc.barnab.core.maps;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class PosterMap {
    private static final HashMap<UUID, Long> LAST_PLACED_POSTER = new HashMap<>();

    public static boolean place(Player player, InteractionHand hand, ItemFrame entity) {
        if (LAST_PLACED_POSTER.containsKey(player.getUUID()) && System.currentTimeMillis() - LAST_PLACED_POSTER.get(player.getUUID()) < 100)
            return false;

        LAST_PLACED_POSTER.put(player.getUUID(), System.currentTimeMillis());

        ItemStack heldItem = player.getItemInHand(hand);

        if(entity.hasFramedMap())
            return false;

        if (!heldItem.is(Items.FILLED_MAP) || !heldItem.has(DataComponents.CUSTOM_DATA))
            return false;

        CompoundTag nbt = Objects.requireNonNull(heldItem.get(DataComponents.CUSTOM_DATA)).copyTag();
        if (!nbt.contains("image_map_id") || !nbt.contains("image_map_owner"))
            return false;

        String mapId = nbt.getString("image_map_id").get();
        String uuid = nbt.getString("image_map_owner").get();

        Optional<MapImage> mapImage = MapLoader.getPlayerMaps(UUID.fromString(uuid)).mapList.stream().filter(m -> m.getId().equals(mapId)).findFirst();

        if (mapImage.isEmpty())
            return false;

        Direction playerDir = Direction.fromYRot(player.getViewYRot(1.0f));
        List<ItemFrame> frames = getFrames(entity, playerDir, mapImage.get().getWidth(), mapImage.get().getHeight());
        if (frames.size() < mapImage.get().getWidth() * mapImage.get().getHeight()) {
            player.sendSystemMessage(Component.literal("Failed to place poster map. Not enough item frames were available.").withStyle(ChatFormatting.RED));
            return false;
        }

        BlockPos origin = entity.blockPosition();

        // Async to prevent bug where bottom left map gets rotated
        CompletableFuture.supplyAsync(() -> {
            int i = 0;
            for (ItemFrame frame : frames) {
                int rotation = getFrameRotation(entity, player);
                ItemStack mapItem = MapItem.fromMapImageForFrame(mapImage.get(), i, origin, rotation);
                frame.setItem(mapItem);
                frame.setRotation(rotation);
                i++;
            }
            return true;
        });

        return true;
    }

    public static boolean destroy(Player player, ItemFrame entity) {
        if (LAST_PLACED_POSTER.containsKey(player.getUUID()) && System.currentTimeMillis() - LAST_PLACED_POSTER.get(player.getUUID()) < 100)
            return false;

        LAST_PLACED_POSTER.put(player.getUUID(), System.currentTimeMillis());

        if(!entity.hasFramedMap())
            return false;

        ItemStack framedMap = entity.getItem();
        if (!framedMap.has(DataComponents.CUSTOM_DATA))
            return false;

        CompoundTag nbt = Objects.requireNonNull(framedMap.get(DataComponents.CUSTOM_DATA)).copyTag();
        if (
                !nbt.contains("image_map_id")
                || !nbt.contains("image_map_origin_x") || !nbt.contains("image_map_origin_y") || !nbt.contains("image_map_origin_z")
                || !nbt.contains("image_map_width") || !nbt.contains("image_map_height")
        )
            return false;

        String mapId = nbt.getString("image_map_id").get();
        int originX = nbt.getInt("image_map_origin_x").get();
        int originY = nbt.getInt("image_map_origin_y").get();
        int originZ = nbt.getInt("image_map_origin_z").get();
        int rotation = nbt.getInt("image_map_rotation").get();
        int width = nbt.getInt("image_map_width").get();
        int height = nbt.getInt("image_map_height").get();

        Direction playerDir = Direction.UP;
        if (entity.getDirection() == Direction.UP) {
            playerDir = switch (rotation) {
                case 0 -> Direction.NORTH;
                case 1 -> Direction.EAST;
                case 2 -> Direction.SOUTH;
                case 3 -> Direction.WEST;
                default -> Direction.UP;
            };
        } else if (entity.getDirection() == Direction.DOWN) {
            playerDir = switch (rotation) {
                case 0 -> Direction.SOUTH;
                case 1 -> Direction.WEST;
                case 2 -> Direction.NORTH;
                case 3 -> Direction.EAST;
                default -> Direction.UP;
            };
        }

        BlockPos origin = new BlockPos(originX, originY, originZ);

        List<ItemFrame> frames = getFrames(entity, playerDir, width, height, origin);

        // If the item frame isn't actually in the area don't delete them
        // Prevents bug causing remote map deletion to be possible (kind of funny)
        if (!frames.contains(entity)) {
            return false;
        }

        for (ItemFrame frame : frames) {
            if (!frame.hasFramedMap())
                continue;

            ItemStack mapItem = frame.getItem();
            CompoundTag framedNbt = Objects.requireNonNull(mapItem.get(DataComponents.CUSTOM_DATA)).copyTag();
            if (
                    !framedNbt.contains("image_map_id")
                    || !framedNbt.contains("image_map_origin_x") || !framedNbt.contains("image_map_origin_y") || !framedNbt.contains("image_map_origin_z")
                    || !framedNbt.contains("image_map_width") || !framedNbt.contains("image_map_height")
            )
                continue;

            String framedMapId = nbt.getString("image_map_id").get();
            int framedOriginX = nbt.getInt("image_map_origin_x").get();
            int framedOriginY = nbt.getInt("image_map_origin_y").get();
            int framedOriginZ = nbt.getInt("image_map_origin_z").get();

            if (mapId.equals(framedMapId) && originX == framedOriginX && originY == framedOriginY && originZ == framedOriginZ)
                frame.hurtServer((ServerLevel) frame.level(), new DamageSource(new DamageSources(entity.registryAccess()).source(DamageTypes.PLAYER_ATTACK).typeHolder(), player), 0.0f);
        }

        return true;
    }

    private static int getFrameRotation(ItemFrame entity, Player player) {
        if (entity.getDirection() == Direction.UP) {
            // Floor
            Direction playerYawDir = Direction.fromYRot(player.getViewYRot(1.0f));
            return switch (playerYawDir) {
                case NORTH -> 0;
                case EAST -> 1;
                case SOUTH -> 2;
                case WEST -> 3;
                default -> 0; // :3
            };
        } else if (entity.getDirection() == Direction.DOWN) {
            // Ceiling
            Direction playerYawDir = Direction.fromYRot(player.getViewYRot(1.0f));
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

    private static List<ItemFrame> getFrames(ItemFrame entity, Direction playerDir, int width, int height) {
        return getFrames(entity, playerDir, width, height, entity.blockPosition());
    }

    private static List<ItemFrame> getFrames(ItemFrame entity, Direction playerDir, int width, int height, BlockPos origin) {
        if (entity.getDirection() == Direction.UP) {
            // Floor
            Direction rightDir = switch (playerDir) {
                case NORTH -> Direction.EAST;
                case EAST -> Direction.SOUTH;
                case SOUTH -> Direction.WEST;
                case WEST -> Direction.NORTH;
                default -> Direction.UP; // :3
            };

            Level world = entity.level();

            int xSize = (width - 1) * rightDir.getStepX() + (height - 1) * playerDir.getStepX();
            int zSize = (width - 1) * rightDir.getStepZ() + (height - 1) * playerDir.getStepZ();

            BlockPos endPos = origin.offset(new Vec3i(xSize, 0, zSize));

            List<ItemFrame> allFrames = world.getEntities(EntityTypeTest.forClass(ItemFrame.class), AABB.encapsulatingFullBlocks(origin, endPos), e -> {
                if (e instanceof ItemFrame i) {
                    return i.isAlive() && (!i.hasFramedMap() || i.getItem().has(DataComponents.CUSTOM_DATA)) && i.getDirection().equals(entity.getDirection());
                }
                return false;
            });

            allFrames.sort((a, b) -> {
                BlockPos aPos = a.blockPosition();
                BlockPos bPos = b.blockPosition();

                int xUpDiff = (aPos.getX() - bPos.getX()) * playerDir.getStepX();
                int zUpDiff = (aPos.getZ() - bPos.getZ()) * playerDir.getStepZ();
                if (xUpDiff + zUpDiff != 0) {
                    return xUpDiff + zUpDiff;
                }

                int xRightDiff = (aPos.getX() - bPos.getX()) * rightDir.getStepX();
                int zRightDiff = (aPos.getZ() - bPos.getZ()) * rightDir.getStepZ();
                return xRightDiff + zRightDiff;
            });

            return allFrames;
        } else if (entity.getDirection() == Direction.DOWN) {
            // Ceiling
            Direction rightDir = switch (playerDir) {
                case NORTH -> Direction.EAST;
                case EAST -> Direction.SOUTH;
                case SOUTH -> Direction.WEST;
                case WEST -> Direction.NORTH;
                default -> Direction.UP; // :3
            };

            Level world = entity.level();

            int xSize = (width - 1) * rightDir.getStepX() + (height - 1) * -playerDir.getStepX();
            int zSize = (width - 1) * rightDir.getStepZ() + (height - 1) * -playerDir.getStepZ();

            BlockPos endPos = origin.offset(new Vec3i(xSize, 0, zSize));

            List<ItemFrame> allFrames = world.getEntities(EntityTypeTest.forClass(ItemFrame.class), AABB.encapsulatingFullBlocks(origin, endPos), e -> {
                if (e instanceof ItemFrame i) {
                    return i.isAlive() && (!i.hasFramedMap() || i.getItem().has(DataComponents.CUSTOM_DATA)) && i.getDirection().equals(entity.getDirection());
                }
                return false;
            });

            allFrames.sort((a, b) -> {
                BlockPos aPos = a.blockPosition();
                BlockPos bPos = b.blockPosition();

                int xUpDiff = (aPos.getX() - bPos.getX()) * -playerDir.getStepX();
                int zUpDiff = (aPos.getZ() - bPos.getZ()) * -playerDir.getStepZ();
                if (xUpDiff + zUpDiff != 0) {
                    return xUpDiff + zUpDiff;
                }

                int xRightDiff = (aPos.getX() - bPos.getX()) * rightDir.getStepX();
                int zRightDiff = (aPos.getZ() - bPos.getZ()) * rightDir.getStepZ();
                return xRightDiff + zRightDiff;
            });

            return allFrames;
        } else {
            // Wall
            Direction rightDir = switch (entity.getDirection()) {
                case NORTH -> Direction.WEST;
                case EAST -> Direction.NORTH;
                case SOUTH -> Direction.EAST;
                case WEST -> Direction.SOUTH;
                default -> Direction.UP; // :3
            };

            Level world = entity.level();

            int xSize = (width - 1) * rightDir.getStepX();
            int zSize = (width - 1) * rightDir.getStepZ();

            BlockPos endPos = origin.offset(new Vec3i(xSize, height - 1, zSize));

            List<ItemFrame> allFrames = world.getEntities(EntityTypeTest.forClass(ItemFrame.class), AABB.encapsulatingFullBlocks(origin, endPos), e -> {
                if (e instanceof ItemFrame i) {
                    return i.isAlive() && (!i.hasFramedMap() || i.getItem().has(DataComponents.CUSTOM_DATA)) && i.getDirection().equals(entity.getDirection());
                }
                return false;
            });

            allFrames.sort((a, b) -> {
                BlockPos aPos = a.blockPosition();
                BlockPos bPos = b.blockPosition();
                if (aPos.getY() == bPos.getY()) {
                    int xDiff = (aPos.getX() - bPos.getX()) * rightDir.getStepX();
                    int zDiff = (aPos.getZ() - bPos.getZ()) * rightDir.getStepZ();
                    return xDiff + zDiff;
                } else {
                    return aPos.getY() - bPos.getY();
                }
            });

            return allFrames;
        }
    }
}

package cc.barnab.core.maps;

import cc.barnab.ImageMap;
import com.mojang.serialization.DataResult;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.MapColorComponent;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Unit;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

public class MapItem {
    public static ItemStack fromMapImage(MapImage mapImage, UUID ownerUUID) {
        return fromMapImage(mapImage, 0, ownerUUID);
    }
    public static ItemStack fromMapImage(MapImage mapImage, int mapIndex, UUID ownerUUID) {
        boolean isSingle = mapImage.getType() == MapImageType.SINGLE;

        if (mapImage.getMapIds() == null) {
            ImageMap.LOGGER.warn("Map " + mapImage.getId() + " had no map IDs");
            return ItemStack.EMPTY;
        }

        MapIdComponent mapId = new MapIdComponent(mapImage.getMapIds().get(mapIndex));

        ItemStack mapItem = Items.FILLED_MAP.getDefaultStack();
        mapItem.set(DataComponentTypes.ITEM_NAME, Text.literal(mapImage.getName()).formatted(Formatting.GREEN, Formatting.BOLD));

        Text mapTypeText = Text.literal(isSingle ? "Single map" : mapImage.getWidth() + "x" + mapImage.getHeight() + " map")
                .setStyle(Style.EMPTY.withItalic(false).withColor(Formatting.WHITE));

        Text mapIdText = Text.literal("ID: " + mapImage.getId())
                .setStyle(Style.EMPTY.withItalic(false).withColor(Formatting.GRAY));

        mapItem.set(DataComponentTypes.LORE, LoreComponent.DEFAULT.with(mapTypeText).with(mapIdText));
        mapItem.set(DataComponentTypes.MAP_ID, mapId);
        mapItem.set(DataComponentTypes.HIDE_ADDITIONAL_TOOLTIP, Unit.INSTANCE);

        if (isSingle) {
            mapItem.set(DataComponentTypes.MAP_COLOR, new MapColorComponent(43690)); // dark aqua
        } else {
            mapItem.set(DataComponentTypes.MAP_COLOR, new MapColorComponent(43520)); // dark green
        }

        // Custom data
        NbtCompound nbt = new NbtCompound();
        nbt.put("image_map_id", NbtString.of(mapImage.getId()));
        nbt.put("image_map_owner", NbtString.of(ownerUUID.toString()));

        NbtComponent nbtComponent = NbtComponent.of(nbt);
        mapItem.set(DataComponentTypes.CUSTOM_DATA, nbtComponent);

        return mapItem;
    }


    public static ItemStack fromMapImageForFrame(MapImage mapImage, int mapIndex, BlockPos origin, int rotation) {
        boolean isSingle = mapImage.getType() == MapImageType.SINGLE;

        MapIdComponent mapId = new MapIdComponent(mapImage.getMapIds().get(mapIndex));

        ItemStack mapItem = Items.FILLED_MAP.getDefaultStack();
        if (isSingle) {
            mapItem.set(DataComponentTypes.ITEM_NAME, Text.literal(mapImage.getName()).formatted(Formatting.GREEN, Formatting.BOLD));
            mapItem.set(DataComponentTypes.MAP_COLOR, new MapColorComponent(43690)); // dark aqua
        } else {
            int x = mapIndex % mapImage.getWidth();
            int y = mapIndex / mapImage.getWidth();
            mapItem.set(DataComponentTypes.ITEM_NAME,
                    Text.literal(mapImage.getName()).formatted(Formatting.GREEN, Formatting.BOLD)
                        .append(Text.literal(String.format(" (%d, %d)", x, y).formatted(Formatting.GRAY)))
            );
            mapItem.set(DataComponentTypes.MAP_COLOR, new MapColorComponent(43520)); // dark green
        }

        Text mapTypeText = Text.literal(isSingle ? "Single map" : mapImage.getWidth() + "x" + mapImage.getHeight() + " map")
                .setStyle(Style.EMPTY.withItalic(false).withColor(Formatting.WHITE));

        Text mapIdText = Text.literal("ID: " + mapImage.getId())
                .setStyle(Style.EMPTY.withItalic(false).withColor(Formatting.GRAY));

        mapItem.set(DataComponentTypes.LORE, LoreComponent.DEFAULT.with(mapTypeText).with(mapIdText));
        mapItem.set(DataComponentTypes.MAP_ID, mapId);
        mapItem.set(DataComponentTypes.HIDE_ADDITIONAL_TOOLTIP, Unit.INSTANCE);

        // Custom data
        NbtCompound nbt = new NbtCompound();
        nbt.put("image_map_id", NbtString.of(mapImage.getId()));
        nbt.put("image_map_origin_x", NbtInt.of(origin.getX()));
        nbt.put("image_map_origin_y", NbtInt.of(origin.getY()));
        nbt.put("image_map_origin_z", NbtInt.of(origin.getZ()));
        nbt.put("image_map_rotation", NbtInt.of(rotation));
        nbt.put("image_map_width", NbtInt.of(mapImage.getWidth()));
        nbt.put("image_map_height", NbtInt.of(mapImage.getHeight()));

        NbtComponent nbtComponent = NbtComponent.of(nbt);
        mapItem.set(DataComponentTypes.CUSTOM_DATA, nbtComponent);

        return mapItem;
    }

    public static ItemStack fromMapImageIndividual(MapImage mapImage, int mapIndex) {
        boolean isSingle = mapImage.getType() == MapImageType.SINGLE;

        if (mapImage.getMapIds() == null) {
            ImageMap.LOGGER.warn("Map " + mapImage.getId() + " had no map IDs");
            return ItemStack.EMPTY;
        }

        MapIdComponent mapId = new MapIdComponent(mapImage.getMapIds().get(mapIndex));

        ItemStack mapItem = Items.FILLED_MAP.getDefaultStack();

        Text mapTypeText = Text.literal(isSingle ? "Single map" : mapImage.getWidth() + "x" + mapImage.getHeight() + " map")
                .setStyle(Style.EMPTY.withItalic(false).withColor(Formatting.WHITE));

        Text mapIdText = Text.literal("ID: " + mapImage.getId())
                .setStyle(Style.EMPTY.withItalic(false).withColor(Formatting.GRAY));

        mapItem.set(DataComponentTypes.LORE, LoreComponent.DEFAULT.with(mapTypeText).with(mapIdText));
        mapItem.set(DataComponentTypes.MAP_ID, mapId);
        mapItem.set(DataComponentTypes.HIDE_ADDITIONAL_TOOLTIP, Unit.INSTANCE);

        if (isSingle) {
            mapItem.set(DataComponentTypes.ITEM_NAME, Text.literal(mapImage.getName()).formatted(Formatting.GREEN, Formatting.BOLD));
            mapItem.set(DataComponentTypes.MAP_COLOR, new MapColorComponent(43690)); // dark aqua
        } else {
            int x = mapIndex % mapImage.getWidth();
            int y = mapIndex / mapImage.getWidth();
            mapItem.set(DataComponentTypes.ITEM_NAME,
                    Text.literal(mapImage.getName()).formatted(Formatting.GREEN, Formatting.BOLD)
                            .append(Text.literal(String.format(" (%d, %d)", x, y).formatted(Formatting.GRAY)))
            );
            mapItem.set(DataComponentTypes.MAP_COLOR, new MapColorComponent(43520)); // dark green
        }

        return mapItem;
    }
}

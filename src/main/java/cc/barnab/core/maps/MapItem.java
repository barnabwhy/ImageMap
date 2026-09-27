package cc.barnab.core.maps;

import cc.barnab.ImageMap;
import com.mojang.serialization.DataResult;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.saveddata.maps.MapId;

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

        MapId mapId = new MapId(mapImage.getMapIds().get(mapIndex));

        ItemStack mapItem = Items.FILLED_MAP.getDefaultInstance();
        mapItem.set(DataComponents.ITEM_NAME, Component.literal(mapImage.getName()).withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));

        Component mapTypeText = Component.literal(isSingle ? "Single map" : mapImage.getWidth() + "x" + mapImage.getHeight() + " map")
                .setStyle(Style.EMPTY.withItalic(false).withColor(ChatFormatting.WHITE));

        Component mapIdText = Component.literal("ID: " + mapImage.getId())
                .setStyle(Style.EMPTY.withItalic(false).withColor(ChatFormatting.GRAY));

        mapItem.set(DataComponents.LORE, ItemLore.EMPTY.withLineAdded(mapTypeText).withLineAdded(mapIdText));
        mapItem.set(DataComponents.MAP_ID, mapId);
        mapItem.set(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT.withHidden(DataComponents.MAP_ID, true));

        // Custom data
        CompoundTag nbt = new CompoundTag();
        nbt.put("image_map_id", StringTag.valueOf(mapImage.getId()));
        nbt.put("image_map_owner", StringTag.valueOf(ownerUUID.toString()));

        CustomData nbtComponent = CustomData.of(nbt);
        mapItem.set(DataComponents.CUSTOM_DATA, nbtComponent);

        return mapItem;
    }


    public static ItemStack fromMapImageForFrame(MapImage mapImage, int mapIndex, BlockPos origin, int rotation) {
        boolean isSingle = mapImage.getType() == MapImageType.SINGLE;

        MapId mapId = new MapId(mapImage.getMapIds().get(mapIndex));

        ItemStack mapItem = Items.FILLED_MAP.getDefaultInstance();
        if (isSingle) {
            mapItem.set(DataComponents.ITEM_NAME, Component.literal(mapImage.getName()).withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
        } else {
            int x = mapIndex % mapImage.getWidth();
            int y = mapIndex / mapImage.getWidth();
            mapItem.set(DataComponents.ITEM_NAME,
                    Component.literal(mapImage.getName()).withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD)
                        .append(Component.literal(String.format(" (%d, %d)", x, y).formatted(ChatFormatting.GRAY)))
            );
        }

        Component mapTypeText = Component.literal(isSingle ? "Single map" : mapImage.getWidth() + "x" + mapImage.getHeight() + " map")
                .setStyle(Style.EMPTY.withItalic(false).withColor(ChatFormatting.WHITE));

        Component mapIdText = Component.literal("ID: " + mapImage.getId())
                .setStyle(Style.EMPTY.withItalic(false).withColor(ChatFormatting.GRAY));

        mapItem.set(DataComponents.LORE, ItemLore.EMPTY.withLineAdded(mapTypeText).withLineAdded(mapIdText));
        mapItem.set(DataComponents.MAP_ID, mapId);
        mapItem.set(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT.withHidden(DataComponents.MAP_ID, true));

        // Custom data
        CompoundTag nbt = new CompoundTag();
        nbt.put("image_map_id", StringTag.valueOf(mapImage.getId()));
        nbt.put("image_map_origin_x", IntTag.valueOf(origin.getX()));
        nbt.put("image_map_origin_y", IntTag.valueOf(origin.getY()));
        nbt.put("image_map_origin_z", IntTag.valueOf(origin.getZ()));
        nbt.put("image_map_rotation", IntTag.valueOf(rotation));
        nbt.put("image_map_width", IntTag.valueOf(mapImage.getWidth()));
        nbt.put("image_map_height", IntTag.valueOf(mapImage.getHeight()));

        CustomData nbtComponent = CustomData.of(nbt);
        mapItem.set(DataComponents.CUSTOM_DATA, nbtComponent);

        return mapItem;
    }

    public static ItemStack fromMapImageIndividual(MapImage mapImage, int mapIndex) {
        boolean isSingle = mapImage.getType() == MapImageType.SINGLE;

        if (mapImage.getMapIds() == null) {
            ImageMap.LOGGER.warn("Map " + mapImage.getId() + " had no map IDs");
            return ItemStack.EMPTY;
        }

        MapId mapId = new MapId(mapImage.getMapIds().get(mapIndex));

        ItemStack mapItem = Items.FILLED_MAP.getDefaultInstance();

        Component mapTypeText = Component.literal(isSingle ? "Single map" : mapImage.getWidth() + "x" + mapImage.getHeight() + " map")
                .setStyle(Style.EMPTY.withItalic(false).withColor(ChatFormatting.WHITE));

        Component mapIdText = Component.literal("ID: " + mapImage.getId())
                .setStyle(Style.EMPTY.withItalic(false).withColor(ChatFormatting.GRAY));

        mapItem.set(DataComponents.LORE, ItemLore.EMPTY.withLineAdded(mapTypeText).withLineAdded(mapIdText));
        mapItem.set(DataComponents.MAP_ID, mapId);
        mapItem.set(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT.withHidden(DataComponents.MAP_ID, true));

        if (isSingle) {
            mapItem.set(DataComponents.ITEM_NAME, Component.literal(mapImage.getName()).withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
        } else {
            int x = mapIndex % mapImage.getWidth();
            int y = mapIndex / mapImage.getWidth();
            mapItem.set(DataComponents.ITEM_NAME,
                    Component.literal(mapImage.getName()).withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD)
                            .append(Component.literal(String.format(" (%d, %d)", x, y).formatted(ChatFormatting.GRAY)))
            );
        }

        return mapItem;
    }
}

package cc.barnab.core.commands;

import cc.barnab.ImageMap;
import cc.barnab.core.gui.ChestGUIClickType;
import cc.barnab.core.gui.ChestGUIScreenHandlerFactory;
import cc.barnab.core.maps.MapImage;
import cc.barnab.core.maps.MapImageType;
import cc.barnab.core.maps.MapItem;
import cc.barnab.core.maps.MapLoader;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.ClickType;
import net.minecraft.util.Formatting;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class MapsCommand {
    public static int executeCommand(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayerOrThrow();

        UUID targetPlayerUUID = player.getUuid();
        String targetPlayerName = player.getName().getString();
        try {
            String playerName = context.getArgument("player", String.class);
            ServerPlayerEntity targetPlayer = source.getServer().getPlayerManager().getPlayer(playerName);
            if (targetPlayer != null) {
                targetPlayerUUID = targetPlayer.getUuid();
                targetPlayerName = targetPlayer.getName().getString();
            } else {
                // Try user cache
                Optional<GameProfile> profileOptional = Objects.requireNonNull(source.getServer().getUserCache()).findByName(playerName);
                if (profileOptional.isPresent()) {
                    GameProfile profile = profileOptional.get();
                    targetPlayerUUID = profile.getId();
                    targetPlayerName = profile.getName();
                }
            }
        } catch(Exception ignored) {}

        List<MapImage> mapList = MapLoader.getPlayerMaps(targetPlayerUUID).mapList;

        try {
            String titleOwningText = (targetPlayerUUID == player.getUuid()) ? "Your" : targetPlayerName + "'s";
            ChestGUIScreenHandlerFactory factory = new ChestGUIScreenHandlerFactory(titleOwningText + " maps (" + mapList.size() + ")");

            openMapsPage(factory, targetPlayerUUID, mapList, 0);

            player.openHandledScreen(factory);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return 1;
    }

    public static void openMapsPage(ChestGUIScreenHandlerFactory factory, UUID targetUUID, List<MapImage> mapList, int page) {
        // Clear in case this is not the initial open
        factory.clearClickCallbacks();
        factory.clear();

        int totalMaps = 0;
        for (MapImage map : mapList) {
            totalMaps += map.getWidth() * map.getHeight();
        }

        for (int i = 0; i < 45; i++) {
            int idx = page * 45 + i;
            if (idx >= mapList.size())
                break;

            MapImage map = mapList.get(idx);
            ItemStack mapItem = MapItem.fromMapImage(map, targetUUID);

            factory.forceSetStack(i, mapItem);

            factory.setClickCallback(i, (clickType) -> {
                if (clickType == ChestGUIClickType.SHIFT_CLICK) {
                    openMapDetailsPage(factory, targetUUID, mapList, idx, page, 0);
                    return false;
                }
                return true;
            });
        }

        // Lock bottom row
        factory.setSlotLocked(45, true);
        factory.setSlotLocked(46, true);
        factory.setSlotLocked(47, true);
        factory.setSlotLocked(48, true);
        factory.setSlotLocked(49, true);
        factory.setSlotLocked(50, true);
        factory.setSlotLocked(51, true);
        factory.setSlotLocked(52, true);
        factory.setSlotLocked(53, true);

        // Add stats book
        Text imagesRendered = Text.literal(String.format("%,d", mapList.size())).setStyle(Style.EMPTY.withItalic(false).withColor(Formatting.WHITE))
                .append(Text.literal(" images rendered").formatted(Formatting.GRAY));
        Text mapsUsed = Text.literal(String.format("%,d", totalMaps)).setStyle(Style.EMPTY.withItalic(false).withColor(Formatting.WHITE))
                .append(Text.literal(" Minecraft maps used").formatted(Formatting.GRAY));

        ItemStack statsBook = Items.ENCHANTED_BOOK.getDefaultStack();
        statsBook.set(DataComponentTypes.ITEM_NAME, Text.literal("Usage statistics").formatted(Formatting.BLUE));
        statsBook.set(DataComponentTypes.LORE, LoreComponent.DEFAULT.with(imagesRendered).with(mapsUsed));
        factory.forceSetStack(49, statsBook);

        int pageCount = mapList.size() / 45;

        // Add page button
        if (page > 0) {
            Text goToPageText = Text.literal("Go to page ").setStyle(Style.EMPTY.withItalic(false).withColor(Formatting.GRAY))
                    .append(Text.literal(String.valueOf(page)).formatted(Formatting.WHITE))
                    .append(" of ")
                    .append(Text.literal(String.valueOf(pageCount + 1)).formatted(Formatting.WHITE));

            ItemStack prevPageArrow = Items.ARROW.getDefaultStack();
            prevPageArrow.set(DataComponentTypes.ITEM_NAME, Text.literal("Previous page"));
            prevPageArrow.set(DataComponentTypes.LORE, LoreComponent.DEFAULT.with(goToPageText));
            factory.forceSetStack(45, prevPageArrow);

            factory.setClickCallback(45, (clickType) -> {
                openMapsPage(factory, targetUUID, mapList, page - 1);
                return false;
            });
        }

        if (page < pageCount) {
            Text goToPageText = Text.literal("Go to page ").setStyle(Style.EMPTY.withItalic(false).withColor(Formatting.GRAY))
                    .append(Text.literal(String.valueOf(page + 1)).formatted(Formatting.WHITE))
                    .append(" of ")
                    .append(Text.literal(String.valueOf(pageCount + 1)).formatted(Formatting.WHITE));

            ItemStack nextPageArrow = Items.ARROW.getDefaultStack();
            nextPageArrow.set(DataComponentTypes.ITEM_NAME, Text.literal("Next page"));
            nextPageArrow.set(DataComponentTypes.LORE, LoreComponent.DEFAULT.with(goToPageText));
            factory.forceSetStack(53, nextPageArrow);

            factory.setClickCallback(53, (clickType) -> {
                openMapsPage(factory, targetUUID, mapList, page + 1);
                return false;
            });
        }
    }

    public static void openMapDetailsPage(ChestGUIScreenHandlerFactory factory, UUID targetUUID, List<MapImage> mapList, int mapIndex, int returnPage, int page) {
        factory.clearClickCallbacks();
        factory.clear();

        MapImage map = mapList.get(mapIndex);
        List<Integer> mapIds = map.getMapIds();
        for (int i = 0; i < 45; i++) {
            int idx = page * 45 + i;
            if (idx >= mapIds.size())
                break;

            ItemStack mapItem = MapItem.fromMapImageIndividual(map, idx);
            factory.forceSetStack(i, mapItem);
        }

        // Add return button
        ItemStack returnBarrier = Items.BARRIER.getDefaultStack();
        returnBarrier.set(DataComponentTypes.ITEM_NAME, Text.literal("Return to map list").formatted(Formatting.RED));
        factory.forceSetStack(49, returnBarrier);

        factory.setClickCallback(49, (clickType) -> {
            openMapsPage(factory, targetUUID, mapList, returnPage);
            return false;
        });


        // Add page button
        int pageCount = mapIds.size() / 45;

        if (page > 0) {
            Text goToPageText = Text.literal("Go to page ").setStyle(Style.EMPTY.withItalic(false).withColor(Formatting.GRAY))
                    .append(Text.literal(String.valueOf(page)).formatted(Formatting.WHITE))
                    .append(" of ")
                    .append(Text.literal(String.valueOf(pageCount + 1)).formatted(Formatting.WHITE));

            ItemStack prevPageArrow = Items.ARROW.getDefaultStack();
            prevPageArrow.set(DataComponentTypes.ITEM_NAME, Text.literal("Previous page"));
            prevPageArrow.set(DataComponentTypes.LORE, LoreComponent.DEFAULT.with(goToPageText));
            factory.forceSetStack(45, prevPageArrow);

            factory.setClickCallback(45, (clickType) -> {
                openMapDetailsPage(factory, targetUUID, mapList, mapIndex, returnPage, page - 1);
                return false;
            });
        }
        if (page < pageCount) {
            Text goToPageText = Text.literal("Go to page ").setStyle(Style.EMPTY.withItalic(false).withColor(Formatting.GRAY))
                    .append(Text.literal(String.valueOf(page + 1)).formatted(Formatting.WHITE))
                    .append(" of ")
                    .append(Text.literal(String.valueOf(pageCount + 1)).formatted(Formatting.WHITE));

            ItemStack nextPageArrow = Items.ARROW.getDefaultStack();
            nextPageArrow.set(DataComponentTypes.ITEM_NAME, Text.literal("Next page"));
            nextPageArrow.set(DataComponentTypes.LORE, LoreComponent.DEFAULT.with(goToPageText));
            factory.forceSetStack(53, nextPageArrow);

            factory.setClickCallback(53, (clickType) -> {
                openMapDetailsPage(factory, targetUUID, mapList, mapIndex, returnPage, page + 1);
                return false;
            });
        }
    }
}

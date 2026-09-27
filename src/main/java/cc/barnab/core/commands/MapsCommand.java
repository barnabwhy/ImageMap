package cc.barnab.core.commands;

import cc.barnab.ImageMap;
import cc.barnab.core.gui.ChestGUIClickType;
import cc.barnab.core.gui.ChestGUIScreenHandlerFactory;
import cc.barnab.core.maps.MapImage;
import cc.barnab.core.maps.MapImageType;
import cc.barnab.core.maps.MapItem;
import cc.barnab.core.maps.MapLoader;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.services.response.NameAndId;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class MapsCommand {
    public static int executeCommand(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();

        UUID targetPlayerUUID = player.getUUID();
        String targetPlayerName = player.getName().getString();
        try {
            String playerName = context.getArgument("player", String.class);
            ServerPlayer targetPlayer = source.getServer().getPlayerList().getPlayer(playerName);
            if (targetPlayer != null) {
                targetPlayerUUID = targetPlayer.getUUID();
                targetPlayerName = targetPlayer.getName().getString();
            } else {
                // Try user cache
                Optional<NameAndId> profileOptional = Objects.requireNonNull(source.getServer().services().profileRepository()).findProfileByName(playerName);
                if (profileOptional.isPresent()) {
                    NameAndId profile = profileOptional.get();
                    targetPlayerUUID = profile.id();
                    targetPlayerName = profile.name();
                }
            }
        } catch(Exception ignored) {}

        List<MapImage> mapList = MapLoader.getPlayerMaps(targetPlayerUUID).mapList;

        try {
            String titleOwningText = (targetPlayerUUID == player.getUUID()) ? "Your" : targetPlayerName + "'s";
            ChestGUIScreenHandlerFactory factory = new ChestGUIScreenHandlerFactory(titleOwningText + " maps (" + mapList.size() + ")");

            openMapsPage(factory, targetPlayerUUID, mapList, 0);

            player.openMenu(factory);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return 1;
    }

    public static void openMapsPage(ChestGUIScreenHandlerFactory factory, UUID targetUUID, List<MapImage> mapList, int page) {
        // Clear in case this is not the initial open
        factory.clearClickCallbacks();
        factory.clearContent();

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
        Component imagesRendered = Component.literal(String.format("%,d", mapList.size())).setStyle(Style.EMPTY.withItalic(false).withColor(ChatFormatting.WHITE))
                .append(Component.literal(" images rendered").withStyle(ChatFormatting.GRAY));
        Component mapsUsed = Component.literal(String.format("%,d", totalMaps)).setStyle(Style.EMPTY.withItalic(false).withColor(ChatFormatting.WHITE))
                .append(Component.literal(" Minecraft maps used").withStyle(ChatFormatting.GRAY));

        ItemStack statsBook = Items.ENCHANTED_BOOK.getDefaultInstance();
        statsBook.set(DataComponents.ITEM_NAME, Component.literal("Usage statistics").withStyle(ChatFormatting.BLUE));
        statsBook.set(DataComponents.LORE, ItemLore.EMPTY.withLineAdded(imagesRendered).withLineAdded(mapsUsed));
        factory.forceSetStack(49, statsBook);

        int pageCount = mapList.size() / 45;

        // Add page button
        if (page > 0) {
            Component goToPageText = Component.literal("Go to page ").setStyle(Style.EMPTY.withItalic(false).withColor(ChatFormatting.GRAY))
                    .append(Component.literal(String.valueOf(page)).withStyle(ChatFormatting.WHITE))
                    .append(" of ")
                    .append(Component.literal(String.valueOf(pageCount + 1)).withStyle(ChatFormatting.WHITE));

            ItemStack prevPageArrow = Items.ARROW.getDefaultInstance();
            prevPageArrow.set(DataComponents.ITEM_NAME, Component.literal("Previous page"));
            prevPageArrow.set(DataComponents.LORE, ItemLore.EMPTY.withLineAdded(goToPageText));
            factory.forceSetStack(45, prevPageArrow);

            factory.setClickCallback(45, (clickType) -> {
                openMapsPage(factory, targetUUID, mapList, page - 1);
                return false;
            });
        }

        if (page < pageCount) {
            Component goToPageText = Component.literal("Go to page ").setStyle(Style.EMPTY.withItalic(false).withColor(ChatFormatting.GRAY))
                    .append(Component.literal(String.valueOf(page + 1)).withStyle(ChatFormatting.WHITE))
                    .append(" of ")
                    .append(Component.literal(String.valueOf(pageCount + 1)).withStyle(ChatFormatting.WHITE));

            ItemStack nextPageArrow = Items.ARROW.getDefaultInstance();
            nextPageArrow.set(DataComponents.ITEM_NAME, Component.literal("Next page"));
            nextPageArrow.set(DataComponents.LORE, ItemLore.EMPTY.withLineAdded(goToPageText));
            factory.forceSetStack(53, nextPageArrow);

            factory.setClickCallback(53, (clickType) -> {
                openMapsPage(factory, targetUUID, mapList, page + 1);
                return false;
            });
        }
    }

    public static void openMapDetailsPage(ChestGUIScreenHandlerFactory factory, UUID targetUUID, List<MapImage> mapList, int mapIndex, int returnPage, int page) {
        factory.clearClickCallbacks();
        factory.clearContent();

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
        ItemStack returnBarrier = Items.BARRIER.getDefaultInstance();
        returnBarrier.set(DataComponents.ITEM_NAME, Component.literal("Return to map list").withStyle(ChatFormatting.RED));
        factory.forceSetStack(49, returnBarrier);

        factory.setClickCallback(49, (clickType) -> {
            openMapsPage(factory, targetUUID, mapList, returnPage);
            return false;
        });


        // Add page button
        int pageCount = mapIds.size() / 45;

        if (page > 0) {
            Component goToPageText = Component.literal("Go to page ").setStyle(Style.EMPTY.withItalic(false).withColor(ChatFormatting.GRAY))
                    .append(Component.literal(String.valueOf(page)).withStyle(ChatFormatting.WHITE))
                    .append(" of ")
                    .append(Component.literal(String.valueOf(pageCount + 1)).withStyle(ChatFormatting.WHITE));

            ItemStack prevPageArrow = Items.ARROW.getDefaultInstance();
            prevPageArrow.set(DataComponents.ITEM_NAME, Component.literal("Previous page"));
            prevPageArrow.set(DataComponents.LORE, ItemLore.EMPTY.withLineAdded(goToPageText));
            factory.forceSetStack(45, prevPageArrow);

            factory.setClickCallback(45, (clickType) -> {
                openMapDetailsPage(factory, targetUUID, mapList, mapIndex, returnPage, page - 1);
                return false;
            });
        }
        if (page < pageCount) {
            Component goToPageText = Component.literal("Go to page ").setStyle(Style.EMPTY.withItalic(false).withColor(ChatFormatting.GRAY))
                    .append(Component.literal(String.valueOf(page + 1)).withStyle(ChatFormatting.WHITE))
                    .append(" of ")
                    .append(Component.literal(String.valueOf(pageCount + 1)).withStyle(ChatFormatting.WHITE));

            ItemStack nextPageArrow = Items.ARROW.getDefaultInstance();
            nextPageArrow.set(DataComponents.ITEM_NAME, Component.literal("Next page"));
            nextPageArrow.set(DataComponents.LORE, ItemLore.EMPTY.withLineAdded(goToPageText));
            factory.forceSetStack(53, nextPageArrow);

            factory.setClickCallback(53, (clickType) -> {
                openMapDetailsPage(factory, targetUUID, mapList, mapIndex, returnPage, page + 1);
                return false;
            });
        }
    }
}

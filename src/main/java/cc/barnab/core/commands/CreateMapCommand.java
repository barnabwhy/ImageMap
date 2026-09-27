package cc.barnab.core.commands;

import cc.barnab.ImageMap;
import cc.barnab.core.maps.*;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public class CreateMapCommand {
    public static int executeCommand(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();

        ServerPlayer player = source.getPlayer();

        if (player == null) {
            source.sendFailure(Component.literal("Command can only be run by players").withStyle(ChatFormatting.RED));
            return 1;
        }

        int width = context.getArgument("width", Integer.class);
        int height = context.getArgument("height", Integer.class);
        String mode = context.getArgument("mode", String.class);
        String url = context.getArgument("url", String.class);

        if (width > ImageMap.CONFIG.maxWidth) {
            source.sendFailure(Component.literal(String.format("Map image would be too wide (%d > %d)", width, ImageMap.CONFIG.maxWidth)).withStyle(ChatFormatting.RED));
            return 1;
        }

        if (height > ImageMap.CONFIG.maxHeight) {
            source.sendFailure(Component.literal(String.format("Map image would be too tall (%d > %d)", height, ImageMap.CONFIG.maxHeight)).withStyle(ChatFormatting.RED));
            return 1;
        }

        source.sendSystemMessage(Component.literal(String.format("Creating %dx%d map image...", width, height)).withStyle(ChatFormatting.AQUA));

        MapFillMode fillMode = switch (mode) {
            case "stretch" -> MapFillMode.STRETCH;
            case "contain" -> MapFillMode.CONTAIN;
            default -> MapFillMode.COVER;
        };

        long startTime = System.currentTimeMillis();

        MapRenderer.downloadImage(url)
            .thenAccept(image -> {
                if (image == null) {
                    source.sendFailure(Component.literal("Failed to download image. It was likely in an unsupported format.").withStyle(ChatFormatting.RED));
                    return;
                }

                MapRenderer.renderMapImage(source.getPlayer(), source.getLevel(), image, width, height, fillMode)
                    .thenAccept(mapImage -> {
                        MapLoader.addPlayerMap(player.getUUID(), mapImage);

                        ItemStack mapItem = MapItem.fromMapImage(mapImage, player.getUUID());
                        player.addItem(mapItem);

                        long timeElapsed = System.currentTimeMillis() - startTime;
                        source.sendSuccess(() -> Component.literal(String.format("Created map image in %.2fs", (double)timeElapsed / 1000.0)).withStyle(ChatFormatting.GREEN), false);
                    });
            });

        return 1;
    }
}

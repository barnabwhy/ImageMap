package cc.barnab.core.commands;

import cc.barnab.core.maps.*;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class CreateMapCommand {
    public static int executeCommand(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();

        ServerPlayerEntity player = source.getPlayer();

        if (player == null) {
            source.sendFeedback(() -> Text.literal("Command can only be run by players").formatted(Formatting.RED), false);
            return 1;
        }

        int width = context.getArgument("width", Integer.class);
        int height = context.getArgument("height", Integer.class);
        String mode = context.getArgument("mode", String.class);
        String url = context.getArgument("url", String.class);

        source.sendFeedback(() -> Text.literal(String.format("Creating %dx%d map image...", width, height)).formatted(Formatting.AQUA), false);

        MapFillMode fillMode = switch (mode) {
            case "stretch" -> MapFillMode.STRETCH;
            case "contain" -> MapFillMode.CONTAIN;
            default -> MapFillMode.COVER;
        };

        long startTime = System.currentTimeMillis();

        MapRenderer.downloadImage(url)
            .thenAccept(image -> {
                if (image == null) {
                    source.sendFeedback(() -> Text.literal("Failed to download image. It was likely in an unsupported format.").formatted(Formatting.RED), false);
                    return;
                }

                MapRenderer.renderMapImage(source.getPlayer(), source.getWorld(), image, width, height, fillMode)
                    .thenAccept(mapImage -> {
                        MapLoader.addPlayerMap(player.getUuid(), mapImage);

                        ItemStack mapItem = MapItem.fromMapImage(mapImage, player.getUuid());
                        player.giveItemStack(mapItem);

                        long timeElapsed = System.currentTimeMillis() - startTime;
                        source.sendFeedback(() -> Text.literal(String.format("Created map image in %.2fs", (double)timeElapsed / 1000.0)).formatted(Formatting.GREEN), false);
                    });
            });

        return 1;
    }
}

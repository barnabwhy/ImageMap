package cc.barnab.core.commands;

import cc.barnab.ImageMap;
import cc.barnab.core.ImageMapConfig;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class ReloadCommand {
    public static int executeCommand(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        source.sendFeedback(() -> Text.literal("Reloading config...").formatted(Formatting.GOLD), false);
        ImageMap.CONFIG = ImageMapConfig.loadOrCreateConfig();
        source.sendFeedback(() -> Text.literal("Complete.").formatted(Formatting.GREEN), false);

        return 1;
    }
}

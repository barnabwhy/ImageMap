package cc.barnab.core.commands;

import cc.barnab.ImageMap;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class HelpCommand {
    public static int executeCommand(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();

        Text helpText = Text.literal("ImageMap v"+ ImageMap.VERSION).formatted(Formatting.YELLOW)
                .append("\n")
                .append(Text.literal("/tomap <width> <height> <mode> <url>").formatted(Formatting.GOLD))
                .append(" ")
                .append(Text.literal("Turn an image into a map").formatted(Formatting.YELLOW))
                .append("\n")
                .append(Text.literal("/maps [player]").formatted(Formatting.GOLD))
                .append(" ")
                .append(Text.literal("Open the map selection GUI").formatted(Formatting.YELLOW))
                .append("\n")
                .append(Text.literal("/imagemap help").formatted(Formatting.GOLD))
                .append(" ")
                .append(Text.literal("View this help text").formatted(Formatting.YELLOW))
                .append("\n")
                .append(Text.literal("/imagemap reload").formatted(Formatting.GOLD))
                .append(" ")
                .append(Text.literal("Reload plugin config").formatted(Formatting.YELLOW));

        source.sendFeedback(() -> helpText, false);

        return 1;
    }
}

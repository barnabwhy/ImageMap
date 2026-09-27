package cc.barnab.core.commands;

import cc.barnab.ImageMap;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

public class HelpCommand {
    public static int executeCommand(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();

        Component helpText = Component.literal("ImageMap v"+ ImageMap.VERSION).withStyle(ChatFormatting.YELLOW)
                .append("\n")
                .append(Component.literal("/tomap <width> <height> <mode> <url>").withStyle(ChatFormatting.GOLD))
                .append(" ")
                .append(Component.literal("Turn an image into a map").withStyle(ChatFormatting.YELLOW))
                .append("\n")
                .append(Component.literal("/maps [player]").withStyle(ChatFormatting.GOLD))
                .append(" ")
                .append(Component.literal("Open the map selection GUI").withStyle(ChatFormatting.YELLOW))
                .append("\n")
                .append(Component.literal("/imagemap help").withStyle(ChatFormatting.GOLD))
                .append(" ")
                .append(Component.literal("View this help text").withStyle(ChatFormatting.YELLOW))
                .append("\n")
                .append(Component.literal("/imagemap reload").withStyle(ChatFormatting.GOLD))
                .append(" ")
                .append(Component.literal("Reload plugin config").withStyle(ChatFormatting.YELLOW));

        source.sendSuccess(() -> helpText, false);

        return 1;
    }
}

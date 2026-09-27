package cc.barnab.core.commands;

import cc.barnab.ImageMap;
import cc.barnab.core.ImageMapConfig;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

public class ReloadCommand {
    public static int executeCommand(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        source.sendSystemMessage(Component.literal("Reloading config...").withStyle(ChatFormatting.GOLD));
        ImageMap.CONFIG = ImageMapConfig.loadOrCreateConfig();
        source.sendSuccess(() -> Component.literal("Complete.").withStyle(ChatFormatting.GREEN), false);

        return 1;
    }
}

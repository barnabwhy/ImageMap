package cc.barnab.core;

import cc.barnab.ImageMap;
import cc.barnab.core.commands.*;
import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.Entity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class Commands {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        // Help command
        dispatcher.register(literal("imagemap")
                .requires(source -> source.hasPermissionLevel(ImageMap.CONFIG.minPermLevel))
                .executes(HelpCommand::executeCommand)
                .then(literal("help")
                        .executes(HelpCommand::executeCommand)
                )
        );
        // Reload command
        dispatcher.register(literal("imagemap")
                .requires(source -> source.hasPermissionLevel(4))
                .then(literal("reload")
                        .executes(ReloadCommand::executeCommand)
                )
        );
        // Migrate command
        dispatcher.register(literal("imagemap")
            .requires(source -> source.hasPermissionLevel(4))
            .then(literal("migrate")
                .then(argument("source", StringArgumentType.word()).suggests(new MigrateSourceSuggestionProvider())
                    .executes(MigrateCommand::executeCommand)
                    .then(argument("code", StringArgumentType.word())
                            .executes(MigrateCommand::executeCommand)
                    )
                )
            )
        );
        // Create map command
        dispatcher.register(literal("imagemap")
                .requires(source -> source.hasPermissionLevel(ImageMap.CONFIG.minPermLevel))
                .then(literal("new")
                    .then(argument("width", IntegerArgumentType.integer(1))
                            .then(argument("height", IntegerArgumentType.integer(1))
                                    .then(argument("mode", StringArgumentType.word()).suggests(new ModeSuggestionProvider())
                                            .then(argument("url", StringArgumentType.greedyString())
                                                    .executes(CreateMapCommand::executeCommand)
                                            )
                                    )
                            )
                    )
                )
        );
        dispatcher.register(literal("tomap")
                .requires(source -> source.hasPermissionLevel(ImageMap.CONFIG.minPermLevel))
                .then(argument("width", IntegerArgumentType.integer(1))
                    .then(argument("height", IntegerArgumentType.integer(1))
                        .then(argument("mode", StringArgumentType.word()).suggests(new ModeSuggestionProvider())
                            .then(argument("url", StringArgumentType.greedyString())
                                    .executes(CreateMapCommand::executeCommand)
                        )
                    )
                )
            )
        );
        // Map list command
        dispatcher.register(literal("maps")
                .requires(source -> source.hasPermissionLevel(ImageMap.CONFIG.minPermLevel))
                .executes(MapsCommand::executeCommand)
                .then(argument("player", StringArgumentType.word()).suggests(new PlayerSuggestionProvider())
                    .executes(MapsCommand::executeCommand)
                )
        );
    }

    static class ModeSuggestionProvider implements SuggestionProvider<ServerCommandSource> {
        @Override
        public CompletableFuture<Suggestions> getSuggestions(CommandContext<ServerCommandSource> context,
                                                             SuggestionsBuilder builder) throws CommandSyntaxException {
            builder.suggest("stretch");
            builder.suggest("contain");
            builder.suggest("cover");
            return builder.buildFuture();
        }
    }

    static class PlayerSuggestionProvider implements SuggestionProvider<ServerCommandSource> {
        @Override
        public CompletableFuture<Suggestions> getSuggestions(CommandContext<ServerCommandSource> context,
                                                             SuggestionsBuilder builder) throws CommandSyntaxException {
            if (context.getSource() instanceof ServerCommandSource source) {
                return CommandSource.suggestMatching(Lists.transform(Lists.transform(source.getServer().getPlayerManager().getPlayerList(), Entity::getName), Text::getString), builder);
            }
            return builder.buildFuture();
        }
    }

    static class MigrateSourceSuggestionProvider implements SuggestionProvider<ServerCommandSource> {
        @Override
        public CompletableFuture<Suggestions> getSuggestions(CommandContext<ServerCommandSource> context,
                                                             SuggestionsBuilder builder) throws CommandSyntaxException {
            builder.suggest("ImageOnMap");
            return builder.buildFuture();
        }
    }
}

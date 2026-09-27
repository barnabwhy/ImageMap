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
import net.minecraft.commands.*;
import net.minecraft.server.permissions.PermissionCheck;
import net.minecraft.server.permissions.PermissionProviderCheck;
import net.minecraft.world.entity.Entity;
import 	net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class CustomCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess, Commands.CommandSelection environment) {
        PermissionProviderCheck<@NotNull CommandSourceStack> permissionCheck = getServerCommandSourcePermissionSourcePredicate();

        // Help command
        dispatcher.register(literal("imagemap")
                .requires(permissionCheck)
                .executes(HelpCommand::executeCommand)
                .then(literal("help")
                        .executes(HelpCommand::executeCommand)
                )
        );
        // Reload command
        dispatcher.register(literal("imagemap")
                .requires(Commands.hasPermission(Commands.LEVEL_OWNERS))
                .then(literal("reload")
                        .executes(ReloadCommand::executeCommand)
                )
        );
        // Migrate command - No longer supported
//        dispatcher.register(literal("imagemap")
//            .requires(source -> source.hasPermissionLevel(4))
//            .then(literal("migrate")
//                .then(argument("source", StringArgumentType.word()).suggests(new MigrateSourceSuggestionProvider())
//                    .executes(MigrateCommand::executeCommand)
//                    .then(argument("code", StringArgumentType.word())
//                            .executes(MigrateCommand::executeCommand)
//                    )
//                )
//            )
//        );
        // Create map command
        dispatcher.register(literal("imagemap")
                .requires(permissionCheck)
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
                .requires(permissionCheck)
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
                .requires(permissionCheck)
                .executes(MapsCommand::executeCommand)
                .then(argument("player", StringArgumentType.word()).suggests(new PlayerSuggestionProvider())
                    .executes(MapsCommand::executeCommand)
                )
        );
    }

    static class ModeSuggestionProvider implements SuggestionProvider<CommandSourceStack> {
        @Override
        public CompletableFuture<Suggestions> getSuggestions(CommandContext<CommandSourceStack> context,
                                                             SuggestionsBuilder builder) throws CommandSyntaxException {
            builder.suggest("stretch");
            builder.suggest("contain");
            builder.suggest("cover");
            return builder.buildFuture();
        }
    }

    static class PlayerSuggestionProvider implements SuggestionProvider<CommandSourceStack> {
        @Override
        public CompletableFuture<Suggestions> getSuggestions(CommandContext<CommandSourceStack> context,
                                                             SuggestionsBuilder builder) throws CommandSyntaxException {
            if (context.getSource() instanceof CommandSourceStack source) {
                return SharedSuggestionProvider.suggest(Lists.transform(Lists.transform(source.getServer().getPlayerList().getPlayers(), Entity::getName), Component::getString), builder);
            }
            return builder.buildFuture();
        }
    }

    static class MigrateSourceSuggestionProvider implements SuggestionProvider<CommandSourceStack> {
        @Override
        public CompletableFuture<Suggestions> getSuggestions(CommandContext<CommandSourceStack> context,
                                                             SuggestionsBuilder builder) throws CommandSyntaxException {
            //builder.suggest("ImageOnMap");
            return builder.buildFuture();
        }
    }

    private static @NotNull PermissionProviderCheck<@NotNull CommandSourceStack> getServerCommandSourcePermissionSourcePredicate() {
        final PermissionCheck[] permissionLevels = {
                Commands.LEVEL_ALL,
                Commands.LEVEL_MODERATORS,
                Commands.LEVEL_GAMEMASTERS,
                Commands.LEVEL_ADMINS,
                Commands.LEVEL_OWNERS,
        };

        return Commands.hasPermission(permissionLevels[Math.clamp(ImageMap.CONFIG.minPermLevel, 0, permissionLevels.length-1)]);
    }
}

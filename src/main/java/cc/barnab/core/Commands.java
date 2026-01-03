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
import net.minecraft.command.permission.PermissionCheck;
import net.minecraft.command.permission.PermissionSourcePredicate;
import net.minecraft.entity.Entity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class Commands {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        PermissionSourcePredicate<ServerCommandSource> permissionCheck = getServerCommandSourcePermissionSourcePredicate();

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
                .requires(CommandManager.requirePermissionLevel(CommandManager.OWNERS_CHECK))
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
            //builder.suggest("ImageOnMap");
            return builder.buildFuture();
        }
    }

    private static @NotNull PermissionSourcePredicate<ServerCommandSource> getServerCommandSourcePermissionSourcePredicate() {
        final PermissionCheck[] permissionLevels = {
                CommandManager.ALWAYS_PASS_CHECK,
                CommandManager.MODERATORS_CHECK,
                CommandManager.GAMEMASTERS_CHECK,
                CommandManager.ADMINS_CHECK,
                CommandManager.OWNERS_CHECK,
        };

        return CommandManager.requirePermissionLevel(permissionLevels[Math.clamp(ImageMap.CONFIG.minPermLevel, 0, permissionLevels.length-1)]);
    }
}

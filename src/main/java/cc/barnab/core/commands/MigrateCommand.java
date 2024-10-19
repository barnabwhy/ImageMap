package cc.barnab.core.commands;

import cc.barnab.core.maps.MapLoader;
import cc.barnab.core.maps.Migrator;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.concurrent.CompletableFuture;

public class MigrateCommand {
    public static int executeCommand(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();

        String migrationSource = context.getArgument("source", String.class);
        String password = null;
        try {
             password = context.getArgument("code", String.class);
        } catch (IllegalArgumentException ignored) { }

        if (password == null) {
            source.sendFeedback(() -> Text.literal(
                    "Migrating is a long process and should not be run multiple times and may break some maps. Are you sure you want to migrate?"
            ).append("\nIf you wish to continue run:\n /imagemap migrate " + migrationSource + " " + Migrator.getPassword())
                    .formatted(Formatting.RED), false);
            return 1;
        }

        String finalPassword = password;
        switch (migrationSource.toLowerCase()) {
            case "imageonmap" -> {
                CompletableFuture.supplyAsync(() -> {
                    long startTime = System.currentTimeMillis();

                    int migrated = Migrator.migrateImageOnMap(source, finalPassword);
                    if (migrated == -1) {
                        source.sendFeedback(() ->
                                Text.literal("A migration is already in progress. Please wait until it is done.")
                                        .formatted(Formatting.YELLOW), false);
                    } else if (migrated == -2) {
                        source.sendFeedback(() ->
                                Text.literal("Incorrect code inputted. Migration will not be started.")
                                        .formatted(Formatting.YELLOW), false);
                    } else {
                        long timeElapsed = System.currentTimeMillis() - startTime;
                        source.sendFeedback(() ->
                                Text.literal("Migration from ImageOnMap complete").formatted(Formatting.AQUA), true);
                        source.sendFeedback(() ->
                                Text.literal(String.format("Migrated %d maps in %.2fs", migrated, ((double)timeElapsed) / 1000.0))
                                        .formatted(Formatting.GREEN), true);
                    }
                    return null;
                });
            }
            default -> {
                source.sendFeedback(() -> Text.literal("Can't start migration: Unknown source.").formatted(Formatting.RED), false);
                return 1;
            }
        }

        return 1;
    }
}

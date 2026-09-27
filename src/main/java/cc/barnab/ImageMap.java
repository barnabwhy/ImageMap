package cc.barnab;

import cc.barnab.core.CustomCommands;
import cc.barnab.core.ImageMapConfig;
import cc.barnab.core.maps.MapLoader;
import cc.barnab.core.maps.PosterMap;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.InteractionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

public class ImageMap implements ModInitializer {
	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
    public static final Logger LOGGER = LoggerFactory.getLogger("image-map");
	public static ImageMapConfig CONFIG = ImageMapConfig.loadOrCreateConfig();
	public static String VERSION = FabricLoader.getInstance().getModContainer("image-map").get().getMetadata().getVersion().getFriendlyString();

	@Override
	public void onInitialize() {
		CommandRegistrationCallback.EVENT.register(CustomCommands::register);

		MapLoader.loadPlayerMaps();

		UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
			if (entity instanceof ItemFrame itemFrameEntity) {
				if (PosterMap.place(player, hand, itemFrameEntity))
					return InteractionResult.SUCCESS;
			}

			return InteractionResult.PASS;
		});

		AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
			if (entity instanceof ItemFrame itemFrameEntity) {
				if (player.isCrouching()) {
					if (PosterMap.destroy(player, itemFrameEntity))
						return InteractionResult.SUCCESS;
				}
			}

			return InteractionResult.PASS;
		});

		ServerLifecycleEvents.AFTER_SAVE.register((server, flush, force) -> {
			CompletableFuture.supplyAsync(() -> {
				MapLoader.savePlayerMaps(true);
				return null;
			});
		});
	}
}
package cc.barnab;

import cc.barnab.core.Commands;
import cc.barnab.core.ImageMapConfig;
import cc.barnab.core.maps.MapLoader;
import cc.barnab.core.maps.PosterMap;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.util.ActionResult;
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
		CommandRegistrationCallback.EVENT.register(Commands::register);

		MapLoader.loadPlayerMaps();

		UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
			if (entity instanceof ItemFrameEntity itemFrameEntity) {
				if (PosterMap.place(player, hand, itemFrameEntity))
					return ActionResult.SUCCESS;
			}

			return ActionResult.PASS;
		});

		AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
			if (entity instanceof ItemFrameEntity itemFrameEntity) {
				if (player.isSneaking()) {
					if (PosterMap.destroy(player, itemFrameEntity))
						return ActionResult.SUCCESS;
				}
			}

			return ActionResult.PASS;
		});

		ServerLifecycleEvents.AFTER_SAVE.register((server, flush, force) -> {
			CompletableFuture.supplyAsync(() -> {
				MapLoader.savePlayerMaps(true);
				return null;
			});
		});
	}
}
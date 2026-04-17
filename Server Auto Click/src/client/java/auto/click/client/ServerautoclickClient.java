package auto.click.client;

import auto.click.client.mixin.MinecraftInvoker;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class ServerautoclickClient implements ClientModInitializer {
	private static final double MIN_INTERVAL_SECONDS = 0.5D;
	private static final double MAX_INTERVAL_SECONDS = 60.0D;

	private static boolean enabled = false;
	private static long intervalMillis = 1000L;
	private static ClickType clickType = ClickType.LEFT;
	private static long nextClickAtMillis = 0L;

	@Override
	public void onInitializeClient() {
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
			ClientCommands.literal("autoclick")
				.then(ClientCommands.literal("off")
					.executes(context -> {
						enabled = false;
						context.getSource().sendFeedback(Component.literal("Auto click disabled."));
						return 1;
					}))
				.then(ClientCommands.argument("interval", DoubleArgumentType.doubleArg(MIN_INTERVAL_SECONDS, MAX_INTERVAL_SECONDS))
					.then(ClientCommands.literal("left")
						.executes(context -> startAutoClick(context.getSource(), DoubleArgumentType.getDouble(context, "interval"), ClickType.LEFT)))
					.then(ClientCommands.literal("right")
						.executes(context -> startAutoClick(context.getSource(), DoubleArgumentType.getDouble(context, "interval"), ClickType.RIGHT))))
		));

		ClientTickEvents.END_CLIENT_TICK.register(ServerautoclickClient::onClientTick);
	}

	private static int startAutoClick(net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource source, double intervalSeconds, ClickType type) {
		intervalMillis = Math.round(intervalSeconds * 1000.0D);
		clickType = type;
		enabled = true;
		nextClickAtMillis = System.currentTimeMillis();
		source.sendFeedback(Component.literal("Auto click enabled: every " + intervalSeconds + "s (" + type.name().toLowerCase() + ")."));
		return 1;
	}

	private static void onClientTick(Minecraft client) {
		if (!enabled) {
			return;
		}

		if (client.player == null || client.level == null || client.screen != null) {
			return;
		}

		long now = System.currentTimeMillis();
		if (now < nextClickAtMillis) {
			return;
		}

		nextClickAtMillis = now + intervalMillis;
		MinecraftInvoker invoker = (MinecraftInvoker) client;
		if (clickType == ClickType.LEFT) {
			invoker.invokeStartAttack();
		} else {
			invoker.invokeStartUseItem();
		}
	}

	private enum ClickType {
		LEFT,
		RIGHT
	}
}
package auto.click.command;

import auto.click.service.AutoClickService;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class AutoClickCommand {
	private static final double MIN_INTERVAL_SECONDS = 0.5D;
	private static final double MAX_INTERVAL_SECONDS = 60.0D;
	private static final double MIN_DURATION_SECONDS = 0.5D;
	private static final double MAX_DURATION_SECONDS = 600.0D;

	private AutoClickCommand() {
	}

	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(
				Commands.literal("autoattack")
					.then(Commands.literal("off")
						.executes(AutoClickCommand::disableAutoAttack))
					.then(Commands.argument("interval", DoubleArgumentType.doubleArg(MIN_INTERVAL_SECONDS, MAX_INTERVAL_SECONDS))
						.then(Commands.literal("infinite")
							.executes(AutoClickCommand::enableInfiniteAutoAttack))
						.then(Commands.argument("duration", DoubleArgumentType.doubleArg(MIN_DURATION_SECONDS, MAX_DURATION_SECONDS))
							.executes(AutoClickCommand::enableAutoAttack)))
			);

			dispatcher.register(
				Commands.literal("autoconsume")
					.then(Commands.literal("off")
						.executes(AutoClickCommand::disableAutoConsume))
					.then(Commands.literal("infinite")
						.executes(AutoClickCommand::enableInfiniteAutoConsume))
					.then(Commands.argument("duration", DoubleArgumentType.doubleArg(MIN_DURATION_SECONDS, MAX_DURATION_SECONDS))
						.executes(AutoClickCommand::enableAutoConsume))
			);
		});
	}

	private static int enableAutoAttack(CommandContext<CommandSourceStack> context) {
		ServerPlayer player = context.getSource().getPlayer();
		if (player == null) {
			context.getSource().sendFailure(Component.literal("This command can only be run by a player."));
			return 0;
		}

		double intervalSeconds = DoubleArgumentType.getDouble(context, "interval");
		double durationSeconds = DoubleArgumentType.getDouble(context, "duration");
		AutoClickService.enableAutoAttack(player, intervalSeconds, durationSeconds);
		context.getSource().sendSuccess(() -> Component.literal(
			"Auto attack enabled: every " + intervalSeconds + "s for " + durationSeconds + "s."
		), false);
		return 1;
	}

	private static int enableInfiniteAutoAttack(CommandContext<CommandSourceStack> context) {
		ServerPlayer player = context.getSource().getPlayer();
		if (player == null) {
			context.getSource().sendFailure(Component.literal("This command can only be run by a player."));
			return 0;
		}

		double intervalSeconds = DoubleArgumentType.getDouble(context, "interval");
		AutoClickService.enableAutoAttackInfinite(player, intervalSeconds);
		context.getSource().sendSuccess(() -> Component.literal(
			"Auto attack enabled: every " + intervalSeconds + "s with infinite duration."
		), false);
		return 1;
	}

	private static int enableAutoConsume(CommandContext<CommandSourceStack> context) {
		ServerPlayer player = context.getSource().getPlayer();
		if (player == null) {
			context.getSource().sendFailure(Component.literal("This command can only be run by a player."));
			return 0;
		}

		double durationSeconds = DoubleArgumentType.getDouble(context, "duration");
		AutoClickService.enableAutoConsume(player, durationSeconds);
		context.getSource().sendSuccess(() -> Component.literal(
			"Auto consume enabled for " + durationSeconds + "s."
		), false);
		return 1;
	}

	private static int enableInfiniteAutoConsume(CommandContext<CommandSourceStack> context) {
		ServerPlayer player = context.getSource().getPlayer();
		if (player == null) {
			context.getSource().sendFailure(Component.literal("This command can only be run by a player."));
			return 0;
		}

		AutoClickService.enableAutoConsumeInfinite(player);
		context.getSource().sendSuccess(() -> Component.literal("Auto consume enabled with infinite duration."), false);
		return 1;
	}

	private static int disableAutoAttack(CommandContext<CommandSourceStack> context) {
		ServerPlayer player = context.getSource().getPlayer();
		if (player == null) {
			context.getSource().sendFailure(Component.literal("This command can only be run by a player."));
			return 0;
		}

		AutoClickService.disableAutoAttack(player);
		context.getSource().sendSuccess(() -> Component.literal("Auto attack disabled."), false);
		return 1;
	}

	private static int disableAutoConsume(CommandContext<CommandSourceStack> context) {
		ServerPlayer player = context.getSource().getPlayer();
		if (player == null) {
			context.getSource().sendFailure(Component.literal("This command can only be run by a player."));
			return 0;
		}

		AutoClickService.disableAutoConsume(player);
		context.getSource().sendSuccess(() -> Component.literal("Auto consume disabled."), false);
		return 1;
	}
}
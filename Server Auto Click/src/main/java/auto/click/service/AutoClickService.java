package auto.click.service;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AutoClickService {
	private static final double ENTITY_REACH = 4.5D;
	private static final long INFINITE_END = Long.MAX_VALUE;
	private static final long AUTO_ATTACK_STATUS_INTERVAL_MILLIS = 1000L;
	private static final long AUTO_CONSUME_RETRY_INTERVAL_MILLIS = 500L;
	private static final Component AUTO_ATTACK_ON_MESSAGE = Component.literal("Auto Attack: ON").withStyle(ChatFormatting.GREEN);
	private static final Map<UUID, ActiveAutoAttack> ACTIVE_ATTACKS = new ConcurrentHashMap<>();
	private static final Map<UUID, ActiveAutoConsume> ACTIVE_CONSUMES = new ConcurrentHashMap<>();

	private AutoClickService() {
	}

	public static void registerTickHandler() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			long now = System.currentTimeMillis();
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				UUID playerId = player.getUUID();

				ActiveAutoAttack attack = ACTIVE_ATTACKS.get(playerId);
				if (attack != null) {
					if (now >= attack.endAtMillis()) {
						ACTIVE_ATTACKS.remove(playerId);
					} else {
						sendAutoAttackStatus(player, attack, now);
						if (now >= attack.nextAttackAtMillis()) {
							attack.scheduleNext(now);
							performAttack(player);
						}
					}
				}

				ActiveAutoConsume consume = ACTIVE_CONSUMES.get(playerId);
				if (consume != null) {
					if (now >= consume.endAtMillis()) {
						ACTIVE_CONSUMES.remove(playerId);
					} else if (!player.isUsingItem() && now >= consume.nextAttemptAtMillis()) {
						consume.scheduleNextAttempt(now);
						startConsumingFood(player);
					}
				}
			}
		});
	}

	public static void enableAutoAttack(ServerPlayer player, double intervalSeconds, double durationSeconds) {
		long intervalMillis = Math.round(intervalSeconds * 1000.0D);
		long now = System.currentTimeMillis();
		long durationMillis = Math.round(durationSeconds * 1000.0D);
		ACTIVE_ATTACKS.put(player.getUUID(), new ActiveAutoAttack(intervalMillis, now, now + durationMillis));
	}

	public static void enableAutoAttackInfinite(ServerPlayer player, double intervalSeconds) {
		long intervalMillis = Math.round(intervalSeconds * 1000.0D);
		long now = System.currentTimeMillis();
		ACTIVE_ATTACKS.put(player.getUUID(), new ActiveAutoAttack(intervalMillis, now, INFINITE_END));
	}

	public static void disableAutoAttack(ServerPlayer player) {
		ACTIVE_ATTACKS.remove(player.getUUID());
	}

	public static void enableAutoConsume(ServerPlayer player, double durationSeconds) {
		long now = System.currentTimeMillis();
		long durationMillis = Math.round(durationSeconds * 1000.0D);
		ACTIVE_CONSUMES.put(player.getUUID(), new ActiveAutoConsume(now + durationMillis, now));
	}

	public static void enableAutoConsumeInfinite(ServerPlayer player) {
		ACTIVE_CONSUMES.put(player.getUUID(), new ActiveAutoConsume(INFINITE_END, System.currentTimeMillis()));
	}

	public static void disableAutoConsume(ServerPlayer player) {
		ACTIVE_CONSUMES.remove(player.getUUID());
	}

	private static void performAttack(ServerPlayer player) {
		Entity target = findTargetEntity(player, ENTITY_REACH);
		if (target != null) {
			player.attack(target);
			player.swing(InteractionHand.MAIN_HAND, true);
		}
	}

	private static void sendAutoAttackStatus(ServerPlayer player, ActiveAutoAttack attack, long now) {
		if (now < attack.nextStatusAtMillis()) {
			return;
		}

		attack.scheduleNextStatus(now);
		player.sendSystemMessage(AUTO_ATTACK_ON_MESSAGE, true);
	}

	private static void startConsumingFood(ServerPlayer player) {
		if (tryStartConsumingFood(player, InteractionHand.MAIN_HAND)) {
			return;
		}

		tryStartConsumingFood(player, InteractionHand.OFF_HAND);
	}

	private static boolean tryStartConsumingFood(ServerPlayer player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (!canConsumeFood(player, stack)) {
			return false;
		}

		player.gameMode.useItem(player, player.level(), stack, hand);
		return player.isUsingItem();
	}

	private static boolean canConsumeFood(ServerPlayer player, ItemStack stack) {
		if (stack.isEmpty()) {
			return false;
		}

		FoodProperties food = stack.get(DataComponents.FOOD);
		Consumable consumable = stack.get(DataComponents.CONSUMABLE);
		return food != null && consumable != null && consumable.canConsume(player, stack);
	}

	private static Entity findTargetEntity(ServerPlayer player, double reachDistance) {
		Vec3 start = player.getEyePosition();
		Vec3 look = player.getViewVector(1.0F);
		Vec3 end = start.add(look.scale(reachDistance));
		AABB searchBox = player.getBoundingBox().expandTowards(look.scale(reachDistance)).inflate(1.0D);

		EntityHitResult hitResult = ProjectileUtil.getEntityHitResult(
			player,
			start,
			end,
			searchBox,
			entity -> entity != null && entity.isAlive() && entity != player && entity.isPickable(),
			reachDistance * reachDistance
		);

		return hitResult == null ? null : hitResult.getEntity();
	}

	private static final class ActiveAutoAttack {
		private final long intervalMillis;
		private final long endAtMillis;
		private long nextAttackAtMillis;
		private long nextStatusAtMillis;

		private ActiveAutoAttack(long intervalMillis, long nextAttackAtMillis, long endAtMillis) {
			this.intervalMillis = intervalMillis;
			this.nextAttackAtMillis = nextAttackAtMillis;
			this.endAtMillis = endAtMillis;
			this.nextStatusAtMillis = nextAttackAtMillis;
		}

		private long endAtMillis() {
			return endAtMillis;
		}

		private long nextAttackAtMillis() {
			return nextAttackAtMillis;
		}

		private long nextStatusAtMillis() {
			return nextStatusAtMillis;
		}

		private void scheduleNext(long now) {
			nextAttackAtMillis = now + intervalMillis;
		}

		private void scheduleNextStatus(long now) {
			nextStatusAtMillis = now + AUTO_ATTACK_STATUS_INTERVAL_MILLIS;
		}
	}

	private static final class ActiveAutoConsume {
		private final long endAtMillis;
		private long nextAttemptAtMillis;

		private ActiveAutoConsume(long endAtMillis, long nextAttemptAtMillis) {
			this.endAtMillis = endAtMillis;
			this.nextAttemptAtMillis = nextAttemptAtMillis;
		}

		private long endAtMillis() {
			return endAtMillis;
		}

		private long nextAttemptAtMillis() {
			return nextAttemptAtMillis;
		}

		private void scheduleNextAttempt(long now) {
			nextAttemptAtMillis = now + AUTO_CONSUME_RETRY_INTERVAL_MILLIS;
		}
	}
}

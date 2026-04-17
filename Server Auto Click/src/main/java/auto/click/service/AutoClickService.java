package auto.click.service;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AutoClickService {
	private static final double ENTITY_REACH = 4.5D;
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
					} else if (now >= attack.nextAttackAtMillis()) {
						attack.scheduleNext(now);
						performAttack(player);
					}
				}

				ActiveAutoConsume consume = ACTIVE_CONSUMES.get(playerId);
				if (consume != null) {
					if (now >= consume.endAtMillis()) {
						ACTIVE_CONSUMES.remove(playerId);
					} else {
						performConsumeTick(player);
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

	public static void disableAutoAttack(ServerPlayer player) {
		ACTIVE_ATTACKS.remove(player.getUUID());
	}

 	public static void enableAutoConsume(ServerPlayer player, double durationSeconds) {
		long now = System.currentTimeMillis();
		long durationMillis = Math.round(durationSeconds * 1000.0D);
		ACTIVE_CONSUMES.put(player.getUUID(), new ActiveAutoConsume(now + durationMillis));
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

	private static void performConsumeTick(ServerPlayer player) {
		player.gameMode.useItem(player, player.level(), player.getItemInHand(InteractionHand.MAIN_HAND), InteractionHand.MAIN_HAND);
		player.swing(InteractionHand.MAIN_HAND, true);
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

		private ActiveAutoAttack(long intervalMillis, long nextAttackAtMillis, long endAtMillis) {
			this.intervalMillis = intervalMillis;
			this.nextAttackAtMillis = nextAttackAtMillis;
			this.endAtMillis = endAtMillis;
		}

		private long endAtMillis() {
			return endAtMillis;
		}

		private long nextAttackAtMillis() {
			return nextAttackAtMillis;
		}

		private void scheduleNext(long now) {
			nextAttackAtMillis = now + intervalMillis;
		}
	}

	private record ActiveAutoConsume(long endAtMillis) {
	}
}
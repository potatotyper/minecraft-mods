package com.centralwaypoint.waypoint;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

import java.time.Instant;

public class Waypoint {
	private String name;
	private int x;
	private int y;
	private int z;
	private String dimensionId;
	private String createdByUuid;
	private String createdByName;
	private long createdAtEpochMillis;

	public Waypoint() {
	}

	public Waypoint(
		String name,
		int x,
		int y,
		int z,
		String dimensionId,
		String createdByUuid,
		String createdByName,
		long createdAtEpochMillis
	) {
		this.name = name;
		this.x = x;
		this.y = y;
		this.z = z;
		this.dimensionId = dimensionId;
		this.createdByUuid = createdByUuid;
		this.createdByName = createdByName;
		this.createdAtEpochMillis = createdAtEpochMillis;
	}

	public static Waypoint fromPlayer(String name, ServerPlayer player) {
		BlockPos position = player.blockPosition();
		return new Waypoint(
			name,
			position.getX(),
			position.getY(),
			position.getZ(),
			player.level().dimension().identifier().toString(),
			player.getUUID().toString(),
			player.getName().getString(),
			Instant.now().toEpochMilli()
		);
	}

	public static Waypoint fromCoordinates(String name, int x, int y, int z, ServerPlayer player) {
		return new Waypoint(
			name,
			x,
			y,
			z,
			player.level().dimension().identifier().toString(),
			player.getUUID().toString(),
			player.getName().getString(),
			Instant.now().toEpochMilli()
		);
	}

	public BlockPos blockPos() {
		return new BlockPos(x, y, z);
	}

	public String getName() {
		return name;
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	public int getZ() {
		return z;
	}

	public String getDimensionId() {
		return dimensionId;
	}

	public String getCreatedByUuid() {
		return createdByUuid;
	}

	public String getCreatedByName() {
		return createdByName;
	}

	public long getCreatedAtEpochMillis() {
		return createdAtEpochMillis;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setX(int x) {
		this.x = x;
	}

	public void setY(int y) {
		this.y = y;
	}

	public void setZ(int z) {
		this.z = z;
	}

	public void setDimensionId(String dimensionId) {
		this.dimensionId = dimensionId;
	}

	public void setCreatedByUuid(String createdByUuid) {
		this.createdByUuid = createdByUuid;
	}

	public void setCreatedByName(String createdByName) {
		this.createdByName = createdByName;
	}

	public void setCreatedAtEpochMillis(long createdAtEpochMillis) {
		this.createdAtEpochMillis = createdAtEpochMillis;
	}
}

package com.centralwaypoint.waypoint;

import java.util.LinkedHashMap;
import java.util.Map;

public class WaypointStore {
	private Map<String, Waypoint> waypoints = new LinkedHashMap<>();

	public static WaypointStore createDefault() {
		return new WaypointStore();
	}

	public Map<String, Waypoint> getWaypoints() {
		if (waypoints == null) {
			waypoints = new LinkedHashMap<>();
		}
		return waypoints;
	}

	public void setWaypoints(Map<String, Waypoint> waypoints) {
		this.waypoints = waypoints == null ? new LinkedHashMap<>() : waypoints;
	}
}

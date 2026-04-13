package com.potatotyper.noelytra.config;

import java.util.LinkedHashSet;
import java.util.Set;

public class NoElytraConfig {
	private Set<String> allowedDimensions = new LinkedHashSet<>();

	public static NoElytraConfig createDefault() {
		NoElytraConfig config = new NoElytraConfig();
		config.allowedDimensions.add("minecraft:the_end");
		return config;
	}

	public Set<String> getAllowedDimensions() {
		return allowedDimensions;
	}

	public boolean isDimensionAllowed(String dimensionId) {
		return allowedDimensions.contains(dimensionId);
	}

	public boolean allowDimension(String dimensionId) {
		return allowedDimensions.add(dimensionId);
	}

	public boolean denyDimension(String dimensionId) {
		return allowedDimensions.remove(dimensionId);
	}
}
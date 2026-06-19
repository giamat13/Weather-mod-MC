package com.weather_by_giamatamat.core;

import java.util.EnumSet;
import java.util.Set;
import java.util.Locale;

/**
 * A module that can be installed into an Alerter by right-clicking the structure with the
 * matching item. Each installed module unlocks warnings for one or more {@link DisasterType}s.
 *
 * <ul>
 *   <li>Alerter + Lightning Rod = tornado &amp; hurricane warnings</li>
 *   <li>Alerter + Dirt          = earthquake warnings</li>
 *   <li>Alerter + Fire Charge   = wildfire warnings</li>
 *   <li>Alerter + Stone         = meteor warnings</li>
 *   <li>Alerter + Stone + Fire Charge = volcanic eruption warnings (derived)</li>
 * </ul>
 */
public enum AlertModule {
	LIGHTNING_ROD(DisasterType.TORNADO, DisasterType.HURRICANE),
	DIRT(DisasterType.EARTHQUAKE),
	FIRE_CHARGE(DisasterType.FIRE),
	STONE(DisasterType.METEOR);

	private final EnumSet<DisasterType> directWarnings;

	AlertModule(final DisasterType... warnings) {
		this.directWarnings = EnumSet.noneOf(DisasterType.class);
		for (DisasterType type : warnings) {
			this.directWarnings.add(type);
		}
	}

	public EnumSet<DisasterType> directWarnings() {
		return this.directWarnings;
	}

	public static AlertModule byName(final String name) {
		if (name == null) {
			return null;
		}
		String normalized = name.trim().toUpperCase(Locale.ROOT);
		for (AlertModule module : values()) {
			if (module.name().equals(normalized)) {
				return module;
			}
		}
		return null;
	}

	/**
	 * Computes the full set of disaster types an alerter warns about, given its installed modules.
	 * Volcanic eruptions are unlocked only when both {@link #STONE} and {@link #FIRE_CHARGE} are present.
	 */
	public static EnumSet<DisasterType> warnedTypes(final Set<AlertModule> modules) {
		EnumSet<DisasterType> result = EnumSet.noneOf(DisasterType.class);
		for (AlertModule module : modules) {
			result.addAll(module.directWarnings());
		}
		if (modules.contains(STONE) && modules.contains(FIRE_CHARGE)) {
			result.add(DisasterType.VOLCANO);
		}
		return result;
	}
}

package com.weather.disaster;

/**
 * The flavours of meteor, each with its own impact behaviour and rarity.
 *
 * <p>Three of them can be picked directly from the command
 * ({@code /naturaldisasters meteor <delay> <id>}):
 * <ul>
 *   <li>{@link #LIGHT} (id 1) — the weakest, most common one: no explosion at all,
 *       just rains useful items from the sky behind a trail of fire.</li>
 *   <li>{@link #CRATER} (id 2) — a strong fireball that blasts a big bowl-shaped
 *       crater into the ground.</li>
 *   <li>{@link #EXTINCTION} (id 3) — the rare extinction event: boils away every bit
 *       of water nearby (it seeps back with the rain), turns random blocks to lava
 *       and kills everything around the impact. Alerters warn about it far in advance.</li>
 * </ul>
 *
 * <p>{@link #DEBRIS} and {@link #FIREBALL} are the original two; they still appear in the
 * random auto-spawn roll but have no command id.</p>
 */
public enum MeteorType {
	// id  explosion debris scatter fire  rarity
	DEBRIS(0, 2.5f, 20, 16.0, 6, 0.0),
	FIREBALL(0, 7.0f, 6, 8.0, 2, 0.0),
	LIGHT(1, 0.0f, 0, 18.0, 8, 0.50),
	CRATER(2, 9.0f, 8, 12.0, 4, 0.35),
	EXTINCTION(3, 40.0f, 6, 14.0, 10, 0.05);

	/** Command selector (1/2/3), or 0 for types that can only be rolled randomly. */
	public final int commandId;
	/** Strength of the impact explosion (0 for the non-exploding LIGHT meteor). */
	public final float explosionPower;
	/** How many debris blocks rain down on impact. */
	public final int debrisCount;
	/** Radius over which debris, items and fire are scattered. */
	public final double scatterRadius;
	/** How many stray fire patches are lit around the impact. */
	public final int fireCount;
	/** Relative chance of being chosen in the random auto-spawn roll (0 = never rolled). */
	public final double rarityWeight;

	MeteorType(int commandId, float explosionPower, int debrisCount, double scatterRadius,
			int fireCount, double rarityWeight) {
		this.commandId = commandId;
		this.explosionPower = explosionPower;
		this.debrisCount = debrisCount;
		this.scatterRadius = scatterRadius;
		this.fireCount = fireCount;
		this.rarityWeight = rarityWeight;
	}

	/** The meteor selected by a command id (1/2/3), or {@code null} if none matches. */
	public static MeteorType byCommandId(int id) {
		for (MeteorType type : values()) {
			if (type.commandId == id) {
				return type;
			}
		}
		return null;
	}
}

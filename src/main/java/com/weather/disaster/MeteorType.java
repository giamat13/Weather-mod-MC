package com.weather.disaster;

/**
 * The two flavours of meteor.
 *
 * <ul>
 *   <li>{@link #DEBRIS} — the weak, common one: scatters volcanic/stone debris (as
 *       falling blocks) over a wide area with the odd patch of fire, and only a
 *       small impact blast.</li>
 *   <li>{@link #FIREBALL} — a rare, very strong fireball: a big fiery explosion and
 *       crater with just a little debris.</li>
 * </ul>
 */
public enum MeteorType {
	DEBRIS(2.5f, 20, 16.0, 6),
	FIREBALL(7.0f, 6, 8.0, 2);

	/** Strength of the impact explosion. */
	public final float explosionPower;
	/** How many debris blocks rain down on impact. */
	public final int debrisCount;
	/** Radius over which debris and fire are scattered. */
	public final double scatterRadius;
	/** How many stray fire patches are lit around the impact. */
	public final int fireCount;

	MeteorType(float explosionPower, int debrisCount, double scatterRadius, int fireCount) {
		this.explosionPower = explosionPower;
		this.debrisCount = debrisCount;
		this.scatterRadius = scatterRadius;
		this.fireCount = fireCount;
	}
}

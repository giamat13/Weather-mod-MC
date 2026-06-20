package com.weather.disaster;

/**
 * The kinds of natural disasters and their tuning parameters.
 *
 * <p>A hurricane is intentionally bigger, longer and stronger than a tornado.
 */
public enum DisasterType {
	// name        hebrew      radius height life  drift pull  up    swirl blocks/tick hurts grab
	TORNADO("Tornado", "טורנדו", 8.0, 28, 600, 0.45, 0.35, 0.95, 0.80, 2, false, 1.0),
	HURRICANE("Hurricane", "הוריקן", 18.0, 42, 1200, 0.28, 0.55, 1.25, 1.10, 5, true, 1.7);

	/** Horizontal radius of influence, in blocks. */
	public final double radius;
	/** How tall the funnel reaches, in blocks. */
	public final int height;
	/** How long the disaster stays alive, in ticks (20 ticks = 1 second). */
	public final int lifetimeTicks;
	/** How fast the disaster wanders across the world, in blocks per tick. */
	public final double driftPerTick;
	/** How strongly entities are pulled toward the eye. */
	public final double pullStrength;
	/** How strongly entities and blocks are flung upward. */
	public final double upStrength;
	/** How strongly the swirl spins entities tangentially. */
	public final double swirlStrength;
	/** How many blocks are torn up into falling blocks each tick. */
	public final int blocksPerTick;
	/** Whether flung blocks deal damage to entities they hit. */
	public final boolean blocksHurtEntities;
	/**
	 * How forcefully the storm rips up blocks. The chance of grabbing a given block is
	 * {@code blockGrabStrength / (1 + blockHardness)}, so tougher (slower-to-break) blocks
	 * are far less likely to be taken, and a hurricane (higher value) grabs more than a tornado.
	 */
	public final double blockGrabStrength;

	public final String englishName;
	public final String hebrewName;

	DisasterType(String englishName, String hebrewName, double radius, int height, int lifetimeTicks,
			double driftPerTick, double pullStrength, double upStrength, double swirlStrength,
			int blocksPerTick, boolean blocksHurtEntities, double blockGrabStrength) {
		this.englishName = englishName;
		this.hebrewName = hebrewName;
		this.radius = radius;
		this.height = height;
		this.lifetimeTicks = lifetimeTicks;
		this.driftPerTick = driftPerTick;
		this.pullStrength = pullStrength;
		this.upStrength = upStrength;
		this.swirlStrength = swirlStrength;
		this.blocksPerTick = blocksPerTick;
		this.blocksHurtEntities = blocksHurtEntities;
		this.blockGrabStrength = blockGrabStrength;
	}
}

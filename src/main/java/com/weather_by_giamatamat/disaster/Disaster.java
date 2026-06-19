package com.weather_by_giamatamat.disaster;

import com.weather_by_giamatamat.core.DisasterType;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/**
 * Base class for a discrete disaster event. Each disaster runs through a warning phase
 * (during which Alerters light up) followed by an active phase (the actual destruction),
 * then finishes and is removed.
 */
public abstract class Disaster {
	protected final ServerLevel level;
	protected final DisasterType type;
	protected Vec3 center;
	protected int age = 0;
	private boolean finished = false;

	protected Disaster(final ServerLevel level, final DisasterType type, final Vec3 center) {
		this.level = level;
		this.type = type;
		this.center = center;
	}

	/** Drives one server tick of this disaster. */
	public final void tick() {
		if (finished) {
			return;
		}
		int warn = warnTicks();
		int active = activeTicks();
		if (age == 0) {
			onStart();
		}
		if (age < warn) {
			onWarnTick();
		} else if (age < warn + active) {
			if (age == warn) {
				onImpact();
			}
			onActiveTick();
		} else {
			onEnd();
			finished = true;
		}
		age++;
	}

	protected abstract int warnTicks();

	protected abstract int activeTicks();

	protected void onStart() {
	}

	protected void onWarnTick() {
	}

	protected void onImpact() {
	}

	protected abstract void onActiveTick();

	protected void onEnd() {
	}

	public boolean isWarning() {
		return age < warnTicks();
	}

	public boolean isActive() {
		int warn = warnTicks();
		return age >= warn && age < warn + activeTicks();
	}

	public boolean isFinished() {
		return finished;
	}

	public int ticksUntilImpact() {
		return Math.max(0, warnTicks() - age);
	}

	public DisasterType type() {
		return this.type;
	}

	public ServerLevel level() {
		return this.level;
	}

	public Vec3 center() {
		return this.center;
	}

	public BlockPos centerPos() {
		return BlockPos.containing(this.center);
	}

	/** Approximate radius of effect, in blocks (used only for flavour / scaling). */
	public double radius() {
		return 8.0;
	}
}

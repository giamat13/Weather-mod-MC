package com.weather_by_giamatamat.alerter;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.weather_by_giamatamat.core.AlertModule;
import com.weather_by_giamatamat.core.DisasterType;

import net.minecraft.core.BlockPos;

import org.jspecify.annotations.Nullable;

/**
 * A built Alerter multiblock, identified by the position of its central redstone lamp.
 * Persisted (position + installed modules); the live status light is transient.
 */
public class Alerter {
	public static final Codec<Alerter> CODEC = RecordCodecBuilder.create(
		instance -> instance.group(
				BlockPos.CODEC.fieldOf("pos").forGetter(a -> a.pos),
				Codec.STRING.listOf().optionalFieldOf("modules", List.of()).forGetter(Alerter::moduleNames)
			)
			.apply(instance, Alerter::fromNames)
	);

	private final BlockPos pos;
	private final EnumSet<AlertModule> modules = EnumSet.noneOf(AlertModule.class);

	// Transient live state (not persisted).
	private AlertLevel status = AlertLevel.GREEN;
	private @Nullable DisasterType nearestType = null;

	public Alerter(final BlockPos pos) {
		this.pos = pos.immutable();
	}

	private static Alerter fromNames(final BlockPos pos, final List<String> moduleNames) {
		Alerter alerter = new Alerter(pos);
		for (String name : moduleNames) {
			AlertModule module = AlertModule.byName(name);
			if (module != null) {
				alerter.modules.add(module);
			}
		}
		return alerter;
	}

	private List<String> moduleNames() {
		List<String> names = new ArrayList<>(this.modules.size());
		for (AlertModule module : this.modules) {
			names.add(module.name());
		}
		return names;
	}

	public BlockPos pos() {
		return this.pos;
	}

	public EnumSet<AlertModule> modules() {
		return this.modules;
	}

	/** @return true if the module was newly added. */
	public boolean installModule(final AlertModule module) {
		return this.modules.add(module);
	}

	public EnumSet<DisasterType> warnedTypes() {
		return AlertModule.warnedTypes(this.modules);
	}

	public AlertLevel status() {
		return this.status;
	}

	public void setStatus(final AlertLevel status) {
		this.status = status;
	}

	public @Nullable DisasterType nearestType() {
		return this.nearestType;
	}

	public void setNearestType(final @Nullable DisasterType nearestType) {
		this.nearestType = nearestType;
	}
}

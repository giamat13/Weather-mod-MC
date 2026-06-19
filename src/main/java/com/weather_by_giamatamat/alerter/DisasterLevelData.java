package com.weather_by_giamatamat.alerter;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import org.jspecify.annotations.Nullable;

/**
 * Per-dimension persistent list of Alerter structures.
 */
public class DisasterLevelData extends SavedData {
	public static final Codec<DisasterLevelData> CODEC = RecordCodecBuilder.create(
		instance -> instance.group(
				Alerter.CODEC.listOf().optionalFieldOf("alerters", List.of()).forGetter(DisasterLevelData::alertersForSave)
			)
			.apply(instance, DisasterLevelData::new)
	);

	public static final SavedDataType<DisasterLevelData> TYPE = new SavedDataType<>(
		Identifier.fromNamespaceAndPath("weather-by-giamatamat", "alerters"),
		DisasterLevelData::new,
		CODEC,
		DataFixTypes.SAVED_DATA_RAIDS
	);

	private final List<Alerter> alerters = new ArrayList<>();

	public DisasterLevelData() {
	}

	private DisasterLevelData(final List<Alerter> alerters) {
		this.alerters.addAll(alerters);
	}

	public static DisasterLevelData get(final ServerLevel level) {
		return level.getDataStorage().computeIfAbsent(TYPE);
	}

	private List<Alerter> alertersForSave() {
		return new ArrayList<>(this.alerters);
	}

	public List<Alerter> alerters() {
		return this.alerters;
	}

	public @Nullable Alerter findAt(final BlockPos pos) {
		for (Alerter alerter : this.alerters) {
			if (alerter.pos().equals(pos)) {
				return alerter;
			}
		}
		return null;
	}

	public Alerter getOrCreate(final BlockPos pos) {
		Alerter existing = findAt(pos);
		if (existing != null) {
			return existing;
		}
		Alerter created = new Alerter(pos);
		this.alerters.add(created);
		setDirty();
		return created;
	}

	public void remove(final Alerter alerter) {
		if (this.alerters.remove(alerter)) {
			setDirty();
		}
	}
}

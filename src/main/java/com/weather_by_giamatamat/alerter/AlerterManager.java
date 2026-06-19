package com.weather_by_giamatamat.alerter;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.weather_by_giamatamat.core.AlertModule;
import com.weather_by_giamatamat.core.DisasterType;
import com.weather_by_giamatamat.core.ModConfig;
import com.weather_by_giamatamat.disaster.Disaster;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import org.jspecify.annotations.Nullable;

/**
 * Detects, configures and animates the Alerter multiblock: a 3×3×3 structure with a redstone lamp
 * at the centre, a red stained-glass block directly above it, and iron blocks everywhere else.
 * Right-clicking the structure with a module item (lightning rod / dirt / fire charge / stone)
 * installs the corresponding warning module.
 */
public final class AlerterManager {
	private static final int COLOR_GREEN = 0x33DD33;
	private static final int COLOR_YELLOW = 0xFFE000;
	private static final int COLOR_RED = 0xFF2020;

	private static @Nullable Map<Item, AlertModule> itemModules;
	private static @Nullable Block redGlass;

	private AlerterManager() {
	}

	private static void ensureResolved() {
		if (itemModules != null) {
			return;
		}
		Map<Item, AlertModule> map = new HashMap<>();
		for (Item lightningRod : Items.LIGHTNING_ROD.asList()) {
			map.put(lightningRod, AlertModule.LIGHTNING_ROD);
		}
		map.put(Items.DIRT, AlertModule.DIRT);
		map.put(Items.FIRE_CHARGE, AlertModule.FIRE_CHARGE);
		map.put(Items.STONE, AlertModule.STONE);
		itemModules = map;
		redGlass = Blocks.STAINED_GLASS.pick(DyeColor.RED);
	}

	// --- Interaction: install modules ---------------------------------------

	public static InteractionResult onUseBlock(final Player player, final Level world,
			final InteractionHand hand, final BlockHitResult hit) {
		if (world.isClientSide() || !(world instanceof ServerLevel level) || hand != InteractionHand.MAIN_HAND) {
			return InteractionResult.PASS;
		}
		ItemStack stack = player.getItemInHand(hand);
		if (stack.isEmpty()) {
			return InteractionResult.PASS;
		}
		ensureResolved();
		AlertModule module = itemModules.get(stack.getItem());
		if (module == null) {
			return InteractionResult.PASS;
		}
		BlockPos center = findAlerterCenter(level, hit.getBlockPos());
		if (center == null) {
			return InteractionResult.PASS;
		}
		DisasterLevelData data = DisasterLevelData.get(level);
		Alerter alerter = data.getOrCreate(center);
		boolean added = alerter.installModule(module);
		data.setDirty();
		if (!player.getAbilities().instabuild) {
			stack.shrink(1);
		}
		level.playSound(null, center, SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 1.0F, 1.4F);
		EnumSet<DisasterType> warned = alerter.warnedTypes();
		Component summary = Component.literal(warned.isEmpty() ? "nothing yet" : describeTypes(warned));
		player.sendSystemMessage(Component.literal(
				(added ? "Installed " : "Already installed ") + module.name().toLowerCase() + " module. Now warns: ")
			.append(summary.copy().withStyle(ChatFormatting.AQUA)));
		return InteractionResult.SUCCESS;
	}

	private static String describeTypes(final EnumSet<DisasterType> types) {
		StringBuilder sb = new StringBuilder();
		for (DisasterType type : types) {
			if (sb.length() > 0) {
				sb.append(", ");
			}
			sb.append(type.displayName());
		}
		return sb.toString();
	}

	// --- Structure detection ------------------------------------------------

	public static @Nullable BlockPos findAlerterCenter(final Level world, final BlockPos clicked) {
		ensureResolved();
		for (int dx = -1; dx <= 1; dx++) {
			for (int dy = -1; dy <= 1; dy++) {
				for (int dz = -1; dz <= 1; dz++) {
					BlockPos candidate = clicked.offset(dx, dy, dz);
					if (world.getBlockState(candidate).is(Blocks.REDSTONE_LAMP) && validate(world, candidate)) {
						return candidate;
					}
				}
			}
		}
		return null;
	}

	public static boolean validate(final Level world, final BlockPos center) {
		ensureResolved();
		if (!world.getBlockState(center).is(Blocks.REDSTONE_LAMP)) {
			return false;
		}
		if (!world.getBlockState(center.above()).is(redGlass)) {
			return false;
		}
		for (int dx = -1; dx <= 1; dx++) {
			for (int dy = -1; dy <= 1; dy++) {
				for (int dz = -1; dz <= 1; dz++) {
					if (dx == 0 && dz == 0 && (dy == 0 || dy == 1)) {
						continue; // centre lamp and the red glass above it
					}
					if (!world.getBlockState(center.offset(dx, dy, dz)).is(Blocks.IRON_BLOCK)) {
						return false;
					}
				}
			}
		}
		return true;
	}

	// --- Live status light --------------------------------------------------

	/**
	 * Refreshes one alerter's status from the active disasters and emits its light / beep.
	 * @return false if the structure is no longer valid and should be removed.
	 */
	public static boolean updateAlerter(final ServerLevel level, final Alerter alerter, final List<Disaster> active) {
		if (!validate(level, alerter.pos())) {
			return false;
		}
		EnumSet<DisasterType> warned = alerter.warnedTypes();
		Vec3 anchor = Vec3.atCenterOf(alerter.pos());
		double best = Double.MAX_VALUE;
		DisasterType bestType = null;
		for (Disaster disaster : active) {
			if (disaster.level() != level || !warned.contains(disaster.type())) {
				continue;
			}
			double dist = Math.sqrt(disaster.center().distanceToSqr(anchor));
			if (dist < best) {
				best = dist;
				bestType = disaster.type();
			}
		}
		AlertLevel status;
		if (best <= ModConfig.ALERT_RED_RANGE) {
			status = AlertLevel.RED;
		} else if (best <= ModConfig.ALERT_YELLOW_RANGE) {
			status = AlertLevel.YELLOW;
		} else {
			status = AlertLevel.GREEN;
		}
		alerter.setStatus(status);
		alerter.setNearestType(status == AlertLevel.GREEN ? null : bestType);
		emit(level, alerter, status);
		return true;
	}

	private static void emit(final ServerLevel level, final Alerter alerter, final AlertLevel status) {
		BlockPos glass = alerter.pos().above();
		double x = glass.getX() + 0.5;
		double y = glass.getY() + 0.9;
		double z = glass.getZ() + 0.5;
		int color = switch (status) {
			case GREEN -> COLOR_GREEN;
			case YELLOW -> COLOR_YELLOW;
			case RED -> COLOR_RED;
		};
		DustParticleOptions dust = new DustParticleOptions(color, 1.6F);
		level.sendParticles(dust, x, y, z, 4, 0.18, 0.25, 0.18, 0.0);
		long time = level.getGameTime();
		if (status == AlertLevel.RED && time % 10 == 0) {
			level.playSound(null, x, y, z, SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 4.0F, 2.0F);
		} else if (status == AlertLevel.YELLOW && time % 40 == 0) {
			level.playSound(null, x, y, z, SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 1.0F, 1.2F);
		}
	}
}

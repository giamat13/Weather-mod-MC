package com.weather.mixin;

import com.weather.registry.ModRegistry;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds the mod's items into the vanilla creative tabs (this fabric-api build has no
 * ItemGroupEvents). The alerters live in the Redstone Blocks tab.
 */
@Mixin(CreativeModeTab.class)
public class CreativeModeTabMixin {
	private static final Identifier REDSTONE_TAB = Identifier.fromNamespaceAndPath("minecraft", "redstone_blocks");

	@Inject(method = "buildContents", at = @At("TAIL"))
	private void weatherMod$addAlerters(CreativeModeTab.ItemDisplayParameters params, CallbackInfo ci) {
		CreativeModeTab self = (CreativeModeTab) (Object) this;
		Identifier id = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(self);
		if (id == null || !id.equals(REDSTONE_TAB)) {
			return;
		}
		for (var item : new ItemStack[] {
			new ItemStack(ModRegistry.USELESS_ALERTER),
			new ItemStack(ModRegistry.DISASTER_SHIELD),
			new ItemStack(ModRegistry.TORNADO_HURRICANE_ALERTER),
			new ItemStack(ModRegistry.EARTHQUAKE_ALERTER),
			new ItemStack(ModRegistry.FIRE_ALERTER),
			new ItemStack(ModRegistry.METEOR_ALERTER),
			new ItemStack(ModRegistry.VOLCANO_ALERTER)
		}) {
			self.getDisplayItems().add(item);
			self.getSearchTabDisplayItems().add(item.copy());
		}
	}
}

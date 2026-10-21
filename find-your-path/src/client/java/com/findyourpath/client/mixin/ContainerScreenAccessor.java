package com.findyourpath.client.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Lets the crafting guide find where the container's slots are drawn on screen. */
@Mixin(AbstractContainerScreen.class)
public interface ContainerScreenAccessor {
	@Accessor("leftPos")
	int findyourpath$leftPos();

	@Accessor("topPos")
	int findyourpath$topPos();
}

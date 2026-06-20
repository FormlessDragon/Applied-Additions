package com.formlesslab.ae2additions.mixin;

import ae2.client.gui.ICompositeWidget;
import ae2.client.gui.WidgetContainer;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = WidgetContainer.class, remap = false)
public interface WidgetContainerAccessor {
    @Accessor("compositeWidgets")
    Object2ObjectLinkedOpenHashMap<String, ICompositeWidget> getCompositeWidgets();
}
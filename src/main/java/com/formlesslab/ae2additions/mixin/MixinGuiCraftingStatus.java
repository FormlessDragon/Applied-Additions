package com.formlesslab.ae2additions.mixin;

import ae2.client.gui.me.crafting.GuiCraftingCPU;
import ae2.client.gui.me.crafting.GuiCraftingStatus;
import ae2.client.gui.style.GuiStyle;
import ae2.client.gui.widgets.Scrollbar;
import ae2.container.implementations.ContainerCraftingStatus;
import com.formlesslab.ae2additions.client.gui.widgets.GroupedCPUSelectionList;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.text.ITextComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GuiCraftingStatus.class, remap = false)
public abstract class MixinGuiCraftingStatus extends GuiCraftingCPU<ContainerCraftingStatus> {
    protected MixinGuiCraftingStatus(
        ContainerCraftingStatus container,
        InventoryPlayer playerInventory,
        ITextComponent title,
        GuiStyle style
    ) {
        super(container, playerInventory, title, style);
    }

    @Inject(
        method = "<init>(Lae2/container/implementations/ContainerCraftingStatus;Lnet/minecraft/entity/player/InventoryPlayer;Lnet/minecraft/util/text/ITextComponent;Lae2/client/gui/style/GuiStyle;)V",
        at = @At("TAIL")
    )
    private void ae2additions$replaceCpuSelectionList(
        ContainerCraftingStatus container,
        InventoryPlayer playerInventory,
        ITextComponent title,
        GuiStyle style,
        CallbackInfo ci
    ) {
        Scrollbar scrollbar = (Scrollbar) this.widgets.getComposite("selectCpuScrollbar");
        GroupedCPUSelectionList groupedList = new GroupedCPUSelectionList(container, scrollbar, style,
            () -> Math.max(1, this.getCraftingRows() - 1));
        groupedList.setSize(
            style.getWidget("selectCpuList").getWidth(),
            style.getWidget("selectCpuList").getHeight());
        ((WidgetContainerAccessor) this.widgets).ae2additions$getCompositeWidgets()
            .put("selectCpuList", groupedList);
    }
}

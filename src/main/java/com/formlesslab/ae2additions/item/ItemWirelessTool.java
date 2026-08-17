package com.formlesslab.ae2additions.item;

import ae2.util.InteractionUtil;
import com.formlesslab.ae2additions.wireless.WirelessLinking;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

import java.util.List;

public class ItemWirelessTool extends Item {
    public ItemWirelessTool() {
        this.setMaxStackSize(1);
        this.setNoRepair();
    }

    @Override
    public void addInformation(ItemStack stack, World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        if (WirelessLinking.hasLocator(stack)) {
            BlockPos pos = WirelessLinking.getLocatorPos(stack);
            tooltip.add(TextFormatting.GRAY + new TextComponentTranslation("wireless.tooltip", pos.getX(), pos.getY(), pos.getZ()).getUnformattedText());
            tooltip.add(TextFormatting.GRAY + new TextComponentTranslation("wireless.use.tooltip.02").getUnformattedText());
        } else {
            tooltip.add(TextFormatting.GRAY + new TextComponentTranslation("wireless.use.tooltip.01").getUnformattedText());
        }
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World worldIn, EntityPlayer playerIn, EnumHand handIn) {
        ItemStack stack = playerIn.getHeldItem(handIn);
        if (InteractionUtil.isInAlternateUseMode(playerIn)) {
            if (!worldIn.isRemote) {
                WirelessLinking.clearLocator(stack);
                playerIn.sendStatusMessage(new TextComponentTranslation("chat.wireless_connect.clear"), true);
            }
            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        }
        return new ActionResult<>(EnumActionResult.PASS, stack);
    }
}

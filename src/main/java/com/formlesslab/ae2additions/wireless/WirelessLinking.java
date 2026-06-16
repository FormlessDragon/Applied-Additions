package com.formlesslab.ae2additions.wireless;

import com.formlesslab.ae2additions.api.WirelessEndpoint;
import com.formlesslab.ae2additions.api.WirelessFail;
import com.formlesslab.ae2additions.init.ModConfig;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

public final class WirelessLinking {
    private static final String LOCATOR_TAG = "wireless_locator";
    private static final String FREQ_TAG = "freq";
    private static final String DIM_TAG = "dim";
    private static final String X_TAG = "x";
    private static final String Y_TAG = "y";
    private static final String Z_TAG = "z";

    private WirelessLinking() {
    }

    public static EnumActionResult useTool(EntityPlayer player, ItemStack stack, WirelessEndpoint current) {
        World world = current.getEndpointWorld();
        if (world.isRemote) {
            return EnumActionResult.SUCCESS;
        }

        NBTTagCompound locator = getLocator(stack);
        if (locator != null && locator.getLong(FREQ_TAG) != 0) {
            return finishLink(player, stack, current, locator);
        }

        int port = current.allocatePort();
        if (port < 0) {
            player.sendStatusMessage(WirelessFail.OUT_OF_PORT.text(), true);
            return EnumActionResult.FAIL;
        }

        long frequency = current.getNewFrequency();
        setLocator(stack, frequency, world.provider.getDimension(), current.getEndpointPos());
        BlockPos pos = current.getEndpointPos();
        player.sendStatusMessage(new TextComponentTranslation(
            "chat.wireless_bind", pos.getX(), pos.getY(), pos.getZ()), true);
        return EnumActionResult.SUCCESS;
    }

    public static void clearLocator(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag != null) {
            tag.removeTag(LOCATOR_TAG);
        }
    }

    public static boolean hasLocator(ItemStack stack) {
        return getLocator(stack) != null;
    }

    public static BlockPos getLocatorPos(ItemStack stack) {
        NBTTagCompound locator = getLocator(stack);
        if (locator == null) {
            return BlockPos.ORIGIN;
        }
        return new BlockPos(locator.getInteger(X_TAG), locator.getInteger(Y_TAG), locator.getInteger(Z_TAG));
    }

    private static EnumActionResult finishLink(EntityPlayer player, ItemStack stack, WirelessEndpoint current,
                                               NBTTagCompound locator) {
        World world = current.getEndpointWorld();
        BlockPos currentPos = current.getEndpointPos();
        int otherDim = locator.getInteger(DIM_TAG);
        BlockPos otherPos = new BlockPos(locator.getInteger(X_TAG), locator.getInteger(Y_TAG), locator.getInteger(Z_TAG));
        long frequency = locator.getLong(FREQ_TAG);

        if (otherDim != world.provider.getDimension()) {
            player.sendStatusMessage(WirelessFail.CROSS_DIMENSION.text(), true);
            return EnumActionResult.FAIL;
        }
        if (otherPos.equals(currentPos)) {
            player.sendStatusMessage(WirelessFail.SELF_REFERENCE.text(), true);
            return EnumActionResult.FAIL;
        }
        if (Math.sqrt(otherPos.distanceSq(currentPos)) > ModConfig.wirelessConnectorMaxRange) {
            player.sendStatusMessage(WirelessFail.OUT_OF_RANGE.text(), true);
            return EnumActionResult.FAIL;
        }

        MinecraftServer server = world.getMinecraftServer();
        WorldServer otherWorld = server != null ? server.getWorld(otherDim) : null;
        if (otherWorld == null) {
            player.sendStatusMessage(WirelessFail.MISSING.text(), true);
            return EnumActionResult.FAIL;
        }

        TileEntity otherTile = otherWorld.getTileEntity(otherPos);
        if (!(otherTile instanceof WirelessEndpoint other)) {
            player.sendStatusMessage(WirelessFail.MISSING.text(), true);
            return EnumActionResult.FAIL;
        }

        int currentPort = current.allocatePort();
        if (currentPort < 0) {
            player.sendStatusMessage(WirelessFail.OUT_OF_PORT.text(), true);
            return EnumActionResult.FAIL;
        }
        int otherPort = other.allocatePort();
        if (otherPort < 0) {
            player.sendStatusMessage(WirelessFail.OUT_OF_PORT.text(), true);
            return EnumActionResult.FAIL;
        }

        other.setFrequency(frequency, otherPort);
        current.setFrequency(frequency, currentPort);
        clearLocator(stack);
        player.sendStatusMessage(new TextComponentTranslation(
            "chat.wireless_connect", currentPos.getX(), currentPos.getY(), currentPos.getZ()), true);
        return EnumActionResult.SUCCESS;
    }

    private static NBTTagCompound getLocator(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null || !tag.hasKey(LOCATOR_TAG, 10)) {
            return null;
        }
        return tag.getCompoundTag(LOCATOR_TAG);
    }

    private static void setLocator(ItemStack stack, long frequency, int dimension, BlockPos pos) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            stack.setTagCompound(tag);
        }
        NBTTagCompound locator = new NBTTagCompound();
        locator.setLong(FREQ_TAG, frequency);
        locator.setInteger(DIM_TAG, dimension);
        locator.setInteger(X_TAG, pos.getX());
        locator.setInteger(Y_TAG, pos.getY());
        locator.setInteger(Z_TAG, pos.getZ());
        tag.setTag(LOCATOR_TAG, locator);
    }
}

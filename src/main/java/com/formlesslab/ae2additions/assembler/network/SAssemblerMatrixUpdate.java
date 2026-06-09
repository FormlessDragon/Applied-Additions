package com.formlesslab.ae2additions.assembler.network;

import com.formlesslab.ae2additions.assembler.client.gui.GuiAssemblerMatrix;
import com.formlesslab.ae2additions.network.ModClientboundPacket;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.IOException;

public class SAssemblerMatrixUpdate extends ModClientboundPacket {
    private long patternId;
    private Int2ObjectMap<ItemStack> updateMap = new Int2ObjectOpenHashMap<>();

    public SAssemblerMatrixUpdate() {
    }

    public SAssemblerMatrixUpdate(long patternId, Int2ObjectMap<ItemStack> updateMap) {
        this.patternId = patternId;
        this.updateMap = new Int2ObjectOpenHashMap<>(updateMap);
    }

    @Override
    protected void read(ByteBuf buf) {
        PacketBuffer packet = new PacketBuffer(buf);
        this.patternId = packet.readLong();
        this.updateMap = new Int2ObjectOpenHashMap<>();
        int size = packet.readVarInt();
        for (int i = 0; i < size; i++) {
            int slot = packet.readVarInt();
            try {
                this.updateMap.put(slot, packet.readItemStack());
            } catch (IOException e) {
                throw new IllegalArgumentException("Could not read assembler matrix pattern stack", e);
            }
        }
    }

    @Override
    protected void write(ByteBuf buf) {
        PacketBuffer packet = new PacketBuffer(buf);
        packet.writeLong(this.patternId);
        packet.writeVarInt(this.updateMap.size());
        for (Int2ObjectMap.Entry<ItemStack> entry : this.updateMap.int2ObjectEntrySet()) {
            packet.writeVarInt(entry.getIntKey());
            packet.writeItemStack(entry.getValue());
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void handleClient(Minecraft minecraft) {
        if (minecraft.currentScreen instanceof GuiAssemblerMatrix<?>) {
            ((GuiAssemblerMatrix<?>) minecraft.currentScreen).receiveUpdate(this.patternId, this.updateMap);
        }
    }
}

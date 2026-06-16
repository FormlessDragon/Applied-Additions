package com.formlesslab.ae2additions.client.util;

import net.minecraft.network.PacketBuffer;

public record CraftingStatusCpuMetadata(int serial, int clusterId, boolean remainingCapacity) {
    public static CraftingStatusCpuMetadata readFromPacket(PacketBuffer buffer) {
        return new CraftingStatusCpuMetadata(buffer.readInt(), buffer.readInt(), buffer.readBoolean());
    }

    public void writeToPacket(PacketBuffer buffer) {
        buffer.writeInt(this.serial);
        buffer.writeInt(this.clusterId);
        buffer.writeBoolean(this.remainingCapacity);
    }

    public boolean quantum() {
        return this.clusterId > 0;
    }
}

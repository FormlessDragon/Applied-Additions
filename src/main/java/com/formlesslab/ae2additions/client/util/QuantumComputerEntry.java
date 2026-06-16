package com.formlesslab.ae2additions.client.util;

import ae2.api.config.CpuSelectionMode;
import ae2.api.stacks.GenericStack;
import ae2.text.TextComponents;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.text.ITextComponent;
import org.jetbrains.annotations.Nullable;

public record QuantumComputerEntry(
        int serial,
        long storage,
        int coProcessors,
        @Nullable ITextComponent name,
        CpuSelectionMode mode,
        @Nullable GenericStack currentJob,
        float progress,
        long elapsedTimeNanos,
        int clusterId,
        boolean isRemainingCapacity
) {
    public static QuantumComputerEntry readFromPacket(PacketBuffer buffer) {
        return new QuantumComputerEntry(
                buffer.readInt(),
                buffer.readLong(),
                buffer.readInt(),
                TextComponents.readFromPacket(buffer),
                buffer.readEnumValue(CpuSelectionMode.class),
                GenericStack.readBuffer(buffer), buffer.readFloat(),
                buffer.readVarLong(),
                buffer.readInt(),
                buffer.readBoolean()
        );
    }

    public void writeToPacket(PacketBuffer buffer) {
        buffer.writeInt(this.serial);
        buffer.writeLong(this.storage);
        buffer.writeInt(this.coProcessors);
        TextComponents.writeToPacket(buffer, this.name);
        buffer.writeEnumValue(this.mode);
        GenericStack.writeBuffer(this.currentJob, buffer);
        buffer.writeFloat(this.progress);
        buffer.writeVarLong(this.elapsedTimeNanos);
        buffer.writeInt(this.clusterId);
        buffer.writeBoolean(this.isRemainingCapacity);
    }
}

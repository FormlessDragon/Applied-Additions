package com.formlesslab.ae2additions.client.util;

import ae2.container.guisync.PacketWritable;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import net.minecraft.network.PacketBuffer;

import java.util.List;

@SuppressWarnings("unused")
public record QuantumComputerList(List<QuantumComputerEntry> cpus) implements PacketWritable {
    private static final int MAX_CPU_LIST_ENTRIES = 1024;
    private static final int MIN_CPU_LIST_ENTRY_BYTES = 18;

    public QuantumComputerList {
        cpus = List.copyOf(cpus);
    }

    public QuantumComputerList(ByteBuf data) {
        this(readCpus(data));
    }

    public void writeToPacket(ByteBuf data) {
        PacketBuffer buffer = new PacketBuffer(data);
        buffer.writeInt(this.cpus.size());

        for (QuantumComputerEntry cpu : this.cpus) {
            cpu.writeToPacket(buffer);
        }
    }

    private static List<QuantumComputerEntry> readCpus(ByteBuf data) {
        PacketBuffer buffer = new PacketBuffer(data);
        int count = buffer.readInt();
        if (count < 0 || count > MAX_CPU_LIST_ENTRIES
                || count > buffer.readableBytes() / MIN_CPU_LIST_ENTRY_BYTES) {
            throw new IllegalArgumentException("Invalid quantum CPU list entry count: " + count);
        }

        ObjectList<QuantumComputerEntry> readCpus = new ObjectArrayList<>(count);

        for (int i = 0; i < count; i++) {
            readCpus.add(QuantumComputerEntry.readFromPacket(buffer));
        }

        return List.copyOf(readCpus);
    }
}

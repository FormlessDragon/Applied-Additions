package com.formlesslab.ae2additions.client.util;

import ae2.container.guisync.PacketWritable;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import net.minecraft.network.PacketBuffer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public record CraftingStatusCpuMetadataList(List<CraftingStatusCpuMetadata> entries) implements PacketWritable {
    public static final CraftingStatusCpuMetadataList EMPTY = new CraftingStatusCpuMetadataList(List.of());
    private static final int MAX_METADATA_ENTRIES = 1024;
    private static final int METADATA_ENTRY_BYTES = 9;

    public CraftingStatusCpuMetadataList {
        entries = List.copyOf(entries);
    }

    public CraftingStatusCpuMetadataList(ByteBuf data) {
        this(readEntries(data));
    }

    @Override
    public void writeToPacket(ByteBuf data) {
        PacketBuffer buffer = new PacketBuffer(data);
        buffer.writeInt(this.entries.size());
        for (CraftingStatusCpuMetadata entry : this.entries) {
            entry.writeToPacket(buffer);
        }
    }

    public Map<Integer, CraftingStatusCpuMetadata> bySerial() {
        Map<Integer, CraftingStatusCpuMetadata> result = new HashMap<>();
        for (CraftingStatusCpuMetadata entry : this.entries) {
            result.put(entry.serial(), entry);
        }
        return result;
    }

    private static List<CraftingStatusCpuMetadata> readEntries(ByteBuf data) {
        PacketBuffer buffer = new PacketBuffer(data);
        int count = buffer.readInt();
        if (count < 0 || count > MAX_METADATA_ENTRIES || count > buffer.readableBytes() / METADATA_ENTRY_BYTES) {
            throw new IllegalArgumentException("Invalid crafting CPU metadata entry count: " + count);
        }

        ObjectList<CraftingStatusCpuMetadata> entries = new ObjectArrayList<>(count);
        for (int i = 0; i < count; i++) {
            entries.add(CraftingStatusCpuMetadata.readFromPacket(buffer));
        }
        return List.copyOf(entries);
    }
}

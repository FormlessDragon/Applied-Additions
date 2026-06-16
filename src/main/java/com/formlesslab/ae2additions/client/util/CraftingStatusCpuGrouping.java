package com.formlesslab.ae2additions.client.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CraftingStatusCpuGrouping {
    private CraftingStatusCpuGrouping() {
    }

    public static List<Integer> orderSerials(
        List<Integer> serials,
        Map<Integer, CraftingStatusCpuMetadata> metadataBySerial
    ) {
        List<Integer> ordered = new ArrayList<>(serials.size());
        Map<Integer, List<Integer>> pendingGroups = new LinkedHashMap<>();

        for (Integer serial : serials) {
            CraftingStatusCpuMetadata metadata = metadataBySerial.get(serial);
            if (metadata == null || !metadata.quantum()) {
                ordered.add(serial);
                continue;
            }

            List<Integer> group = pendingGroups.computeIfAbsent(metadata.clusterId(), ignored -> new ArrayList<>());
            group.add(serial);
            if (group.size() == 1) {
                ordered.add(serial);
            }
        }

        Comparator<Integer> groupMemberComparator = Comparator
            .comparing((Integer serial) -> metadataBySerial.get(serial).remainingCapacity())
            .thenComparingInt(Integer::intValue);
        for (Map.Entry<Integer, List<Integer>> entry : pendingGroups.entrySet()) {
            List<Integer> group = entry.getValue();
            Integer firstSeen = group.getFirst();
            group.sort(groupMemberComparator);
            int placeholder = ordered.indexOf(firstSeen);
            ordered.remove(placeholder);
            ordered.addAll(placeholder, group);
        }

        return ordered;
    }
}

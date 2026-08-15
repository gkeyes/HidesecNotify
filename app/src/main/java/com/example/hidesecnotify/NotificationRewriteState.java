package com.example.hidesecnotify;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

final class NotificationRewriteState {
    private final Map<Object, Set<String>> rewrittenFields = new WeakHashMap<>();

    synchronized void updateField(Object builder, String field, boolean rewritten) {
        if (builder == null) {
            return;
        }

        Set<String> fields = rewrittenFields.get(builder);
        if (rewritten) {
            if (fields == null) {
                fields = new HashSet<>();
                rewrittenFields.put(builder, fields);
            }
            fields.add(field);
            return;
        }

        if (fields == null) {
            return;
        }
        fields.remove(field);
        if (fields.isEmpty()) {
            rewrittenFields.remove(builder);
        }
    }

    synchronized boolean hasRewrittenField(Object builder) {
        return builder != null && rewrittenFields.containsKey(builder);
    }
}

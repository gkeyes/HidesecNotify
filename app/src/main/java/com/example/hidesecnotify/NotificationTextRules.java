package com.example.hidesecnotify;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class NotificationTextRules {
    static final String TARGET_PACKAGE = "com.miui.securitycore";

    private static final String FAKE_PREFIX = "系统后台自动清理";
    private static final String FAKE_SUFFIX = "个文件";
    private static final Pattern SECOND_SPACE_NOTIFICATION =
            Pattern.compile("^手机分身中有(.+)条通知提醒$");

    private NotificationTextRules() {
    }

    static CharSequence rewrite(CharSequence original) {
        if (original == null) {
            return null;
        }

        Matcher matcher = SECOND_SPACE_NOTIFICATION.matcher(original.toString());
        if (!matcher.matches()) {
            return null;
        }

        return FAKE_PREFIX + matcher.group(1) + FAKE_SUFFIX;
    }

    static boolean isRewrittenText(CharSequence text) {
        if (text == null) {
            return false;
        }

        String value = text.toString();
        return value.startsWith(FAKE_PREFIX) && value.endsWith(FAKE_SUFFIX);
    }
}

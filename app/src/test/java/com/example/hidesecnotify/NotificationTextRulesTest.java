package com.example.hidesecnotify;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class NotificationTextRulesTest {
    @Test
    public void rewriteSecondSpaceNotification() {
        assertEquals(
                "系统后台自动清理3个文件",
                NotificationTextRules.rewrite("手机分身中有3条通知提醒"));
    }

    @Test
    public void keepsNonMatchingNotificationUntouched() {
        assertNull(NotificationTextRules.rewrite("系统安全提醒"));
        assertNull(NotificationTextRules.rewrite("手机分身中有3条未读消息"));
        assertNull(NotificationTextRules.rewrite("手机分身中有任意条通知提醒"));
        assertNull(NotificationTextRules.rewrite(null));
    }

    @Test
    public void hooksOnlySecurityCoreRemoteProcess() {
        assertTrue(NotificationTextRules.shouldHook(
                "com.miui.securitycore", "com.miui.securitycore.remote"));
        assertFalse(NotificationTextRules.shouldHook(
                "com.miui.securitycore", "com.miui.securitycore"));
        assertFalse(NotificationTextRules.shouldHook(
                "com.example.other", "com.miui.securitycore.remote"));
    }
}

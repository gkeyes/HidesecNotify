package com.example.hidesecnotify;

import android.app.Notification;
import android.os.Bundle;
import android.util.Log;

import java.lang.reflect.Method;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface;

public final class HidesecNotifyModule extends XposedModule {
    private static final String TAG = "HidesecNotify";

    @Override
    public void onModuleLoaded(XposedModuleInterface.ModuleLoadedParam param) {
        log(Log.INFO, TAG, "Module loaded in process: " + safeProcessName(param));
    }

    @Override
    public void onPackageLoaded(XposedModuleInterface.PackageLoadedParam param) {
        if (!NotificationTextRules.TARGET_PACKAGE.equals(param.getPackageName())) {
            return;
        }

        try {
            Class<?> builderClass = Class.forName(
                    "android.app.Notification$Builder",
                    false,
                    param.getDefaultClassLoader());

            hookTextSetter(builderClass, "setContentTitle");
            hookTextSetter(builderClass, "setContentText");
            hookTextSetter(builderClass, "setTicker");
            hookBuild(builderClass);

            log(Log.INFO, TAG, "Notification hooks registered");
        } catch (Throwable t) {
            log(Log.ERROR, TAG, "Hook registration failed", t);
        }
    }

    private void hookTextSetter(Class<?> builderClass, String methodName) throws NoSuchMethodException {
        Method method = builderClass.getDeclaredMethod(methodName, CharSequence.class);
        method.setAccessible(true);

        hook(method)
                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                .intercept(chain -> {
                    CharSequence replacement = NotificationTextRules.rewrite((CharSequence) chain.getArg(0));
                    if (replacement == null) {
                        return chain.proceed();
                    }

                    return chain.proceed(new Object[]{replacement});
                });
    }

    private void hookBuild(Class<?> builderClass) throws NoSuchMethodException {
        Method method = builderClass.getDeclaredMethod("build");
        method.setAccessible(true);

        hook(method)
                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                .intercept(chain -> {
                    Notification notification = (Notification) chain.proceed();
                    if (isRewrittenNotification(notification)) {
                        notification.contentIntent = null;
                    }
                    return notification;
                });
    }

    private boolean isRewrittenNotification(Notification notification) {
        if (notification == null) {
            return false;
        }
        if (NotificationTextRules.isRewrittenText(notification.tickerText)) {
            return true;
        }

        Bundle extras = notification.extras;
        if (extras == null) {
            return false;
        }

        return NotificationTextRules.isRewrittenText(extras.getCharSequence(Notification.EXTRA_TITLE))
                || NotificationTextRules.isRewrittenText(extras.getCharSequence(Notification.EXTRA_TEXT))
                || NotificationTextRules.isRewrittenText(extras.getCharSequence(Notification.EXTRA_BIG_TEXT))
                || containsRewrittenText(extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES));
    }

    private boolean containsRewrittenText(CharSequence[] values) {
        if (values == null) {
            return false;
        }
        for (CharSequence value : values) {
            if (NotificationTextRules.isRewrittenText(value)) {
                return true;
            }
        }
        return false;
    }

    private String safeProcessName(XposedModuleInterface.ModuleLoadedParam param) {
        try {
            return param.getProcessName();
        } catch (Throwable ignored) {
            return "<unknown>";
        }
    }
}

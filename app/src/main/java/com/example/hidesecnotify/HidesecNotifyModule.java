package com.example.hidesecnotify;

import android.app.Notification;
import android.util.Log;

import java.lang.reflect.Method;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface;

public final class HidesecNotifyModule extends XposedModule {
    private static final String TAG = "HidesecNotify";
    private final NotificationRewriteState rewriteState = new NotificationRewriteState();
    private String processName;

    @Override
    public void onModuleLoaded(XposedModuleInterface.ModuleLoadedParam param) {
        processName = safeProcessName(param);
        log(Log.INFO, TAG, "Module loaded in process: " + processName);
    }

    @Override
    public void onPackageLoaded(XposedModuleInterface.PackageLoadedParam param) {
        if (!NotificationTextRules.shouldHook(param.getPackageName(), processName)) {
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
                    Object builder = chain.getThisObject();
                    if (replacement == null) {
                        Object result = chain.proceed();
                        rewriteState.updateField(builder, methodName, false);
                        return result;
                    }

                    Object result = chain.proceed(new Object[]{replacement});
                    rewriteState.updateField(builder, methodName, true);
                    return result;
                });
    }

    private void hookBuild(Class<?> builderClass) throws NoSuchMethodException {
        Method method = builderClass.getDeclaredMethod("build");
        method.setAccessible(true);

        hook(method)
                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                .intercept(chain -> {
                    Notification notification = (Notification) chain.proceed();
                    if (notification != null
                            && rewriteState.hasRewrittenField(chain.getThisObject())) {
                        notification.contentIntent = null;
                    }
                    return notification;
                });
    }

    private String safeProcessName(XposedModuleInterface.ModuleLoadedParam param) {
        try {
            return param.getProcessName();
        } catch (Throwable ignored) {
            return "<unknown>";
        }
    }
}

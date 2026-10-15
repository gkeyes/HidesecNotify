package com.example.hidesecnotify;

import android.app.Notification;
import android.util.Log;

import java.lang.reflect.Method;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface;

/**
 * Entry point (see META-INF/xposed/java_init.list).
 *
 * <p>Inside the SecurityCore remote process, rewrites the "second space" notification text
 * set on {@link Notification.Builder} and drops the content intent of notifications whose
 * text was rewritten, so tapping them does nothing.
 */
public final class HidesecNotifyModule extends XposedModule {
    private static final String TAG = "HidesecNotify";
    private static final String BUILDER_CLASS = "android.app.Notification$Builder";
    private static final String[] TEXT_SETTERS = {"setContentTitle", "setContentText", "setTicker"};

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

        Class<?> builderClass = loadBuilderClass(param.getDefaultClassLoader());
        if (builderClass == null) {
            return;
        }

        // Register each hook on its own so one missing method does not skip the others.
        int registered = 0;
        for (String setter : TEXT_SETTERS) {
            if (tryHook(setter, () -> hookTextSetter(builderClass, setter))) {
                registered++;
            }
        }
        if (tryHook("build", () -> hookBuild(builderClass))) {
            registered++;
        }

        log(Log.INFO, TAG, "Notification hooks registered: " + registered + "/" + (TEXT_SETTERS.length + 1));
    }

    /** Rewrites a matching text argument and records on the builder whether it was rewritten. */
    private void hookTextSetter(Class<?> builderClass, String methodName) throws NoSuchMethodException {
        Method method = builderClass.getDeclaredMethod(methodName, CharSequence.class);
        method.setAccessible(true);

        hook(method)
                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                .intercept(chain -> {
                    Object arg = chain.getArg(0);
                    CharSequence replacement = arg instanceof CharSequence
                            ? NotificationTextRules.rewrite((CharSequence) arg)
                            : null;
                    Object builder = chain.getThisObject();

                    Object result = replacement == null
                            ? chain.proceed()
                            : chain.proceed(new Object[]{replacement});
                    rewriteState.updateField(builder, methodName, replacement != null);
                    return result;
                });
    }

    /** Clears the content intent of notifications built from a builder with rewritten text. */
    private void hookBuild(Class<?> builderClass) throws NoSuchMethodException {
        Method method = builderClass.getDeclaredMethod("build");
        method.setAccessible(true);

        hook(method)
                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                .intercept(chain -> {
                    Object result = chain.proceed();
                    if (result instanceof Notification
                            && rewriteState.hasRewrittenField(chain.getThisObject())) {
                        ((Notification) result).contentIntent = null;
                    }
                    return result;
                });
    }

    private Class<?> loadBuilderClass(ClassLoader classLoader) {
        try {
            return Class.forName(BUILDER_CLASS, false, classLoader);
        } catch (Throwable t) {
            log(Log.ERROR, TAG, "Cannot load " + BUILDER_CLASS, t);
            return null;
        }
    }

    private boolean tryHook(String methodName, HookRegistration registration) {
        try {
            registration.register();
            return true;
        } catch (Throwable t) {
            log(Log.ERROR, TAG, "Failed to hook Notification.Builder#" + methodName, t);
            return false;
        }
    }

    private static String safeProcessName(XposedModuleInterface.ModuleLoadedParam param) {
        try {
            return param.getProcessName();
        } catch (Throwable ignored) {
            return "<unknown>";
        }
    }

    @FunctionalInterface
    private interface HookRegistration {
        void register() throws Exception;
    }
}

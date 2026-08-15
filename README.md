# HidesecNotify

HidesecNotify is a modern LibXposed module for MIUI/HyperOS. It rewrites the Second Space notification text from `手机分身中有N条通知提醒` to `系统后台自动清理N个文件` and disables the click action only for notifications whose text was rewritten.

## Build

The project is intentionally small and uses the modern LibXposed API as a `compileOnly` dependency.

```bash
gradle assembleRelease
```

The GitHub Actions workflow installs Gradle and Android SDK packages, then builds and uploads the APK. Pushing a tag like `v1.0.0` also creates a GitHub Release.

## Scope

The module declares a static scope for `com.miui.securitycore`. At runtime it
registers notification hooks only in `com.miui.securitycore.remote` and tracks
the exact `Notification.Builder` whose Second Space text was rewritten, so
other SecurityCore notifications keep their click actions.

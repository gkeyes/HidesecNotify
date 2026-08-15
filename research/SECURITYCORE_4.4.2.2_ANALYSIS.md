# SecurityCoreAdd 4.4.2.2 analysis

## Sample

- File: `research/input/SecurityCoreAdd.apk`
- Size: `25,552,222` bytes
- SHA-256: `72afefd934c649e4613b3b2a0fc709ec4f0951e3ea8b35866c9f2ed089198c7e`
- Package: `com.miui.securitycore`
- Version: `4.4.2.2-4-260714` (`40004422`)
- Signature: APK Signature Scheme v3, Xiaomi/MIUI certificate

## Notification path

1. `SecondSpaceNotificationService` runs in `com.miui.securitycore.remote`.
2. Its notification listener observes notifications posted by the other Android
   user and increments a per-user counter. It does not replace or clear the
   source app notification's `PendingIntent`.
3. The service formats the owner-space summary title with
   `second_space_notification_format`, whose Simplified Chinese value is
   `手机分身中有%d条通知提醒`.
4. The service creates `com.miui.xspace.TO_CHANGE_USER` and wraps it with
   `PendingIntent.getService(...)`.
5. The shared notification factory passes that intent to
   `Notification.Builder.setContentIntent(...)`, then builds and posts the
   summary as notification ID `100`.

Relevant decompiled files:

- `research/decompiled/sources/com/miui/securityspace/service/notificaiton/SecondSpaceNotificationService.java`
- `research/decompiled/sources/Q2/a.java`
- `research/decompiled/sources/y2/a.java`
- `research/decompiled/resources/res/values-zh-rCN/plurals.xml`
- `research/decompiled/resources/AndroidManifest.xml`

## Diagnosis

The sampled APK does not support the hypothesis that HidesecNotify can directly
clear another app process's notification click action: the module's static scope
is `com.miui.securitycore`, while source-app notifications are only observed by
SecurityCore's listener.

The previous module implementation still had an avoidable false-positive path:
after `Notification.Builder.build()`, it inferred provenance from the final text.
Any SecurityCore notification matching the replacement prefix and suffix could
therefore lose its click action even if this module had not rewritten it.

## Adaptation

- Track the exact `Notification.Builder` and setter field that the module
  rewrote; only that builder loses `contentIntent`.
- Remove the provenance check based on final notification text.
- Register hooks only in `com.miui.securitycore.remote`, the process declared by
  this APK for `SecondSpaceNotificationService`.
- Match only the numeric `%d` shape used by the APK resource.

This analysis is static. It does not claim LSPosed activation or on-device
notification behavior was verified.

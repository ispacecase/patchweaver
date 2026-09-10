# patchweaver

Personal [Morphe](https://morphe.software) patches, laid out per the official
[morphe-patches-template](https://github.com/MorpheApp/morphe-patches-template).

## What's included

**Selectable install backend** — adds a setting to F-Droid (`org.fdroid.fdroid` 1.23.2,
versionCode `1023052`) to choose how APKs get installed:

- System / F-Droid default (unchanged behavior)
- [Shizuku](https://shizuku.rikka.app) — silent install via `pm install`, no confirmation UI
- InstallerX — routed via an explicit `ACTION_INSTALL_PACKAGE` intent
- A custom installer package, picked from a list of installed apps that actually handle that
  intent

The hook is `InstallerFactory.create(Context, App, Apk)` — the single point where F-Droid
normally picks between `FileInstaller` / `PrivilegedInstaller` / `SessionInstaller` /
`DefaultInstaller`. `SessionInstaller` calls Android's `PackageInstaller` session APIs directly
and bypasses any external install-intent handler, so the override has to happen at that
selection point rather than further downstream.

## Project layout

```
patches/
  src/main/kotlin/app/fdroidbackends/InstallBackendsPatch.kt
stub/
  # Compile-time-only stand-ins for org.fdroid.fdroid.{installer,data} classes. Installer's
  # constructor and abstract methods are package-private/protected, so the extension classes
  # that subclass it have to live in that exact package to link — this module lets them compile
  # against the real signatures without bundling a real F-Droid class.
extensions/
  extension/
    src/main/java/org/fdroid/fdroid/installer/...   # same package as the real Installer class
    src/main/java/app/fdroidbackends/...             # everything else (prefs, settings UI,
                                                       # Shizuku service)
apply-tool/
  # Applies a .mpp to an APK and signs it — see below.
```

## Building

This repo resolves `app.morphe.patches` (the Gradle plugin) and `app.morphe:morphe-patcher` from
local sibling checkouts instead of GitHub Packages, which requires authentication even for
public packages:

```
some-parent-dir/
  patchweaver/                       (this repo)
  morphe-patches-gradle-plugin/      git clone https://github.com/MorpheApp/morphe-patches-gradle-plugin
  morphe-patcher/                    git clone https://github.com/MorpheApp/morphe-patcher
```

Then:

```bash
./gradlew :patches:buildAndroid
```

produces `patches/build/libs/patches-1.0.0.mpp`.

## Applying the patch

Load the `.mpp` into [Morphe Manager](https://github.com/MorpheApp/morphe-manager) or
[Morphe Desktop](https://github.com/MorpheApp/morphe-desktop) if you have either set up.

Otherwise, `apply-tool` is a small standalone runner (there's no Morphe CLI to lean on) that
applies the patch and signs the result:

```bash
./gradlew :apply-tool:installDist

# Generate a signing key once, if you don't have one:
keytool -genkeypair -keystore debug.keystore -alias fdroidbackends \
  -keyalg RSA -keysize 2048 -validity 10000 -storepass fdroidbackends -keypass fdroidbackends \
  -dname "CN=Morphe F-Droid Backends"

./apply-tool/build/install/apply-tool/bin/apply-tool \
  path/to/org.fdroid.fdroid.apk \
  patches/build/libs/patches-1.0.0.mpp \
  fdroid-patched.apk \
  debug.keystore fdroidbackends fdroidbackends fdroidbackends
```

Installing `fdroid-patched.apk` requires uninstalling any existing F-Droid signed with a
different key first (normal Android behavior for a different signing cert).

## Status

Verified on-device (Shizuku Plus + a real InstallerX build): resource/manifest patch applies,
fingerprint matches, the extension DEX links against the real app classes, and the Shizuku
install path works end-to-end (permission prompt, bind, `pm install`). InstallerX/custom-package
routing is implemented and installs correctly but is still lightly tested. Uninstall flows,
notification edge cases, and Private Space are untouched.

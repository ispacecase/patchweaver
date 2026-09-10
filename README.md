# 👋🧩 patchweaver

Personal collection of [Morphe](https://morphe.software) patches. F-Droid is the first one in
here — more may get added over time as I patch other apps I use.

## ❓ About

Patches for apps I like.

### How to use these patches

Click here to add these patches to Morphe: https://morphe.software/add-source?github=ispacecase/patchweaver

## 🩹 Patches list

<!-- PATCHES_START EXPANDED -->

<!-- Do not modify this section by hand. The patch list is generated when release.yml creates a
     new release. -->

#### A list of patches will automatically be shown here after the first release is created.

Until then: **Selectable install backend** (F-Droid, `org.fdroid.fdroid` 1.23.2) — adds a
setting to choose how F-Droid installs APKs: system default, Shizuku (silent, no confirmation
UI), InstallerX, or a custom installer package picked from a list of installed apps.

<!-- PATCHES_END -->

### 🛠️ Building locally

This repo resolves `app.morphe.patches` (the Gradle plugin) and `app.morphe:morphe-patcher` from
GitHub Packages, which requires an authenticated GitHub token even for public packages:

```bash
export GITHUB_TOKEN="$(gh auth token)"
export GITHUB_ACTOR="$(gh api user --jq .login)"
```

(a token needs the `read:packages` scope — `gh auth refresh -s read:packages` adds it). A
`flake.nix`/`.envrc` are included for `nix develop`/`direnv` users, which export these
automatically.

- Run `./gradlew buildAndroid`
- The built patches `.mpp` file is found in `patches/build/libs/patches-*.mpp`
- Patch the `.mpp` file using [Morphe Desktop](https://github.com/MorpheApp/morphe-desktop) like
  any other patch bundle, or with `apply-tool` in this repo if you don't have that set up:

```bash
./gradlew :apply-tool:installDist

# Generate a signing key once, if you don't have one:
keytool -genkeypair -keystore debug.keystore -alias patchweaver \
  -keyalg RSA -keysize 2048 -validity 10000 -storepass patchweaver -keypass patchweaver \
  -dname "CN=patchweaver"

./apply-tool/build/install/apply-tool/bin/apply-tool \
  path/to/input.apk patches/build/libs/patches-*.mpp output.apk \
  debug.keystore patchweaver patchweaver patchweaver
```

See the [Morphe documentation](https://github.com/MorpheApp/morphe-documentation) for more
information.

## 📜 License

patchweaver is licensed under the [GNU General Public License v3.0](LICENSE), with additional
conditions under GPLv3 Section 7 — see [NOTICE](NOTICE).

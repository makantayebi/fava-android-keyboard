# fava-android (repo: fava-android-keyboard)

Android IME (keyboard app) for the Fava Persian layout. The app is
currently at `v0.1.0`, built from the `v0.0.5` script — see "Versioning"
below for why those numbers differ.

## Source of truth

The keyboard layouts — `app/src/main/res/xml/avayi_keys.xml` and
`avayi_keys_shifted.xml` — are generated from
[`fava-standard`](https://github.com/makantayebi/fava-standard)'s
`versions/v0.0.5/layout.yaml` by its `tools/render.py`, same as the
Linux implementation. Don't hand-edit them — update `layout.yaml`,
re-run the generator, and copy the two files over:

```bash
cd fava-standard
python3 tools/render.py v0.0.5
cp versions/v0.0.5/generated/avayi_keys.xml         ../fava-android-keyboard/app/src/main/res/xml/
cp versions/v0.0.5/generated/avayi_keys_shifted.xml ../fava-android-keyboard/app/src/main/res/xml/
```

Unlike the Linux/XKB side, Android needs no Compose-style workaround for
multi-character keys (shift+Q/W/E's اُ/اِ/اَ, B's وْ) —
`android:keyOutputText` accepts any string directly, so
`FavaInputMethodService.onText()` is one generic handler for every
character key, no per-key logic.

Two things here are **Android-specific rendering choices**, not part of
the universal script — the underlying `layout.yaml` is unaffected:
- The `{`/`}`/`\` cluster (`AD11`, `AD12`, `BKSL`) is hidden —
  `ANDROID_HIDE_KEYS` in `tools/render.py` — a touchscreen doesn't need
  rarely-used punctuation eating into row-2 space the way a physical
  keyboard's dedicated keys do.
- `ANDROID_KEY_LABEL` shows the Persian name نیم‌فاصله for ZWNJ's key,
  not the diagram's Latin abbreviation — a phone key has room for it.

## Globe key

The bottom row includes a 🌐 key (code 11) between shift and space, same
role as Gboard's own globe key: it hands off to Android's system IME
switcher rather than the app owning any switcher UI itself —
`switchToNextInputMethod(false)` on API 28+, falling back to
`InputMethodManager.showInputMethodPicker()` on older devices. See
`FavaInputMethodService.switchToNextIme()`.

## Versioning

This app has its own `versionName`/`versionCode`
(`app/build.gradle.kts`), tracked in `fava-standard/targets.yaml` as
this target's `app_version` — separate from `script_version`, which is
the `fava-standard` layout version (`layout.yaml`) this app's keyboard
XML was generated from. The globe key is an Android-only UI feature
with no effect on `layout.yaml`, so it bumped `app_version` to `0.1.0`
without touching `script_version` (still `0.0.5`).

## Shift is momentary, not a lock

`FavaInputMethodService.kt` applies shift to exactly the next character
typed, then reverts automatically — like a real keyboard's shift, not
caps-lock. Handled entirely in `onKey`/`onText`; the XML key itself
deliberately omits `isSticky`.

## Building

```bash
./gradlew assembleDebug      # app/build/outputs/apk/debug/fava-<version>.apk
./gradlew assembleRelease    # signed, if keystore.properties exists — see below
```

Both commands apply `tools/render.py`'s custom output naming, so the
APK itself is named `fava-0.0.5.apk`, not `app-debug.apk`.

Built and verified on an emulator before being committed — not just
"it compiled": installed, enabled as the active IME, and the
committed text confirmed key-by-key (regular keys, the multi-character
shift+Q/W/E and B keys, and the momentary-shift revert behavior) via
`uiautomator dump`, not just a screenshot.

## Release signing

`fava-release.keystore` (git-ignored, **back this up yourself** — losing
it means no future release build can ever be signed as the same app
again) and `keystore.properties` (also git-ignored) hold the real
signing key. `app/build.gradle.kts` reads the keystore path/alias/
passwords from `keystore.properties` if present; `assembleRelease`
produces an unsigned build if it's missing instead of failing, so a
fresh clone still builds for local testing.

## Known limitation

Built on `android.inputmethodservice.Keyboard`/`KeyboardView` — the
classic, long-standing IME framework, which still works fine but is
marked deprecated in favor of fully custom views. Fine for now; a
future version could move to a custom-drawn keyboard view instead.

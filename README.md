# fava-android (repo: fava-android-keyboard)

Android IME (keyboard app) for the Fava Persian layout. The app is
currently at `v0.2.5`, built from the Android-specific layout fork
`layouts/android/v0.0.6` — see "Versioning" below for why these numbers
are all independent of each other.

## Source of truth

The keyboard layouts — `app/src/main/res/xml/avayi_keys.xml` and
`avayi_keys_shifted.xml` — are generated from
[`fava-standard`](https://github.com/makantayebi/fava-standard)'s
`layouts/android/v0.0.6/layout.yaml` by its `tools/render.py --target
android`. This is **not** the same file the Linux/XKB implementation
uses — Android has its own fork of the layout, independently versioned,
because some changes make sense for a touchscreen keyboard and not a
physical one (see "Action keys" below for the first example). See
`fava-standard/layouts/README.md` for the full base-script-vs-fork
model. Don't hand-edit the generated XML — update
`layouts/android/v0.0.6/layout.yaml`, re-run the generator, and copy
the two files over:

```bash
cd fava-standard
python3 tools/render.py v0.0.6 --target android
cp layouts/android/v0.0.6/generated/avayi_keys.xml         ../fava-android-keyboard/app/src/main/res/xml/
cp layouts/android/v0.0.6/generated/avayi_keys_shifted.xml ../fava-android-keyboard/app/src/main/res/xml/
```

Unlike the Linux/XKB side, Android needs no Compose-style workaround for
multi-character keys (shift+Q/W/E's اُ/اِ/اَ, B's وْ) —
`android:keyOutputText` accepts any string directly, so
`FavaInputMethodService.onText()` is one generic handler for every
character key, no per-key logic.

## Action keys (not every key types a character)

As of `layouts/android/v0.0.6`, a key's `base`/`shift` can be the
literal value `"BACKSPACE"` instead of a `"U+XXXX"` codepoint — a key
that performs an action instead of typing text (the `/` key, `AB10`, is
the first one). `tools/render.py` checks for this explicitly at each
render site rather than teaching `cp_list()` about it, since "what
codepoints does this type" isn't the right question once a key isn't
typing anything. On Android it becomes
`android:codes="-5" android:keyIcon="@android:drawable/ic_input_delete"`
— the same code `FavaInputMethodService.onKey()` already handles for
the function row's own delete key, so no new Kotlin was needed. This
is a real key-mapping change (it lives in `layout.yaml`, forked), not
just a rendering choice — a touchscreen benefits from an easy-to-reach
second backspace in a way a physical keyboard doesn't, which is why it
diverges from the base script instead of changing it universally.

Two things below remain **pure rendering choices** within the Android
target's renderer, not key-mapping changes — they'd apply the same way
whether Android were still tracking the base script or forking it:
- The `{`/`}`/`\` cluster (`AD11`, `AD12`, `BKSL`) is hidden —
  `ANDROID_HIDE_KEYS` in `tools/render.py` — a touchscreen doesn't need
  rarely-used punctuation eating into row-2 space the way a physical
  keyboard's dedicated keys do.
- `ANDROID_KEY_LABEL` shows a compact symbol (◂┆▸) for ZWNJ's key — the
  real Persian term نیم‌فاصله, even wrapped to two lines, was too wide
  for a ~10%-width key at the keyboard's label text size.

## Globe key

The bottom row includes a 🌐 key (code 11) between shift and space, same
role as Gboard's own globe key: it hands off to Android's system IME
switcher via `InputMethodManager.showInputMethodPicker()` rather than
the app owning any switcher UI itself. See
`FavaInputMethodService.switchToNextIme()`. For the switcher to be
reachable at all, `res/xml/method.xml` must declare
`android:supportsSwitchingToNextInputMethod="true"` — without it the
system silently refuses to switch.

## Versioning

Three independent version numbers, tracked in `fava-standard/targets.yaml`:

- **`app_version`** — this app's own `versionName`/`versionCode`
  (`app/build.gradle.kts`). Moves on any change to the app itself —
  UI, bug fixes, the globe key — regardless of whether `layout.yaml`
  was involved at all.
- **`layout_version`** — which version of *this target's own fork*
  (`fava-standard/layouts/android/`) the keyboard XML was generated
  from. Moves only when the actual key mapping changes for Android
  specifically (like the backspace key above).
- **`forked_from_script_version`** — which base `fava-standard`
  `versions/` release this fork diverged from. Doesn't move on its
  own; it's a record of where Android's fork started, so you can tell
  what base-script changes (if any, in the future) it might be missing.

The globe key and the boundary-overflow fixes only ever touched
`app_version` (`0.1.0` → `0.2.5`) — no `layout.yaml` involved. The
backspace key is the first change to touch `layout_version` (`0.0.6`,
forked from base `0.0.5`).

## Keyboard must stay within screen bounds

`android:keyWidth` percentages in a row already sum to 100% — any
non-zero `android:horizontalGap`/`verticalGap` on the `<Keyboard>` root
is *added on top* of that, not subtracted from it. A `4dp`/`6dp` gap
tried briefly for a "floating key" look overflowed the screen on all
sides (worst on row 1's 13 keys: 12 internal gaps × 4dp), and pushed
the bottom row down far enough to render under the system nav bar.
Gaps are back to `0px`; the floating-key look instead comes from
`<inset>` padding baked into `key_bg_normal.xml`/`key_bg_pressed.xml`,
which doesn't affect row width/height math at all. Also added
`setDecorFitsSystemWindows(true)` in `onCreateInputView()` — targetSdk
35 forces edge-to-edge by default, which otherwise lets the IME window
draw under the nav bar instead of being inset above it.

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

Both commands apply a custom output naming based on `versionName`, so
the APK itself is named `fava-0.1.1.apk`, not `app-debug.apk`.

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

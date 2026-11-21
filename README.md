# fava-android (repo: fava-android-keyboard)

Android IME (keyboard app) for the Fava Persian layout. The app is
currently at `v0.3.1`, built from the Android-specific layout fork
`layouts/android/v0.0.8` — see "Versioning" below for why these numbers
are all independent of each other.

## Source of truth

The keyboard layouts — `app/src/main/res/xml/avayi_keys.xml`,
`avayi_keys_shifted.xml`, and `avayi_keys_symbols.xml` (the ۱۲۳ page,
see "Three pages" below) — are generated from
[`fava-standard`](https://github.com/makantayebi/fava-standard)'s
`layouts/android/v0.0.8/layout.yaml` by its `tools/render.py --target
android`. This is **not** the same file the Linux/XKB implementation
uses — Android has its own fork of the layout, independently versioned,
because some changes make sense for a touchscreen keyboard and not a
physical one (see "Action keys" below for the first example). See
`fava-standard/layouts/README.md` for the full base-script-vs-fork
model. Don't hand-edit the generated XML — update
`layouts/android/v0.0.8/layout.yaml`, re-run the generator, and copy
the three files over:

```bash
cd fava-standard
python3 tools/render.py v0.0.8 --target android
cp layouts/android/v0.0.8/generated/avayi_keys.xml         ../fava-android-keyboard/app/src/main/res/xml/
cp layouts/android/v0.0.8/generated/avayi_keys_shifted.xml ../fava-android-keyboard/app/src/main/res/xml/
cp layouts/android/v0.0.8/generated/avayi_keys_symbols.xml ../fava-android-keyboard/app/src/main/res/xml/
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

`v0.0.7` adds a second action sentinel, `"SHIFT"` — the physical shift
control itself can now live anywhere in the main key grid instead of
only ever being hardcoded into the Android function row. `AB01` is
`"SHIFT"` as of this version (shift relocated there from the function
row; see "Relocated keys" below) — `render_android_keyboard` emits the
same `android:codes="-1" android:isModifier="true"` key at that grid
position instead.

Three things below remain **pure rendering choices** within the
Android target's renderer, not key-mapping changes — they'd apply the
same way whether Android were still tracking the base script or
forking it:
- The `}`/`\` pair (`AD12`, `BKSL`) is hidden — `ANDROID_HIDE_KEYS` in
  `tools/render.py` — a touchscreen doesn't need rarely-used
  punctuation eating into row space the way a physical keyboard's
  dedicated keys do. `AD11` (originally paired with these as `{`/`[`)
  is shown again as of `v0.0.8`, repurposed entirely for ج (see
  "Top row removed" below) — its original `{`/`[` values are gone on
  Android, not relocated anywhere.
- `AB09` (the dot key) is hidden too, as of `v0.0.7` — it only ever
  typed `.`, already reachable via `shift+AB06` (د) since `v0.0.6`, so
  removing it frees up space for the relocated shift key without the
  row getting cramped.
- `ANDROID_KEY_LABEL` shows a compact symbol (◂┆▸) for ZWNJ's key — the
  real Persian term نیم‌فاصله, even wrapped to two lines, was too wide
  for a ~10%-width key at the keyboard's label text size.

## Relocated keys (v0.0.7)

Shift moves from the function row to `AB01` (left of ژ); ZWNJ, which
used to live at `AB01`, moves to the function row instead (right of
space). ZWNJ's codepoint for the function row comes from a new
non-physical pseudo-key, `FUNCROW_ZWNJ`, in `layout.yaml` — not
hardcoded in `tools/render.py` — so the file stays the one source of
truth for what character ends up where, even off the main grid.

The function row's own dedicated delete key is removed in the same
version — redundant now that `AB10` is a backspace (`v0.0.6`) and
shift no longer needs a function-row slot to vacate.

## Three pages: base, shift, ۱۲۳ (v0.0.7)

A key's `layout.yaml` spec can now carry a third level, `symbols` —
same "omitted = same as base" fallback convention already used for
`shift`, extended one level further.
`render_android_keyboard(layout, page=...)` takes `"base"`, `"shift"`,
or `"symbols"` instead of a `shifted` boolean, and `avayi_keys_symbols.xml`
is generated the same way as the other two pages. Only the `AD` and
`AC` rows have `symbols` values set as of `v0.0.7` (digits and
punctuation respectively) — `AE` (already digits), `AB`, and the
function row are identical to the base page.

The ۱۲۳ key sits where shift used to (function row, leftmost slot,
code 12) — `FavaInputMethodService.toggleSymbols()` switches to
`symbolsKeyboard` and flips the key's own label to `ABC` (baked
directly into `avayi_keys_symbols.xml`, not computed at runtime).
Unlike shift, this toggle is **sticky**, not momentary — it stays on
the symbols page until pressed again, matching every other mobile
keyboard's own 123/ABC key. Known limitation: shift is still present
at `AB01` on the symbols page too (since the `AB` row doesn't change
there), and pressing it jumps to the shifted *letters* page rather
than a "shifted symbols" page that doesn't exist — no separate shifted
variant of the symbols page was requested.

## Top row removed, ی/یْ, and the ۱۲۳ page reshuffled (v0.0.8)

The entire top row (`TLDE` + `AE01`-`AE12`) is hidden — nothing it
typed is lost: the digits were already on the ۱۲۳ page, `×` already
had a home there too, and `÷` moved to the ۱۲۳ page at `X`/ژ. Row `AD`
(now the visual top row) gained an 11th key: `U` is ع, and `AD11`
(previously hidden, paired with `{`/`[`) is shown again holding ج,
displaced from `U`.

Android adopts the base script's `v0.0.6` ی/یْ split (see
`fava-standard/versions/v0.0.6/notes.md`) but **swaps which physical
key gets which reading**: the base script puts یْ (the /j/ consonant)
on `Y`/`AD06` and ی (the /i/ vowel) on `D`/`AC03`; Android does the
reverse. An intentional, explicit divergence — not a copy error.

The ۱۲۳ page went through a large, deliberate reshuffle — currency
signs (`€` `¥` `﷼` `$`), Persian-specific punctuation (`٪` `؍` `«` `»`),
and math symbols (`+` `=` `-`) each landed through an explicit chain of
relocations (e.g. `$` was at `F`, moved to `C` to make room for `÷`
moved from `X` to make room for `€`) rather than being placed once and
left. See `layouts/android/v0.0.8/layout.yaml` in `fava-standard` for
the full per-key reasoning — every relocation has a comment explaining
where its value came from and why.

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
backspace key (`0.0.6`), the shift/ZWNJ relocation + ۱۲۳ page
(`0.0.7`), and the top-row removal + ی/یْ adoption + ۱۲۳ reshuffle
(`0.0.8`, now forked from base `0.0.6` rather than `0.0.5`) are the
changes that moved `layout_version`.

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
the APK itself is named `fava-0.3.1.apk`, not `app-debug.apk`.

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

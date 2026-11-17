# fava-android

Android IME (keyboard app) for the Fava Persian layout (currently `v0.0.4`).

## Source of truth

The keyboard layouts — `app/src/main/res/xml/avayi_keys.xml` and
`avayi_keys_shifted.xml` — are generated from
[`fava-standard`](https://github.com/makantayebi/fava-standard)'s
`versions/v0.0.4/layout.yaml` by its `tools/render.py`, same as the
Linux implementation. Don't hand-edit them — update `layout.yaml`,
re-run the generator, and copy the two files over:

```bash
cd fava-standard
python3 tools/render.py v0.0.4
cp versions/v0.0.4/generated/avayi_keys.xml      ../fava-android-keyboard/app/src/main/res/xml/
cp versions/v0.0.4/generated/avayi_keys_shifted.xml ../fava-android-keyboard/app/src/main/res/xml/
```

Unlike the Linux/XKB side, Android needs no Compose-style workaround for
the multi-character shift+Q/W/E keys (اُ/اِ/اَ) — `android:keyOutputText`
accepts any string directly, so `FavaInputMethodService.onText()` is one
generic handler for every character key, no per-key logic.

## Building

```bash
./gradlew assembleDebug
```
Output: `app/build/outputs/apk/debug/app-debug.apk`. Built and verified
(installed on an emulator, enabled as the active IME, confirmed correct
committed text key-by-key — including the shift+Q/W/E multi-character
behavior) before this was committed.

## Known limitation

Built on `android.inputmethodservice.Keyboard`/`KeyboardView` — the
classic, long-standing IME framework, which still works fine but is
marked deprecated in favor of fully custom views. Fine for now; a
future version could move to a custom-drawn keyboard view instead.

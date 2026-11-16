# fava-android

Android IME (keyboard app) for the Fava Persian layout.

**Status: not started.** No code yet.

## When work starts here

The authoritative key mapping lives in
[`fava-standard`](https://github.com/makantayebi/fava-standard) — build the IME's
key map from the latest `versions/<x>/layout.yaml` there rather than
hand-copying from the Windows or Ubuntu implementations, which have their
own platform-specific quirks (e.g. Windows pads some diacritic keys with
a leading space that Android's own IME text composition almost certainly
shouldn't replicate).

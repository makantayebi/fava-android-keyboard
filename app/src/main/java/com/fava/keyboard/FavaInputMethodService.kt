package com.fava.keyboard

import android.inputmethodservice.InputMethodService
import android.inputmethodservice.Keyboard
import android.inputmethodservice.KeyboardView
import android.view.KeyEvent
import android.view.View

/**
 * IME for the Fava Persian layout. Both keyboard pages (avayi_keys.xml,
 * avayi_keys_shifted.xml) are generated from fava-standard's
 * versions/v0.0.5/layout.yaml by tools/render.py — this class only
 * handles the few function keys (shift/delete/enter); every character
 * key carries its own android:keyOutputText and is handled generically
 * by onText() below, no per-key logic needed.
 */
class FavaInputMethodService : InputMethodService(), KeyboardView.OnKeyboardActionListener {

    private lateinit var keyboardView: KeyboardView
    private lateinit var baseKeyboard: Keyboard
    private lateinit var shiftedKeyboard: Keyboard

    // Momentary shift, like a real keyboard — shift applies to exactly the
    // next character, then reverts. Not a caps-lock-style toggle.
    private var shiftActive = false

    override fun onCreateInputView(): View {
        baseKeyboard = Keyboard(this, R.xml.avayi_keys)
        shiftedKeyboard = Keyboard(this, R.xml.avayi_keys_shifted)

        keyboardView = layoutInflater.inflate(R.layout.input_view, null) as KeyboardView
        keyboardView.keyboard = baseKeyboard
        keyboardView.setOnKeyboardActionListener(this)
        return keyboardView
    }

    override fun onStartInputView(info: android.view.inputmethod.EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        setShift(false)
    }

    private fun setShift(active: Boolean) {
        shiftActive = active
        keyboardView.keyboard = if (active) shiftedKeyboard else baseKeyboard
        keyboardView.isShifted = active
        keyboardView.invalidateAllKeys()
    }

    override fun onKey(primaryCode: Int, keyCodes: IntArray?) {
        val ic = currentInputConnection ?: return
        when (primaryCode) {
            Keyboard.KEYCODE_DELETE -> ic.deleteSurroundingText(1, 0)
            Keyboard.KEYCODE_SHIFT -> setShift(!shiftActive)
            ENTER_CODE -> ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
        }
    }

    override fun onText(text: CharSequence?) {
        if (text.isNullOrEmpty()) return
        currentInputConnection?.commitText(text, 1)
        if (shiftActive) setShift(false)  // momentary — one character, then back to base
    }

    override fun onPress(primaryCode: Int) {}
    override fun onRelease(primaryCode: Int) {}
    override fun swipeLeft() {}
    override fun swipeRight() {}
    override fun swipeDown() {}
    override fun swipeUp() {}

    companion object {
        private const val ENTER_CODE = 10
    }
}

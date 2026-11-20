package com.fava.keyboard

import android.graphics.Rect
import android.inputmethodservice.InputMethodService
import android.inputmethodservice.Keyboard
import android.inputmethodservice.KeyboardView
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * IME for the Fava Persian layout. Both keyboard pages (avayi_keys.xml,
 * avayi_keys_shifted.xml) are generated from fava-standard's
 * layouts/android/v0.0.6/layout.yaml (an Android-specific fork of the
 * base script, not versions/ — see fava-standard/layouts/README.md) by
 * tools/render.py --target android — this class only
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

        // targetSdk 35 forces edge-to-edge by default, which otherwise lets
        // this window draw under the nav bar — setDecorFitsSystemWindows alone
        // doesn't reliably stop that for an IME-type window, so pad the view
        // by the nav bar's own height directly, on top of the XML's 6dp.
        val basePaddingBottom = keyboardView.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(keyboardView) { v, insets ->
            val navBar = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            v.setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, basePaddingBottom + navBar.bottom)
            insets
        }
        return keyboardView
    }

    override fun onStartInputView(info: android.view.inputmethod.EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        setShift(false)

        // Keys at the bottom corners otherwise fall inside the system's
        // back/home gesture-nav detection zone, which swallows the touch
        // before it reaches onKey — exclude the keyboard's own view from that.
        keyboardView.post {
            keyboardView.systemGestureExclusionRects = listOf(Rect(0, 0, keyboardView.width, keyboardView.height))
        }
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
            GLOBE_CODE -> switchToNextIme()
        }
    }

    // Same role as Gboard's own globe key: hand off to the system's IME
    // switcher rather than owning a UI for it ourselves. showInputMethodPicker()
    // (not switchToNextInputMethod) because the latter silently no-ops in some
    // environments instead of cycling — this always surfaces a visible picker.
    private fun switchToNextIme() {
        (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker()
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
        private const val GLOBE_CODE = 11
    }
}

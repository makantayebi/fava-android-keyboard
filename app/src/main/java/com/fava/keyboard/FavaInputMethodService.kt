package com.fava.keyboard

import android.graphics.Rect
import android.inputmethodservice.InputMethodService
import android.inputmethodservice.Keyboard
import android.inputmethodservice.KeyboardView
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * IME for the Fava Persian layout. Three keyboard pages (avayi_keys.xml,
 * avayi_keys_shifted.xml, avayi_keys_symbols.xml) are generated from
 * fava-standard's layouts/android/v0.0.10/layout.yaml (an Android-specific
 * fork of the base script, not versions/ — see fava-standard/layouts/README.md)
 * by tools/render.py --target android — this class only
 * handles the few function keys (shift/delete/enter/the ۱۲۳ toggle);
 * every character key carries its own android:keyOutputText and is handled
 * generically by onText() below, no per-key logic needed.
 *
 * The emoji picker (EMOJI_CODE) is a different kind of thing entirely — a
 * Gboard-style scrollable, categorized panel (emoji_view.xml + EmojiData.kt),
 * not a rendered Keyboard page, so it's built here directly rather than via
 * render.py/layout.yaml.
 */
class FavaInputMethodService : InputMethodService(), KeyboardView.OnKeyboardActionListener {

    private lateinit var keyboardView: KeyboardView
    private lateinit var baseKeyboard: Keyboard
    private lateinit var shiftedKeyboard: Keyboard
    private lateinit var symbolsKeyboard: Keyboard
    private lateinit var emojiPanel: View

    // Exactly one of these four is ever showing. SHIFT is momentary, like a
    // real keyboard — applies to exactly the next character, then reverts
    // to BASE automatically. SYMBOLS and EMOJI are sticky, like every other
    // mobile keyboard's own 123/emoji key — they stay up until pressed again.
    private enum class Page { BASE, SHIFT, SYMBOLS, EMOJI }
    private var currentPage = Page.BASE

    override fun onCreateInputView(): View {
        baseKeyboard = Keyboard(this, R.xml.avayi_keys)
        shiftedKeyboard = Keyboard(this, R.xml.avayi_keys_shifted)
        symbolsKeyboard = Keyboard(this, R.xml.avayi_keys_symbols)

        val root = layoutInflater.inflate(R.layout.input_view, null)
        keyboardView = root.findViewById(R.id.keyboard_view)
        keyboardView.keyboard = baseKeyboard
        keyboardView.setOnKeyboardActionListener(this)

        emojiPanel = root.findViewById(R.id.emoji_panel)
        setupEmojiPanel(root)

        // targetSdk 35 forces edge-to-edge by default, which otherwise lets
        // this window draw under the nav bar — setDecorFitsSystemWindows alone
        // doesn't reliably stop that for an IME-type window, so pad the view
        // by the nav bar's own height directly, on top of the XML's 6dp.
        // Applied to the root (not just keyboardView) since the emoji panel
        // needs the same nav-bar clearance when it's the visible one.
        val basePaddingBottom = root.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val navBar = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            v.setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, basePaddingBottom + navBar.bottom)
            insets
        }
        return root
    }

    private fun setupEmojiPanel(root: View) {
        val tabsRow = root.findViewById<LinearLayout>(R.id.emoji_tabs)
        val content = root.findViewById<LinearLayout>(R.id.emoji_content)
        val scroll = root.findViewById<ScrollView>(R.id.emoji_scroll)
        val backToKeyboard = root.findViewById<TextView>(R.id.emoji_back_to_keyboard)
        val backspaceBtn = root.findViewById<ImageButton>(R.id.emoji_backspace)

        val density = resources.displayMetrics.density
        val columns = 8

        for (category in EMOJI_CATEGORIES) {
            val header = TextView(this).apply {
                text = category.icon
                textSize = 14f
                setTextColor(0xFF999999.toInt())
                setPadding((8 * density).toInt(), (8 * density).toInt(), (8 * density).toInt(), (4 * density).toInt())
            }
            content.addView(header)

            val tab = TextView(this).apply {
                text = category.icon
                textSize = 20f
                gravity = Gravity.CENTER
                setPadding((14 * density).toInt(), 0, (14 * density).toInt(), 0)
                setOnClickListener { scroll.scrollTo(0, header.top) }
            }
            tabsRow.addView(tab)

            var row: LinearLayout? = null
            category.emoji.forEachIndexed { i, emoji ->
                if (i % columns == 0) {
                    row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
                    content.addView(row)
                }
                val cell = TextView(this).apply {
                    text = emoji
                    textSize = 22f
                    gravity = Gravity.CENTER
                    layoutParams = LinearLayout.LayoutParams(0, (44 * density).toInt(), 1f)
                    setOnClickListener {
                        currentInputConnection?.commitText(emoji, 1)
                        keyboardView.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    }
                }
                row?.addView(cell)
            }
        }

        backToKeyboard.setOnClickListener { toggleEmoji() }
        backspaceBtn.setOnClickListener {
            currentInputConnection?.deleteSurroundingText(1, 0)
            keyboardView.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }

    override fun onStartInputView(info: android.view.inputmethod.EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        showPage(Page.BASE)

        // Keys at the bottom corners otherwise fall inside the system's
        // back/home gesture-nav detection zone, which swallows the touch
        // before it reaches onKey — exclude the keyboard's own view from that.
        keyboardView.post {
            keyboardView.systemGestureExclusionRects = listOf(Rect(0, 0, keyboardView.width, keyboardView.height))
        }
    }

    private fun showPage(page: Page) {
        currentPage = page
        if (page == Page.EMOJI) {
            keyboardView.visibility = View.GONE
            emojiPanel.visibility = View.VISIBLE
            return
        }
        emojiPanel.visibility = View.GONE
        keyboardView.visibility = View.VISIBLE
        keyboardView.keyboard = if (page == Page.SHIFT) shiftedKeyboard
            else if (page == Page.SYMBOLS) symbolsKeyboard
            else baseKeyboard
        keyboardView.isShifted = page == Page.SHIFT
        keyboardView.invalidateAllKeys()
    }

    private fun setShift(active: Boolean) = showPage(if (active) Page.SHIFT else Page.BASE)
    private fun toggleSymbols() = showPage(if (currentPage == Page.SYMBOLS) Page.BASE else Page.SYMBOLS)
    private fun toggleEmoji() = showPage(if (currentPage == Page.EMOJI) Page.BASE else Page.EMOJI)

    override fun onKey(primaryCode: Int, keyCodes: IntArray?) {
        val ic = currentInputConnection ?: return
        when (primaryCode) {
            Keyboard.KEYCODE_DELETE -> ic.deleteSurroundingText(1, 0)
            Keyboard.KEYCODE_SHIFT -> setShift(currentPage != Page.SHIFT)
            ENTER_CODE -> ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
            GLOBE_CODE -> switchToNextIme()
            SYMBOLS_CODE -> toggleSymbols()
            EMOJI_CODE -> toggleEmoji()
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
        if (currentPage == Page.SHIFT) showPage(Page.BASE)  // momentary — one character, then back to base
    }

    override fun onPress(primaryCode: Int) {
        keyboardView.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }
    override fun onRelease(primaryCode: Int) {}
    override fun swipeLeft() {}
    override fun swipeRight() {}
    override fun swipeDown() {}
    override fun swipeUp() {}

    companion object {
        private const val ENTER_CODE = 10
        private const val GLOBE_CODE = 11
        private const val SYMBOLS_CODE = 12
        private const val EMOJI_CODE = 13
    }
}

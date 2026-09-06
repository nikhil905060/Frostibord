package com.frost.keyboard

import android.content.Context
import android.content.res.Configuration
import android.inputmethodservice.InputMethodService
import android.inputmethodservice.Keyboard
import android.inputmethodservice.KeyboardView
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast

class FrostKeyboardService : InputMethodService(), KeyboardView.OnKeyboardActionListener {

    private lateinit var keyboardView: FrostKeyboardView
    private lateinit var keyboard: Keyboard
    private var capsLock = false

    override fun onCreateInputView(): View {
        keyboardView = layoutInflater.inflate(R.layout.keyboard_view, null) as FrostKeyboardView
        keyboard = Keyboard(this, R.xml.qwerty)
        keyboardView.keyboard = keyboard
        keyboardView.setOnKeyboardActionListener(this)
        keyboardView.isPreviewEnabled = true

        keyboardView.cursorMoveListener = { direction -> moveCursor(direction) }
        keyboardView.wordDeleteListener = { deletePreviousWord() }

        val isDark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
        keyboardView.setDarkMode(isDark)

        keyboardView.post {
            val bmp = WallpaperBlurHelper.getBlurredWallpaper(
                this, keyboardView.width, keyboardView.height
            )
            keyboardView.setBackgroundBitmap(bmp)
        }
        return keyboardView
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        keyboardView.invalidate()
    }

    private fun moveCursor(direction: Int) {
        val ic = currentInputConnection ?: return
        val keyCode = if (direction > 0) KeyEvent.KEYCODE_DPAD_RIGHT else KeyEvent.KEYCODE_DPAD_LEFT
        ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
        ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
    }

    private fun deletePreviousWord() {
        val ic = currentInputConnection ?: return
        val before = ic.getTextBeforeCursor(50, 0) ?: return
        if (before.isEmpty()) return
        val end = before.length
        var i = end - 1
        while (i >= 0 && before[i] == ' ') i--
        while (i >= 0 && before[i] != ' ') i--
        val deleteCount = end - (i + 1)
        if (deleteCount > 0) ic.deleteSurroundingText(deleteCount, 0)
    }

    private fun vibrateKey() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(15, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(15)
        }
    }

    override fun onKey(primaryCode: Int, keyCodes: IntArray?) {
        val ic = currentInputConnection ?: return
        when (primaryCode) {
            Keyboard.KEYCODE_DELETE -> ic.deleteSurroundingText(1, 0)
            Keyboard.KEYCODE_SHIFT -> {
                capsLock = !capsLock
                keyboard.isShifted = capsLock
                keyboardView.invalidateAllKeys()
            }
            Keyboard.KEYCODE_DONE -> {
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
            }
            Keyboard.KEYCODE_MODE_CHANGE -> {
                // Hook for a symbols/number layout swap; extend as needed.
            }
            -10 -> {
                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showInputMethodPicker()
            }
            -11 -> {
                Toast.makeText(this, "Voice typing not set up yet", Toast.LENGTH_SHORT).show()
            }
            32 -> ic.commitText(" ", 1)
            else -> {
                var code = primaryCode.toChar()
                if (Character.isLetter(code) && (capsLock || keyboard.isShifted)) {
                    code = Character.toUpperCase(code)
                }
                ic.commitText(code.toString(), 1)
                if (!capsLock && keyboard.isShifted) {
                    keyboard.isShifted = false
                    keyboardView.invalidateAllKeys()
                }
            }
        }
    }

    override fun onPress(primaryCode: Int) {
        vibrateKey()
    }

    override fun onRelease(primaryCode: Int) {}
    override fun onText(text: CharSequence?) {}
    override fun swipeLeft() {}
    override fun swipeRight() {}
    override fun swipeDown() {}
    override fun swipeUp() {}
}

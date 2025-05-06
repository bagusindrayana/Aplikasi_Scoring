package com.stinkmtul.mytarget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.Button
import android.widget.LinearLayout

class CustomKeyboard @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    interface KeyboardListener {
        fun onKeyPressed(value: String)
        fun onBackspace()
    }

    private var listener: KeyboardListener? = null

    init {
        LayoutInflater.from(context).inflate(R.layout.custom_keyboard, this, true)

        setupButton(R.id.button_0, "10")
        setupButton(R.id.button_1, "1")
        setupButton(R.id.button_2, "2")
        setupButton(R.id.button_3, "3")
        setupButton(R.id.button_4, "4")
        setupButton(R.id.button_5, "5")
        setupButton(R.id.button_6, "6")
        setupButton(R.id.button_7, "7")
        setupButton(R.id.button_8, "8")
        setupButton(R.id.button_9, "9")

        setupButton(R.id.button_x, "X")
        setupButton(R.id.button_m, "M")
    }

    private fun setupButton(buttonId: Int, value: String) {
        findViewById<Button>(buttonId).setOnClickListener {
            listener?.onKeyPressed(value)
        }
    }

    fun setKeyboardListener(listener: KeyboardListener) {
        this.listener = listener
    }
}
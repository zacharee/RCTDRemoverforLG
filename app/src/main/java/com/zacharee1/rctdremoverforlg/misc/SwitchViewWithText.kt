package com.zacharee1.rctdremoverforlg.misc

import android.content.Context
import android.util.AttributeSet
import android.widget.CompoundButton
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import com.zacharee1.rctdremoverforlg.R

class SwitchViewWithText(context: Context, attrs: AttributeSet) : LinearLayout(context, attrs) {
    private val mSwitch: Switch?
    private val mSubText: TextView?
    private val mTitleText: TextView?

    private var subText: String?
        get() = mSubText?.text.toString()
        set(value) {
            mSubText?.text = value
        }

    private var text: String?
        get() = mTitleText?.text.toString()
        set(value) {
            mTitleText?.text = value
        }

    var isChecked: Boolean
        get() = mSwitch?.isChecked as Boolean
        set(value) {
            mSwitch?.isChecked = value
        }

    init {
        inflate(getContext(), R.layout.switch_with_text, this)

        val a = context.theme.obtainStyledAttributes(
                attrs,
                R.styleable.SwitchViewWithText,
                0, 0)

        mSwitch = findViewById(R.id.switchView)
        mTitleText = findViewById(R.id.titleText)
        mSubText = findViewById(R.id.subText)

        try {
            subText = a.getString(R.styleable.SwitchViewWithText_subText)
            text = a.getString(R.styleable.SwitchViewWithText_text)
        } finally {
            a.recycle()
        }

        mSwitch.isFocusable = false
        mTitleText.isFocusable = false
        mSubText.isFocusable = false

        if (subText == null || subText?.isEmpty() as Boolean) {
            mSubText.visibility = GONE
        }
    }

    fun setOnCheckedChangeListener(listener: (CompoundButton, Boolean) -> Unit) {
        mSwitch?.setOnCheckedChangeListener(listener)
    }
}
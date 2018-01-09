package com.zacharee1.rctdremoverforlg.misc

import android.content.Context
import android.graphics.Color
import android.support.v7.widget.AppCompatTextView
import android.text.Html
import android.text.TextUtils
import android.util.AttributeSet
import com.zacharee1.rctdremoverforlg.R

class TerminalView(context: Context, attrs: AttributeSet) : AppCompatTextView(context, attrs) {
    init {
        setBackgroundColor(Color.BLACK)
        setTextColor(Color.WHITE)
        setTextAppearance(R.style.TerminalFont)

        val px = Utils.pxToDp(getContext(), 16)
        setPadding(px, 0, px, 0)
    }

    fun addText(textToAdd: String) {
        val currentText = text
        val format = TextUtils.concat(currentText, Html.fromHtml(textToAdd.replace("\n", "<br><br>")))

        text = format
    }
}
package com.zacharee1.rctdremoverforlg.misc;

import android.content.Context;
import android.graphics.Color;
import android.support.v7.widget.AppCompatTextView;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;

import com.zacharee1.rctdremoverforlg.R;

import java.util.Locale;


public class TerminalView extends AppCompatTextView {
    public TerminalView(Context context, AttributeSet attrs) {
        super(context, attrs);

        setBackgroundColor(Color.BLACK);
        setTextColor(Color.WHITE);
        setTextAppearance(R.style.TerminalFont);

        int px = Utils.pxToDp(getContext(), 16);
        setPadding(px, 0, px, 0);
    }

    public void addText(String textToAdd) {
        CharSequence currentText = getText();
        CharSequence format = TextUtils.concat(currentText, Html.fromHtml(textToAdd.replace("\n", "<br><br>")));

        setText(format);
    }
}

package com.zacharee1.rctdremoverforlg.misc;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.zacharee1.rctdremoverforlg.R;


public class SwitchViewWithText extends LinearLayout {
    private String subText;
    private String text;

    private Switch mSwitch;
    private TextView mSubText;
    private TextView mTitleText;

    public SwitchViewWithText(Context context, AttributeSet attrs) {
        super(context, attrs);

        inflate(getContext(), R.layout.switch_with_text, this);

        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.SwitchViewWithText,
                0, 0);

        try {
            subText = a.getString(R.styleable.SwitchViewWithText_subText);
            text = a.getString(R.styleable.SwitchViewWithText_text);
        } finally {
            a.recycle();
        }

        mSwitch = findViewById(R.id.switchView);
        mTitleText = findViewById(R.id.titleText);
        mSubText = findViewById(R.id.subText);

        mTitleText.setText(text);
        mSubText.setText(subText);

        mSwitch.setFocusable(false);
        mTitleText.setFocusable(false);
        mSubText.setFocusable(false);

        if (subText == null || subText.isEmpty()) {
            mSubText.setVisibility(GONE);
        }
    }

    public void setChecked(boolean checked) {
        mSwitch.setChecked(checked);
    }

    public void setText(CharSequence text) {
        mTitleText.setText(text);
    }

    public void setSubText(CharSequence subText) {
        mSubText.setText(subText);
    }

    public void setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener listener) {
        mSwitch.setOnCheckedChangeListener(listener);
    }

    public boolean isChecked() {
        return mSwitch.isChecked();
    }

    public CharSequence getText() {
        return mTitleText.getText();
    }

    public CharSequence getSubText() {
        return mSubText.getText();
    }
}

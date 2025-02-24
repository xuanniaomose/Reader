package com.xuanniao.reader.ui.book;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.text.method.KeyListener;
import android.util.AttributeSet;
import android.view.View;
import androidx.appcompat.widget.AppCompatEditText;

public class ClearEditText extends AppCompatEditText implements View.OnFocusChangeListener {
    private Drawable mClearDrawable;
    private boolean mHasFoucs = false;
    private KeyListener mListener;

    public ClearEditText (Context context) {
        this(context, null);
    }

    public ClearEditText (Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.editTextStyle);
    }

    public ClearEditText (Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public void init () {
        mHasFoucs = false;
        //拿到原本的KeyListener
        mListener = getKeyListener();
        //设置省略号在结尾
        setEllipsize(TextUtils.TruncateAt.END);
    }

    @Override
    public void onFocusChange (View v, boolean hasFocus) {
        this.mHasFoucs = hasFocus;
        if (hasFocus) {
            //等50毫秒后执行定位光标位置
            postDelayed(() -> setSelection(getText().length(), getText().length()), 50);
//            setClearIconVisible(getText().length() > 0);
            //KeyListener设置回原本的KeyListener
            setKeyListener(mListener);
        } else {
            //KeyListener设置为空
            setKeyListener(null);
//            setClearIconVisible(false);
//            UIUtil.hideKeyboardFrom(getContext(), v.getWindowToken());
//            mFoucsChangeHandler.update();
        }
    }
}

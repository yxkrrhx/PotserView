package com.example.yishuinanfeng.jigsawapplication.util;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

/**
 * Created by hzm on 2017/10/30.
 */

public class SoftInputUtil {
    static  SoftInputUtil inputMethodHelper;

    InputMethodManager inputMethodManager;

    SoftInputUtil(Context mContext) {
        inputMethodManager = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
    }

    public synchronized static SoftInputUtil getInstance(Context mContext) {
        synchronized (SoftInputUtil.class) {
            if (inputMethodHelper == null) {
                inputMethodHelper = new SoftInputUtil(mContext);
            }
            return inputMethodHelper;
        }
    }
    /**
     * 显示软键盘
     *
     * @param view
     */
    public void showSoftInput(View view) {
        inputMethodManager.showSoftInput(view, 0);
    }

    /**
     * 隐藏输入法
     */
    public void hideSoftInput(View view) {
        if (inputMethodManager.isActive()) {
            Log.d("hickey", "hideSoftInput:" + "hideSoftInputFromWindow");
            inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

}

package com.example.yishuinanfeng.jigsawapplication.model;

/**
 * Created by hzm on 2017/10/31.
 */

public class TextModel {


    float deltaX; //相对起始位置的偏移量
    float deltaY;
    public float getDeltaX() {
        return deltaX;
    }

    public void setDeltaX(float deltaX) {
        this.deltaX = deltaX;
    }

    public float getDeltaY() {
        return deltaY;
    }

    public void setDeltaY(float deltaY) {
        this.deltaY = deltaY;
    }
    private String text;//需要绘制的用户输入的文本文字

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

}

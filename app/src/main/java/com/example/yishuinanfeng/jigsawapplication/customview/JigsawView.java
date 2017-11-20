package com.example.yishuinanfeng.jigsawapplication.customview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

import com.example.yishuinanfeng.jigsawapplication.model.HollowModel;
import com.example.yishuinanfeng.jigsawapplication.model.PictureModel;
import com.example.yishuinanfeng.jigsawapplication.model.TextModel;

import java.util.ArrayList;


/**
 * Created by Hendricks on 2017/6/8.
 * 操作多个拼图处理的自定义View
 */

public class JigsawView extends View {
    private static String KEY_SUPER = "key_super";
    private static String KEY_PICTURE_MODELS = "key_picture_models";
    //绘制文字的画笔
    private TextPaint mTextPaint = new TextPaint (Paint.ANTI_ALIAS_FLAG);
    Typeface mFace;

    //绘制图片的画笔
    private Paint mMaimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    //绘制高亮边框的画笔
    private Paint mSelectPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private PorterDuffXfermode mPorterDuffXfermodeClear = new PorterDuffXfermode(PorterDuff.Mode.CLEAR);

    private Bitmap mBitmapBackGround;

    private Matrix mMatrix = new Matrix();

    private float mLastX;
    private float mLastY;

    private float mDownX;
    private float mDownY;

    private double mLastFingerDistance;
    private double mLastDegree;

    private boolean mIsDoubleFinger;

    private Path mPath = new Path();

    private ArrayList<PictureModel> mPictureModels;

    //触摸点对应的图片模型
    private PictureModel mPicModelTouch;

    private PictureSelectListener mPictureSelectListener;
    private PictureNoSelectListener mPictureNoSelectListener;

    private PictureCancelSelectListener mPictureCancelSelectListener;

    private boolean mIsNeedHighlight = true;
    private boolean ON_CLICK_TEXT;

    @Override
    protected Parcelable onSaveInstanceState() {
        Bundle bundle = new Bundle();
        bundle.putParcelable(KEY_SUPER,super.onSaveInstanceState());
        bundle.putSerializable(KEY_PICTURE_MODELS,mPictureModels);
        return bundle;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state instanceof Bundle){
            Bundle bundle = (Bundle) state;
            if (bundle.getSerializable(KEY_PICTURE_MODELS) instanceof ArrayList){
                mPictureModels = (ArrayList<PictureModel>) bundle.getSerializable(KEY_PICTURE_MODELS);
            }
            super.onRestoreInstanceState((bundle).getParcelable(KEY_SUPER));
            return;
        }
        super.onRestoreInstanceState(state);
    }

    public void setPictureCancelSelectListener(PictureCancelSelectListener pictureCancelSelectListner) {
        mPictureCancelSelectListener = pictureCancelSelectListner;
    }

    public void setPictureSelectListener(PictureSelectListener mPictureSelectListener) {
        this.mPictureSelectListener = mPictureSelectListener;
    }

    public void setPictureNoSelectListener(PictureNoSelectListener mPictureNoSelectListener) {
        this.mPictureNoSelectListener = mPictureNoSelectListener;
    }

    public JigsawView setPictureModels(ArrayList<PictureModel> mPictureModels) {
        this.mPictureModels = mPictureModels;
        makePicFillHollow();
        return this;
    }


    public JigsawView setBitmapBackGround(Bitmap mBitmapBackGround) {
        this.mBitmapBackGround = mBitmapBackGround;
        return this;
    }

    public JigsawView(Context context) {
        super(context);
        init();
    }


    public JigsawView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public JigsawView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        //关闭硬件加速
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        setBackgroundColor(Color.WHITE);
        mSelectPaint.setColor(Color.RED);
        mSelectPaint.setStyle(Paint.Style.STROKE);
        mSelectPaint.setStrokeWidth(6);

        //实例化自定义字体
        mFace = Typeface.createFromAsset(getContext().getAssets(),"font/huakanghaibao.ttc");
        mTextPaint.setColor(Color.BLUE);
        mTextPaint.setStyle(Paint.Style.FILL);
        mTextPaint.setTextSize(35);
        mTextPaint.setTypeface(mFace);
        mTextPaint.setStrokeWidth(3);

    }

    public void setNeedHighlight(boolean needHighlight) {
        mIsNeedHighlight = needHighlight;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mPictureModels != null && mPictureModels.size() > 0 && mBitmapBackGround != null) {
            //循环遍历画要处理的图片
            for (PictureModel pictureModel : mPictureModels) {
                Bitmap bitmapPicture = pictureModel.getBitmapPicture();
                int pictureX = pictureModel.getPictureX();
                int pictureY = pictureModel.getPictureY();
                float scaleX = pictureModel.getScaleX();
                float scaleY = pictureModel.getScaleY();
                float rotateDelta = pictureModel.getRotate();

                HollowModel hollowModel = pictureModel.getHollowModel();
                ArrayList<Path> paths = hollowModel.getPathList();
                if (paths != null && paths.size() > 0) {
                    for (Path tempPath : paths) {
                        mPath.addPath(tempPath);
                    }
                    drawPicture(canvas, bitmapPicture, pictureX, pictureY, scaleX, scaleY, rotateDelta, hollowModel, mPath);
                } else {
                    drawPicture(canvas, bitmapPicture, pictureX, pictureY, scaleX, scaleY, rotateDelta, hollowModel, null);
                }
            }

            for (PictureModel pictureModel : mPictureModels) {
                TextModel textModel = pictureModel.getTextModel();
                HollowModel hollowModel = pictureModel.getHollowModel();
                ArrayList<Path> paths = hollowModel.getPathList();
                if (paths != null && paths.size() > 0) {
                    for (Path tempPath : paths) {
                        mPath.addPath(tempPath);
                    }
                    drawText(canvas,hollowModel,textModel,mPath);
                    mPath.reset();
                } else {
                    drawText(canvas,hollowModel,textModel,null);
                }
            }

            //新建一个layer，新建的layer放置在canvas默认layer的上部，当我们执行了canvas.saveLayer()之后，我们所有的绘制操作都绘制到了我们新建的layer上，而不是canvas默认的layer。
            int layerId = canvas.saveLayer(0, 0, canvas.getWidth(), canvas.getHeight(), null, Canvas.ALL_SAVE_FLAG);

            drawBackGround(canvas);

            //循环遍历画镂空部分
            for (PictureModel pictureModel : mPictureModels) {
                int hollowX = pictureModel.getHollowModel().getHollowX();
                int hollowY = pictureModel.getHollowModel().getHollowY();
                int hollowWidth = pictureModel.getHollowModel().getWidth();
                int hollowHeight = pictureModel.getHollowModel().getHeight();
                ArrayList<Path> paths = pictureModel.getHollowModel().getPathList();
                if (paths != null && paths.size() > 0) {
                    for (Path tempPath : paths) {
                        mPath.addPath(tempPath);
                    }
                    drawHollow(canvas, hollowX, hollowY, hollowWidth, hollowHeight, mPath);
                    mPath.reset();
                } else {
                    drawHollow(canvas, hollowX, hollowY, hollowWidth, hollowHeight, null);
                }
            }

            //把这个layer绘制到canvas默认的layer上去
            canvas.restoreToCount(layerId);

            //绘制选择图片高亮边框
            for (PictureModel pictureModel : mPictureModels) {
                if (pictureModel.isSelect() && mIsNeedHighlight) {
                    canvas.drawRect(getSelectRect(pictureModel), mSelectPaint);
                }
            }

        }
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mBitmapBackGround == null) {
            throw new RuntimeException("mBitmapBackGround is null!");
        }
        int resultWidth = 0;
        int resultHeight = 0;

        int specWidthSize = MeasureSpec.getSize(widthMeasureSpec);
        int specHeightSize = MeasureSpec.getSize(heightMeasureSpec);
        int specWidthMode = MeasureSpec.getMode(widthMeasureSpec);
        int specHeightMode = MeasureSpec.getMode(heightMeasureSpec);

        switch (specWidthMode) {
            case MeasureSpec.EXACTLY:
            case MeasureSpec.UNSPECIFIED:
                resultWidth = specWidthSize;
                break;
            case MeasureSpec.AT_MOST:
                resultWidth = mBitmapBackGround.getWidth() < specWidthSize ? mBitmapBackGround.getWidth() : specWidthSize;
                break;
        }

        switch (specHeightMode) {
            case MeasureSpec.EXACTLY:
            case MeasureSpec.UNSPECIFIED:
                resultHeight = specHeightSize;
                break;
            case MeasureSpec.AT_MOST:
                resultHeight = mBitmapBackGround.getHeight() < specHeightSize ? mBitmapBackGround.getHeight() : specHeightSize;
                break;
        }
        //如果是wrap_content,就让View的大小和背景图一样
        setMeasuredDimension(resultWidth, resultHeight);
    }


    private void drawBackGround(Canvas canvas) {
        canvas.drawBitmap(mBitmapBackGround, 0, 0, null);
    }


    /**
     * 画输入的文字
     */
    private void drawText(Canvas canvas,HollowModel hollowModel,TextModel textModel, Path path ) {

        canvas.save();

        //以下是对应镂空部分相交的处理，需要完善，或者不需要
        if (path != null) {
            Matrix matrix1 = new Matrix();
            RectF rect = new RectF();
            path.computeBounds(rect, true);

            int width = (int) rect.width();
            int height = (int) rect.height();

            float hollowScaleX = hollowModel.getWidth() / (float) width;
            float hollowScaleY = hollowModel.getHeight() / (float) height;

            matrix1.postScale(hollowScaleX, hollowScaleY);
            path.transform(matrix1);
            //平移path
            path.offset(hollowModel.getHollowX(), hollowModel.getHollowY());
            //让文字只能绘制在镂空内部，防止滑动到另一个拼图的区域中
            canvas.clipPath(path);
            path.reset();
        } else {
            int hollowX = hollowModel.getHollowX();
            int hollowY = hollowModel.getHollowY();
            int hollowWidth = hollowModel.getWidth();
            int hollowHeight = hollowModel.getHeight();
            //让文字只能绘制在镂空内部，防止滑动到另一个拼图的区域中
            canvas.clipRect(hollowX, hollowY, hollowX + hollowWidth, hollowY + hollowHeight);
        }
        if (textModel!=null && textModel.getText()!=null) {//注意绘制是相对于全屏的
//            canvas.drawText(textModel.getText(), hollowModel.getHollowX() + textModel.getDeltaX(), hollowModel.getHollowY() + textModel.getDeltaY(), mTextPaint);
            canvas.translate( hollowModel.getHollowX() + textModel.getDeltaX(), hollowModel.getHollowY() + textModel.getDeltaY());
            StaticLayout myStaticLayout = new StaticLayout(textModel.getText(), mTextPaint, hollowModel.getWidth(), Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
            myStaticLayout.draw(canvas);
        }
        canvas.restore();
    }

    /**
     * 画需要处理的图片
     */
    private void drawPicture(Canvas canvas, Bitmap bitmapPicture, int coordinateX, int coordinateY, float scaleX, float scaleY, float rotateDelta
            , HollowModel hollowModel, Path path) {
        int picCenterWidth = bitmapPicture.getWidth() / 2;
        int picCenterHeight = bitmapPicture.getHeight() / 2;
        mMatrix.postTranslate(coordinateX, coordinateY);
        mMatrix.postScale(scaleX, scaleY, coordinateX + picCenterWidth, coordinateY + picCenterHeight);
        mMatrix.postRotate(rotateDelta, coordinateX + picCenterWidth, coordinateY + picCenterHeight);
        canvas.save();

        //以下是对应镂空部分相交的处理，需要完善，或者不需要
        if (path != null) {
            Matrix matrix1 = new Matrix();
            RectF rect = new RectF();
            path.computeBounds(rect, true);

            int width = (int) rect.width();
            int height = (int) rect.height();

            float hollowScaleX = hollowModel.getWidth() / (float) width;
            float hollowScaleY = hollowModel.getHeight() / (float) height;

            matrix1.postScale(hollowScaleX, hollowScaleY);
            path.transform(matrix1);
            //平移path
            path.offset(hollowModel.getHollowX(), hollowModel.getHollowY());
            //让图片只能绘制在镂空内部，防止滑动到另一个拼图的区域中
            canvas.clipPath(path);
            path.reset();
        } else {
            int hollowX = hollowModel.getHollowX();
            int hollowY = hollowModel.getHollowY();
            int hollowWidth = hollowModel.getWidth();
            int hollowHeight = hollowModel.getHeight();
            //让图片只能绘制在镂空内部，防止滑动到另一个拼图的区域中
            canvas.clipRect(hollowX, hollowY, hollowX + hollowWidth, hollowY + hollowHeight);
        }
        canvas.drawBitmap(bitmapPicture, mMatrix, null);
        canvas.restore();
        mMatrix.reset();
    }

    /**
     * 画底图和镂空部分
     */
    private void drawHollow(Canvas canvas, int hollowX, int hollowY, int hollowWidth, int hollowHeight, Path path) {
        mMaimPaint.setXfermode(mPorterDuffXfermodeClear);
        //画镂空
        if (path != null) {
            canvas.save();
            canvas.translate(hollowX, hollowY);
            //缩放镂空部分大小
            scalePathRegion(canvas, hollowWidth, hollowHeight, path);
            canvas.drawPath(path, mMaimPaint);
            canvas.restore();
            mMaimPaint.setXfermode(null);
        } else {
            Rect rect = new Rect(hollowX, hollowY, hollowX + hollowWidth, hollowY + hollowHeight);
            canvas.save();
            canvas.drawRect(rect, mMaimPaint);
            canvas.restore();
            mMaimPaint.setXfermode(null);
        }
    }


    /**
     * 缩放镂空部分大小
     *
     *
     */
    private void scalePathRegion(Canvas canvas, int hollowWidth, int hollowHeight, Path path) {
        //使得不规则的镂空图形填充指定的Rect区域
        RectF rect = new RectF();
        path.computeBounds(rect, true);

        int width = (int) rect.width();
        int height = (int) rect.height();

        float scaleX = hollowWidth / (float) width;
        float scaleY = hollowHeight / (float) height;

        canvas.scale(scaleX, scaleY);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mPictureModels == null || mPictureModels.size() == 0) {
            return true;
        }
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_POINTER_DOWN:

                //双指模式
                if (event.getPointerCount() == 2) {
                    //mPicModelTouch为当前触摸到的操作图片模型
                    mPicModelTouch = getHandlePicModel(event);
                    if (mPicModelTouch != null) {
                        // mPicModelTouch.setSelect(true);
                        //重置图片的选中状态
                        resetNoTouchPicsState();
                        mPicModelTouch.setSelect(true);
                        //两手指的距离
                        mLastFingerDistance = distanceBetweenFingers(event);
                        //两手指间的角度
                        mLastDegree = rotation(event);
                        mIsDoubleFinger = true;
                        invalidate();
                    }
                }
                break;

            //单指模式
            case MotionEvent.ACTION_DOWN:
                //记录上一次事件的位置
                mLastX = event.getX();
                mLastY = event.getY();
                //记录Down事件的位置
                mDownX = event.getX();
                mDownY = event.getY();
                //获取被点击的图片模型
                mPicModelTouch = getHandlePicModel(event);
                if (mPicModelTouch != null) {
                    if (checkTextLocation(mPicModelTouch,(int)event.getX(),(int)event.getY())){
                        ON_CLICK_TEXT=true;
                    }
                    //每次down重置其他picture选中状态
                    resetNoTouchPicsState();
                    mPicModelTouch.setSelect(true);
                    invalidate();
                }
                break;
            case MotionEvent.ACTION_MOVE:
                switch (event.getPointerCount()) {
                    //单指模式
                    case 1:
                        if (!mIsDoubleFinger) {
                            if (mPicModelTouch != null) {
                                //记录每次事件在x,y方向上移动
                                int dx = (int) (event.getX() - mLastX);
                                int dy = (int) (event.getY() - mLastY);
                                int tempX = mPicModelTouch.getPictureX() + dx;
                                int tempY = mPicModelTouch.getPictureY() + dy;

                                if (checkPictureLocation(mPicModelTouch, tempX, tempY)) {

                                    if (ON_CLICK_TEXT){
                                        TextModel textModel=mPicModelTouch.getTextModel();
                                        if (textModel!=null) {
                                            textModel.setDeltaX(textModel.getDeltaX()+dx);
                                            textModel.setDeltaY(textModel.getDeltaY()+dy);
                                            mLastX = event.getX();
                                            mLastY = event.getY();
                                        }
                                    }else {
                                        //检查到没有越出镂空部分才真正赋值给mPicModelTouch
                                        mPicModelTouch.setPictureX(tempX);
                                        mPicModelTouch.setPictureY(tempY);
                                        //保存上一次的位置，以便下次事件算出相对位移
                                        mLastX = event.getX();
                                        mLastY = event.getY();
                                        //修改了mPicModelTouch的位置后刷新View
                                    }

                                    invalidate();
                                }
                            }
                        }
                        break;

                    //双指模式
                    case 2:
                        if (mPicModelTouch != null) {
                            //算出两根手指的距离
                            double fingerDistance = distanceBetweenFingers(event);
                            //当前的旋转角度
                            double currentDegree = rotation(event);
                            //当前手指距离和上一次的手指距离的比即为图片缩放比
                            float scaleRatioDelta = (float) (fingerDistance / mLastFingerDistance);
                            float rotateDelta = (float) (currentDegree - mLastDegree);

                            float tempScaleX = scaleRatioDelta * mPicModelTouch.getScaleX();
                            float tempScaleY = scaleRatioDelta * mPicModelTouch.getScaleY();
                            //对缩放比做限制
                            if (Math.abs(tempScaleX) < 3 && Math.abs(tempScaleX) > 0.3 &&
                                    Math.abs(tempScaleY) < 3 && Math.abs(tempScaleY) > 0.3) {
                                //没有超出缩放比才真正赋值给模型
                                mPicModelTouch.setScaleX(tempScaleX);
                                mPicModelTouch.setScaleY(tempScaleY);
                                mPicModelTouch.setRotate(mPicModelTouch.getRotate() + rotateDelta);
                                //修改模型之后，刷新View
                                invalidate();
                                //记录上一次的两手指距离以便下次计算出相对的位置以算出缩放系数
                                mLastFingerDistance = fingerDistance;
                            }
                            //记录上次的角度以便下一个事件计算出角度变化值
                            mLastDegree = currentDegree;
                        }
                        break;
                }
                break;
            //两手指都离开屏幕
            case MotionEvent.ACTION_UP:
//                for (PictureModel pictureModel : mPictureModels) {
//                    pictureModel.setSelect(false);
//                }
                mIsDoubleFinger = false;
                ON_CLICK_TEXT=false;
                double distance = getDisBetweenPoints(event);

                if (mPicModelTouch != null) {
                    //是否属于滑动，非滑动则改变选中状态
                    if (distance < ViewConfiguration.getTouchSlop()) {
                        if (mPicModelTouch.isLastSelect()) {
                            mPicModelTouch.setSelect(false);
                            mPicModelTouch.setLastSelect(false);
                            if (mPictureCancelSelectListener != null) {
                                mPictureCancelSelectListener.onPictureCancelSelect();
                            }

                        } else {
                            mPicModelTouch.setSelect(true);
                            mPicModelTouch.setLastSelect(true);
                            //选中的回调
                            if (mPictureSelectListener != null) {
                                mPictureSelectListener.onPictureSelect(mPicModelTouch);
                            }
                        }
                        invalidate();
                    } else {
                        //滑动则取消所有选择的状态
                        mPicModelTouch.setSelect(false);
                        mPicModelTouch.setLastSelect(false);
                        //取消状态之后刷新View
                        invalidate();
                    }
                } else {
                    //如果没有图片被选中，则取消所有图片的选中状态
                    for (PictureModel pictureModel : mPictureModels) {
                        pictureModel.setLastSelect(false);
                    }
                    //没有拼图被选中的回调
                    if (mPictureNoSelectListener != null) {
                        mPictureNoSelectListener.onPictureNoSelect();
                    }
                    //取消所有图片选中状态后刷新View
                    invalidate();
                }
                break;
            //双指模式中其中一手指离开屏幕，取消当前被选中图片的选中状态
            case MotionEvent.ACTION_POINTER_UP:
                if (mPicModelTouch != null) {
                    mPicModelTouch.setSelect(false);
                    invalidate();
                }
        }
        return true;
    }

    private void resetNoTouchPicsState() {
        //每次down重置其他picture选中状态
        for (PictureModel model : mPictureModels) {
            if (model != mPicModelTouch) {
                model.setSelect(false);
                model.setLastSelect(false);
            }
        }
    }


    /**
     * 外部刷新该View所用
     */
    public void refreshView() {
        invalidate();
    }

    /**
     * 设置选中图片的高亮边框
     */
    private Rect getSelectRect(PictureModel picModel) {
        int hollowX = picModel.getHollowModel().getHollowX();
        int hollowY = picModel.getHollowModel().getHollowY();
        int hollowWidth = picModel.getHollowModel().getWidth();
        int hollowHeight = picModel.getHollowModel().getHeight();

        return new Rect(hollowX, hollowY, hollowX + hollowWidth, hollowY + hollowHeight);
    }


    /**
     * 根据事件点击区域得到对应的PictureModel，如果没有点击到图片所在区域则返回null
     *
     *
     */
    private PictureModel getHandlePicModel(MotionEvent event) {
        switch (event.getPointerCount()) {
            case 1:
                int x = (int) event.getX();
                int y = (int) event.getY();
                for (PictureModel picModel : mPictureModels) {
                    int hollowX = picModel.getHollowModel().getHollowX();
                    int hollowY = picModel.getHollowModel().getHollowY();
                    int hollowWidth = picModel.getHollowModel().getWidth();
                    int hollowHeight = picModel.getHollowModel().getHeight();

                    Rect rect = new Rect(hollowX, hollowY, hollowX + hollowWidth, hollowY + hollowHeight);
                    //点在矩形区域中
                    if (rect.contains(x, y)) {
                        return picModel;
                    }
                }
                break;
            case 2:
                int x0 = (int) event.getX(0);
                int y0 = (int) event.getY(0);
                int x1 = (int) event.getX(1);
                int y1 = (int) event.getY(1);
                for (PictureModel picModel : mPictureModels) {
                    int hollowX = picModel.getHollowModel().getHollowX();
                    int hollowY = picModel.getHollowModel().getHollowY();
                    int hollowWidth = picModel.getHollowModel().getWidth();
                    int hollowHeight = picModel.getHollowModel().getHeight();

                    Rect rect = new Rect(hollowX, hollowY, hollowX + hollowWidth, hollowY + hollowHeight);
                    //两个点都在该矩形区域
                    if (rect.contains(x0, y0) || rect.contains(x1, y1)) {
                        return picModel;
                    }
                }
                break;
            default:
                break;

        }
        return null;
    }


    /**
     * 检查图片范围是否超出窗口,此方法还要完善
     */
    private boolean checkPictureLocation(PictureModel mPictureModel, int tempX, int tempY) {
        HollowModel hollowModel = mPictureModel.getHollowModel();
        Bitmap picture = mPictureModel.getBitmapPicture();
        return (tempY < hollowModel.getHollowY() + hollowModel.getHeight()) && (tempY + picture.getHeight() > hollowModel.getHollowY())
                && (tempX < hollowModel.getHollowX() + hollowModel.getWidth()) && (tempX + picture.getWidth() > hollowModel.getHollowX());
    }

    /**
     * 检查文本是否再点击的范围内
     */

    private boolean checkTextLocation(PictureModel mPictureModel,int x,int y) {
        HollowModel hollowModel = mPictureModel.getHollowModel();
        TextModel textModel=mPictureModel.getTextModel();

        if (textModel!=null){
           String text=textModel.getText();
            if (!TextUtils.isEmpty(text)) {

                StaticLayout myStaticLayout = new StaticLayout(textModel.getText(), mTextPaint, hollowModel.getWidth(), Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
                int width = myStaticLayout.getWidth();//文本的宽度
                int height = myStaticLayout.getHeight();//文本的高度

                Rect rect = new Rect(hollowModel.getHollowX()+(int) textModel.getDeltaX(), hollowModel.getHollowY()+(int)textModel.getDeltaY(), hollowModel.getHollowX()+(int) textModel.getDeltaX() + width, hollowModel.getHollowY()+(int)textModel.getDeltaY() + height);

                Log.e("hzm","width:"+width+"|"+"height:"+height);
                Log.e("hzm","left:"+rect.left+"|"+"right:"+rect.right+"top:"+rect.top+"bottom:"+rect.bottom);
                Log.e("hzm","x:"+x+"|"+"y:"+y);
                if (rect.contains(x,y)){
                    Log.e("hzm","true");
                }else {
                    Log.e("hzm","false");
                }

                return (y < hollowModel.getHollowY() + hollowModel.getHeight()) && (y + height > hollowModel.getHollowY())
                        && (x < hollowModel.getHollowX() + hollowModel.getWidth()) && (x + width > hollowModel.getHollowX()) && rect.contains(x,y);
            }
        }
        return false;
    }



    /**
     * 使图片尺寸居中填充镂空部分对应的矩形
     */
    private void makePicFillHollow() {
        for (PictureModel jigsawPictureModel : mPictureModels) {
            HollowModel hollow = jigsawPictureModel.getHollowModel();
            Bitmap bitmapPicture = jigsawPictureModel.getBitmapPicture();
            if (bitmapPicture != null) {
                int hollowX = hollow.getHollowX();
                int hollowY = hollow.getHollowY();
                int hollowWidth = hollow.getWidth();
                int hollowHeight = hollow.getHeight();
                int hollowCenterX = hollowX + hollowWidth / 2;
                int hollowCenterY = hollowY + hollowHeight / 2;

                int pictureWidth = bitmapPicture.getWidth();
                int pictureHeight = bitmapPicture.getHeight();

                float scaleX = hollowWidth / (float) pictureWidth;
                float scaleY = hollowHeight / (float) pictureHeight;
                //取大者
                float scale = (scaleX > scaleY) ? scaleX : scaleY;

                Bitmap sourceBitmap = jigsawPictureModel.getBitmapPicture();
                Bitmap dstBitmap = Bitmap.createScaledBitmap(sourceBitmap, (int) (sourceBitmap.getWidth() * scale)
                        , (int) (sourceBitmap.getHeight() * scale), true);
                //   sourceBitmap.recycle();

                //jigsawPictureModel.setScale(scale);
                jigsawPictureModel.setBitmapPicture(dstBitmap);
                //图片位置由镂空部分位置决定
                jigsawPictureModel.setPictureX(hollowCenterX - dstBitmap.getWidth() / 2);
                jigsawPictureModel.setPictureY(hollowCenterY - dstBitmap.getHeight() / 2);
            }
        }
    }

    /**
     * 计算两个手指之间的距离。
     *
     * @param event
     * @return 两个手指之间的距离
     */
    private double distanceBetweenFingers(MotionEvent event) {
        float disX = Math.abs(event.getX(0) - event.getX(1));
        float disY = Math.abs(event.getY(0) - event.getY(1));
        return Math.sqrt(disX * disX + disY * disY);
    }


    // 取旋转角度
    private float rotation(MotionEvent event) {
        double disX = (event.getX(0) - event.getX(1));
        double disY = (event.getY(0) - event.getY(1));
        //弧度
        double radians = Math.atan2(disY, disX);
        return (float) Math.toDegrees(radians);
    }

    private double getDisBetweenPoints(MotionEvent event) {
        float disX = Math.abs(event.getX() - mDownX);
        float disY = Math.abs(event.getY() - mDownY);
        return Math.sqrt(disX * disX + disY * disY);
    }

    /**
     * 图片选中的回调接口
     */
    public interface PictureSelectListener {
        void onPictureSelect(PictureModel pictureModel);
    }

    public interface PictureNoSelectListener {
        void onPictureNoSelect();
    }

    /**
     * 某个图片取消了选中状态
     */
    public interface PictureCancelSelectListener {
        void onPictureCancelSelect();
    }

    //返回true，就是让这个View变成文本可编辑的状态，默认返回false。
    @Override
    public boolean onCheckIsTextEditor() {
        return true;
    }


    //这个是和输入法输入内容的桥梁。
    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        return new MyInputConnection(this,false);//super.onCreateInputConnection(outAttrs);
    }


    //有文本输入，当然也有按键输入，也别注意的是有些输入法输入数字并非用commitText方法传递，而是用按键来代替，比如KeyCode_1是代表1等。


    //在自定义控件中
    class MyInputConnection extends BaseInputConnection {
        public MyInputConnection(View targetView, boolean fullEditor) {
            super(targetView, fullEditor);
        }
        //这个是当输入法输入了字符，包括表情，字母、文字、数字和符号。我们可以通过text筛选出我们不想让显示到自定义view上面。
        public boolean commitText(CharSequence text, int newCursorPosition){
            Log.e("hzm", "commitText:" + text + "\t" + newCursorPosition);
            for (PictureModel pictureModel : mPictureModels) {
               if (pictureModel!=null && pictureModel.isSelect()){

                   if (pictureModel.getTextModel()!=null) {
                       pictureModel.getTextModel().setText(pictureModel.getTextModel().getText()+text.toString());
                   }else {
                       TextModel textModel=new TextModel();
                       textModel.setDeltaX(0);
                       textModel.setDeltaY(0);
                       textModel.setText(text.toString());
                       pictureModel.setTextModel(textModel);
                   }
                   postInvalidate();
                   if (mOnTextInputListener!=null)
                        mOnTextInputListener.onTextFinsh(pictureModel);
               }
            }
            return true;
        }

        @Override
        public boolean sendKeyEvent(KeyEvent event) {
            /** 当手指离开的按键的时候 */
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                Log.e("hzm", "sendKeyEvent:KeyCode=" + event.getKeyCode());
                if (event.getKeyCode() == KeyEvent.KEYCODE_DEL) {//点击了删除键
                    for (PictureModel pictureModel : mPictureModels) {
                        if (pictureModel!=null && pictureModel.isSelect()){

                            if (pictureModel.getTextModel()!=null && pictureModel.getTextModel().getText()!=null && pictureModel.getTextModel().getText().toString().length()>=1) {
                                pictureModel.getTextModel().setText(pictureModel.getTextModel().getText().toString().substring(0,pictureModel.getTextModel().getText().toString().length()-1));
                            }
                            postInvalidate();
                            if (mOnTextInputListener!=null)
                                mOnTextInputListener.onTextFinsh(pictureModel);
                        }
                    }
                } else if (event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {//点击了回车键

                } else {

                }
            }
            return true;
        }


        //当然删除的时候也会触发
        @Override
        public boolean deleteSurroundingText(int beforeLength, int afterLength) {
            Log.e("hzm", "deleteSurroundingText " + "beforeLength=" + beforeLength + " afterLength=" + afterLength);
            return true;
        }

        @Override //这个方法基本上会出现在切换输入法类型，点击回车（完成、搜索、发送、下一步）点击输入法右上角隐藏按钮会触发。
        public boolean finishComposingText() {
            //结束组合文本输入的时候
            Log.e("hzm", "finishComposingText");
            return true;
        }

    }

    /**
     *  文字输入的回调
     */

    OnTextInputListener mOnTextInputListener;
    public interface OnTextInputListener {
        void onTextFinsh(PictureModel pictureModel);
    }

    public void setOnTextInputListener(OnTextInputListener mOnTextInputListener) {
        mOnTextInputListener = mOnTextInputListener;
    }


}


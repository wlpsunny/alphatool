package com.sensoro.loratool.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import com.sensoro.loratool.R;

/**
 * Created by sensoro on 16/10/11.
 */

public class BatteryView extends View {

    private Paint mBatteryPaint;
    private Paint mPowerPaint;
    private float mBatteryStroke = 2.0f;

    /**
     * 屏幕的高宽
     *
     * @param context
     */
    private int measureWidth;
    private int measureHeight;

    /**
     * 电池参数
     *
     * @param context
     */
    private float mBatteryHeight = 30.0f;// 电池高度
    private float mBatteryWidth = 80.0f;// 电池的宽度
    private float mCapHeight = (float) (mBatteryHeight*0.5);
    private float mCapWidth = (float) (mBatteryWidth*0.1);
    private int mBattery = 0;

    /**
     * 电池电量
     *
     * @param context
     */
    private float mPowerPadding = 5.0f;
    private float mPowerHeight = mBatteryHeight - mBatteryStroke
            - mPowerPadding;
    private float mPowerWidth = mBatteryWidth - mBatteryStroke - mPowerPadding
            * 2;// 电池身体的总宽度
    private float mPower = 0f;

    /**
     * 矩形
     */
    private RectF mBatteryRectF;
    private RectF mCapRectF;
    private RectF mPowerRectF;
    private boolean isMeasure=false;

    public BatteryView(Context context) {
        super(context);
        initView();
    }

    public BatteryView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public BatteryView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView();
    }

    public void setBattery(final float length) {
//        this.mBattery = length;
//        measure(MeasureSpec.EXACTLY,MeasureSpec.EXACTLY);
        if(!isMeasure){
            new Thread(new Runnable() {
                @Override
                public void run() {
                    while (!isMeasure){
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    float rate = length / 100f;
                    mPowerWidth = rate*(mBatteryWidth-8);
                    mPowerRectF.set(4, 4, mPowerWidth, mBatteryHeight-4);
                    mCapHeight = (float) (mBatteryHeight*0.5);
                    mBatteryRectF.set(0,0,mBatteryWidth,mBatteryHeight);
                    mCapRectF.set(mBatteryWidth,mBatteryHeight/2-mCapHeight/2,measureWidth,mBatteryHeight/2+mCapHeight/2);
                    postInvalidate();

                }
            }).start();
        }else{
            float rate = length / 100f;
            this.mPowerWidth = rate*(mBatteryWidth-8);
            this.mPowerRectF.set(4, 4, mPowerWidth, mBatteryHeight-4);
            mCapHeight = (float) (mBatteryHeight*0.5);
            mBatteryRectF.set(0,0,mBatteryWidth,mBatteryHeight);
            mCapRectF.set(mBatteryWidth,mBatteryHeight/2-mCapHeight/2,measureWidth,mBatteryHeight/2+mCapHeight/2);
            this.postInvalidate();
        }

    }


    private void initView() {
        /**
         * 设置电池画笔
         */
        mBatteryPaint = new Paint();
        mBatteryPaint.setColor(Color.GRAY);
        mBatteryPaint.setStrokeWidth(mBatteryStroke);
        mBatteryPaint.setStyle(Paint.Style.STROKE);
        mBatteryPaint.setAntiAlias(true);
        /**
         * 电量画笔
         */
        mPowerPaint = new Paint();
        mPowerPaint.setColor(getResources().getColor(R.color.slide_menu_bg));
        mPowerPaint.setStyle(Paint.Style.FILL);
        mPowerPaint.setStrokeWidth(mBatteryStroke);
        mPowerPaint.setAntiAlias(true);
        /**
         * 设置电池矩形
         */
        mBatteryRectF = new RectF(0, 0, mBatteryWidth,
                mBatteryHeight);

        /**
         * 设置电池盖矩形
         */

        mCapRectF = new RectF(mBatteryWidth, mBatteryHeight/2-mCapHeight/2,
                mBatteryWidth+mCapWidth, mBatteryHeight / 2 + mCapHeight / 2);

        /**
         * 设置电量的矩形
         */

        mPowerRectF = new RectF(0, 0, 0, mBatteryHeight);

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        measureHeight = MeasureSpec.getSize(heightMeasureSpec);
        measureWidth = MeasureSpec.getSize(widthMeasureSpec);
        mBatteryWidth = (float) (measureWidth-measureWidth*0.1);
        mBatteryHeight = measureHeight;
        isMeasure = true;
        setMeasuredDimension(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.save();
//        canvas.translate(measureWidth /2 + 150 , measureHeight/2  );
//        canvas.rotate(270);
//        canvas.scale(0.6f, 0.8f, measureWidth /2 + 150,  measureHeight/2);
        canvas.drawRoundRect(mBatteryRectF, 2.0f, 2.0f, mBatteryPaint);
        canvas.drawRoundRect(mCapRectF, 2.0f, 2.0f, mBatteryPaint);

        canvas.drawRect(mPowerRectF, mPowerPaint);

        canvas.restore();

    }
}
/*
 * Copyright (C) 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.drenit.thevolumenormaliser;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

class SoundLevelView extends View {
	private Drawable mGreen;
	private Drawable mRed;
	private Paint mBackgroundPaint;
	private Paint mLinePaint;
	
	private int mHeight;
	private int mWidth;
	
	private double mThreshold = 0;
	private int mVol = 0;
	
	
	public SoundLevelView(Context context, AttributeSet attrs) {
        super(context, attrs);  
 
        mGreen = context.getResources().getDrawable(R.drawable.greenbar);
		mRed = context.getResources().getDrawable(R.drawable.redbar);
		mHeight = mGreen.getIntrinsicHeight();
		setMinimumHeight(mHeight*10);
		mWidth  = mGreen.getIntrinsicWidth();
		setMinimumWidth(mWidth);
		
		mBackgroundPaint = new Paint();
        mBackgroundPaint.setColor(Color.BLACK);
        
        mLinePaint = new Paint();
        mLinePaint.setColor(Color.WHITE);
        mLinePaint.setStrokeWidth(5);
	}
	
	public void setLevel(int volume, double threshold) {
		if (volume == mVol && threshold == mThreshold) return;
		mVol = volume;
		mThreshold = threshold;
		invalidate();
	}
	
	@Override
	public void onDraw(Canvas canvas) {
		canvas.drawPaint(mBackgroundPaint);
		float pos = (float) ((11 - mThreshold) * mHeight);
		canvas.drawLine(5, pos, mWidth, pos, mLinePaint);
		for (int i=0; i<= mVol; i++) {
			Drawable bar;
			if (i<mThreshold) bar = mGreen;
			else             bar = mRed;

			bar.setBounds(0, (10-i)*mHeight, mWidth, (10-i+1)*mHeight);
			bar.draw(canvas);
		}
	}
}
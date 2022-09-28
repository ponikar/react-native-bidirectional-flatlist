package com.reactnativebidirectionalflatlist.scroll;

import android.util.Log;
import android.view.View;
import android.widget.OverScroller;

import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import com.facebook.react.uimanager.ThemedReactContext;

import com.facebook.common.logging.FLog;
import com.facebook.react.common.ReactConstants;
import com.facebook.react.views.scroll.ReactScrollView;

import java.lang.reflect.Field;

public class ScrollView extends ReactScrollView {

  private OverScroller mScroller;
  private boolean mTriedToGetScroller;
  protected double mShiftHeight = 0;
  protected double mShiftOffset = 0;

  public ScrollView(ThemedReactContext context) {
    super(context, null);
  }

  public void setShiftHeight(double shiftHeight) {
    mShiftHeight = shiftHeight;
    Log.d("ScrollView", "set shiftHeight " + shiftHeight);
  }

  public void setShiftOffset(double shiftOffset) {
    mShiftOffset = shiftOffset;
    Log.d("ScrollView", "set shiftOffset " + shiftOffset);
  }

  @Override
  public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
    super.onLayoutChange(v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom);
    int scrollWindowHeight = getHeight() - getPaddingBottom() - getPaddingTop();
    if(mShiftHeight != 0 && mShiftOffset <= getScrollY() + scrollWindowHeight / 2) {
      // correct
      scrollTo(0, getScrollY() + (int)mShiftHeight);
      if(getOverScrollerFromParent() != null && !getOverScrollerFromParent().isFinished()) {
        // get current directed velocity from scroller
        int direction = getOverScrollerFromParent().getFinalY() - getOverScrollerFromParent().getStartY() > 0 ? 1 : -1;
        float velocity = getOverScrollerFromParent().getCurrVelocity() * direction;
        // stop and restart animation again
        getOverScrollerFromParent().abortAnimation();
        mScroller.fling(
          getScrollX(), // startX
          getScrollY(), // startY
          0, // velocityX
          (int)velocity, // velocityY
          0, // minX
          0, // maxX
          0, // minY
          Integer.MAX_VALUE, // maxY
          0, // overX
          scrollWindowHeight / 2 // overY
        );
        ViewCompat.postInvalidateOnAnimation(this);
      }
    }
    mShiftHeight = 0;
    mShiftOffset = 0;
  }

  @Nullable
  private OverScroller getOverScrollerFromParent() {
    if(mTriedToGetScroller) {
      return mScroller;
    }
    mTriedToGetScroller = true;
    Field field = null;
    try {
      field = ReactScrollView.class.getDeclaredField("mScroller");
      field.setAccessible(true);
    } catch (NoSuchFieldException e) {
      FLog.w(
        "ScrollView",
        "Failed to get mScroller field for ScrollView! "
          + "This app will exhibit the bounce-back scrolling bug :(");
    }

    if(field != null) {
      Object scrollerValue = null;
      try {
        scrollerValue = field.get(this);
        if (scrollerValue instanceof OverScroller) {
          mScroller = (OverScroller) scrollerValue;
        } else {
          FLog.w(
            ReactConstants.TAG,
            "Failed to cast mScroller field in ScrollView (probably due to OEM changes to AOSP)! "
              + "This app will exhibit the bounce-back scrolling bug :(");
          mScroller = null;
        }
      } catch (IllegalAccessException e) {
        throw new RuntimeException("Failed to get mScroller from ScrollView!", e);
      }
    }
    return mScroller;
  }
}

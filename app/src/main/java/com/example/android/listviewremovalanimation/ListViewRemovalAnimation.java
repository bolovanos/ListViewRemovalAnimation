/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.example.android.listviewremovalanimation;

import java.util.ArrayList;
import java.util.HashMap;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewTreeObserver;
import android.widget.ListView;

/**
 * This example shows how to use a swipe effect to remove items from a ListView,
 * and how to use animations to complete the swipe as well as to animate the other
 * items in the list into their final places.
 *
 * Watch the associated video for this demo on the DevBytes channel of developer.android.com
 * or on YouTube at https://www.youtube.com/watch?v=NewCSg2JKLk.
 */
public class ListViewRemovalAnimation extends Activity {

    StableArrayAdapter mAdapter;
    ListView mListView;
    BackgroundContainer mBackgroundContainer;
    boolean mSwiping = false;
    boolean mItemPressed = false;
    HashMap<Long, Integer> mItemIdTopMap = new HashMap<Long, Integer>();
    Context context;
    int mAmountOfCheeses = 16;
    int mRefillWithCheesesOn = -1;

    private static final int SWIPE_DURATION = 250;
    private static final int MOVE_DURATION = 150;
    private final static String TAG = "ListViewRemovalAnimation";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.context = this;
        setContentView(R.layout.activity_list_view_deletion);

        mBackgroundContainer = (BackgroundContainer) findViewById(R.id.listViewBackground);
        mListView = (ListView) findViewById(R.id.listview);
        android.util.Log.d("Debug", "d=" + mListView.getDivider());
        final ArrayList<String> cheeseList = new ArrayList<String>();

        if (mAmountOfCheeses >= Cheeses.sCheeseStrings.length) {
            mAmountOfCheeses = Cheeses.sCheeseStrings.length;
        }


        for (int i = 0; i < mAmountOfCheeses; ++i) {
            cheeseList.add(Cheeses.sCheeseStrings[i]);
        }
        mAdapter = new StableArrayAdapter(this,R.layout.opaque_text_view, cheeseList,
                mTouchListener);
        mListView.setAdapter(mAdapter);
    }

    /**
     * Handle touch events to fade/move dragged items as they are swiped out
     */
    private View.OnTouchListener mTouchListener = new View.OnTouchListener() {

        float mDownX;
        private int mSwipeSlop = -1;

        @Override
        public boolean onTouch(final View v, MotionEvent event) {


            if (mRefillWithCheesesOn < mListView.getChildCount()) {
                mRefillWithCheesesOn = mListView.getChildCount();
            }

            if (mSwipeSlop < 0) {
                mSwipeSlop = ViewConfiguration.get(ListViewRemovalAnimation.this).
                        getScaledTouchSlop();
            }
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    if (mItemPressed) {
                        // Multi-item swipes not handled
                        Log.d(TAG, "MotionEvent.ACTION_DOWN-a1");
                        return false;
                    }
                    Log.d(TAG, "MotionEvent.ACTION_DOWN-a2");
                    mItemPressed = true;
                    mDownX = event.getX();
                    break;
                case MotionEvent.ACTION_CANCEL:
                    Log.d(TAG, "MotionEvent.ACTION_CANCEL-b1");
                    v.setAlpha(1);
                    v.setTranslationX(0);
                    mItemPressed = false;
                    break;
                case MotionEvent.ACTION_MOVE:
                {
                    Log.d(TAG, "MotionEvent.ACTION_MOVE-c1");
                    float x = event.getX() + v.getTranslationX();
                    float deltaX = x - mDownX;
                    float deltaXAbs = Math.abs(deltaX);
                    if (!mSwiping) {
                        Log.d(TAG, "MotionEvent.ACTION_MOVE-c2");
                        if (deltaXAbs > mSwipeSlop) {
                            Log.d(TAG, "MotionEvent.ACTION_MOVE-c3");
                            mSwiping = true;
                            mListView.requestDisallowInterceptTouchEvent(true);
                            mBackgroundContainer.showBackground(v.getTop(), v.getHeight());
                        }
                    }
                    if (mSwiping) {
                        Log.d(TAG, "MotionEvent.ACTION_MOVE-c4");
                        v.setTranslationX((x - mDownX));
                        v.setAlpha(1 - deltaXAbs / v.getWidth());
                    }
                }
                break;
                case MotionEvent.ACTION_UP:
                {
                    Log.d(TAG, "MotionEvent.ACTION_UP-d1");
                    // User let go - figure out whether to animate the view out, or back into place
                    if (mSwiping) {
                        Log.d(TAG, "MotionEvent.ACTION_UP-d2");
                        float x = event.getX() + v.getTranslationX();
                        float deltaX = x - mDownX;
                        float deltaXAbs = Math.abs(deltaX);
                        float fractionCovered;
                        float endX;
                        float endAlpha;
                        final boolean remove;
                        if (deltaXAbs > v.getWidth() / 4) {
                            Log.d(TAG, "MotionEvent.ACTION_UP-d3");
                            // Greater than a quarter of the width - animate it out
                            fractionCovered = deltaXAbs / v.getWidth();
                            endX = deltaX < 0 ? -v.getWidth() : v.getWidth();
                            endAlpha = 0;
                            remove = true;
                        } else {
                            Log.d(TAG, "MotionEvent.ACTION_UP-d4");
                            // Not far enough - animate it back
                            fractionCovered = 1 - (deltaXAbs / v.getWidth());
                            endX = 0;
                            endAlpha = 1;
                            remove = false;
                        }
                        // Animate position and alpha of swiped item
                        // NOTE: This is a simplified version of swipe behavior, for the
                        // purposes of this demo about animation. A real version should use
                        // velocity (via the VelocityTracker class) to send the item off or
                        // back at an appropriate speed.
                        long duration = (int) ((1 - fractionCovered) * SWIPE_DURATION);
                        mListView.setEnabled(false);
                        v.animate().setDuration(duration).
                                alpha(endAlpha).translationX(endX).
                                withEndAction(new Runnable() {
                                    @Override
                                    public void run() {
                                        // Restore animated values
                                        v.setAlpha(1);
                                        v.setTranslationX(0);
                                        if (remove) {
                                            Log.d(TAG, "MotionEvent.ACTION_UP-d5");
                                            animateRemoval(mListView, v);
                                        } else {
                                            Log.d(TAG, "MotionEvent.ACTION_UP-d6");
                                            mBackgroundContainer.hideBackground();
                                            mSwiping = false;
                                            mListView.setEnabled(true);
                                        }
                                    }
                                });
                    }
                }
                mItemPressed = false;
                break;
                default:
                    return false;
            }
            return true;
        }
    };

    /**
     * This method animates all other views in the ListView container (not including ignoreView)
     * into their final positions. It is called after ignoreView has been removed from the
     * adapter, but before layout has been run. The approach here is to figure out where
     * everything is now, then allow layout to run, then figure out where everything is after
     * layout, and then to run animations between all of those start/end positions.
     */
    private void animateRemoval(final ListView listview, View viewToRemove) {

        Log.d(TAG, "...animateRemoval, mItemIdTopMapA: " + mItemIdTopMap);
        int firstVisiblePosition = listview.getFirstVisiblePosition();
        for (int i = 0; i < listview.getChildCount(); ++i) {
            View child = listview.getChildAt(i);
            if (child != viewToRemove) {
                int position = firstVisiblePosition + i;
                long itemId = mAdapter.getItemId(position);
                mItemIdTopMap.put(itemId, child.getTop());
            }
        }
        Log.d(TAG, "...animateRemoval, mItemIdTopMapB: " + mItemIdTopMap);
        // Delete the item from the adapter
        int position = mListView.getPositionForView(viewToRemove);
        mAdapter.remove(mAdapter.getItem(position));

        final ViewTreeObserver observer = listview.getViewTreeObserver();
        observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            public boolean onPreDraw() {
                observer.removeOnPreDrawListener(this);
                Log.d(TAG, "...animateRemoval, onPreDraw");
                boolean firstAnimation = true;
                int firstVisiblePosition = listview.getFirstVisiblePosition();
                for (int i = 0; i < listview.getChildCount(); ++i) {
                    final View child = listview.getChildAt(i);
                    int position = firstVisiblePosition + i;
                    long itemId = mAdapter.getItemId(position);
                    Integer startTop = mItemIdTopMap.get(itemId);
                    int top = child.getTop();
                    if (startTop != null) {
                        if (startTop != top) {
                            int delta = startTop - top;
                            child.setTranslationY(delta);
                            child.animate().setDuration(MOVE_DURATION).translationY(0);
                            if (firstAnimation) {
                                child.animate().withEndAction(new Runnable() {
                                    public void run() {
                                        mBackgroundContainer.hideBackground();
                                        mSwiping = false;
                                        mListView.setEnabled(true);
                                    }
                                });
                                firstAnimation = false;
                            }
                        }
                    } else {
                        // Animate new views along with the others. The catch is that they did not
                        // exist in the start state, so we must calculate their starting position
                        // based on neighboring views.
                        int childHeight = child.getHeight() + listview.getDividerHeight();
                        startTop = top + (i > 0 ? childHeight : -childHeight);
                        int delta = startTop - top;
                        child.setTranslationY(delta);
                        child.animate().setDuration(MOVE_DURATION).translationY(0);
                        if (firstAnimation) {
                            child.animate().withEndAction(new Runnable() {
                                public void run() {
                                    mBackgroundContainer.hideBackground();
                                    mSwiping = false;
                                    mListView.setEnabled(true);
                                }
                            });
                            firstAnimation = false;
                        }
                    }
                }
                mItemIdTopMap.clear();
                Log.d(TAG, "|   ...mListView.getChildCount(): " + mListView.getChildCount()
                        + ", mAdapter.getCount():" + mAdapter.getCount());


                if (mListView.getChildCount() == mRefillWithCheesesOn - 3) {
                    final ArrayList<String> cheeseList = new ArrayList<String>();
                    for (int i = 0; i < mAmountOfCheeses; ++i) {
                        cheeseList.add(Cheeses.sCheeseStrings[i]);
                    }
                    mAdapter = new StableArrayAdapter(context,R.layout.opaque_text_view, cheeseList,
                            mTouchListener);
                    mListView.setAdapter(mAdapter);
                    Log.d(TAG, "|    ...mListView updated");
                }


                return true;
            }
        });
    }

}

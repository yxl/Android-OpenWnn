/*
 * Copyright (C) 2008,2009  OMRON SOFTWARE Co., Ltd.
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

package jp.co.omronsoft.openwnn;

import java.lang.Math;
import java.lang.CharSequence;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.EditText;
import android.graphics.Paint;
import android.text.method.MovementMethod;

import android.util.Log;

/**
 * The view class of candidate text.
 * <br>
 * This is the body part of view used by <code>TextCandidatesViewManager</code>.
 *
 * @author Copyright (C) 2009, OMRON SOFTWARE CO., LTD.  All Rights Reserved.
 */
public class TextCandidatesView extends EditText {
    /** Touch position's tolerance level for selecting a candidate */
    private static final float THRESH = 20.0F;
    /** Previous touch position (X axis) */
    private float mPreviousTouchX = -1.0F;
    /** Previous touch position (Y axis) */
    private float mPreviousTouchY = -1.0F;

    /**
     * Constructor
     *
     * @param context       The context
     */
    public TextCandidatesView(Context context) {
        super(context);
    }

    /**
     * Constructor
     *
     * @param context       The context
     * @param attrs         The set of attributes
     */
    public TextCandidatesView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * Constructor
     *
     * @param context       The context
     * @param attrs         The set of attributes
     * @param defStyle      the style
     */
    public TextCandidatesView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    /** Manager which uses this class instance */
    public TextCandidatesViewManager mParent;
    /** Array matching a cursor position and a candidate ID */
    public int[] mPositionToWordIndexArray;
    /** Array of candidates */
    public WnnWord[] mWnnWordArray;
    /** Start position of a candidate on the text view */
    public int[] mStartPositionArray;
    /** End position of a candidate on the text view */
    public int[] mEndPositionArray;

    /**
     * Judge whether the candidate on the touch position is selected.
     *
     * @param event  event of the motion
     */
    private boolean isSelectingCandidate(MotionEvent event) {
        if (mPreviousTouchX == -1.0 || mPreviousTouchY == -1.0) {
            return false;
        }
	
        if (Math.abs(mPreviousTouchX - event.getX()) < THRESH &&
            Math.abs(mPreviousTouchY - event.getY()) < THRESH) {
            return true;
        } else {
            return false;
        }
    }

    /** @see android.widget.EditText.onTouchEvent */
    @Override public boolean onTouchEvent(MotionEvent event) {
        /* Move cursor by TextEvent  */
        MovementMethod aMethod = getDefaultMovementMethod();
        if (aMethod != null) {
            int currentAction = event.getAction();
            event.setAction(MotionEvent.ACTION_UP);
            aMethod.onTouchEvent(this, this.getText(), event);
            event.setAction(currentAction);
        } else {
            return false;
        }

        /* after moving cursor, high-light a candidate at the cursor position */
        setCursorVisible(false);
        int position = getSelectionEnd();
        if(position == 0){
        	return true;
        }
        int wordIndex = mPositionToWordIndexArray[position];
        WnnWord aWord = null;
        if (wordIndex != -1) {
            aWord = mWnnWordArray[wordIndex];
            int startPosition = mStartPositionArray[wordIndex];
            int endPosition = mEndPositionArray[wordIndex];
            setSelection(startPosition, endPosition);
            setCursorVisible(true);
            invalidate();
        }

        /* event handling for candidate selection */
        try {
            switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mPreviousTouchX = event.getX();
                mPreviousTouchY = event.getY();
                return true;
            case MotionEvent.ACTION_UP:
                if (aWord != null) {
                    mParent.selectCandidate(aWord);
                    return true;
                } else {
                    return false;
                }
            case MotionEvent.ACTION_MOVE:
                return true;
            default:
                return true;
            }
        } catch (Exception ex) {
        }

        return false;
    }
}


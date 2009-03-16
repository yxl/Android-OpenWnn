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

package jp.co.omronsoft.openwnn.EN;


import jp.co.omronsoft.openwnn.*;
import junit.framework.Assert;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.media.MediaPlayer;
import android.os.Vibrator;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.View.OnTouchListener;
import android.widget.LinearLayout;
import android.widget.ScrollView;

/**
 * The default candidates view manager using <code>EditView</code>.
 *
 * @author Copyright (C) 2009, OMRON SOFTWARE CO., LTD.  All Rights Reserved.
 */
public class TextCandidatesViewManagerEN extends TextCandidatesViewManager {

    /**
     * Constructor
     *
     * @param displayLimit      The limit of display
     */
    public TextCandidatesViewManagerEN(int displayLimit) {
           super(displayLimit);
    }
	
    /**
     * Display the candidates.
     * @param converter  <code>WnnEngine</code> which holds candidates.
     * @param dispFirst  Whether it is the first time displaying the candidates
     * @param maxLine    The maximum number of displaying lines
     */

	synchronized protected void displayCandidates(WnnEngine converter, boolean dispFirst, int maxLine) {
        if (converter == null) {
            return;
        }

        /* Clear for the first time */
        if (dispFirst) {
            clearCandidates();
            this.mConverter = converter;
            mLastWord = null;
        }

        /* Concatinate the candidates already got and the last one in dispFirst mode */
        StringBuffer tmp = new StringBuffer();
        tmp.append(mCandidates);
        if ((!dispFirst) && (mLastWord != null) && (mLastWord.candidate.length() != 0)) {
            mWnnWordArray[mWordCount] = mLastWord;
            mStartPositionArray[mWordCount] = tmp.length();
            tmp.append(mLastWord.candidate);
            mEndPositionArray[mWordCount] = tmp.length();
            tmp.append(CANDIDATE_SEPARATOR);
            mWordCount++;
            mLastWord = null;
        }


        /* Get candidates */
        WnnWord result;
        while ((result = converter.getNextCandidate()) != null && mWordCount < mDisplayLimit) {
            mWnnWordArray[mWordCount] = result;
            mStartPositionArray[mWordCount] = tmp.length();
            tmp.append(result.candidate);
            mEndPositionArray[mWordCount] = tmp.length();
            tmp.append(CANDIDATE_SEPARATOR);
            mWordCount++;
            if (maxLine == -1) {
                continue;
            }
	    
            mViewBodyText.setText(tmp);
            /* [Should be improved...] 
               The view has to be laid out to use getLineCount().
               For the very first time, layout has to be called manually.
               However, the code below doesn't work ... */
            int lineNum = mViewBodyText.getLineCount();
            if (lineNum == 0) {
                lineNum = countLineUsingMeasureText(mViewBodyText.getText());
                if (lineNum == -1) {
                    return;
                }
            }
            if (dispFirst &&  lineNum > maxLine) {
                mLastWord = result;
                if (mWordCount > 1) {
                    tmp.delete(mStartPositionArray[mWordCount - 1], tmp.length());
                } else {
                    return;
                }
                mWordCount--;
                mWordCountInNormalView = mWordCount;
                break;
            }
        }

        /* save the candidate string */
        mCandidates.delete(0, mCandidates.length());
        mCandidates.append(tmp);
        for (int i = 0; i < mWordCount; i++) {
            int j;
            for (j = mStartPositionArray[i]; j <= mEndPositionArray[i]; j++) {
                mPositionToWordIndexArray[j] = i;
            }
            mPositionToWordIndexArray[j] = mPositionToWordIndexArray[j + 1] = -1;
        }
        
  

        if (mWordCount < 1) {
            mWnn.setCandidatesViewShown(true);
            return;
        }

        int displayEndCount = 
            ((mViewType == CandidatesViewManager.VIEW_TYPE_NORMAL) &&
             (mWordCountInNormalView != -1))
            ? mWordCountInNormalView : mWordCount;
        int endPosition = mEndPositionArray[displayEndCount - 1];
        endPosition += CANDIDATE_SEPARATOR.length();
        Assert.assertTrue(endPosition <= mCandidates.length());

        mViewBodyText.setText(mCandidates.subSequence(0, endPosition));
        addNewlineIfNecessary();

        /* Set EditText */
        mViewBodyText.setSelection(0, 0);
        mViewBodyText.setCursorVisible(false);
        mViewBodyText.requestFocus();

        if (mAutoHideMode && !(mViewBody.isShown())) {
            mWnn.setCandidatesViewShown(true);
        }
        return;
    }

    /** @see CandidatesViewManager */
    public void clearCandidates() {
        mViewBodyText.setText("");
	
        mCandidates.delete(0, mCandidates.length());
        mWordCount = 0;
        mWordCountInNormalView = -1;
        mViewBodyText.setCursorVisible(false);
        mLastWord = null;
        if (mAutoHideMode && mViewBody.isShown()) {
            mWnn.setCandidatesViewShown(true);
        }
    }

}


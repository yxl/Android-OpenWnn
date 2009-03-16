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
public class TextCandidatesViewManager implements CandidatesViewManager, OnTouchListener {
    /** Height of a line */
    public static final int LINE_HEIGHT = 47;
    /** Number of lines to display (Portrait) */
    public static final int LINE_NUM_PORTRAIT       = 2;
    /** Number of lines to full-display (Portrait) */
    public static final int LINE_NUM_PORTRAIT_FULL  = 5;
    /** Number of lines to display (Landscape) */
    public static final int LINE_NUM_LANDSCAPE      = 1;
    /** Number of lines to full-display (Landscape) */
    public static final int LINE_NUM_LANDSCAPE_FULL = 6;

    /** Separator */
    public static final String CANDIDATE_SEPARATOR = "\u3000 ";

    /** Body view of the candidates list */
    protected ViewGroup  mViewBody;
    /** Scroller of <code>mViewBodyText</code> */
    protected ScrollView mViewBodyScroll;
    /** Body of the list view */
    protected TextCandidatesView   mViewBodyText;
    
    /** <code>OpenWnn</code> instance using this manager */
    protected OpenWnn    mWnn;
    /** View type (VIEW_TYPE_NORMAL or VIEW_TYPE_FULL or VIEW_TYPE_CLOSE) */
    protected int        mViewType;
    /** Portrait display(<code>true</code>) or landscape(<code>false</code>) */
    protected boolean    mPortrait;

    protected int        mViewWidth;
    protected boolean    mAutoHideMode;
    protected WnnEngine  mConverter;
    protected int        mDisplayLimit;
    protected WnnWord    mLastWord;

    private Vibrator mVibrator = null;
    private MediaPlayer mSound = null;

    protected int mWordCount;
    protected int mWordCountInNormalView;
    protected int[] mPositionToWordIndexArray;
    protected StringBuffer mCandidates;
    protected int[] mStartPositionArray;
    protected int[] mEndPositionArray;
    protected WnnWord[] mWnnWordArray;

    /**
     * Constructor
     */
    public TextCandidatesViewManager() {
        this(300);
    }

    /**
     * Constructor
     *
     * @param displayLimit      The limit of display
     */
    public TextCandidatesViewManager(int displayLimit) {
        this.mDisplayLimit = displayLimit;
        this.mCandidates = new StringBuffer();
        this.mStartPositionArray = new int[displayLimit];
        this.mEndPositionArray = new int[displayLimit];
        this.mPositionToWordIndexArray = new int[52 * 300];
        this.mWnnWordArray = new WnnWord[displayLimit];
    }

    /**
     * Set auto-hide mode.
     * @param hide      <code>true</code> if the view will hidden when no candidate exists;
     *                  <code>false</code> if the view is always shown.
     */
    public void setAutoHide(boolean hide) {
        mAutoHideMode = hide;
    }

    /** @see CandidatesViewManager */
    public View initView(OpenWnn parent, int width, int height) {
        mWnn = parent;
        mViewWidth = width;

        mViewBody = (ViewGroup)parent.getLayoutInflater().inflate(R.layout.candidates, null);
        mViewBodyScroll = (ScrollView)mViewBody.findViewById(R.id.candview_scroll);
        mViewBodyScroll.setOnTouchListener(this);

        mViewBodyText = (TextCandidatesView)mViewBody.findViewById(R.id.text_candidates_view);
        mViewBodyText.setOnTouchListener(this);
        mViewBodyText.setTextSize(18.0f);
        mViewBodyText.setLineSpacing(6.0f, 1.5f);
        mViewBodyText.setIncludeFontPadding(false);
        mViewBodyText.setFocusable(true);
        mViewBodyText.setCursorVisible(false);

        mViewBodyText.mPositionToWordIndexArray = mPositionToWordIndexArray;
        mViewBodyText.mWnnWordArray = mWnnWordArray;
        mViewBodyText.mStartPositionArray = mStartPositionArray;
        mViewBodyText.mEndPositionArray = mEndPositionArray;
        mViewBodyText.mParent = this;

        mPortrait = (height > 450)? true : false;
        setViewType(CandidatesViewManager.VIEW_TYPE_CLOSE);

        return mViewBody;
    }

    /** @see CandidatesViewManager */
    public View getCurrentView() {
        return mViewBody;
    }

    /** @see CandidatesViewManager */
    public void setViewType(int type) {
        mViewType = type;

        boolean readMore = false;
        int height;
        if (type == CandidatesViewManager.VIEW_TYPE_CLOSE) {
            height = 1;
        } else if (mPortrait) {
            if (type == CandidatesViewManager.VIEW_TYPE_NORMAL) {
                mViewBodyScroll.scrollTo(0, 0);
                height = LINE_HEIGHT * LINE_NUM_PORTRAIT;
                mViewBodyText.setMaxLines(LINE_NUM_PORTRAIT);
                mViewBodyText.setLines(LINE_NUM_PORTRAIT);
                if (mWordCount > 1) {
                    int displayEndCount = (mWordCountInNormalView != -1)	? mWordCountInNormalView : mWordCount;
                    int endPosition = mEndPositionArray[displayEndCount - 1];
                    mViewBodyText.setText(mCandidates.subSequence(0, endPosition));
                    mViewBodyText.setSelection(0, 0);
                    mViewBodyText.setCursorVisible(false);
                }
            } else {
                height = LINE_HEIGHT * LINE_NUM_PORTRAIT_FULL;
                mViewBodyText.setMaxLines(mDisplayLimit);
                readMore = true;
            }
        } else {
            if (type == CandidatesViewManager.VIEW_TYPE_NORMAL) {
                mViewBodyScroll.scrollTo(0, 0);
                height = LINE_HEIGHT * LINE_NUM_LANDSCAPE;
                mViewBodyText.setMaxLines(LINE_NUM_LANDSCAPE);
                mViewBodyText.setLines(LINE_NUM_LANDSCAPE);
                if (mWordCount > 1) {
                    int displayEndCount = (mWordCountInNormalView != -1)	? mWordCountInNormalView : mWordCount;
                    int endPosition = mEndPositionArray[displayEndCount - 1];
                    mViewBodyText.setText(mCandidates.subSequence(0, endPosition));
                    mViewBodyText.setSelection(0, 0);
                    mViewBodyText.setCursorVisible(false);
                }
            } else {
                height = LINE_HEIGHT * LINE_NUM_LANDSCAPE_FULL;
                mViewBodyText.setMaxLines(mDisplayLimit);
                readMore = true;
            }
        }

        mViewBody.updateViewLayout(mViewBodyScroll,
                                   new LinearLayout.LayoutParams(mViewWidth, height));

        addNewlineIfNecessary();

        if (readMore) {
            displayCandidates(this.mConverter, false, -1);
        }
    }

    /** @see CandidatesViewManager */
    public int getViewType() {
        return mViewType;
    }

    /** @see CandidatesViewManager */
    public void displayCandidates(WnnEngine converter) {
        int maxLine = getMaxLine();
        displayCandidates(converter, true, maxLine);
    }

    /** @see CandidatesViewManager */
    private int getMaxLine() {
        int maxLine = -1;
        if (mViewType == CandidatesViewManager.VIEW_TYPE_NORMAL) {
            if (mPortrait) {
                maxLine = LINE_NUM_PORTRAIT;
            } else {
                maxLine = LINE_NUM_LANDSCAPE;
            }
        }
        return maxLine;
    }

    /**
     * Add a new line if necessary.
     */
    protected void addNewlineIfNecessary() {
        int maxLine = getMaxLine();
        int lineNum = mViewBodyText.getLineCount();
        if (lineNum == 0) {
            lineNum = countLineUsingMeasureText(mViewBodyText.getText());
        }
        int textLength = mViewBodyText.length();
        mPositionToWordIndexArray[textLength + 1] = -1;
        mPositionToWordIndexArray[textLength + 2] = -1;
        mPositionToWordIndexArray[textLength + 3] = -1;
        for (int i = 0; i < maxLine - lineNum; i++) {
            mViewBodyText.append("\n");
        }
        if (mPortrait && maxLine == -1 && lineNum == 1) {
            mViewBodyText.append("\n");
        }
        return;
    }

    /**
     * Count lines using <code>Paint.measureText()</code>
     *
     * @param text  The text to display
     * @return  Number of lines
     */
    protected int countLineUsingMeasureText(CharSequence text) {
        StringBuffer tmpText = new StringBuffer(text);
        mStartPositionArray[mWordCount] = tmpText.length();
        int padding =
            ViewConfiguration.getScrollBarSize() +
            mViewBodyText.getPaddingLeft() +       
            mViewBodyText.getPaddingRight();       
        Paint p = new Paint();
        int lineCount = 1;
        int start = 0;
        for (int i = 0; i < mWordCount; i++) {
            if (tmpText.length() < start ||
                tmpText.length() < mStartPositionArray[i + 1]) {
                return 1;
            }
            float lineLength = p.measureText(tmpText, start, mStartPositionArray[i + 1]);
            lineLength *= 1.5;
            if (lineLength > (mViewWidth - padding)) {
                lineCount++;
                start = mStartPositionArray[i];
                i--;
            }
        }
        return lineCount;
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
            mWnn.setCandidatesViewShown(false);
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

        if (mAutoHideMode && mViewBody.isShown()) {
            mWnn.setCandidatesViewShown(false);
        }
    }

    /** @see CandidatesViewManager */
    public void setPreferences(SharedPreferences pref) {
        try {
            if (pref.getBoolean("key_vibration", false)) {
                mVibrator = (Vibrator)mWnn.getSystemService(Context.VIBRATOR_SERVICE);
            } else {
                mVibrator = null;
            }
            if (pref.getBoolean("key_sound", false)) {
                mSound = MediaPlayer.create(mWnn, R.raw.type);
            } else {
                mSound = null;
            }
        } catch (Exception ex) {
            Log.d("iwnn", "NO VIBRATOR");
        }
    }

    /** Touch point (X axis) */
    private float firstTouchX = 0;
    /** Touch point (Y axis) */
    private float firstTouchY = 0;

    /**
     * Whether it is the gesture to get all candidates.
     */
    private boolean isGettingAllCandidates(float downX, float downY, float upX, float upY) {
        final float threshY = 50.0F;
        return (Math.abs(downY - upY) > threshY);
    }
    
    /**
     * Implementation of OnTouchListener
     *
     * @param v         The associated view
     * @param event     The event object
     * @return          <code>true</code> if the event is processed in this listener;
     *                  <code>false</code> if the event must be passed on other listener.
     */
    synchronized public boolean onTouch(View v, MotionEvent event) {
        if (v == mViewBodyScroll) {
            mViewBodyText.setCursorVisible(false);
        } else if (v == mViewBodyText) {
        }

        try {
            switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                firstTouchX = event.getX();
                firstTouchY = event.getY();
                return true;

            case MotionEvent.ACTION_UP:
                float x = event.getX();
                float y = event.getY();
                if (firstTouchY != 0) {
                    if (mViewType == CandidatesViewManager.VIEW_TYPE_NORMAL) {
                        if (isGettingAllCandidates(firstTouchX, firstTouchY, x, y)) {
                            if (mVibrator != null) {
                                try { mVibrator.vibrate(30); } catch (Exception ex) { }
                            }
                            mWnn.onEvent(new OpenWnnEvent(OpenWnnEvent.LIST_CANDIDATES_FULL));
                            firstTouchY = 0;
                            return true;
                        }
                    } else if (mViewBodyScroll.getScrollY() == 0) {
                        if (y - firstTouchY > 50.0) {
                            if (mVibrator != null) {
                                try { mVibrator.vibrate(30); } catch (Exception ex) { }
                            }
                            mWnn.onEvent(new OpenWnnEvent(OpenWnnEvent.LIST_CANDIDATES_NORMAL));
                            return true;
                        }
                    }
                }
                return false;

            case MotionEvent.ACTION_MOVE:
                if (firstTouchY == 0) {
                    firstTouchY = event.getY();
                    return true;
                } else if (mViewType == CandidatesViewManager.VIEW_TYPE_NORMAL) {
                    return false;
                }
                return false;
            }
        } catch (Exception ex) {
            /* just ignore the event if an error occurs */
        }

        return false;
    }

    /**
     * Select a candidate.
     * <br>
     * This method notices the selected word to <code>OpenWnn</code>.
     *
     * @param word  The selected word
     */
    public void selectCandidate(WnnWord word) {
        if (mVibrator != null) {
            try { mVibrator.vibrate(30); } catch (Exception ex) { }
        }
        if (mSound != null) {
            try { mSound.seekTo(0); mSound.start(); } catch (Exception ex) { }
        }
        mWnn.onEvent(new OpenWnnEvent(OpenWnnEvent.SELECT_CANDIDATE, word));
    }

}


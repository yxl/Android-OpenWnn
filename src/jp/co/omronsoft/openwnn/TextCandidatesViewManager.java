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

import java.util.ArrayList;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.os.Vibrator;
import android.text.Layout;
import android.text.Styled;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.view.GestureDetector;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.EditText;
import android.text.TextPaint;

/**
 * The default candidates view manager class using {@link EditText}.
 *
 * @author Copyright (C) 2009 OMRON SOFTWARE CO., LTD.  All Rights Reserved.
 */
public class TextCandidatesViewManager implements CandidatesViewManager, OnTouchListener,
                                                         GestureDetector.OnGestureListener {
    /** Height of a line */
    public static final int LINE_HEIGHT = 48;
    /** Number of lines to display (Portrait) */
    public static final int LINE_NUM_PORTRAIT       = 2;
    /** Number of lines to full-display (Portrait) */
    public static final int LINE_NUM_PORTRAIT_FULL  = 5;
    /** Number of lines to display (Landscape) */
    public static final int LINE_NUM_LANDSCAPE      = 1;
    /** Number of lines to full-display (Landscape) */
    public static final int LINE_NUM_LANDSCAPE_FULL = 3;

    /** Separator */
    public static final String CANDIDATE_SEPARATOR = "\u3000 ";
    
    /** Maximum lines */
    private static final int DISPLAY_LINE_MAX_COUNT = 1000;

    /** Adjusted value for detecting the selected candidate */
    private static final int TOUCH_ADJUSTED_VALUE = 1;

    /** Body view of the candidates list */
    private ViewGroup  mViewBody;
    /** Scroller of {@code mViewBodyText} */
    private ScrollView mViewBodyScroll;
    /** Body of the list view */
    private EditText mViewBodyText;
    /** Text displayed bottom of the view when there are more candidates. */
    private TextView mReadMoreText;
    
    /** {@link OpenWnn} instance using this manager */
    private OpenWnn mWnn;
    /** View type (VIEW_TYPE_NORMAL or VIEW_TYPE_FULL or VIEW_TYPE_CLOSE) */
    private int mViewType;
    /** Portrait display({@code true}) or landscape({@code false}) */
    private boolean mPortrait;

    /** Width of the view */
    private int mViewWidth;
    /** Height of the view */
    private int mViewHeight;
    /** Whether hide the view if there is no candidates */
    private boolean mAutoHideMode;
    /** The converter to be get candidates from and notice the selected candidate to. */
    private WnnEngine mConverter;
    /** Limitation of displaying candidates */
    private int mDisplayLimit;
    /** The last displaying word */
    private WnnWord mLastWord;

    /** Vibrator for touch vibration */
    private Vibrator mVibrator = null;
    /** MediaPlayer for click sound */
    private MediaPlayer mSound = null;

    /** Number of candidates displaying */
    private int mWordCount;
    private int mWordCountInNormalView;
    /** List of word's index to convert from the position of the cursor */
    private ArrayList<Integer> mPositionToWordIndexArray;
    /** The string to display candidates */
    private StringBuffer mCandidates;
    /** List of the start position of each candidate */
    private ArrayList<Integer> mStartPositionArray;
    /** List of the end position of each candidate */
    private ArrayList<Integer> mEndPositionArray;
    /** List of candidates */
    private ArrayList<WnnWord> mWnnWordArray;

    /** Gesture detector */
    private GestureDetector mGestureDetector;
    /** The word pressed */
    private WnnWord mWord;
    /** Text on the select button */
    private String mSelectBottonText = null;
    /** Text on the cancel button */
    private String mCancelBottonText = null;

    /** Character width of the candidate area */
    private int mLineLength = 0;
    /** Word count on a single line in the candidate area */
    private int mLineWordCount = -1;

    /** {@code true} if the hardware keyboard is shown */
    private boolean mHardKeyboardHidden = true;

    /** {@code true} if the candidate delete state is selected */
    private boolean mCandidateDeleteState = false;

    /** {@code true} if the full screen mode is selected */
    private boolean mIsFullView = false;

    /** {@code true} if the selection state is started */
    private boolean mHasStartedSelect = false;

    /** {@code true} if the candidate list is created */
    private boolean mHasCreatedCandidateList = false;

    /** The event object for "touch" */
    private MotionEvent mMotionEvent = null;

    /** The offset when the candidates is flowed out the candidate window */
    private int mDisplayEndOffset = 0;
    /** {@code true} if there are more candidates to display. */
    private boolean mCanReadMore = false;
    
    /**
     * Constructor
     */
    public TextCandidatesViewManager() {
        this(-1);
    }

    /**
     * Constructor
     *
     * @param displayLimit      The limit of display
     */
    public TextCandidatesViewManager(int displayLimit) {
        this.mDisplayLimit = displayLimit;
        this.mCandidates = new StringBuffer();
        this.mStartPositionArray = new ArrayList<Integer>();
        this.mEndPositionArray = new ArrayList<Integer>();
        this.mPositionToWordIndexArray = new ArrayList<Integer>();
        this.mWnnWordArray = new ArrayList<WnnWord>();
        this.mAutoHideMode = true;
    }

    /**
     * Set auto-hide mode.
     * @param hide      {@code true} if the view will hidden when no candidate exists;
     *                  {@code false} if the view is always shown.
     */
    public void setAutoHide(boolean hide) {
        mAutoHideMode = hide;
    }

    /** @see CandidatesViewManager */
    public View initView(OpenWnn parent, int width, int height) {
        mWnn = parent;
        mViewWidth = width;

        mSelectBottonText =  mWnn.getResources().getString(R.string.button_candidate_select);
        mCancelBottonText = mWnn.getResources().getString(R.string.button_candidate_cancel);

        mViewBody = (ViewGroup)parent.getLayoutInflater().inflate(R.layout.candidates, null);

        mViewBodyScroll = (ScrollView)mViewBody.findViewById(R.id.candview_scroll);
        mViewBodyScroll.setOnTouchListener(this);

        mViewBodyText = (EditText)mViewBody.findViewById(R.id.text_candidates_view);
        mViewBodyText.setOnTouchListener(this);
        mViewBodyText.setTextSize(18.0f);
        mViewBodyText.setLineSpacing(6.0f, 1.5f);
        mViewBodyText.setIncludeFontPadding(false);
        mViewBodyText.setFocusable(true);
        mViewBodyText.setCursorVisible(false);
        mViewBodyText.setGravity(Gravity.TOP);
        
        mReadMoreText = (TextView)mViewBody.findViewById(R.id.read_more_text);
        mReadMoreText.setText(mWnn.getResources().getString(R.string.read_more));
        mReadMoreText.setTextSize(24.0f);

        mPortrait = (height > 450)? true : false;
        setViewType(CandidatesViewManager.VIEW_TYPE_CLOSE);

        mGestureDetector = new GestureDetector(this);
        return mViewBody;
    }

    /** @see CandidatesViewManager#getCurrentView */
    public View getCurrentView() {
        return mViewBody;
    }

    /** @see CandidatesViewManager#setViewType */
    public void setViewType(int type) {
        boolean readMore = setViewLayout(type);
        addNewlineIfNecessary();

        if (readMore) {
            displayCandidates(this.mConverter, false, -1);
        } else { 
            if (type == CandidatesViewManager.VIEW_TYPE_NORMAL) {
                mIsFullView = false;
                if (mDisplayEndOffset > 0) {
                    int maxLine = getMaxLine();
                    displayCandidates(this.mConverter, false, maxLine);
                } else {
                    setReadMore();
                }
            } else {
                if (mViewBody.isShown()) {
                	mWnn.setCandidatesViewShown(false);
                }
            }
        }
    }

    /**
     * Set the view layout
     *
     * @param type      View type
     * @return          {@code true} if display is updated; {@code false} if otherwise
     */
    private boolean setViewLayout(int type) {
        mViewType = type;
        boolean readMore = false;
        int height;
        if (type == CandidatesViewManager.VIEW_TYPE_CLOSE) {
        	mViewBody.setVisibility(View.GONE);
            return false;
        }
        
        mViewBody.setVisibility(View.VISIBLE);

        if (mPortrait) {
            if (type == CandidatesViewManager.VIEW_TYPE_NORMAL) {
                mViewBodyScroll.scrollTo(0, 0);
                height = LINE_HEIGHT * LINE_NUM_PORTRAIT;
                mViewBodyText.setMaxLines(LINE_NUM_PORTRAIT);
                mViewBodyText.setLines(LINE_NUM_PORTRAIT);
                if (mWordCount > 1) {
                    int displayEndCount = (mWordCountInNormalView != -1)    ? mWordCountInNormalView : mWordCount;
                    int endPosition = mEndPositionArray.get(displayEndCount - 1);
                    mViewBodyText.setText(mCandidates.subSequence(0, endPosition));
                    mViewBodyText.setSelection(0, 0);
                    mViewBodyText.setCursorVisible(false);
                }
                mViewBodyText.setMinimumHeight(height);
            } else {
                height = LINE_HEIGHT * LINE_NUM_PORTRAIT_FULL;
                mViewBodyText.setMaxLines(DISPLAY_LINE_MAX_COUNT);
                readMore = true;
                mViewBodyText.setMinimumHeight(height);
            }
        } else {
            if (type == CandidatesViewManager.VIEW_TYPE_NORMAL) {
                mViewBodyScroll.scrollTo(0, 0);
                height = LINE_HEIGHT * LINE_NUM_LANDSCAPE;
                mViewBodyText.setMaxLines(LINE_NUM_LANDSCAPE);
                mViewBodyText.setLines(LINE_NUM_LANDSCAPE);
                if (mWordCount > 1) {
                    int displayEndCount = (mWordCountInNormalView != -1)    ? mWordCountInNormalView : mWordCount;
                    int endPosition = mEndPositionArray.get(displayEndCount - 1);
                    mViewBodyText.setText(mCandidates.subSequence(0, endPosition));
                    mViewBodyText.setSelection(0, 0);
                    mViewBodyText.setCursorVisible(false);
                }
                mViewBodyText.setMinimumHeight(height);
            } else {
                height = LINE_HEIGHT * LINE_NUM_LANDSCAPE_FULL * ((!mHardKeyboardHidden) ? 2 : 1);
                mViewBodyText.setMaxLines(DISPLAY_LINE_MAX_COUNT);
                readMore = true;
                mViewBodyText.setMinimumHeight(height);
            }
        }

        mViewBody.updateViewLayout(mViewBodyScroll,
                                   new FrameLayout.LayoutParams(mViewWidth, height));

        mViewHeight = height;
        return readMore;
    }

    /** @see CandidatesViewManager#getViewType */
    public int getViewType() {
        return mViewType;
    }

    /** @see CandidatesViewManager#displayCandidates */
    public void displayCandidates(WnnEngine converter) {
        mCanReadMore = false;
        mDisplayEndOffset = 0;
        mIsFullView = false;
        int maxLine = getMaxLine();
        displayCandidates(converter, true, maxLine);
    }

    /** @see CandidatesViewManager#getMaxLine */
    private int getMaxLine() {
        int maxLine = (mPortrait) ? LINE_NUM_PORTRAIT : LINE_NUM_LANDSCAPE;
        return maxLine;
    }

    /**
     * Add a new line if necessary.
     */
    private void addNewlineIfNecessary() {
        int maxLine = getMaxLine();
        int lineNum = mViewBodyText.getLineCount();
        if (lineNum == 0) {
            lineNum = countLineUsingMeasureText(mViewBodyText.getText());
        }
        int textLength = mViewBodyText.length();
        for (int i = 0; i < 3; i++) {
            mPositionToWordIndexArray.add(textLength + i,-1);
        }
        for (int i = 0; i < maxLine - lineNum; i++) {
            mViewBodyText.append("\n");
        }
        if (mPortrait && maxLine == -1 && lineNum == 1) {
            mViewBodyText.append("\n");
        }
        return;
    }

    /**
     * Count lines using {@link Paint#measureText}.
     *
     * @param text      The text to display
     * @return          Number of lines
     */
    private int countLineUsingMeasureText(CharSequence text) {
        StringBuffer tmpText = new StringBuffer(text);
        mStartPositionArray.add(mWordCount,tmpText.length());
        int padding =
            ViewConfiguration.getScrollBarSize() +
            mViewBodyText.getPaddingLeft() +       
            mViewBodyText.getPaddingRight();       
        TextPaint p = mViewBodyText.getPaint();
        int lineCount = 1;
        int start = 0;
        for (int i = 0; i < mWordCount; i++) {
            if (tmpText.length() < start ||
                tmpText.length() < mStartPositionArray.get(i + 1)) {
                return 1;
            }
            float lineLength = measureText(p, tmpText, start, mStartPositionArray.get(i + 1));
            if (lineLength > (mViewWidth - padding)) {
                lineCount++;
                start = mStartPositionArray.get(i);
                i--;
            }
        }
        return lineCount;
    }

    /**
     * Display the candidates.
     * 
     * @param converter  {@link WnnEngine} which holds candidates.
     * @param dispFirst  Whether it is the first time displaying the candidates
     * @param maxLine    The maximum number of displaying lines
     */
    synchronized private void displayCandidates(WnnEngine converter, boolean dispFirst, int maxLine) {
        if (converter == null) {
            return;
        }

        mHasStartedSelect = false;

        /* Clear for the first time */
        if (dispFirst) {
            clearCandidates();
            this.mConverter = converter;
            mLastWord = null;
            mHasCreatedCandidateList = true;
        }

        /* Concatenate the candidates already got and the last one in dispFirst mode */
        boolean category = false;
        StringBuffer tmp = new StringBuffer();
        tmp.append(mCandidates);
        if ((!dispFirst) && (mLastWord != null) && (mLastWord.candidate.length() != 0)) {
            mLineWordCount = -1;

            StringBuffer displayText = createDisplayText(mLastWord, maxLine);

            if (category) {
                tmp.append(displayText);
                category = false;
            } else {
                mWnnWordArray.add(mWordCount, mLastWord);
                int i = 0;
                for (i = 0 ; i < tmp.length(); i++) {
                    if (!displayText.subSequence(i, i + 1).equals("\n")) {
                        break;
                    }
                }
                mStartPositionArray.add(mWordCount, tmp.length() + i);
                tmp.append(displayText);
                mEndPositionArray.add(mWordCount, tmp.length());
                tmp.append(CANDIDATE_SEPARATOR);
                mWordCount++;
            }
            mLastWord = null;
        }

        int displayLimit = mDisplayLimit;
        StringBuffer displayText;
        /* Get candidates */
        WnnWord result;
        while ((result = converter.getNextCandidate()) != null && (displayLimit == -1 || mWordCount < displayLimit)) {

            displayText = createDisplayText(result, maxLine);
            if (displayText == null) {
                continue;
            }
            
            mWnnWordArray.add(mWordCount, result);
            int i = 0;
            for (i = 0 ; i < tmp.length(); i++) {
                if (!displayText.subSequence(i, i + 1).equals("\n")) {
                    break;
                }
            }
            mStartPositionArray.add(mWordCount, tmp.length() + i);
            tmp.append(displayText);
            mEndPositionArray.add(mWordCount,tmp.length());
            tmp.append(CANDIDATE_SEPARATOR);
            mWordCount++;

            if (mIsFullView) {
                continue;
            }
        
            mViewBodyText.setText(tmp);
            int lineNum = mViewBodyText.getLineCount();
            if (lineNum == 0) {
                lineNum = countLineUsingMeasureText(mViewBodyText.getText());
                if (lineNum == -1) {
                    return;
                }
            }
            if (dispFirst &&  lineNum > maxLine) {
                if (mWordCount == 1) {
                    setViewLayout(CandidatesViewManager.VIEW_TYPE_FULL);
                    maxLine = -1;
                    mIsFullView = true;
                    continue;
                }               
                
                mCanReadMore = true;

                    mLastWord = result;
                if (mWordCount > 1) {
                    tmp.delete(mStartPositionArray.get(mWordCount - 1), tmp.length());
                    mWnnWordArray.remove(mWordCount -1);
                    mStartPositionArray.remove(mWordCount -1);
                    mEndPositionArray.remove(mWordCount -1);
                } else {
                    return;
                }
                mWordCount--;
                mWordCountInNormalView = mWordCount;
                break;
            } else {
                if (mWordCount == 1) {
                    setViewLayout(CandidatesViewManager.VIEW_TYPE_NORMAL);
                }               
            }
        }
        
        /* save the candidate string */
        mCandidates.delete(0, mCandidates.length());
        mCandidates.append(tmp);
        int j = 0;
        for (int i = 0; i < mWordCount; i++) {
            while (j <= mEndPositionArray.get(i)) {
                if (j < mStartPositionArray.get(i)) {
                    mPositionToWordIndexArray.add(j,-1);
                } else {
                    mPositionToWordIndexArray.add(j,i);
                }
                j++;
            }
            mPositionToWordIndexArray.add(j,-1);    
            mPositionToWordIndexArray.add(j + 1,-1);
        }

        if (mAutoHideMode && mWordCount < 1) {
            mWnn.setCandidatesViewShown(false);
            return;
        }

        int displayEndCount = 
            ((mViewType == CandidatesViewManager.VIEW_TYPE_NORMAL) &&
             (mWordCountInNormalView != -1))
            ? mWordCountInNormalView : mWordCount;
        int endPosition = mEndPositionArray.get(displayEndCount - 1);
        endPosition += CANDIDATE_SEPARATOR.length();

        if (mDisplayEndOffset > 0 && maxLine != -1) {
            mCanReadMore = true;
            String str = mCandidates.substring(0, mDisplayEndOffset);
            StringBuffer sub = new StringBuffer(str);
            sub.append(CANDIDATE_SEPARATOR);
            mViewBodyText.setText(sub.subSequence(0, mDisplayEndOffset + CANDIDATE_SEPARATOR.length()));
        } else {
            mViewBodyText.setText(mCandidates.subSequence(0, endPosition));
            addNewlineIfNecessary();
        }

        /* Set EditText */
        mViewBodyText.setSelection(0, 0);
        mViewBodyText.setCursorVisible(false);
        mViewBodyText.requestFocus();

        setReadMore();

        if (!(mViewBody.isShown())) {
            mWnn.setCandidatesViewShown(true);
        }
        return;
    }

    /**
     * Display {@code mReadMoreText} if there are more candidates.
     *
     */
    private void setReadMore() {
        if (mCanReadMore && !mIsFullView && !mCandidateDeleteState) {
            mReadMoreText.setHeight(mViewHeight);
            mReadMoreText.setVisibility(View.VISIBLE);
        } else {
            mReadMoreText.setVisibility(View.GONE);
        }
    }
        
    /**
     * Create the string to show in the candidate window.
     *
     * @param word      A candidate word
     * @param maxLine   The maximum number of line in the candidate window
     * @return      The string to show
     */
    private StringBuffer createDisplayText(WnnWord word, int maxLine) {
        StringBuffer tmp = new StringBuffer();
        int padding = ViewConfiguration.getScrollBarSize() + 
                      mViewBodyText.getPaddingLeft() +       
                      mViewBodyText.getPaddingRight();       
        int width = mViewWidth - padding;
        TextPaint p = mViewBodyText.getPaint();
        float newLineLength = measureText(p, word.candidate, 0, word.candidate.length());
        float separatorLength = measureText(p, CANDIDATE_SEPARATOR, 0, CANDIDATE_SEPARATOR.length());
        boolean isFirstWordOfLine = (mLineLength == 0);

        int maxWidth = 0;
        int lineLength = 0;
        lineLength += newLineLength;
        maxWidth += width - separatorLength;
        
        mLineLength += newLineLength;
        mLineLength += separatorLength;
        mLineWordCount++;

        if (mLineWordCount == 0) {
            mLineLength = lineLength;
            mLineLength += separatorLength;
        }
        
        if (!isFirstWordOfLine && (width < mLineLength) && mLineWordCount != 0) {
            tmp.append("\n");
            mLineLength = lineLength;
            mLineLength += separatorLength;
            mLineWordCount = 0;
        }
        return adjustDisplaySize(word, tmp, lineLength, maxWidth, maxLine);
    }

    
    /**
     * Adjust the width of specified string
     *
     * @param word              A candidate word
     * @param tmp               A work area
     * @param newLineLength     The line length to show
     * @param maxwidth          The maximum number of width that can be displayed in the candidate window
     * @param maxLine           The maximum number of line in the candidate window
     * @return              The string to show
     */
    private StringBuffer adjustDisplaySize(WnnWord word, StringBuffer tmp, int newLineLength, int maxWidth, int maxLine) {
        StringBuffer string = new StringBuffer(tmp);
        if (newLineLength > maxWidth) {
            TextPaint p = mViewBodyText.getPaint();
            float separatorLength = measureText(p, CANDIDATE_SEPARATOR, 0, CANDIDATE_SEPARATOR.length());
            int length = word.candidate.length();
            int size = 0;
            int count = 0;
            int line = 0;
            float LineLength = 0;
            for (int i = 0 ; i < length;i++) {
                string.append(word.candidate.charAt(i));
                LineLength = measureText(p, string, count, count + 1);
                size += LineLength;
                if (size > maxWidth) {
                    line++;
                    string.delete(string.length() - 1, string.length());
                    if (mDisplayEndOffset == 0 && line == maxLine && mWordCount == 0) {
                        mDisplayEndOffset = count;
                    }
                    string.append("\n");
                    string.append(word.candidate.charAt(i));
                    size = 0;
                    count++;
                    LineLength = measureText(p, string, count, count + 1);
                    size += LineLength;
                }
                count++;
            }

            mLineWordCount = 0;
            mLineLength = newLineLength;
            mLineLength += separatorLength;
        } else {
            string.append(word.candidate);
        }
        return string;
    }
        
        
    /** @see CandidatesViewManager#clearCandidates */
    public void clearCandidates() {
        mViewBodyText.setText("");
    
        mCandidates.delete(0, mCandidates.length());
        mWordCount = 0;
        mWordCountInNormalView = -1;
        mStartPositionArray.clear();
        mEndPositionArray.clear();
        mPositionToWordIndexArray.clear();
        mWnnWordArray.clear();

        mLineLength = 0;
        mLineWordCount = -1;

        if (mAutoHideMode) {
            setViewLayout(CandidatesViewManager.VIEW_TYPE_CLOSE);
        }

        if (mCandidateDeleteState) {
            mViewBodyScroll.removeAllViews();
            mViewBodyScroll.addView(mViewBodyText);
        }
        mCandidateDeleteState = false;

        mHasCreatedCandidateList = false;

        if (mAutoHideMode && mViewBody.isShown()) {
            mWnn.setCandidatesViewShown(false);
        }
        if (!mAutoHideMode) {
            mCanReadMore = false;
            setReadMore();
        }
    }

    /** @see CandidatesViewManager#setPreferences */
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
    
    /**
     * @see OnTouchListener#onTouch
     */
    public boolean onTouch(View v, MotionEvent event) {
        if (mMotionEvent != null) {
            return true;
        }

        mMotionEvent = event;
        boolean ret = mWnn.onEvent(new OpenWnnEvent(OpenWnnEvent.CANDIDATE_VIEW_TOUCH));
        mMotionEvent = null;
        return ret;
    }

    /**
     * Process CANDIDATE_VIEW_TOUCH event.
     * 
     * @return      {@code true} if event is processed; {@code false} if otherwise
     */
    public boolean onTouchSync() {
        if (!mHasCreatedCandidateList) {
            return false;
        }

        mViewBodyText.setCursorVisible(false);
        return mGestureDetector.onTouchEvent(mMotionEvent);
    }

    /**
     * Select a candidate.
     * <br>
     * This method notices the selected word to {@link OpenWnn}.
     *
     * @param word  The selected word
     */
    private void selectCandidate(WnnWord word) {
        setViewLayout(CandidatesViewManager.VIEW_TYPE_NORMAL);
        if (mVibrator != null) {
            try { mVibrator.vibrate(30); } catch (Exception ex) { }
        }
        if (mSound != null) {
            try { mSound.seekTo(0); mSound.start(); } catch (Exception ex) { }
        }
        mWnn.onEvent(new OpenWnnEvent(OpenWnnEvent.SELECT_CANDIDATE, word));
    }

    /**
     * Convert a coordinate into the offset of character
     *
     * @param x     The horizontal position
     * @param y     The vertical position
     * @return  The offset of character
     */
    public int getOffset(int x,int y){
        Layout layout = mViewBodyText.getLayout();
        int line = layout.getLineForVertical(y);
        
        if( y >= layout.getLineTop(line+1) ){
            return layout.getText().length();
        }

        int offset = layout.getOffsetForHorizontal(line,x);
        offset -= TOUCH_ADJUSTED_VALUE;
        if (offset < 0) {
            offset = 0;
        }
        return offset;
    }

    /** from GestureDetector.OnGestureListener class */
    public boolean onDown(MotionEvent arg0) {
        if (!mCandidateDeleteState) {
            int position = getOffset((int)arg0.getX(),(int)arg0.getY());
            int wordIndex = mPositionToWordIndexArray.get(position);
            if (wordIndex != -1) {
                int startPosition = mStartPositionArray.get(wordIndex);
                int endPosition = 0;
                if (mDisplayEndOffset > 0 && getViewType() == CandidatesViewManager.VIEW_TYPE_NORMAL) {
                    endPosition = mDisplayEndOffset + CANDIDATE_SEPARATOR.length();
                } else {
                    endPosition = mEndPositionArray.get(wordIndex);
                }
                mViewBodyText.setSelection(startPosition, endPosition);
                mViewBodyText.setCursorVisible(true);
                mViewBodyText.invalidate();
                mHasStartedSelect = true;
            }
        }
        return true;
    }

    /** from GestureDetector.OnGestureListener class */
    public boolean onFling(MotionEvent arg0, MotionEvent arg1, float arg2, float arg3) {

        if (mCandidateDeleteState) {
            return false;
        }

        boolean consumed = false;
        if (arg1.getY() < arg0.getY()) {
            if (mViewType == CandidatesViewManager.VIEW_TYPE_NORMAL) {
                if (mVibrator != null) {
                    try { mVibrator.vibrate(30); } catch (Exception ex) { }
                }
                mIsFullView = true;
                mWnn.onEvent(new OpenWnnEvent(OpenWnnEvent.LIST_CANDIDATES_FULL));
                consumed = true;
            }
        } else {
            if (mViewBodyScroll.getScrollY() == 0) {
                if (mVibrator != null) {
                    try { mVibrator.vibrate(30); } catch (Exception ex) { }
                }
                mIsFullView = false;
                mWnn.onEvent(new OpenWnnEvent(OpenWnnEvent.LIST_CANDIDATES_NORMAL));
                consumed = true;
            }
        }

        return consumed;
    }

    /** from GestureDetector.OnGestureListener class */
    public void onLongPress(MotionEvent arg0) {
        if (!mHasStartedSelect) {
            return;
        }

        mWord = null;
        int position = getOffset((int)arg0.getX(),(int)arg0.getY());
        if (position < mPositionToWordIndexArray.size()) {
            int wordIndex = mPositionToWordIndexArray.get(position);
            if (wordIndex != -1) {
                mCandidateDeleteState = true;
                mViewBodyScroll.removeAllViews();
                mViewBody.updateViewLayout(mViewBodyScroll,
                                           new FrameLayout.LayoutParams(LayoutParams.FILL_PARENT,
                                                                         LayoutParams.WRAP_CONTENT));
                setReadMore();
                mWord = mWnnWordArray.get(wordIndex);
                LinearLayout mLinerLayout;
                mLinerLayout = new  LinearLayout(mViewBodyScroll.getContext());
                mLinerLayout.setOrientation(LinearLayout.VERTICAL);
                Resources r = mViewBodyScroll.getContext().getResources();
                int color = r.getColor(R.color.candidate_background);
                mLinerLayout.setBackgroundColor(color);
                TextView text = new TextView(mViewBodyScroll.getContext());
                text.setText(mWord.candidate);
                text.setTextColor(mWnn.getResources().getColor(R.color.candidate_text));
                text.setTextSize(mWnn.getResources().getDimension(R.dimen.candidate_delete_word_size));
                text.setGravity(Gravity.CENTER);
                mLinerLayout.addView(text);
                
                LinearLayout linearLayout = new LinearLayout(mViewBodyScroll.getContext());
                linearLayout.setOrientation(LinearLayout.HORIZONTAL);
                
                linearLayout.addView(createSelectButton());

                linearLayout.addView(createCancelButton());
                linearLayout.setGravity(Gravity.CENTER);
                mLinerLayout.addView(linearLayout);
                mViewBodyScroll.addView(mLinerLayout);
            }
        }
    }

    /** from GestureDetector.OnGestureListener class */
    public boolean onScroll(MotionEvent arg0, MotionEvent arg1, float arg2,
            float arg3) {
        return false;
    }

    /** from GestureDetector.OnGestureListener class */
    public void onShowPress(MotionEvent arg0) {
    }

    /** from GestureDetector.OnGestureListener class */
    public boolean onSingleTapUp(MotionEvent arg0) {
        if (!mHasStartedSelect) {
            return true;
        }

        WnnWord word = null;

        if (!mCandidateDeleteState) {
            int position = getOffset((int)arg0.getX(),(int)arg0.getY());
            if (position < mPositionToWordIndexArray.size()) {
                int wordIndex = mPositionToWordIndexArray.get(position);
                if (wordIndex != -1) {
                    word = mWnnWordArray.get(wordIndex);
                }
            
                if (word != null) {
                    selectCandidate(word);
                    return true;
                } else {
                    return false;
                }
            }
        }
        return false;
    }
    
    /**
     * Create the select button.
     *
     * @return Button The button object
     */
    private Button createSelectButton(){
        final Button selectB;
        selectB= new Button(mViewBodyScroll.getContext()) {
            public boolean onTouchEvent(MotionEvent me) {
                boolean ret = super.onTouchEvent(me);
                Drawable d = getBackground();
                switch (me.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    d.setState(View.PRESSED_ENABLED_SELECTED_WINDOW_FOCUSED_STATE_SET);
                    break;
                case MotionEvent.ACTION_UP:
                default:
                    d.clearColorFilter();
                    break;
                }
                return ret;
            }
        };
        selectB.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                selectCandidate(mWord);
            }
        });
        selectB.setText(mSelectBottonText);
        return selectB;
    }

    /**
     * Create the cancel button
     *
     * @return Button       the button object
     */
    private Button createCancelButton(){
        final Button cancelB;
        cancelB= new Button(mViewBodyScroll.getContext()) {
            public boolean onTouchEvent(MotionEvent me) {
                boolean ret = super.onTouchEvent(me);
                Drawable d = getBackground();
                switch (me.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    d.setState(View.PRESSED_ENABLED_SELECTED_WINDOW_FOCUSED_STATE_SET);
                    break;
                case MotionEvent.ACTION_UP:
                default:
                    d.clearColorFilter();
                    break;
                }
                return ret;
            }
        };
        cancelB.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                setViewLayout(CandidatesViewManager.VIEW_TYPE_NORMAL);
                mViewBodyScroll.removeAllViews();
                mCandidateDeleteState = false;
                mWnn.onEvent(new OpenWnnEvent(OpenWnnEvent.UPDATE_CANDIDATE));
                mViewBodyScroll.addView(mViewBodyText);
            }
        });
        cancelB.setText(mCancelBottonText);
        return cancelB;
    }


    /**
     * Set the show state of hardware keyboard
     * 
     * @param hidden    {@code true} if the hardware keyboard is not shown
     */
    public void setHardKeyboardHidden(boolean hidden) {
        mHardKeyboardHidden = hidden;
    }

    /**
     * Retrieve the width of string to draw
     * (Emoji is supported by this method)
     * 
     * @param paint         The information to draw
     * @param text          The string
     * @param start         The start position (specified by the number of character)
     * @param end           The end position (specified by the number of character)
     * @return          The width of string to draw
     */ 
    public int measureText(TextPaint paint, CharSequence text, int start, int end) {
        return (int)Styled.measureText(paint, new TextPaint(), text, start, end, null);
    }
}

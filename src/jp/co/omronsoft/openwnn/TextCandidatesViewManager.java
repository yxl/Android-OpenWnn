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
import android.content.res.Configuration;
import android.content.res.Resources;
import android.media.MediaPlayer;
import android.os.Vibrator;
import android.text.TextUtils;
import android.text.TextPaint;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ImageSpan;
import android.text.style.DynamicDrawableSpan;
import android.util.Log;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.AbsoluteLayout;
import android.widget.ImageView;
import android.graphics.drawable.Drawable;

/**
 * The default candidates view manager class using {@link EditText}.
 *
 * @author Copyright (C) 2009 OMRON SOFTWARE CO., LTD.  All Rights Reserved.
 */
public class TextCandidatesViewManager implements CandidatesViewManager, GestureDetector.OnGestureListener {
    /** Height of a line */
    public static final int LINE_HEIGHT = 34;
    /** Number of lines to display (Portrait) */
    public static final int LINE_NUM_PORTRAIT       = 2;
    /** Number of lines to display (Landscape) */
    public static final int LINE_NUM_LANDSCAPE      = 1;

    /** Maximum lines */
    private static final int DISPLAY_LINE_MAX_COUNT = 1000;
    /** Width of the view */
    private static final int CANDIDATE_MINIMUM_WIDTH = 48;
    /** Height of the view */
    private static final int CANDIDATE_MINIMUM_HEIGHT = 35;
    /** Align the candidate left if the width of the string exceeds this threshold */
    private static final int CANDIDATE_LEFT_ALIGN_THRESHOLD = 120;
    /** Maximum number of displaying candidates par one line (full view mode) */
    private static final int FULL_VIEW_DIV = 4;

    /** Body view of the candidates list */
    private ViewGroup  mViewBody;
    /** Scroller of {@code mViewBodyText} */
    private ScrollView mViewBodyScroll;
    /** Base of {@code mViewCandidateList1st}, {@code mViewCandidateList2nd} */
    private ViewGroup mViewCandidateBase;
    /** Button displayed bottom of the view when there are more candidates. */
    private ImageView mReadMoreButton;
    /** The view of the scaling up candidate */
    private View mViewScaleUp;
    /** Layout for the candidates list on normal view */
    private LinearLayout mViewCandidateList1st;
    /** Layout for the candidates list on full view */
    private AbsoluteLayout mViewCandidateList2nd;
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
    /** Minimum width of a candidate (density support) */
    private int mCandidateMinimumWidth;
    /** Maximum width of a candidate (density support) */
    private int mCandidateMinimumHeight;

    /** Whether hide the view if there is no candidates */
    private boolean mAutoHideMode;
    /** The converter to be get candidates from and notice the selected candidate to. */
    private WnnEngine mConverter;
    /** Limitation of displaying candidates */
    private int mDisplayLimit;

    /** Vibrator for touch vibration */
    private Vibrator mVibrator = null;
    /** MediaPlayer for click sound */
    private MediaPlayer mSound = null;

    /** Number of candidates displaying */
    private int mWordCount;
    /** List of candidates */
    private ArrayList<WnnWord> mWnnWordArray;

    /** Gesture detector */
    private GestureDetector mGestureDetector;
    /** The word pressed */
    private WnnWord mWord;
    /** Character width of the candidate area */
    private int mLineLength = 0;
    /** Number of lines displayed */
    private int mLineCount = 1;

    /** {@code true} if the candidate delete state is selected */
    private boolean mIsScaleUp = false;

    /** {@code true} if the full screen mode is selected */
    private boolean mIsFullView = false;

    /** The event object for "touch" */
    private MotionEvent mMotionEvent = null;

    /** The offset when the candidates is flowed out the candidate window */
    private int mDisplayEndOffset = 0;
    /** {@code true} if there are more candidates to display. */
    private boolean mCanReadMore = false;
    /** Width of {@code mReadMoreButton} */
    private int mReadMoreButtonWidth = 0;
    /** Color of the candidates */
    private int mTextColor = 0;
    /** Template object for each candidate and normal/full view change button */
    private TextView mViewCandidateTemplate;
    /** Number of candidates in full view */
    private int mFullViewWordCount;
    /** Number of candidates in the current line (in full view) */
    private int mFullViewOccupyCount;
    /** View of the previous candidate (in full view) */
    private TextView mFullViewPrevView;
    /** Id of the top line view (in full view) */
    private int mFullViewPrevLineTopId;
    /** Layout of the previous candidate (in full view) */
    private ViewGroup.LayoutParams mFullViewPrevParams;
    /** Whether all candidates is displayed */
    private boolean mCreateCandidateDone;
    /** Number of lines in normal view */
    private int mNormalViewWordCountOfLine;
    /** general infomation about a display */
    private final DisplayMetrics mMetrics = new DisplayMetrics();

    /** Event listener for touching a candidate */
    private OnTouchListener mCandidateOnTouch = new OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                if (mMotionEvent != null) {
                    return true;
                }

                if ((event.getAction() == MotionEvent.ACTION_UP)
                    && (v instanceof TextView)) {
                    Drawable d = v.getBackground();
                    if (d != null) {
                        d.setState(new int[] {});
                    }
                }

                mMotionEvent = event;
                boolean ret = mWnn.onEvent(new OpenWnnEvent(OpenWnnEvent.CANDIDATE_VIEW_TOUCH));
                mMotionEvent = null;
                return ret;
            }
        };
    
    
    /** Event listener for clicking a candidate */
    private OnClickListener mCandidateOnClick = new OnClickListener() {
            public void onClick(View v) {
                if (!v.isShown()) {
                    return;
                }
                
                if (v instanceof TextView) {
                    TextView text = (TextView)v;
                    int wordcount = text.getId();
                    WnnWord word = null;
                    word = mWnnWordArray.get(wordcount);
                    selectCandidate(word);
                }
            }
        };

    /** Event listener for long-clicking a candidate */
    private OnLongClickListener mCandidateOnLongClick = new OnLongClickListener() {
            public boolean onLongClick(View v) {
                if (mViewScaleUp == null) {
                    return false;
                }

                if (!v.isShown()) {
                    return true;
                }

                Drawable d = v.getBackground();
                if (d != null) {
                    if(d.getState().length == 0){
                        return true;
                    }
                }
            
                int wordcount = ((TextView)v).getId();
                mWord = mWnnWordArray.get(wordcount);
                setViewScaleUp(true, mWord);
            
                return true;
            }
        };


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
        this.mWnnWordArray = new ArrayList<WnnWord>();
        this.mAutoHideMode = true;
        mMetrics.setToDefaults();
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
        mViewHeight = height;
        mCandidateMinimumWidth = (int)(CANDIDATE_MINIMUM_WIDTH * mMetrics.density);
        mCandidateMinimumHeight = (int)(CANDIDATE_MINIMUM_HEIGHT * mMetrics.density);
        mPortrait = 
            (parent.getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE);

        Resources r = mWnn.getResources();

        LayoutInflater inflater = parent.getLayoutInflater();
        mViewBody = (ViewGroup)inflater.inflate(R.layout.candidates, null);

        mViewBodyScroll = (ScrollView)mViewBody.findViewById(R.id.candview_scroll);
        mViewBodyScroll.setOnTouchListener(mCandidateOnTouch);

        mViewCandidateBase = (ViewGroup)mViewBody.findViewById(R.id.candview_base);

        createNormalCandidateView();
        mViewCandidateList2nd = (AbsoluteLayout)mViewBody.findViewById(R.id.candidates_2nd_view);

        mReadMoreButtonWidth = r.getDrawable(R.drawable.cand_up).getMinimumWidth();

        mTextColor = r.getColor(R.color.candidate_text);
        
        mReadMoreButton = (ImageView)mViewBody.findViewById(R.id.read_more_text);
        mReadMoreButton.setOnTouchListener(new View.OnTouchListener() {
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        if (mIsFullView) {
                            mReadMoreButton.setImageResource(R.drawable.cand_down_press);
                        } else {
                            mReadMoreButton.setImageResource(R.drawable.cand_up_press);
                        }
                	    break;
                    case MotionEvent.ACTION_UP:
                        if (mIsFullView) {
                            mReadMoreButton.setImageResource(R.drawable.cand_down);
                        } else {
                            mReadMoreButton.setImageResource(R.drawable.cand_up);
                        }
                        break;
                    default:
                        break;
                    }
                    return false;
                }
            });
        mReadMoreButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if (!v.isShown()) {
                        return;
                    }

                    if (mIsFullView) {
                        mIsFullView = false;
                        mWnn.onEvent(new OpenWnnEvent(OpenWnnEvent.LIST_CANDIDATES_NORMAL));
                    } else {
                        mIsFullView = true;
                        mWnn.onEvent(new OpenWnnEvent(OpenWnnEvent.LIST_CANDIDATES_FULL));
                    }
                }
            });

        setViewType(CandidatesViewManager.VIEW_TYPE_CLOSE);

        mGestureDetector = new GestureDetector(this);

        View scaleUp = (View)inflater.inflate(R.layout.candidate_scale_up, null);
        mViewScaleUp = scaleUp;

        /* select button */
        Button b = (Button)scaleUp.findViewById(R.id.candidate_select);
        b.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    selectCandidate(mWord);
                }
            });

        /* cancel button */
        b = (Button)scaleUp.findViewById(R.id.candidate_cancel);
        b.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    setViewLayout(CandidatesViewManager.VIEW_TYPE_NORMAL);
                    mWnn.onEvent(new OpenWnnEvent(OpenWnnEvent.UPDATE_CANDIDATE));
                }
            });

        return mViewBody;
    }

    /**
     * Create the normal candidate view
     */
    private void createNormalCandidateView() {
        mViewCandidateList1st = (LinearLayout)mViewBody.findViewById(R.id.candidates_1st_view);
        mViewCandidateList1st.setOnTouchListener(mCandidateOnTouch);
        mViewCandidateList1st.setOnClickListener(mCandidateOnClick);

        int line = getMaxLine();
        int width = mViewWidth;
        for (int i = 0; i < line; i++) {
            LinearLayout lineView = new LinearLayout(mViewBodyScroll.getContext());
            lineView.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams layoutParams = 
                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                              ViewGroup.LayoutParams.WRAP_CONTENT);
            lineView.setLayoutParams(layoutParams);
            for (int j = 0; j < (width / getCandidateMinimumWidth()); j++) {
                TextView tv = createCandidateView();
                lineView.addView(tv);
            }

            if (i == 0) {
                TextView tv = createCandidateView();
                layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                                                             ViewGroup.LayoutParams.WRAP_CONTENT);
                layoutParams.weight = 0;
                layoutParams.gravity = Gravity.RIGHT;
                tv.setLayoutParams(layoutParams);

                lineView.addView(tv);
                mViewCandidateTemplate = tv;
            }
            mViewCandidateList1st.addView(lineView);
        }
    }

    /** @see CandidatesViewManager#getCurrentView */
    public View getCurrentView() {
        return mViewBody;
    }

    /** @see CandidatesViewManager#setViewType */
    public void setViewType(int type) {
        boolean readMore = setViewLayout(type);

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
        setViewScaleUp(false, null);

        switch (type) {
        case CandidatesViewManager.VIEW_TYPE_CLOSE:
            mViewCandidateBase.setMinimumHeight(-1);
            return false;

        case CandidatesViewManager.VIEW_TYPE_NORMAL:
            mViewBodyScroll.scrollTo(0, 0);
            mViewCandidateList1st.setVisibility(View.VISIBLE);
            mViewCandidateList2nd.setVisibility(View.GONE);
            mViewCandidateBase.setMinimumHeight(-1);
            int line = (mPortrait) ? LINE_NUM_PORTRAIT : LINE_NUM_LANDSCAPE;
            mViewCandidateList1st.setMinimumHeight(getCandidateMinimumHeight() * line);
            return false;

        case CandidatesViewManager.VIEW_TYPE_FULL:
        default:
            mViewCandidateList2nd.setVisibility(View.VISIBLE);
            mViewCandidateBase.setMinimumHeight(mViewHeight);
            return true;
        }
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
        mFullViewWordCount = 0;
        mFullViewOccupyCount = 0;
        mFullViewPrevLineTopId = 0;
        mCreateCandidateDone = false;
        mNormalViewWordCountOfLine = 0;

        clearCandidates();
        mConverter = converter;
        setViewLayout(CandidatesViewManager.VIEW_TYPE_NORMAL);
        
        mViewCandidateTemplate.setVisibility(View.VISIBLE);
        mViewCandidateTemplate.setBackgroundResource(R.drawable.cand_back);

        displayCandidates(converter, true, getMaxLine());
    }

    /** @see CandidatesViewManager#getMaxLine */
    private int getMaxLine() {
        int maxLine = (mPortrait) ? LINE_NUM_PORTRAIT : LINE_NUM_LANDSCAPE;
        return maxLine;
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

        /* Concatenate the candidates already got and the last one in dispFirst mode */
        int displayLimit = mDisplayLimit;

        boolean isHistorySequence = false;
        boolean isBreak = false;

        /* Get candidates */
        WnnWord result = null;
        while ((displayLimit == -1 || mWordCount < displayLimit)) {
            result = converter.getNextCandidate();

            if (result == null) {
                break;
            }

            setCandidate(false, result);

            if (dispFirst && (maxLine < mLineCount)) {
                mCanReadMore = true;
                isBreak = true;
                break;
            }
        }

        if (!isBreak && !mCreateCandidateDone) {
            /* align left if necessary */
            createNextLine();
            mCreateCandidateDone = true;
        }
        
        if (mWordCount < 1) { /* no candidates */
            if (mAutoHideMode) {
                mWnn.setCandidatesViewShown(false);
                return;
            } else {
                mCanReadMore = false;
                mIsFullView = false;
                setViewLayout(CandidatesViewManager.VIEW_TYPE_NORMAL);
            }
        }

        setReadMore();

        if (!(mViewBody.isShown())) {
            mWnn.setCandidatesViewShown(true);
        }
        return;
    }

    /**
     * Add a candidate into the list.
     * @param isCategory  {@code true}:caption of category, {@code false}:normal word
     * @param word        A candidate word
     */
    private void setCandidate(boolean isCategory, WnnWord word) {
        int textLength = measureText(word.candidate, 0, word.candidate.length());
        TextView template = mViewCandidateTemplate;
        textLength += template.getPaddingLeft() + template.getPaddingRight();
        int maxWidth = mViewWidth;

        TextView textView;
        if (mIsFullView || getMaxLine() < mLineCount) {
            /* Full view */
            int indentWidth = mViewWidth / FULL_VIEW_DIV;
            int occupyCount = Math.min((textLength + indentWidth) / indentWidth, FULL_VIEW_DIV);
            if (isCategory) {
                occupyCount = FULL_VIEW_DIV;
            }

            if (FULL_VIEW_DIV < (mFullViewOccupyCount + occupyCount)) {
                if (FULL_VIEW_DIV != mFullViewOccupyCount) {
                    mFullViewPrevParams.width += (FULL_VIEW_DIV - mFullViewOccupyCount) * indentWidth;
                    mViewCandidateList2nd.updateViewLayout(mFullViewPrevView, mFullViewPrevParams);
                }
                mFullViewOccupyCount = 0;
                mFullViewPrevLineTopId = mFullViewPrevView.getId();
                mLineCount++;
            }

            ViewGroup layout = mViewCandidateList2nd;

            int width = indentWidth * occupyCount;
            int height = getCandidateMinimumHeight();


            ViewGroup.LayoutParams params = buildLayoutParams(mViewCandidateList2nd, width, height);

            textView = (TextView) layout.getChildAt(mFullViewWordCount);
            if (textView == null) {
                textView = createCandidateView();
                textView.setLayoutParams(params);

                mViewCandidateList2nd.addView(textView);
            } else {
                mViewCandidateList2nd.updateViewLayout(textView, params);
            }

            mFullViewOccupyCount += occupyCount;
            mFullViewWordCount++;
            mFullViewPrevView = textView;
            mFullViewPrevParams = params;

        } else {
            textLength = Math.max(textLength, getCandidateMinimumWidth());

            /* Normal view */
            int nextEnd = mLineLength + textLength;
            if (mLineCount == 1) {
                maxWidth -= getCandidateMinimumWidth();
            }

            if ((maxWidth < nextEnd) && (mWordCount != 0)) {
                createNextLine();
                if (getMaxLine() < mLineCount) {
                    mLineLength = 0;
                    /* Call this method again to add the candidate in the full view */
                    setCandidate(isCategory, word);
                    return;
                }
                
                mLineLength = textLength;
            } else {
                mLineLength = nextEnd;
            }

            LinearLayout lineView = (LinearLayout) mViewCandidateList1st.getChildAt(mLineCount - 1);
            textView = (TextView) lineView.getChildAt(mNormalViewWordCountOfLine);

            if (isCategory) {
                if (mLineCount == 1) {
                    mViewCandidateTemplate.setBackgroundDrawable(null);
                }
                mLineLength += CANDIDATE_LEFT_ALIGN_THRESHOLD;
            }

            mNormalViewWordCountOfLine++;
        }

        textView.setText(word.candidate);
        textView.setTextColor(mTextColor);
        textView.setId(mWordCount);
        textView.setVisibility(View.VISIBLE);
        textView.setPressed(false);

        if (isCategory) {
            textView.setOnClickListener(null);
            textView.setOnLongClickListener(null);
            textView.setBackgroundDrawable(null);
        } else {
            textView.setOnClickListener(mCandidateOnClick);
            textView.setOnLongClickListener(mCandidateOnLongClick);
            textView.setBackgroundResource(R.drawable.cand_back);
        }
        textView.setOnTouchListener(mCandidateOnTouch);

        if (maxWidth < textLength) {
            textView.setEllipsize(TextUtils.TruncateAt.END);
        } else {
            textView.setEllipsize(null);
        }

        ImageSpan span = null;
        if (word.candidate.equals(" ")) {
            span = new ImageSpan(mWnn, R.drawable.word_half_space,
                                 DynamicDrawableSpan.ALIGN_BASELINE);
        } else if (word.candidate.equals("\u3000" /* full-width space */)) {
            span = new ImageSpan(mWnn, R.drawable.word_full_space,
                                 DynamicDrawableSpan.ALIGN_BASELINE);
        }

        if (span != null) {
            SpannableString spannable = new SpannableString("   ");
            spannable.setSpan(span, 1, 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE); 
            textView.setText(spannable);
        }

        mWnnWordArray.add(mWordCount, word);
        mWordCount++;
    }

    /**
     * Create AbsoluteLayout.LayoutParams
     * @param layout AbsoluteLayout
     * @param width
     * @param height
     * @return ViewGroup.LayoutParams
     */
    private ViewGroup.LayoutParams buildLayoutParams(AbsoluteLayout layout, int width, int height) {

        int indentWidth = mViewWidth / FULL_VIEW_DIV;
        int x         = indentWidth * mFullViewOccupyCount;
        int nomalLine = (mPortrait) ? LINE_NUM_PORTRAIT : LINE_NUM_LANDSCAPE;
        int y         = getCandidateMinimumHeight() * (mLineCount - nomalLine - 1);
        ViewGroup.LayoutParams params
              = new AbsoluteLayout.LayoutParams(width, height, x, y);

        return params;
    }



            


    /**
     * Create a view for a candidate.
     * @return the view
     */
    private TextView createCandidateView() {
        TextView text = new TextView(mViewBodyScroll.getContext());
        text.setTextSize(20);
        text.setBackgroundResource(R.drawable.cand_back);
        text.setGravity(Gravity.CENTER);
        text.setSingleLine();
        text.setPadding(4, 4, 4, 4);
        text.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                                                           ViewGroup.LayoutParams.WRAP_CONTENT,
                                                           1.0f));
        text.setMinHeight(getCandidateMinimumHeight());
        text.setMinimumWidth(getCandidateMinimumWidth());
        return text;
    }

    /**
     * Display {@code mReadMoreText} if there are more candidates.
     */
    private void setReadMore() {
        if (mIsScaleUp) {
            mReadMoreButton.setVisibility(View.GONE);
            mViewCandidateTemplate.setVisibility(View.GONE);
            return;
        }

        if (mIsFullView) {
            mReadMoreButton.setVisibility(View.VISIBLE);
            mReadMoreButton.setImageResource(R.drawable.cand_down);
        } else {
            if (mCanReadMore) {
                mReadMoreButton.setVisibility(View.VISIBLE);
                mReadMoreButton.setImageResource(R.drawable.cand_up);
            } else {
                mReadMoreButton.setVisibility(View.GONE);
                mViewCandidateTemplate.setVisibility(View.GONE);
            }
        }
    }

    /**
     * Clear the list of the normal candidate view.
     */
    private void clearNormalViewCandidate() {
        LinearLayout candidateList = mViewCandidateList1st;
        int lineNum = candidateList.getChildCount();
        for (int i = 0; i < lineNum; i++) {

            LinearLayout lineView = (LinearLayout)candidateList.getChildAt(i);
            int size = lineView.getChildCount();
            for (int j = 0; j < size; j++) {
                View v = lineView.getChildAt(j);
                v.setVisibility(View.GONE);
            }
        }
    }
        
    /** @see CandidatesViewManager#clearCandidates */
    public void clearCandidates() {
        clearNormalViewCandidate();

        ViewGroup layout = mViewCandidateList2nd;
        int size = layout.getChildCount();
        for (int i = 0; i < size; i++) {
            View v = layout.getChildAt(i);
            v.setVisibility(View.GONE);
        }
    
        mLineCount = 1;
        mWordCount = 0;
        mWnnWordArray.clear();

        mLineLength = 0;

        mIsFullView = false;
        setViewLayout(CandidatesViewManager.VIEW_TYPE_NORMAL);
        if (mAutoHideMode) {
            setViewLayout(CandidatesViewManager.VIEW_TYPE_CLOSE);
        }

        if (mAutoHideMode && mViewBody.isShown()) {
            mWnn.setCandidatesViewShown(false);
        }
        mCanReadMore = false;
        setReadMore();
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
     * Process {@code OpenWnnEvent.CANDIDATE_VIEW_TOUCH} event.
     * 
     * @return      {@code true} if event is processed; {@code false} if otherwise
     */
    public boolean onTouchSync() {
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

    /** @see android.view.GestureDetector.OnGestureListener#onDown */
    public boolean onDown(MotionEvent arg0) {
        return false;
    }

    /** @see android.view.GestureDetector.OnGestureListener#onFling */
    public boolean onFling(MotionEvent arg0, MotionEvent arg1, float arg2, float arg3) {
        if (mIsScaleUp) {
            return false;
        }

        boolean consumed = false;
        if (arg1 != null && arg0 != null && arg1.getY() < arg0.getY()) {
            if ((mViewType == CandidatesViewManager.VIEW_TYPE_NORMAL) && mCanReadMore) {
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

    /** @see android.view.GestureDetector.OnGestureListener#onLongPress */
    public void onLongPress(MotionEvent arg0) {
        return;
    }

    /** @see android.view.GestureDetector.OnGestureListener#onScroll */
    public boolean onScroll(MotionEvent arg0, MotionEvent arg1, float arg2, float arg3) {
        return false;
    }

    /** @see android.view.GestureDetector.OnGestureListener#onShowPress */
    public void onShowPress(MotionEvent arg0) {
    }

    /** @see android.view.GestureDetector.OnGestureListener#onSingleTapUp */
    public boolean onSingleTapUp(MotionEvent arg0) {
        return false;
    }
    
    /**
     * Retrieve the width of string to draw.
     * 
     * @param text          The string
     * @param start         The start position (specified by the number of character)
     * @param end           The end position (specified by the number of character)
     * @return          The width of string to draw
     */ 
    public int measureText(CharSequence text, int start, int end) {
        if (end - start < 3) {
            return getCandidateMinimumWidth();
        }

        TextPaint paint = mViewCandidateTemplate.getPaint();
        return (int)paint.measureText(text, start, end);
    }

    /**
     * Switch list/enlarge view mode.
     * @param up  {@code true}:enlarge, {@code false}:list
     * @param word  The candidate word to be enlarged.
     */
    private void setViewScaleUp(boolean up, WnnWord word) {
        if (up == mIsScaleUp || (mViewScaleUp == null)) {
            return;
        }

        if (up) {
            setViewLayout(CandidatesViewManager.VIEW_TYPE_NORMAL);
            mViewCandidateList1st.setVisibility(View.GONE);
            mViewCandidateBase.setMinimumHeight(-1);
            mViewCandidateBase.addView(mViewScaleUp);
            TextView text = (TextView)mViewScaleUp.findViewById(R.id.candidate_scale_up_text);
            text.setText(word.candidate);
            if (!mPortrait) {
                Resources r = mViewBodyScroll.getContext().getResources();
                text.setTextSize(r.getDimensionPixelSize(R.dimen.candidate_delete_word_size_landscape));
            }

            mIsScaleUp = true;
            setReadMore();
        } else {
            mIsScaleUp = false;
            mViewCandidateBase.removeView(mViewScaleUp);
        }
    }

    /**
     * Create a layout for the next line.
     */
    private void createNextLine() {
        int lineCount = mLineCount;
        if (mIsFullView || getMaxLine() < lineCount) {
            /* Full view */
            mFullViewOccupyCount = 0;
            mFullViewPrevLineTopId = mFullViewPrevView.getId();
        } else {
            /* Normal view */
            LinearLayout lineView = (LinearLayout) mViewCandidateList1st.getChildAt(lineCount - 1);
            float weight = 0;
            if (mLineLength < CANDIDATE_LEFT_ALIGN_THRESHOLD) {
                if (lineCount == 1) {
                    mViewCandidateTemplate.setVisibility(View.GONE);
                }
            } else {
                weight = 1.0f;
            }

            LinearLayout.LayoutParams params
                = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                                                ViewGroup.LayoutParams.WRAP_CONTENT,
                                                weight);
            
            int child = lineView.getChildCount();
            for (int i = 0; i < child; i++) {
                View view = lineView.getChildAt(i);

                if (view != mViewCandidateTemplate) {
                    view.setLayoutParams(params);
                }
            }

            mLineLength = 0;
            mNormalViewWordCountOfLine = 0;
        }
        mLineCount++;
    }

    /**
     * @return the minimum width of a candidate view.
     */
    private int getCandidateMinimumWidth() {
        return mCandidateMinimumWidth;
    }

    /**
     * @return the minimum height of a candidate view.
     */
    private int getCandidateMinimumHeight() {
        return mCandidateMinimumHeight;
    }
}

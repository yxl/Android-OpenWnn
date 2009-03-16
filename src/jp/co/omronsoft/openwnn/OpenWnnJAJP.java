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


import jp.co.omronsoft.openwnn.EN.OpenWnnEngineEN;
import jp.co.omronsoft.openwnn.JAJP.DefaultSoftKeyboardJAJP;
import jp.co.omronsoft.openwnn.JAJP.OpenWnnEngineJAJP;
import jp.co.omronsoft.openwnn.JAJP.Romkan;
import jp.co.omronsoft.openwnn.JAJP.RomkanFullKatakana;
import jp.co.omronsoft.openwnn.JAJP.RomkanHalfKatakana;
import jp.co.omronsoft.openwnn.StrSegmentClause;
import android.content.SharedPreferences;
import android.content.Context;
import android.content.res.Configuration;
import android.inputmethodservice.InputMethodService;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.text.style.CharacterStyle;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.View;
import android.view.KeyCharacterMap;
import android.text.method.MetaKeyKeyListener;
import android.provider.Settings;
import android.view.WindowManager;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * OpenWnn Japanese IME
 *
 * @author Copyright (C) 2009, OMRON SOFTWARE CO., LTD.  All Rights Reserved.
 */
public class OpenWnnJAJP extends OpenWnn {
    /**
     * Mode of the convert engine (Full-width KTAKANA).
     * Use with OpenWnn.CHANGE_MODE event.
     */
    public static final int ENGINE_MODE_FULL_KATAKANA = 101;

    /**
     * Mode of the convert engine (Half-width KATAKANA).
     * Use with OpenWnn.CHANGE_MODE event.
     */
    public static final int ENGINE_MODE_HALF_KATAKANA = 102;

    /**
     * Mode of the convert engine (EISU-KANA conversion).
     * Use with OpenWnn.CHANGE_MODE event.
     */
    public static final int ENGINE_MODE_EISU_KANA = 103;

    /**
     * Mode of the convert engine (Symbol list).
     * Use with OpenWnn.CHANGE_MODE event.
     */
    public static final int ENGINE_MODE_SYMBOL = 104;

    /**
     * Mode of the convert engine (Keyboard type is QWERTY).
     * Use with OpenWnn.CHANGE_MODE event to change ambiguous searching pattern.
     */
    public static final int ENGINE_MODE_OPT_TYPE_QWERTY = 105;

    /**
     * Mode of the convert engine (Keyboard type is 12-keys).
     * Use with OpenWnn.CHANGE_MODE event to change ambiguous searching pattern.
     */
    public static final int ENGINE_MODE_OPT_TYPE_12KEY = 106;

    /** never move cursor in to the composing text (adapting to IMF's specification change) */
    private static final boolean WA_CANNOT_SET_CURSOR_IN_TEXT = true;

    /**
     * Highlight color style for the converted clause
     */
    private static final CharacterStyle SPAN_CONVERT_BGCOLOR_HL  = new BackgroundColorSpan(0xFF8888FF);

    /**
     * Highlight color style for the selected string 
     */
    private static final CharacterStyle SPAN_EXACT_BGCOLOR_HL  = new BackgroundColorSpan(0xFF66CDAA);

    /**
     * Underline style for the composing text
     */
    private static final CharacterStyle SPAN_UNDERLINE   = new UnderlineSpan();

    /** 
     * IME's status for <code>mStatus</code>(no input/no candidates).
     */
    private static final int STATUS_INIT = 0x0000;

    /** 
     * IME's status for <code>mStatus</code>(input characters).
     */
    private static final int STATUS_INPUT = 0x0001;

    /** 
     * IME's status for <code>mStatus</code>(input functional keys).
     */
    private static final int STATUS_INPUT_EDIT      = 0x0003;

    /** 
     * IME's status for <code>mStatus</code>(all candidates are displaied)
     */
    private static final int STATUS_CANDIDATE_FULL  = 0x0010;

    /**
     * Alphabet pattern
     */
    private static final Pattern ENGLISH_CHARACTER = Pattern.compile(".*[a-zA-Z]$");

    /**
     * private area character code got by <code>KeyEvent.getUnicodeChar()</code>
     */
    private static final int PRIVATE_AREA_CODE = 61184;

    /** Maximum length of input string */
    private static final int LIMIT_INPUT_NUMBER = 30;

    /** Bit flag for English auto commit mode (ON) */
    private static final int AUTO_COMMIT_ENGLISH_ON = 0x0000;
    /** Bit flag for English auto commit mode (OFF) */
    private static final int AUTO_COMMIT_ENGLISH_OFF = 0x0001;
    /** Bit flag for English auto commit mode (symbol list) */
    private static final int AUTO_COMMIT_ENGLISH_SYMBOL  = 0x0010;


    /** Convert engine's state */
    private class EngineState {
        /** Definition for <code>EngineState.*</code> (invalid) */
        public static final int INVALID = -1;

        /** Definition for <code>EngineState.dictionarySet</code> (Japanese) */
        public static final int DICTIONARYSET_JP = 0;

        /** Definition for <code>EngineState.dictionarySet</code> (English) */
        public static final int DICTIONARYSET_EN = 1;

        /** Definition for <code>EngineState.convertType</code> (prediction/no conversion) */
        public static final int CONVERT_TYPE_NONE = 0;

        /** Definition for <code>EngineState.convertType</code> (consecutive clause conversion) */
        public static final int CONVERT_TYPE_RENBUN = 1;

        /** Definition for <code>EngineState.convertType</code> (EISU-KANA conversion) */
        public static final int CONVERT_TYPE_EISU_KANA = 2;

        /** Definition for <code>EngineState.temporaryMode</code> (change back to the normal dictionary) */
        public static final int TEMPORARY_DICTIONARY_MODE_NONE = 0;

        /** Definition for <code>EngineState.temporaryMode</code> (change to the symbol dicitonary) */
        public static final int TEMPORARY_DICTIONARY_MODE_SYMBOL = 1;

        /** Definition for <code>EngineState.temporaryMode</code> (change to the user dicitonary) */
        public static final int TEMPORARY_DICTIONARY_MODE_USER = 2;

        /** Definition for <code>EngineState.preferenceDictionary</code> (no prefrence dictionary) */
        public static final int PREFERENCE_DICTIONARY_NONE = 0;

        /** Definition for <code>EngineState.preferenceDictionary</code> (person's name) */
        public static final int PREFERENCE_DICTIONARY_PERSON_NAME = 1;

        /** Definition for EngineState.preferenceDictionary (place name) */
        public static final int PREFERENCE_DICTIONARY_POSTAL_ADDRESS = 2;

        /** Definition for EngineState.preferenceDictionary (email/URI) */
        public static final int PREFERENCE_DICTIONARY_EMAIL_ADDRESS_URI = 3;

        /** Definition for <code>EngineState.keyboard</code> (undefined) */
        public static final int KEYBOARD_UNDEF = 0;

        /** Definition for <code>EngineState.keyboard</code> (undefined) */
        public static final int KEYBOARD_QWERTY = 1;

        /** Definition for <code>EngineState.keyboard</code> (undefined) */
        public static final int KEYBOARD_12KEY  = 2;

        /** Set of dictionaries */
        public int dictionarySet = INVALID;

        /** Type of conversion */
        public int convertType = INVALID;

        /** Temporary mode */
        public int temporaryMode = INVALID;

        /** Preference dictionary setting */
        public int preferenceDictionary = INVALID;

        /** keyboard */
        public int keyboard = INVALID;

        /**
         * Returns whether current type of conversion is consecutive clause(RENBUNSETSU) conversion.
         * @return <code>true</code> if current type of conversion is consecutive clause conversion.
         */
        public boolean isRenbun() {
            return convertType == CONVERT_TYPE_RENBUN;
        }

        /**
         * Returns whether current type of conversion is EISU-KANA conversion.
         * @return <code>true</code> if current type of conversion is EISU-KANA conversion.
         */
        public boolean isEisuKana() {
            return convertType == CONVERT_TYPE_EISU_KANA;
        }

        /**
         * Returns whether current type of conversion is no conversion.
         * @return <code>true</code> if no conversion is executed currently.
         */
        public boolean isConvertState() {
            return convertType != CONVERT_TYPE_NONE;
        }

        /**
         * Check whether or not the mode is "symbol list".
         * @return <code>true</code> if the mode is "symbol list".
         */
        public boolean isSymbolList() {
            return temporaryMode == TEMPORARY_DICTIONARY_MODE_SYMBOL;
        }
    }

    /** IME's status */
    protected int mStatus = STATUS_INIT;

    /** Whether exact match searching or not */
    protected boolean mExactMatchMode = false;

    /** Spannnable string builder for displaying the composing text */
    protected SpannableStringBuilder mDisplayText;

    /** Instance of this service */
    private static OpenWnnJAJP mSelf = null;

    /** Handler for drawing the candidates view */
    private Handler mDelayUpdateHandler;

    /** Backup for switching the converter */
    private WnnEngine mConverterBack;

    /** Backup for switching the pre-converter */
    private LetterConverter mPreConverterBack;

    /** OpenWnn conversion engine for Japanese */
    private OpenWnnEngineJAJP mConverterJAJP;

    /** OpenWnn conversion engine for English */
    private OpenWnnEngineEN mConverterEN;

    /** Conversion engine for listing symbols */
    private SymbolList mConverterSymbolEngineBack;

    /** Symbol lists to display when the symbol key is pressed */
    private static final String[] SYMBOL_LISTS = {
        SymbolList.SYMBOL_JAPANESE_FACE, SymbolList.SYMBOL_JAPANESE, SymbolList.SYMBOL_ENGLISH
    };

    /** Current symbol list */
    private int mCurrentSymbol = 0;

    /** Romaji-to-Kana converter (HIRAGANA) */
    private Romkan mPreConverterHiragana;

    /** Romaji-to-Kana converter (full-width KATAKANA) */
    private RomkanFullKatakana mPreConverterFullKatakana;

    /** Romaji-to-Kana converter (half-width KATAKANA) */
    private RomkanHalfKatakana mPreConverterHalfKatakana;

    /** Conversion Engine's state */
    private EngineState mEngineState = new EngineState();

    /** Whether learning function is active of not. */
    private boolean mEnableLearning = true;

    /** Whether prediction is active or not. */
    private boolean mEnablePrediction = true;

    /** Enable mistyping correction or not */
    private boolean mEnableSpellCorrection;

    /** auto commit state (in English mode) */
    private int mDisableAutoCommitEnglishMask = AUTO_COMMIT_ENGLISH_ON;

    /** Whether removing a space before a separator or not. (in English mode) */
    private boolean mEnableAutoDeleteSpace = false;

    /** Whether appending a space to a selected word or not (in English mode) */
    private boolean mEnableAutoInsertSpace = true;

    /** Number of committed clauses on consecutive clause conversion */
    private int mCommitCount = 0;

    /** Target layer of the <code>ComposingText</code> */
    private int mTargetLayer = 0;

    /** Current orientation of the display */
    private int mOrientation = Configuration.ORIENTATION_UNDEFINED;

    /** Current normal dictionary set */
    private int mPrevDictionarySet = OpenWnnEngineJAJP.DIC_LANG_INIT;

    /** Regular expression pattern for English separators */
    private  Pattern mEnglishAutoCommitDelimiter = null;

    /** List of words in the user dictionary */
    private WnnWord[] mUserDictionaryWords = null;

    /** Shift lock status of the Hardware keyboard */
    private int mHardShift;

    /** Alt lock status of the Hardware keyboard */
    private int mHardAlt;

    /** Shift lock toggle definition */
    private static final int[] mShiftKeyToggle = {0, MetaKeyKeyListener.META_SHIFT_ON, MetaKeyKeyListener.META_CAP_LOCKED};

    /** Alt lock toggle definition */
    private static final int[] mAltKeyToggle = {0, MetaKeyKeyListener.META_ALT_ON, MetaKeyKeyListener.META_ALT_LOCKED};

    /** Auto caps mode */
    private boolean mAutoCaps = false;

    /** current hard keyboard state */
    private int mHardKeyboardHidden = Configuration.HARDKEYBOARDHIDDEN_NO;
    /**  */
    private int mSoftKeyboardModeBack = DefaultSoftKeyboard.KEYMODE_JA_FULL_HIRAGANA;
    /** current hard keyboard state */
    private int mSoftKeyboardTypeBack = DefaultSoftKeyboard.KEYBOARD_QWERTY;

    /**
     * Constructor
     */
    public OpenWnnJAJP() {
        super();
        mSelf = this;
        mComposingText = new ComposingText();
        mCandidatesViewManager = new TextCandidatesViewManager(350);
        mInputViewManager  = new DefaultSoftKeyboardJAJP();
        mConverter = mConverterJAJP = new OpenWnnEngineJAJP("/data/data/jp.co.omronsoft.openwnn/writableJAJP.dic");
        mConverterEN = new OpenWnnEngineEN("/data/data/jp.co.omronsoft.openwnn/writableEN.dic");
        mPreConverter = mPreConverterHiragana = new Romkan();
        mPreConverterFullKatakana = new RomkanFullKatakana();
        mPreConverterHalfKatakana = new RomkanHalfKatakana();

        mDisplayText = new SpannableStringBuilder();
        mAutoHideMode = false;

        mDelayUpdateHandler = new Handler();
    }

    /**
     * Constructor
     *
     * @param context       The context
     */
    public OpenWnnJAJP(Context context) {
        this();
        attachBaseContext(context);
    }

    /** @see jp.co.omronsoft.openwnn.OpenWnn#onCreate */
    @Override public void onCreate() {
        super.onCreate();

        String delimiter = Pattern.quote(getResources().getString(R.string.en_word_separators));
        mEnglishAutoCommitDelimiter = Pattern.compile(".*[" + delimiter + "]$");
        if (mConverterSymbolEngineBack == null) {
            mConverterSymbolEngineBack = new SymbolList(this, SymbolList.LANG_JA);
        }
    }

    /** @see jp.co.omronsoft.openwnn.OpenWnn#onCreateInputView */
    @Override public View onCreateInputView() {
        /* initialize the hard keyboard state (G1 specific) */
        WindowManager wm = (WindowManager)getSystemService(Context.WINDOW_SERVICE);
        if (wm.getDefaultDisplay().getWidth() != 320) {
            mHardKeyboardHidden = Configuration.HARDKEYBOARDHIDDEN_NO;
        } else {
            mHardKeyboardHidden = Configuration.HARDKEYBOARDHIDDEN_YES;
        }

        return super.onCreateInputView();
    }

    /** @see jp.co.omronsoft.openwnn.OpenWnn#onStartInputView */
    @Override public void onStartInputView(EditorInfo attribute, boolean restarting) {

        EngineState state = new EngineState();
        state.temporaryMode = EngineState.TEMPORARY_DICTIONARY_MODE_NONE;
        updateEngineState(state);

        if (mDirectInputMode) {
            DefaultSoftKeyboardJAJP inputManager = ((DefaultSoftKeyboardJAJP)mInputViewManager);
            inputManager.setDefaultKeyboard();
        }

        super.onStartInputView(attribute, restarting);

        mEnableAutoDeleteSpace = false;
        /* initialize views */
        mCandidatesViewManager.clearCandidates();
        /* initialize status */
        mStatus = STATUS_INIT;
        mExactMatchMode = false;       
        /* load preferences */
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);

        /* hardware keyboard support */
        mHardShift = 0;
        mHardAlt   = 0;
        /* auto caps mode */
        try {
            mAutoCaps = (Settings.System.getInt(getContentResolver(), Settings.System.TEXT_AUTO_CAPS) != 0);
        } catch (Exception ex) {
            mAutoCaps = false;
        }

        /* initialize the engine's state */
        fitInputType(pref, attribute);

        ((TextCandidatesViewManager)mCandidatesViewManager).setAutoHide(true);
    }

    /** @see jp.co.omronsoft.openwnn.OpenWnn#onComputeInsets */
    @Override public void onComputeInsets(InputMethodService.Insets outInsets) {
        if (mCandidatesViewManager.getViewType() == CandidatesViewManager.VIEW_TYPE_FULL) {
            outInsets.touchableInsets = InputMethodService.Insets.TOUCHABLE_INSETS_FRAME;
        } else {
            super.onComputeInsets(outInsets);
        }
    }

    /** @see jp.co.omronsoft.openwnn.OpenWnn#isFullscreenMode */
    @Override public boolean isFullscreenMode() {
        boolean ret;
        if (mInputViewManager == null) {
            ret = (mCandidatesViewManager.getViewType() == CandidatesViewManager.VIEW_TYPE_FULL);
        } else {
            ret = false;
        }
        return ret;
    }

    /** @see jp.co.omronsoft.openwnn.OpenWnn#onUpdateSelection */
    @Override public void onUpdateSelection(int oldSelStart, int oldSelEnd, int newSelStart, int newSelEnd, int candidatesStart, int candidatesEnd) {
        if (mComposingText.size(ComposingText.LAYER1) != 0) {
            updateViewStatus(mTargetLayer, false, true);
        }
    }

    /** @see jp.co.omronsoft.openwnn.OpenWnn#onConfigurationChanged */
    @Override public void onConfigurationChanged(Configuration newConfig) {
        try {
            super.onConfigurationChanged(newConfig);
            
            if (mInputConnection != null) {
                if (super.isInputViewShown()) {
                    updateViewStatus(mTargetLayer, true, true);
                }

                /* display orientation */
                if (mOrientation != newConfig.orientation) {
                    mOrientation = newConfig.orientation;
                    commitConvertingText();
                    initializeScreen();
                }

                /* Hardware keyboard */
                mHardKeyboardHidden = newConfig.hardKeyboardHidden;
                if (mHardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO) {
                    /* back up the current software keyboard state */
                    mSoftKeyboardTypeBack = ((DefaultSoftKeyboardJAJP) mInputViewManager).getKeyboardType();
                    mSoftKeyboardModeBack = ((DefaultSoftKeyboardJAJP) mInputViewManager).getKeyMode();
                    
                    /* change to Qwerty mode if using hard keyboard (G1 specific) */
                    ((DefaultSoftKeyboardJAJP) mInputViewManager).changeKeyboardType(DefaultSoftKeyboard.KEYBOARD_QWERTY);
                    
                    if (mPreConverter == null) {
                        /* set default mode for English or direct input */
                        ((DefaultSoftKeyboardJAJP) mInputViewManager).changeKeyMode(DefaultSoftKeyboard.KEYMODE_JA_HALF_ALPHABET);
                    } else {
                        /* set default mode for Japanese */
                        ((DefaultSoftKeyboardJAJP) mInputViewManager).changeKeyMode(DefaultSoftKeyboard.KEYMODE_JA_FULL_HIRAGANA);
                    }
                } else {
                    /* restore the current software keyboard state */
                    ((DefaultSoftKeyboardJAJP) mInputViewManager).changeKeyboardType(mSoftKeyboardTypeBack);
                    ((DefaultSoftKeyboardJAJP) mInputViewManager).changeKeyMode(mSoftKeyboardModeBack);
                }
            }
        } catch (Exception ex) {
            /* do nothing if an error occurs. */
        }
    }

    /** @see jp.co.omronsoft.openwnn.OpenWnn#onEvent */
    @Override synchronized public boolean onEvent(OpenWnnEvent ev) {

        EngineState state;

        /* handling events which are valid when InputConnection is not active. */
        switch (ev.code) {

        case OpenWnnEvent.INITIALIZE_LEARNING_DICTIONARY:
            mConverterEN.initializeDictionary(WnnEngine.DICTIONARY_TYPE_LEARN);
            mConverterJAJP.initializeDictionary(WnnEngine.DICTIONARY_TYPE_LEARN);
            return true;

        case OpenWnnEvent.INITIALIZE_USER_DICTIONARY:
            return mConverterJAJP.initializeDictionary( WnnEngine.DICTIONARY_TYPE_USER );

        case OpenWnnEvent.LIST_WORDS_IN_USER_DICTIONARY:
            mUserDictionaryWords = mConverterJAJP.getUserDictionaryWords( );
            return true;

        case OpenWnnEvent.GET_WORD:
            if (mUserDictionaryWords != null) {
                ev.word = mUserDictionaryWords[0];
                for (int i = 0 ; i < mUserDictionaryWords.length - 1 ; i++) {
                    mUserDictionaryWords[i] = mUserDictionaryWords[i + 1];
                }
                mUserDictionaryWords[mUserDictionaryWords.length - 1] = null;
                if (mUserDictionaryWords[0] == null) {
                    mUserDictionaryWords = null;
                }
                return true;
            }
            break;

        case OpenWnnEvent.ADD_WORD:
            mConverterJAJP.addWord(ev.word);
            return true;

        case OpenWnnEvent.DELETE_WORD:
            mConverterJAJP.deleteWord(ev.word);
            return true;

        case OpenWnnEvent.CHANGE_MODE:
            changeEngineMode(ev.mode);
            break;
        case OpenWnnEvent.UPDATE_CANDIDATE:
            updateCandidateView();
            return true;
        default:
            break;
        }

        if (mDirectInputMode) {
            /* return if InputConnection is not active */
            return false;
        }

        /* notice a break the sequence of input to the converter */
        View candidateView = mCandidatesViewManager.getCurrentView();
        if ((candidateView != null) && !candidateView.isShown()
            && (mComposingText.size(0) == 0)) {
            if (mConverter != null) {
                disableAutoDeleteSpace(ev);
                mConverter.breakSequence();
            }
        }

        /* change back the dictionary if necessary */
        if (!((ev.code == OpenWnnEvent.SELECT_CANDIDATE)
              || (ev.code == OpenWnnEvent.CHANGE_MODE)
              || (ev.code == OpenWnnEvent.LIST_CANDIDATES_NORMAL)
              || (ev.code == OpenWnnEvent.LIST_CANDIDATES_FULL))) {

            int prevTemp = mEngineState.temporaryMode;
            state = new EngineState();
            state.temporaryMode = EngineState.TEMPORARY_DICTIONARY_MODE_NONE;
            updateEngineState(state);
        }

        if (ev.code == OpenWnnEvent.LIST_CANDIDATES_FULL) {
            mCandidatesViewManager.setViewType(CandidatesViewManager.VIEW_TYPE_FULL);
            mStatus |= STATUS_CANDIDATE_FULL;
            return true;
        }

        boolean ret = false;
        switch (ev.code) {
        case OpenWnnEvent.INPUT_CHAR:
            if ((mConverter == null) && (mPreConverter == null)) {
                /* direct input (= full-width alphabet/number input) */
                commitText(false);
                commitText(new String(ev.chars));
                mCandidatesViewManager.clearCandidates();
            } else if (mConverter == null) {
                processSoftKeyboardCodeWithoutConversion(ev.chars);
            } else {
                processSoftKeyboardCode(ev.chars);
            }
            ret = true;
            break;

        case OpenWnnEvent.TOGGLE_CHAR:
            processSoftKeyboardToggleChar(ev.toggleTable);
            ret = true;
            break;

        case OpenWnnEvent.TOGGLE_REVERSE_CHAR:
            if ((mStatus == STATUS_INPUT)
                && !(mEngineState.isConvertState())) {

                int cursor = mComposingText.getCursor(ComposingText.LAYER1);
                if (cursor > 0) {
                    String prevChar = mComposingText.getStrSegment(ComposingText.LAYER1, cursor - 1).string;
                    String c = searchToggleCharacter(prevChar, ev.toggleTable, true);
                    if (c != null) {
                        mComposingText.delete(ComposingText.LAYER1, false);
                        appendStrSegment(new StrSegment(c));
                        updateViewStatusForPrediction(true, true);
                        ret = true;
                        break;
                    }
                }
            }
            break;

        case OpenWnnEvent.REPLACE_CHAR:
            int cursor = mComposingText.getCursor(ComposingText.LAYER1);
            if ((cursor > 0)
                && !(mEngineState.isConvertState())) {

                String search = mComposingText.getStrSegment(ComposingText.LAYER1, cursor - 1).string;
                String c = (String)ev.replaceTable.get(search);
                if (c != null) {
                    mComposingText.delete(1, false);
                    appendStrSegment(new StrSegment(c));
                    updateViewStatusForPrediction(true, true);
                    ret = true;
                    mStatus = STATUS_INPUT_EDIT;
                    break;
                }
            }
            break;

        case OpenWnnEvent.INPUT_KEY:
            /* update shift/alt state */
            int keyCode = ev.keyEvent.getKeyCode();
            switch (keyCode) {
            case KeyEvent.KEYCODE_ALT_LEFT:
            case KeyEvent.KEYCODE_ALT_RIGHT:
                if (ev.keyEvent.getRepeatCount() == 0) {
                    if (++mHardAlt > 2) { mHardAlt = 0; }
                }
                updateMetaKeyStateDisplay();
                return true;

            case KeyEvent.KEYCODE_SHIFT_LEFT:
            case KeyEvent.KEYCODE_SHIFT_RIGHT:
                if (ev.keyEvent.getRepeatCount() == 0) {
                    if (++mHardShift > 2) { mHardShift = 0; }
                }
                updateMetaKeyStateDisplay();
                return true;
            }

            /* handle other key event */
            ret = processKeyEvent(ev.keyEvent);
            break;

        case OpenWnnEvent.INPUT_SOFT_KEY:
            ret = processKeyEvent(ev.keyEvent);
            if (!ret) {
                mInputConnection.sendKeyEvent(ev.keyEvent);
                mInputConnection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, ev.keyEvent.getKeyCode()));
                ret = true;
            }
            break;

        case OpenWnnEvent.SELECT_CANDIDATE:
            if (isEnglishPrediction()) {
                mComposingText.clear();
            }
            mStatus = commitText(ev.word);
            break;

        case OpenWnnEvent.CONVERT:           
            startConvert(EngineState.CONVERT_TYPE_RENBUN);
            break;

        case OpenWnnEvent.COMMIT_COMPOSING_TEXT:
            if (mEngineState.isConvertState()) {
                commitConvertingText();
            } else {
                mComposingText.setCursor(ComposingText.LAYER1,
                                         mComposingText.size(ComposingText.LAYER1));
                mStatus = commitText(true);
            }
            break;
        }

        if (mCandidatesViewManager.getViewType() == CandidatesViewManager.VIEW_TYPE_FULL) {
            mCandidatesViewManager.setViewType(CandidatesViewManager.VIEW_TYPE_NORMAL);
            mStatus &= ~STATUS_CANDIDATE_FULL;
        }

        return ret;
    }

    /** @see jp.co.omronsoft.openwnn.OpenWnn#onEvaluateFullscreenMode */
    @Override public boolean onEvaluateFullscreenMode() {
        /* never use full-screen mode */
        return false;
    }

    /** @see jp.co.omronsoft.openwnn.OpenWnn#onEvaluateInputViewShown */
    @Override public boolean onEvaluateInputViewShown() {
        return true;
    }

    /**
     * Get the instance of this service.
     * <br>
     * Before using this method, the constructor of this service must be invoked.
     *
     * @return      The instance of this service
     */
    public static OpenWnnJAJP getInstance() {
        return mSelf;
    }

    /**
     * Create a <code>StrSegment</code> from a character code.
     * <br>
     * @param charCode character code
     * @return <code>StrSegment</code> created; null if an error occurs.
     */
    private StrSegment createStrSegment(int charCode) {
        if (charCode == 0) {
            return null;
        }
        return new StrSegment(Character.toChars(charCode));
    }

    /**
     * Key event handler.
     *
     * @param ev  a key event
     * @return <code>true</code> if the event is handled in this method.
     */
    private boolean processKeyEvent(KeyEvent ev) {
        int key = ev.getKeyCode();

        /* keys which produce a glyph */
        if (ev.isPrintingKey()) {
            /* do nothing if the character is not able to display or the character is dead key */
            if ((mHardShift > 0 && mHardAlt > 0) ||
                (ev.isAltPressed() == true && ev.isShiftPressed() == true)) {
                int charCode = ev.getUnicodeChar(MetaKeyKeyListener.META_SHIFT_ON | MetaKeyKeyListener.META_ALT_ON);
                if (charCode == 0 || (charCode & KeyCharacterMap.COMBINING_ACCENT) != 0 || charCode == PRIVATE_AREA_CODE) {
                    if (mHardAlt == 1) {
                        mHardAlt = 0;
                    }
                    if (mHardShift == 1) {
                        mHardShift = 0;
                    }
                    updateMetaKeyStateDisplay();
                    return true;
                }
            }

            commitConvertingText();

            EditorInfo edit = getCurrentInputEditorInfo();
            StrSegment str;

            /* get the key character */
            if (mHardShift== 0 && mHardAlt == 0) {
                /* no meta key is locked */
                int shift = (mAutoCaps)? getShiftKeyState(edit) : 0;
                if (shift != mHardShift && (key >= KeyEvent.KEYCODE_A && key <= KeyEvent.KEYCODE_Z)) {
                    /* handling auto caps for a alphabet character */
                    str = createStrSegment(ev.getUnicodeChar(MetaKeyKeyListener.META_SHIFT_ON));
                } else {
                    str = createStrSegment(ev.getUnicodeChar());
                }
            } else {
                str = createStrSegment(ev.getUnicodeChar(mShiftKeyToggle[mHardShift]
                                                         | mAltKeyToggle[mHardAlt]));
                /* back to 0 (off) if 1 (on/not locked) */
                if (mHardAlt == 1) {
                    mHardAlt = 0;
                }
                if (mHardShift == 1) {
                    mHardShift = 0;
                }
                updateMetaKeyStateDisplay();
            }
            
            if (str == null) {
                return true;
            } else if (edit.inputType == EditorInfo.TYPE_CLASS_PHONE) {
                /* commit directly */
                appendStrSegment(str);
                commitText(true);
                return true;
            }

            /* append the character to the composing text if the character is not TAB */
            if (str.string.charAt(0) != '\u0009') {
                boolean commit = false;
                if (mPreConverter == null) {
                    Matcher m = mEnglishAutoCommitDelimiter.matcher(str.string);
                    if (m.matches()) {
                        mEnableAutoInsertSpace = false;
                        commitText(true);
                        mEnableAutoInsertSpace = true;

                        commit = true;
                    }
                    appendStrSegment(str);
                } else {
                    appendStrSegment(str);
                    mPreConverter.convert(mComposingText);
                }

                if (commit) {
                    commitText(true);
                } else {
                    mStatus = STATUS_INPUT;
                    updateViewStatusForPrediction(true, true);
                }
                return true;
            }else{
            	commitText(true);
            	commitText(str.string);
            	return true;
            }

        } else if (key == KeyEvent.KEYCODE_SPACE) {
            /* H/W space key */
            if (ev.isShiftPressed()) {
                /* change Japanese <-> English mode */
                mHardAlt = 0;
                mHardShift = 0;
                updateMetaKeyStateDisplay();
                mConverter = mConverterJAJP;
                if (isEnglishPrediction()) {
                    /* English mode to Japanese mode */
                    ((DefaultSoftKeyboardJAJP) mInputViewManager).changeKeyMode(DefaultSoftKeyboard.KEYMODE_JA_FULL_HIRAGANA);
                } else {
                    /* Japanese mode to English mode */
                    ((DefaultSoftKeyboardJAJP) mInputViewManager).changeKeyMode(DefaultSoftKeyboard.KEYMODE_JA_HALF_ALPHABET);
                }
                return true;

            } else if (ev.isAltPressed()) {
                /* display the symbol list (G1 specific. same as KEYCODE_SYM) */
                mStatus = commitText(true);
                changeEngineMode(ENGINE_MODE_SYMBOL);
                mHardAlt = 0;
                updateMetaKeyStateDisplay();
                return true;
                
            } else if (isEnglishPrediction()) {
                /* Auto commit if English mode */
                if (mComposingText.size(0) == 0) {
                    commitText(" ");
                } else {
                    commitText(true);
                    commitSpaceJustOne();
                }
                return true;
                
            } else {
                /* start consecutive clause conversion if Japanese mode */
                if (mComposingText.size(0) != 0) {
                    startConvert(EngineState.CONVERT_TYPE_RENBUN);
                    return true;
                }
            }

        } else if (key == KeyEvent.KEYCODE_SYM) {
            /* display the symbol list */
            mStatus = commitText(true);
            changeEngineMode(ENGINE_MODE_SYMBOL);
            mHardAlt = 0;
            updateMetaKeyStateDisplay();
            return true;
        }

        /* Functional key */
        if (mComposingText.size(ComposingText.LAYER1) > 0) {
            switch (key) {
            case KeyEvent.KEYCODE_DEL:
                mStatus = STATUS_INPUT_EDIT;
                if (mEngineState.isConvertState()) {
                    mComposingText.setCursor(ComposingText.LAYER1,
                                             mComposingText.toString(ComposingText.LAYER1).length());
                    mExactMatchMode = false;
                } else {
                    mComposingText.delete(ComposingText.LAYER1, false);
                    if (mComposingText.size(ComposingText.LAYER1) == 0) {
                        initializeScreen();
                        return true;
                    }
                }
                updateViewStatusForPrediction(true, true);
                return true;

            case KeyEvent.KEYCODE_BACK:
                if (mCandidatesViewManager.getViewType() == CandidatesViewManager.VIEW_TYPE_FULL) {
                    mCandidatesViewManager.setViewType(CandidatesViewManager.VIEW_TYPE_NORMAL);
                    mStatus &= ~STATUS_CANDIDATE_FULL;
                } else {
                    if (!mEngineState.isConvertState()) {
                        mComposingText.clear();
                    }
                    mStatus = STATUS_INPUT_EDIT;
                    if (mConverter != null) {
                        mConverter.init();
                    }
                    mExactMatchMode = false;
                    mComposingText.setCursor(ComposingText.LAYER1,
                                             mComposingText.toString(ComposingText.LAYER1).length());
                    updateViewStatusForPrediction(true, true);
                }
                return true;

            case KeyEvent.KEYCODE_DPAD_LEFT:
                if (mConverter == null) {
                    commitText(false);
                    return false;
                } else {
                    processLeftKeyEvent();
                    return true;
                }

            case KeyEvent.KEYCODE_DPAD_RIGHT:
                if (mConverter == null) {
                    commitText(false);
                    return false;
                } else {
                    processRightKeyEvent();
                    return true;
                }

            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
                mStatus = commitText(true);
                return true;

            case KeyEvent.KEYCODE_CALL:
                return false;

            default:
                return true;
            }
        } else {
            /* if there is no composing string. */
            if (mCandidatesViewManager.getCurrentView().isShown()) {
                /* displaying relational prediction candidates */
                switch (key) {
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    if (mConverter != null) {
                        /* initialize the converter */
                        mConverter.init();
                    }
                    mStatus = STATUS_INPUT_EDIT;
                    updateViewStatusForPrediction(true, true);
                    return false;

                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    if (mConverter != null) {
                        /* initialize the converter */
                        mConverter.init();
                    }
                    mStatus = STATUS_INPUT_EDIT;
                    updateViewStatusForPrediction(true, true);
                    return false;

                default:
                    return processKeyEventNoInputCandidateShown(ev);
                }
            } else if (key == KeyEvent.KEYCODE_BACK && isInputViewShown()) {
                /*
                 * If 'BACK' key is pressed when the SW-keyboard is shown
                 * and the candidates view is not shown, dissmiss the SW-keyboard.
                 */
                mInputViewManager.closing();
                requestHideSelf(0);
                return true;
            }
        }

        return false;
    }

    /** Thread for updating the candidates view */
    private final Runnable updatePredictionRunnable = new Runnable() {
            public void run() {
                int candidates = 0;
                int cursor = mComposingText.getCursor(ComposingText.LAYER1);

                if (mConverter != null) {
                    if (mExactMatchMode) {
                        /* exact matching */
                        candidates = mConverter.predict(mComposingText, 0, cursor);
                    } else {
                        /* normal prediction */
                        candidates = mConverter.predict(mComposingText, 0, -1);
                    }
                }

                /* update the candidates view */
                if (candidates > 0) {
                    mCandidatesViewManager.displayCandidates(mConverter);
                } else {
                    mCandidatesViewManager.clearCandidates();
                }
            }
        };

    /**
     * Handle a left key event.
     */
    private void processLeftKeyEvent() {
        if (mEngineState.isConvertState()) {
            if (mEngineState.isEisuKana()) {
                mExactMatchMode = true;
            }

            if (1 < mComposingText.getCursor(ComposingText.LAYER1)) {
                mComposingText.moveCursor(ComposingText.LAYER1, -1);
            }
        } else if (mExactMatchMode) {
            mComposingText.moveCursor(ComposingText.LAYER1, -1);
        } else {
            if (isEnglishPrediction()) {
                mComposingText.moveCursor(ComposingText.LAYER1, -1);
            } else {
                mExactMatchMode = true;
            }
        }

        mCommitCount = 0;
        mStatus = STATUS_INPUT_EDIT;
        updateViewStatus(mTargetLayer, true, true);
    }

    /**
     * Handle a right key event.
     */
    private void processRightKeyEvent() {
        int layer = mTargetLayer;
        if (mExactMatchMode || (mEngineState.isConvertState())) {
            int textSize = mComposingText.size(ComposingText.LAYER1);
            if (mComposingText.getCursor(ComposingText.LAYER1) == textSize) {
                mExactMatchMode = false;
                layer = ComposingText.LAYER1;
                EngineState state = new EngineState();
                state.convertType = EngineState.CONVERT_TYPE_NONE;
                updateEngineState(state);
            } else {
                if (mEngineState.isEisuKana()) {
                    mExactMatchMode = true;
                }
                mComposingText.moveCursor(ComposingText.LAYER1, 1);
            }
        } else {
            if (isEnglishPrediction()
                && (mComposingText.getCursor(ComposingText.LAYER1)
                    < mComposingText.size(ComposingText.LAYER1))) {

                mComposingText.moveCursor(ComposingText.LAYER1, 1);
            }
        }

        mCommitCount = 0;
        mStatus = STATUS_INPUT_EDIT;

        updateViewStatus(layer, true, true);
    }

    /**
     * Handle a key event which is not right or left key when the
     * composing text is empty and some candidates are shown.
     *
     * @param ev  a key event
     */
    boolean processKeyEventNoInputCandidateShown(KeyEvent ev) {
        boolean ret = true;

        switch (ev.getKeyCode()) {
        case KeyEvent.KEYCODE_DEL:
            ret = true;
            break;
        case KeyEvent.KEYCODE_ENTER:
        case KeyEvent.KEYCODE_DPAD_UP:
        case KeyEvent.KEYCODE_DPAD_DOWN:
            ret = false;
            break;
            
        case KeyEvent.KEYCODE_CALL:
            return false;
            
        case KeyEvent.KEYCODE_MENU:
            return true;

        default:
            break;
        }

        if (mConverter != null) {
            mConverter.init();
        }
        updateViewStatusForPrediction(true, true);
        return ret;
    }

    /**
     * Update views and the display of the composing text for predict mode.
     *
     * @param updateCandidates  <code>true</code> to update the candidates view
     * @param updateEmptyText
     *   <code>false</code> to update the composing text if it is not empty; <code>true</code> to update always 
     */
    private void updateViewStatusForPrediction(boolean updateCandidates, boolean updateEmptyText) {
        EngineState state = new EngineState();
        state.convertType = EngineState.CONVERT_TYPE_NONE;
        updateEngineState(state);

        updateViewStatus(ComposingText.LAYER1, updateCandidates, updateEmptyText);
    }

    /**
     * Update views and the display of the composing text.
     *
     * @param layer  display layer of the composing text
     * @param updateCandidates  <code>true</code> to update the candidates view
     * @param updateEmptyText
     *   <code>false</code> to update the composing text if it is not empty; <code>true</code> to update always 
     */
    private void updateViewStatus(int layer, boolean updateCandidates, boolean updateEmptyText) {
        mTargetLayer = layer;

        if (updateCandidates) {
            updateCandidateView();
        }
        /* notice to the input view */
        mInputViewManager.onUpdateState(this);

        /* set the candidates view to the normal size */
        if (mCandidatesViewManager.getViewType() != CandidatesViewManager.VIEW_TYPE_NORMAL) {
            mCandidatesViewManager.setViewType(CandidatesViewManager.VIEW_TYPE_NORMAL);
            mStatus &= ~STATUS_CANDIDATE_FULL;
        }
        /* set the text for displaying as the composing text */
        mDisplayText.clear();
        mDisplayText.insert(0, mComposingText.toString(layer));

        /* add decoration to the text */
        int cursor = mComposingText.getCursor(layer);
        if ((mInputConnection != null) && (mDisplayText.length() != 0 || updateEmptyText)) {
            if (cursor != 0) {
                if (mExactMatchMode && (!mEngineState.isEisuKana())) {
                    mDisplayText.setSpan(SPAN_EXACT_BGCOLOR_HL, 0, cursor,
                                         Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }

                if (layer == ComposingText.LAYER2) {
                    /* highlights the first segment */
                    mDisplayText.setSpan(SPAN_CONVERT_BGCOLOR_HL, 0,
                                         mComposingText.toString(layer, 0, 0).length(),
                                         Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }

            mDisplayText.setSpan(SPAN_UNDERLINE, 0, mDisplayText.length(),
                                 Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            int displayCursor = mComposingText.toString(layer, 0, cursor - 1).length();
            /* update the composing text on the EditView */
            mInputConnection.setComposingText(mDisplayText, displayCursor);
        }
    }

    /**
     * Update the candidates view.
     */
    private void updateCandidateView() {
        switch (mTargetLayer) {
        case ComposingText.LAYER0:
        case ComposingText.LAYER1:
            if (mEnablePrediction || mEngineState.isSymbolList()) {
                /* update the candidates view */
                if ((mComposingText.size(ComposingText.LAYER1) != 0)
                    && !mEngineState.isConvertState()) {
                    mDelayUpdateHandler.removeCallbacks(updatePredictionRunnable);
                    mDelayUpdateHandler.postDelayed(updatePredictionRunnable, 250);
                } else {
                    mDelayUpdateHandler.removeCallbacks(updatePredictionRunnable);
                    mDelayUpdateHandler.postDelayed(updatePredictionRunnable, 0);
                }
            } else {
                mDelayUpdateHandler.removeCallbacks(updatePredictionRunnable);
                mCandidatesViewManager.clearCandidates();
            }
            break;
        case ComposingText.LAYER2:
            if (mCommitCount == 0) {
                mDelayUpdateHandler.removeCallbacks(updatePredictionRunnable);
                mConverter.convert(mComposingText);
            }

            int candidates = mConverter.makeCandidateListOf(mCommitCount);

            if (candidates != 0) {
                mComposingText.setCursor(ComposingText.LAYER2, 1);
                mCandidatesViewManager.displayCandidates(mConverter);
            } else {
                mComposingText.setCursor(ComposingText.LAYER1,
                                         mComposingText.toString(ComposingText.LAYER1).length());
                mCandidatesViewManager.clearCandidates();
            }
            break;
        default:
            break;
        }
    }

    /**
     * Commit the displaying composing text.
     *
     * @param learn  <code>true</code> to register the committed string to the learning dictionary.
     */
    private int commitText(boolean learn) {
        if (isEnglishPrediction()) {
            mComposingText.setCursor(ComposingText.LAYER1,
                                     mComposingText.size(ComposingText.LAYER1));
        }

        int layer = mTargetLayer;
        int cursor = mComposingText.getCursor(layer);
        if (cursor == 0) {
            return mStatus;
        }
        String tmp = mComposingText.toString(layer, 0, cursor - 1);

        if (mConverter != null) {
            if (learn) {
                if (mEngineState.isRenbun()) {
                    learnWord(0);
                } else {
                    if (mComposingText.size(ComposingText.LAYER1) != 0) {
                        String stroke = mComposingText.toString(ComposingText.LAYER1, 0, mComposingText.getCursor(layer) - 1);
                        WnnWord word = new WnnWord(tmp, stroke);
                                                   
                        learnWord(word);
                    }
                }
            } else {
                mConverter.breakSequence();
            }
        }
        return commitTextThroughInputConnection(tmp);
    }

    /**
     * Commit a word.
     *
     * @param word  a word to commit
     */
    private int commitText(WnnWord word) {
        if (mConverter != null) {
            learnWord(word);
        }
        return commitTextThroughInputConnection(word.candidate);
    }

    /**
     * Commit a string.
     *
     * @param str  a string to commit
     */
    private void commitText(String str) {
        mInputConnection.commitText(str, str.length());
        mEnableAutoDeleteSpace = true;


        updateViewStatusForPrediction(false, false);
    }

    /**
     * Commit a string through <code>InputConnection</code>.
     *
     * @param string  a string to commit
     */
    private int commitTextThroughInputConnection(String string) {
        int layer = mTargetLayer;
        mInputConnection.commitText(string, string.length());
        int cursor = mComposingText.getCursor(layer);
        if (cursor > 0) {
            mComposingText.deleteStrSegment(layer, 0, mComposingText.getCursor(layer) - 1);
            mComposingText.setCursor(layer, mComposingText.size(layer));
        }
        mExactMatchMode = false;
        mCommitCount++;

        if ((layer == ComposingText.LAYER2) && (mComposingText.size(layer) == 0)) {
            layer = 1;
        }

        boolean commited = autoCommitEnglish();
        mEnableAutoDeleteSpace = true;

        if (layer == ComposingText.LAYER2) {
            EngineState state = new EngineState();
            state.convertType = EngineState.CONVERT_TYPE_RENBUN;
            updateEngineState(state);
            updateViewStatus(layer, !commited, false);
        } else {
            updateViewStatusForPrediction(!commited, false);
        }

        if (mComposingText.size(ComposingText.LAYER0) == 0) {
            return STATUS_INIT;
        } else {
            return STATUS_INPUT_EDIT;
        }
    }

    /**
     * Returns whether it is English prediction mode or not.
     *
     * @return <code>true</code> if it is English prediction mode; otherwise, <code>false</code>.
     */
    private boolean isEnglishPrediction() {
        return ((mConverter != null) && (mPreConverter == null));
    }

    /**
     * Change the conversion engine and the letter converter(Romaji-to-Kana converter).
     *
     * @param mode  engine's mode to be changed
     * @see jp.co.omronsoft.openwnn.OpenWnnEvent.Mode
     * @see jp.co.omronsoft.openwnn.JAJP.DefaultSoftKeyboardJAJP
     */
    private void changeEngineMode(int mode) {
        EngineState state = new EngineState();

        switch (mode) {
        case ENGINE_MODE_OPT_TYPE_QWERTY:
            state.keyboard = EngineState.KEYBOARD_QWERTY;
            updateEngineState(state);
            return;

        case ENGINE_MODE_OPT_TYPE_12KEY:
            state.keyboard = EngineState.KEYBOARD_12KEY;
            updateEngineState(state);
            return;

        case ENGINE_MODE_EISU_KANA:
            if (mEngineState.isEisuKana()) {
                state.temporaryMode = EngineState.TEMPORARY_DICTIONARY_MODE_NONE;
                updateEngineState(state);
                updateViewStatusForPrediction(true, true);
            } else {
                startConvert(EngineState.CONVERT_TYPE_EISU_KANA);
            }
            return;

        case ENGINE_MODE_SYMBOL:
            state.temporaryMode = EngineState.TEMPORARY_DICTIONARY_MODE_SYMBOL;
            updateEngineState(state);
            updateViewStatusForPrediction(true, true);
            return;

        default:
            break;
        }

        mComposingText.delete(ComposingText.LAYER1, false);
        updateViewStatusForPrediction(true, true);

        state = new EngineState();
        state.temporaryMode = EngineState.TEMPORARY_DICTIONARY_MODE_NONE;
        updateEngineState(state);

        state = new EngineState();
        switch (mode) {
        case OpenWnnEvent.Mode.DIRECT:
            /* Full/Half-width number or Full-width alphabet */
            mConverter = null;
            mPreConverter = null;
            break;

        case OpenWnnEvent.Mode.NO_LV1_CONV:
            /* no Romaji-to-Kana conversion (=English prediction mode) */
            state.dictionarySet = EngineState.DICTIONARYSET_EN;
            updateEngineState(state);
            mConverter = mConverterEN;
            mPreConverter = null;
            break;

        case OpenWnnEvent.Mode.NO_LV2_CONV:
            mConverter = null;
            mPreConverter = mPreConverterHiragana;
            break;

        case ENGINE_MODE_FULL_KATAKANA:
            mConverter = null;
            mPreConverter = mPreConverterFullKatakana;
            break;

        case ENGINE_MODE_HALF_KATAKANA:
            mConverter = null;
            mPreConverter = mPreConverterHalfKatakana;
            break;

        default:
            /* HIRAGANA input mode */
            state.dictionarySet = EngineState.DICTIONARYSET_JP;
            updateEngineState(state);
            mConverter = mConverterJAJP;
            mPreConverter = mPreConverterHiragana;
            break;
        }

        mPreConverterBack = mPreConverter;
        mConverterBack = mConverter;
    }

    /**
     * Update the conversion engine's state.
     *
     * @param state  engine's state to be updated
     */
    private void updateEngineState(EngineState state) {
        EngineState myState = mEngineState;

        /* language */
        if ((state.dictionarySet != EngineState.INVALID) 
            && (myState.dictionarySet != state.dictionarySet)) {

            switch (state.dictionarySet) {
            case EngineState.DICTIONARYSET_EN:
                setDictionary(OpenWnnEngineJAJP.DIC_LANG_EN);
                break;

            case EngineState.DICTIONARYSET_JP:
            default:
                setDictionary(OpenWnnEngineJAJP.DIC_LANG_JP);
                break;
            }
            myState.dictionarySet = state.dictionarySet;

            /* update keyboard setting */
            if (state.keyboard == EngineState.INVALID) {
                state.keyboard = myState.keyboard;
            }
        }

        /* type of conversion */
        if ((state.convertType != EngineState.INVALID)
            && (myState.convertType != state.convertType)) {

            switch (state.convertType) {
            case EngineState.CONVERT_TYPE_NONE:
                setDictionary(mPrevDictionarySet);
                break;

            case EngineState.CONVERT_TYPE_EISU_KANA:
                setDictionary(OpenWnnEngineJAJP.DIC_LANG_JP_EISUKANA);
                break;

            case EngineState.CONVERT_TYPE_RENBUN:
            default:
                setDictionary(OpenWnnEngineJAJP.DIC_LANG_JP);
                break;
            }
            myState.convertType = state.convertType;
        }

        /* temporary dictionary */
        if (state.temporaryMode != EngineState.INVALID) {

            switch (state.temporaryMode) {
            case EngineState.TEMPORARY_DICTIONARY_MODE_NONE:
                if (myState.temporaryMode != EngineState.TEMPORARY_DICTIONARY_MODE_NONE) {
                    setDictionary(mPrevDictionarySet);
                    mCurrentSymbol = 0;
                    mPreConverter = mPreConverterBack;
                    mConverter = mConverterBack;
                    mDisableAutoCommitEnglishMask &= ~AUTO_COMMIT_ENGLISH_SYMBOL;
                }
                break;

            case EngineState.TEMPORARY_DICTIONARY_MODE_SYMBOL:
                if (++mCurrentSymbol >= SYMBOL_LISTS.length) {
                    mCurrentSymbol = 0;
                }
                mConverterSymbolEngineBack.setDictionary(SYMBOL_LISTS[mCurrentSymbol]);
                mConverter = mConverterSymbolEngineBack;
                mDisableAutoCommitEnglishMask |= AUTO_COMMIT_ENGLISH_SYMBOL;
                break;


            default:
                break;
            }
            myState.temporaryMode = state.temporaryMode;
        }

        /* preference dictionary */
        if ((state.preferenceDictionary != EngineState.INVALID) 
            && (myState.preferenceDictionary != state.preferenceDictionary)) {

            myState.preferenceDictionary = state.preferenceDictionary;
            setDictionary(mPrevDictionarySet);
        }

        /* keyboard type */
        if (state.keyboard != EngineState.INVALID) {
            switch (state.keyboard) {
            case EngineState.KEYBOARD_12KEY:
                mConverterJAJP.setKeyboardType(OpenWnnEngineJAJP.KEYBOARD_KEYPAD12);
                mConverterEN.setDictionary(OpenWnnEngineEN.DICT_DEFAULT);
                break;
                
            case EngineState.KEYBOARD_QWERTY:
            default:
                mConverterJAJP.setKeyboardType(OpenWnnEngineJAJP.KEYBOARD_QWERTY);
                if (mEnableSpellCorrection) {
                    mConverterEN.setDictionary(OpenWnnEngineEN.DICT_FOR_CORRECT_MISTYPE);
                } else {
                    mConverterEN.setDictionary(OpenWnnEngineEN.DICT_DEFAULT);
                }
                break;
            }
            myState.keyboard = state.keyboard;
        }
    }

    /**
     * Set dictionaries to be used.
     * @param mode  definition of dictionaries
     */
    private void setDictionary(int mode) {
        int target = mode;
        switch (target) {

        case OpenWnnEngineJAJP.DIC_LANG_JP:

            switch (mEngineState.preferenceDictionary) {
            case EngineState.PREFERENCE_DICTIONARY_PERSON_NAME:
                target = OpenWnnEngineJAJP.DIC_LANG_JP_PERSON_NAME;
                break;
            case EngineState.PREFERENCE_DICTIONARY_POSTAL_ADDRESS:
                target = OpenWnnEngineJAJP.DIC_LANG_JP_POSTAL_ADDRESS;
                break;
            default:
                break;
            }

            break;

        case OpenWnnEngineJAJP.DIC_LANG_EN:

            switch (mEngineState.preferenceDictionary) {
            case EngineState.PREFERENCE_DICTIONARY_EMAIL_ADDRESS_URI:
                target = OpenWnnEngineJAJP.DIC_LANG_EN_EMAIL_ADDRESS;
                break;
            default:
                break;
            }

            break;

        default:
            break;
        }
 
        switch (target) {
        case OpenWnnEngineJAJP.DIC_LANG_JP:
        case OpenWnnEngineJAJP.DIC_LANG_EN:
            mPrevDictionarySet = mode;
            break;
        default:
            break;
        }

        mConverterJAJP.setDictionary(target);
    }

    /**
     * Handle a toggle key input event.
     *
     * @param table  table of toggle characters
     */
    private void processSoftKeyboardToggleChar(String[] table) {
        if (table == null) {
            return;
        }

        commitConvertingText();

        boolean toggled = false;
        if (mStatus == STATUS_INPUT) {
            int cursor = mComposingText.getCursor(ComposingText.LAYER1);
            if (cursor > 0) {
                String prevChar = mComposingText.getStrSegment(ComposingText.LAYER1,
                                                               cursor - 1).string;
                String c = searchToggleCharacter(prevChar, table, false);
                if (c != null) {
                    mComposingText.delete(ComposingText.LAYER1, false);
                    appendStrSegment(new StrSegment(c));
                    toggled = true;
                }
            }
        }

        if (!toggled) {
            String str = table[0];
            /* shift on */
            if (isEnglishPrediction() && getShiftKeyState(getCurrentInputEditorInfo()) == 1) {
                char top = table[0].charAt(0);
                if (Character.isLowerCase(top)) {
                    str = Character.toString(Character.toUpperCase(top));
                }
            } 
            appendStrSegment(new StrSegment(str));
        }

        mStatus = STATUS_INPUT;

        updateViewStatusForPrediction(true, true);
    }

    /**
     * Handle character input from the software keyboard without listing candidates.
     *
     * @param chars input character(s)
     */
    private void processSoftKeyboardCodeWithoutConversion(char[] chars) {
        if (chars == null) {
            return;
        }

        ComposingText text = mComposingText;
        text.insertStrSegment(0, ComposingText.LAYER1, new StrSegment(chars));

        if (!isAlphabetLast(text.toString(ComposingText.LAYER1))) {
            /* commit if the input character is not alphabet */
            commitText(false);
        } else {
            boolean completed = mPreConverter.convert(text);
            if (completed) {
                commitText(false);
            } else {
                mStatus = STATUS_INPUT;
                updateViewStatusForPrediction(true, true);
            }
        }
    }

    /**
     * Handle character input from the software keyboard.
     *
     * @param chars input character(s)
     */
    private void processSoftKeyboardCode(char[] chars) {
        if (chars == null) {
            return;
        }

        if ((chars[0] == ' ') || (chars[0] == '\u3000' /* Full-width space */)) {
            if (mComposingText.size(0) == 0) {
                mCandidatesViewManager.clearCandidates();
                commitText(new String(chars));
            } else {
                if (isEnglishPrediction()) {
                    commitText(true);
                    commitSpaceJustOne();
                } else {
                    startConvert(EngineState.CONVERT_TYPE_RENBUN);
                }
            }
        } else {
            commitConvertingText();

            /* Auto-commit a word if it is English and Qwerty mode */
            boolean commit = false;
            if (isEnglishPrediction()
                && (mEngineState.keyboard == EngineState.KEYBOARD_QWERTY)) {

                Matcher m = mEnglishAutoCommitDelimiter.matcher(new String(chars));
                if (m.matches()) {
                    commit = true;
                }
            }
        
            if (commit) {
                mEnableAutoInsertSpace = false;
                commitText(true);
                mEnableAutoInsertSpace = true;

                appendStrSegment(new StrSegment(chars));
                commitText(true);
            } else {
                appendStrSegment(new StrSegment(chars));
                if (mPreConverter != null) {
                    mPreConverter.convert(mComposingText);
                    mStatus = STATUS_INPUT;
                }
                updateViewStatusForPrediction(true, true);
            }
        }
    }

    /**
     * Start consecutive clause conversion or EISU-KANA conversion mode.
     *
     * @param convertType conversion type(<code>EngineState.CONVERT_TYPE_*</code>)
     */
    private void startConvert(int convertType) {
        if (mEngineState.convertType != convertType) {
            /* adjust the cursor position */
            if (!mExactMatchMode) {
                if (convertType == EngineState.CONVERT_TYPE_RENBUN) {
                    /* not specify */
                    mComposingText.setCursor(ComposingText.LAYER1, 0);
                } else {
                    if (mEngineState.isRenbun()) {
                        /* EISU-KANA conversion specifying the position of the segment if previous mode is conversion mode */
                        mExactMatchMode = true;
                    } else {
                        /* specify all range */
                        mComposingText.setCursor(ComposingText.LAYER1,
                                                 mComposingText.size(ComposingText.LAYER1));
                    }
                }
            } 

            if (convertType == EngineState.CONVERT_TYPE_RENBUN) {
                /* clears variables for the prediction */
                mExactMatchMode = false;
            }
            /* clears variables for the convert */
            mCommitCount = 0;

            int layer;
            if (convertType == EngineState.CONVERT_TYPE_EISU_KANA) {
                layer = ComposingText.LAYER1;
            } else {
                layer = ComposingText.LAYER2;
            }

            EngineState state = new EngineState();
            state.convertType = convertType;
            updateEngineState(state);

            updateViewStatus(layer, true, true);
        }
    }

    /**
     * Auto commit a word in English (on half-width alphabet mode).
     *
     * @return  <code>true</code> if auto-committed; otherwise, <code>false</code>.
     */
    private boolean autoCommitEnglish() {
        if (isEnglishPrediction() && (mDisableAutoCommitEnglishMask == AUTO_COMMIT_ENGLISH_ON)) {
            CharSequence seq = mInputConnection.getTextBeforeCursor(2, 0);
            Matcher m = mEnglishAutoCommitDelimiter.matcher(seq);
            if (m.matches()) {
                if ((seq.charAt(0) == ' ') && mEnableAutoDeleteSpace) {
                    mInputConnection.deleteSurroundingText(2, 0);
                    mInputConnection.commitText(seq.subSequence(1, 2), 1);
                }

                mDelayUpdateHandler.removeCallbacks(updatePredictionRunnable);

                mCandidatesViewManager.clearCandidates();
                return true;
            } else {
                if (mEnableAutoInsertSpace) {
                    commitSpaceJustOne();
                }
            }
        }
        return false;
    }

    /**
     * Insert a white space if the previous character is not a white space.
     */
    private void commitSpaceJustOne() {
        CharSequence seq = mInputConnection.getTextBeforeCursor(1, 0);
        if (seq.charAt(0) != ' ') {
            commitText(" ");
        }
    }

    /**
     * Get the shift key state from the editor.
     *
     * @param editor  The editor
     *
     * @return State id of the shift key (0:off, 1:on)
     */
    protected int getShiftKeyState(EditorInfo editor) {
        return (getCurrentInputConnection().getCursorCapsMode(editor.inputType) == 0) ? 0 : 1;
    }

    /**
     * Display current meta-key state.
     */
    private void updateMetaKeyStateDisplay() {
    }

    /**
     * Memory a selected word. 
     * @param word  A selected word
     */
    private void learnWord(WnnWord word) {
        if (mEnableLearning && word != null) {
            mConverter.learn(word);
        }
    }

    /**
     * Memory a clause which is generated by consecutive clause conversion.
     * @param index  Index of a clause
     */
    private void learnWord(int index) {
        ComposingText composingText = mComposingText;

        if (mEnableLearning && composingText.size(ComposingText.LAYER2) > index) {
            StrSegment seg = composingText.getStrSegment(ComposingText.LAYER2, index);
            if (seg instanceof StrSegmentClause) {
                mConverter.learn(((StrSegmentClause)seg).clause);
            } else {
                String stroke = composingText.toString(ComposingText.LAYER1, seg.from, seg.to);
                mConverter.learn(new WnnWord(seg.string, stroke));
            }
        }
    }

    /**
     * Fits an editor info.
     * @param preferences  The preference data.
     * @param info  The editor info.
     */
    private void fitInputType(SharedPreferences preference, EditorInfo info) {
        if (info.inputType == EditorInfo.TYPE_NULL) {
            return;
        }
        mEnableLearning   = preference.getBoolean("opt_enable_learning", true);
        mEnablePrediction = preference.getBoolean("opt_prediction", true);
        mEnableSpellCorrection = preference.getBoolean("opt_spell_correction", true);
        mDisableAutoCommitEnglishMask &= ~AUTO_COMMIT_ENGLISH_ON;
        int preferenceDictionary = EngineState.PREFERENCE_DICTIONARY_NONE;

        if ((info.inputType & EditorInfo.TYPE_MASK_CLASS) ==  EditorInfo.TYPE_CLASS_TEXT) {
            switch (info.inputType & EditorInfo.TYPE_MASK_VARIATION) {
            case EditorInfo.TYPE_TEXT_VARIATION_PERSON_NAME:
                preferenceDictionary = EngineState.PREFERENCE_DICTIONARY_PERSON_NAME;
                break;
                
            case EditorInfo.TYPE_TEXT_VARIATION_PASSWORD:
                mEnableLearning = false;
                mEnablePrediction = false;
                mDisableAutoCommitEnglishMask |= AUTO_COMMIT_ENGLISH_OFF;
                break;

            case EditorInfo.TYPE_TEXT_VARIATION_EMAIL_ADDRESS:
            case EditorInfo.TYPE_TEXT_VARIATION_URI:
                mDisableAutoCommitEnglishMask |= AUTO_COMMIT_ENGLISH_OFF;
                preferenceDictionary = EngineState.PREFERENCE_DICTIONARY_EMAIL_ADDRESS_URI;
                break;

            case EditorInfo.TYPE_TEXT_VARIATION_POSTAL_ADDRESS:
                preferenceDictionary = EngineState.PREFERENCE_DICTIONARY_POSTAL_ADDRESS;
                break;

            default:
                break;
            }
        }
        EngineState state = new EngineState();
        state.preferenceDictionary = preferenceDictionary;
        state.convertType = EngineState.CONVERT_TYPE_NONE;
        if (mHardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO) {
            /* change to Qwerty mode if using hard keyboard (G1 specific) */
            state.keyboard = EngineState.KEYBOARD_QWERTY;
        }
        updateEngineState(state);
    }
    
    /**
     * Append a <code>StrSegment</code> to the composing text
     * <br>
     * If the length of the composing text exceeds
     * <code>LIMIT_INPUT_NUMBER</code>, the appending operation is ignored.
     *
     * @param  str  Input segment
     */
    private void appendStrSegment(StrSegment str) {
        ComposingText composingText = mComposingText;
        
        if (composingText.size(ComposingText.LAYER1) >= LIMIT_INPUT_NUMBER) {
            return;
        }
        composingText.insertStrSegment(ComposingText.LAYER0, ComposingText.LAYER1, str);
        return;
    }

    /**
     * Commit the consecutive clause conversion
     */
    private void commitConvertingText() {
        if (mEngineState.isConvertState()) {
            int size = mComposingText.size(ComposingText.LAYER2);
            for (int i = 0; i < size; i++) {
                learnWord(i);
            }

            String text = mComposingText.toString(ComposingText.LAYER2);
            mInputConnection.commitText(text, text.length());
            initializeScreen();
        }
    }
    
    /**
     * Initialize the screen displayed by IME
     */
    private void initializeScreen() {
        mComposingText.clear();
        mInputConnection.setComposingText("", 0);
        mExactMatchMode = false;       
        mStatus = STATUS_INIT;
        mDelayUpdateHandler.removeCallbacks(updatePredictionRunnable);
        View candidateView = mCandidatesViewManager.getCurrentView();
        if ((candidateView != null) && candidateView.isShown()) {
            mCandidatesViewManager.clearCandidates();
        }
        mInputViewManager.onUpdateState(this);
    }
    
    /**
     * Whether the tail of the string is alphabet or not.
     *
     * @param  str  string
     * @return <code>true</code> if the tail is alphabet; <code>false</code> if otherwise.
     */
    private boolean isAlphabetLast(String str) {
        Matcher m = ENGLISH_CHARACTER.matcher(str);
        return m.matches();
    }


    /**
     * Disable auto-delete-space.
     * @param ev  event
     */
    private void disableAutoDeleteSpace(OpenWnnEvent ev) {
        if (mEnableAutoDeleteSpace) {
            if (!isEnglishPrediction()) {
                mEnableAutoDeleteSpace = false;
                return;
            }

            String input = null;
            if (ev.code == OpenWnnEvent.TOGGLE_CHAR) {
                input = ev.toggleTable[0];
            }
            
            try {
                if ((ev.code == OpenWnnEvent.INPUT_KEY) || (ev.code == OpenWnnEvent.INPUT_SOFT_KEY)) {
                    input = new String(Character.toChars(ev.keyEvent.getUnicodeChar(0)));
                }
            } catch (Exception e) {
                input = null;
            }
            
            if (input != null) {
                Matcher m = mEnglishAutoCommitDelimiter.matcher(input);
                if (!m.matches()) {
                    mEnableAutoDeleteSpace = false;
                }
            } else {
                mEnableAutoDeleteSpace = false;
            }
        }
    }

    /** @see jp.co.omronsoft.openwnn.OpenWnn#onFinishInput */
    @Override public void onFinishInput() {
        if (mInputConnection != null) {
            initializeScreen();
        }
        super.onFinishInput();
    }
}


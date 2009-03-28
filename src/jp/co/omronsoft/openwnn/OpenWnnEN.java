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

import jp.co.omronsoft.openwnn.EN.*;
import android.content.SharedPreferences;
import android.content.Context;
import android.content.res.Configuration;
import android.inputmethodservice.InputMethodService;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.method.MetaKeyKeyListener;
import android.text.style.BackgroundColorSpan;
import android.text.style.CharacterStyle;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;

/**
 * The OpenWnn English IME class.
 *
 * @author Copyright (C) 2009 OMRON SOFTWARE CO., LTD.  All Rights Reserved.
 */
public class OpenWnnEN extends OpenWnn {
	/** A space character */
	private static final char[] SPACE = {' '};
	
	/** Character style of underline */
	private static final CharacterStyle SPAN_UNDERLINE   = new UnderlineSpan();
    /** Highlight color style for the selected string  */
    private static final CharacterStyle SPAN_EXACT_BGCOLOR_HL     = new BackgroundColorSpan(0xFF66CDAA);
    /** Highlight color style for the composing text */
    private static final CharacterStyle SPAN_REMAIN_BGCOLOR_HL    = new BackgroundColorSpan(0xFFF0FFFF);

	/** A private area code(ALT+SHIFT+X) to be ignore (G1 specific). */
	private static final int PRIVATE_AREA_CODE = 61184;
	/** Never move cursor in to the composing text (adapting to IMF's specification change) */
    private static final boolean FIX_CURSOR_TEXT_END = true;

	/** Whether using Emoji or not */
    private static final boolean ENABLE_EMOJI_LIMITATION = true;
	
	/** Spannable string for the composing text */
	protected SpannableStringBuilder mDisplayText;

	/** Handler for drawing the candidates view */
	private Handler mDelayUpdateHandler;
	/** Characters treated as a separator */
	private String mWordSeparators;
	/** Previous event's code */
	private int mPreviousEventCode;

	/** Array of words from the user dictionary */
	private WnnWord[] mUserDictionaryWords = null;

	/** The converter for English prediction/spell correction */
	private OpenWnnEngineEN mConverterEN;
	/** The symbol list generator */
	private SymbolList mSymbolList;
	/** Whether it is displaying symbol list */
	private boolean mSymbolMode;
	/** Whether prediction is enabled */
	private boolean mOptPrediction;
	/** Whether spell correction is enabled */
	private boolean mOptSpellCorrection;
	/** Whether learning is enabled */
	private boolean mOptLearning;
	
	/** SHIFT key state */
	private int mHardShift;
    /** SHIFT key state (pressing) */
	private boolean mShiftPressing;
	/** ALT key state */
	private int mHardAlt;
    /** ALT key state (pressing) */
	private boolean mAltPressing;

	/** Instance of this service */
	private static OpenWnnEN mSelf = null;

	/** Shift lock toggle definition */
	private static final int[] mShiftKeyToggle = {0, MetaKeyKeyListener.META_SHIFT_ON, MetaKeyKeyListener.META_CAP_LOCKED};
	/** Alt lock toggle definition */
	private static final int[] mAltKeyToggle = {0, MetaKeyKeyListener.META_ALT_ON, MetaKeyKeyListener.META_ALT_LOCKED};
	/** Auto caps mode */
	private boolean mAutoCaps = false;
	
	private CandidateFilter mFilter;

    /**
     * Constructor
     */
	public OpenWnnEN() {
		super();
        mSelf = this;

		/* used by OpenWnn */
		mComposingText = new ComposingText();
		mCandidatesViewManager = new TextCandidatesViewManager(-1);
		mInputViewManager = new DefaultSoftKeyboardEN();
		mConverterEN = new OpenWnnEngineEN("/data/data/jp.co.omronsoft.openwnn/writableEN.dic");
		mConverter = mConverterEN;
		mFilter = new CandidateFilter();
		mSymbolList = null;

		/* etc */
		mDisplayText = new SpannableStringBuilder();
		mAutoHideMode = false;
		mDelayUpdateHandler = new Handler();
		mSymbolMode = false;
		mOptPrediction = true;
		mOptSpellCorrection = true;
		mOptLearning = true;
	}

    /**
     * Constructor
     *
     * @param context       The context
     */
    public OpenWnnEN(Context context) {
        this();
        attachBaseContext(context);
    }
	/**
	 * Get the instance of this service.
	 * <br>
	 * Before using this method, the constructor of this service must be invoked.
	 * 
	 * @return		The instance of this object
	 */
	public static OpenWnnEN getInstance() {
		return mSelf;
	}

	/**
	 * Insert a character into the composing text.
	 *
	 * @param chars		A array of character
	 */
	private void insertCharToComposingText(char[] chars) {
		StrSegment seg = new StrSegment(chars);

		if (chars[0] == SPACE[0] || chars[0] == '\u0009') {
			/* if the character is a space, commit the composing text */
			commitText(1);
			commitText(seg.string);
			mComposingText.clear();
		} else if (mWordSeparators.contains(seg.string)) {
			/* if the character is a separator, remove an auto-inserted space and commit the composing text. */
			if (mPreviousEventCode == OpenWnnEvent.SELECT_CANDIDATE) {
				mInputConnection.deleteSurroundingText(1, 0);
			}
			commitText(1);
			commitText(seg.string);
			mComposingText.clear();
		} else {
			mComposingText.insertStrSegment(0, 1, seg);
			updateComposingText(1);
		}
	}

	/**
	 * Insert a character into the composing text.
	 *
	 * @param charCode		A character code
     * @return				{@code true} if success; {@code false} if an error occurs.
	 */
	private boolean insertCharToComposingText(int charCode) {
        if (charCode == 0) {
            return false;
        }
        insertCharToComposingText(Character.toChars(charCode));
        return true;
    }

	/**
	 * Get the shift key state from the editor.
	 *
	 * @param editor	Editor
	 *
	 * @return			State ID of the shift key (0:off, 1:on)
	 */
	protected int getShiftKeyState(EditorInfo editor) {
		return (getCurrentInputConnection().getCursorCapsMode(editor.inputType) == 0) ? 0 : 1;
	}

    /**
     * Set the mode of the symbol list.
     * 
     * @param mode 		{@code SymbolList.SYMBOL_ENGLISH} or {@code null}.
     */
	private void setSymbolMode(String mode) {
		if (mode != null) {
			mDelayUpdateHandler.removeCallbacks(updatePredictionRunnable);
			mSymbolMode = true;
			mSymbolList.setDictionary(mode);
			mConverter = mSymbolList;
		} else {
			if (!mSymbolMode) {
				return;
			}
			mDelayUpdateHandler.removeCallbacks(updatePredictionRunnable);
			mSymbolMode = false;
			mConverter = mConverterEN;
		}
	}

	/***********************************************************************
	 * InputMethodServer
	 ***********************************************************************/
    /** @see jp.co.omronsoft.openwnn.OpenWnn#onCreate */
	@Override public void onCreate() {
		super.onCreate();
		mWordSeparators = getResources().getString(R.string.en_word_separators);

		if (mSymbolList == null) {
			mSymbolList = new SymbolList(this, SymbolList.LANG_EN);
		}
	}
	
    /** @see jp.co.omronsoft.openwnn.OpenWnn#onCreateInputView */
    @Override public View onCreateInputView() {
    	int hiddenState = getResources().getConfiguration().hardKeyboardHidden;
    	boolean hidden = (hiddenState == Configuration.HARDKEYBOARDHIDDEN_YES);
    	((DefaultSoftKeyboardEN) mInputViewManager).setHardKeyboardHidden(hidden);
    	((TextCandidatesViewManager)
    			mCandidatesViewManager).setHardKeyboardHidden(hidden);

        return super.onCreateInputView();
    }

    /** @see jp.co.omronsoft.openwnn.OpenWnn#onStartInputView */
	@Override public void onStartInputView(EditorInfo attribute, boolean restarting) {
		super.onStartInputView(attribute, restarting);

		/* initialize views */
		mCandidatesViewManager.clearCandidates();

		mHardShift = 0;
		mHardAlt   = 0;
        updateMetaKeyStateDisplay();

        /* load preferences */
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);

		/* auto caps mode */
		mAutoCaps = pref.getBoolean("auto_caps", true);

		/* set TextCandidatesViewManager's option */
		((TextCandidatesViewManager)mCandidatesViewManager).setAutoHide(false);

        
		/* display status icon */
 		showStatusIcon(R.drawable.immodeic_half_alphabet);

		/* set prediction & spell correction mode */
		mOptPrediction      = pref.getBoolean("opt_en_prediction", true);
		mOptSpellCorrection = pref.getBoolean("opt_en_spell_correction", true);
		mOptLearning        = pref.getBoolean("opt_en_enable_learning", true);

		/* prediction on/off */
        switch (attribute.inputType & EditorInfo.TYPE_MASK_CLASS) {
        case EditorInfo.TYPE_CLASS_NUMBER:
        case EditorInfo.TYPE_CLASS_DATETIME:
        case EditorInfo.TYPE_CLASS_PHONE:
            mOptPrediction = false;
    		mOptLearning = false;
            break;

        case EditorInfo.TYPE_CLASS_TEXT:
        	switch (attribute.inputType & EditorInfo.TYPE_MASK_VARIATION) {
        	case EditorInfo.TYPE_TEXT_VARIATION_PASSWORD:
            case EditorInfo.TYPE_TEXT_VARIATION_PHONETIC:
            	mOptLearning = false;
        		mOptPrediction = false;
        		break;
        	default:
        		break;
        	}
        }

        /* set engine's mode */
        if (mOptSpellCorrection) {
        	mConverterEN.setDictionary(OpenWnnEngineEN.DICT_FOR_CORRECT_MISTYPE);
        } else {
        	mConverterEN.setDictionary(OpenWnnEngineEN.DICT_DEFAULT);
        }
        /* emoji */
        if (ENABLE_EMOJI_LIMITATION) {
            Bundle bundle = attribute.extras;
            if (bundle != null && bundle.getBoolean("allowEmoji")) {
            	mConverterEN.setFilter(null);
            } else {
            	mFilter.setFilter(CandidateFilter.FILTER_EMOJI);
            	mConverterEN.setFilter(mFilter);
            }
        } else {
        	mConverterEN.setFilter(null);
        }

        /* doesn't learn any word if it is not prediction mode */
        if (!mOptPrediction) {
        	mOptLearning = false;
        }

        if (mComposingText != null) {
        	mComposingText.clear();
        }
        /* initialize the engine's state */
        fitInputType(pref, attribute);
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
	@Override public void onUpdateSelection(int oldSelStart, int oldSelEnd,
			int newSelStart, int newSelEnd, int candidatesStart,
			int candidatesEnd) {
		if (mComposingText.size(1) != 0) {
			updateComposingText(1);
		}
	}

    /** @see jp.co.omronsoft.openwnn.OpenWnn#onConfigurationChanged */
	@Override public void onConfigurationChanged(Configuration newConfig) {
		try {
			super.onConfigurationChanged(newConfig);
			if (mInputConnection != null) {
				updateComposingText(1);
			}
		} catch (Exception ex) {
		}
	}

    /** @see jp.co.omronsoft.openwnn.OpenWnn#onEvaluateFullscreenMode */
	@Override public boolean onEvaluateFullscreenMode() {
		return false;
	}

    /** @see jp.co.omronsoft.openwnn.OpenWnn#onEvaluateInputViewShown */
	@Override public boolean onEvaluateInputViewShown() {
		return true;
	}

	/***********************************************************************
	 * OpenWnn
	 ***********************************************************************/
    /** @see jp.co.omronsoft.openwnn.OpenWnn#onEvent */
	@Override synchronized public boolean onEvent(OpenWnnEvent ev) {
        /* handling events which are valid when InputConnection is not active. */
        switch (ev.code) {
        
        case OpenWnnEvent.KEYUP:
            onKeyUpEvent(ev.keyEvent);
            return true;
            
        case OpenWnnEvent.INITIALIZE_LEARNING_DICTIONARY:
            return mConverterEN.initializeDictionary( WnnEngine.DICTIONARY_TYPE_LEARN );

        case OpenWnnEvent.INITIALIZE_USER_DICTIONARY:
            return mConverterEN.initializeDictionary( WnnEngine.DICTIONARY_TYPE_USER );

        case OpenWnnEvent.LIST_WORDS_IN_USER_DICTIONARY:
            mUserDictionaryWords = mConverterEN.getUserDictionaryWords( );
            return true;

		case OpenWnnEvent.GET_WORD:
			if( mUserDictionaryWords != null ) {
				ev.word = mUserDictionaryWords[ 0 ];
				for( int i = 0 ; i < mUserDictionaryWords.length-1 ; i++ ) {
					mUserDictionaryWords[ i ] = mUserDictionaryWords[ i + 1 ];
				}
				mUserDictionaryWords[ mUserDictionaryWords.length-1 ] = null;
				if( mUserDictionaryWords[ 0 ] == null ) {
					mUserDictionaryWords = null;
				}
				return true;
			}
            break;

		case OpenWnnEvent.ADD_WORD:
			mConverterEN.addWord(ev.word);
			return true;

		case OpenWnnEvent.DELETE_WORD:
			mConverterEN.deleteWord(ev.word);
			return true;

		case OpenWnnEvent.CHANGE_MODE:
            return false;
            
        case OpenWnnEvent.CHANGE_INPUT_VIEW:
        	setInputView(onCreateInputView());
            return true;

        case OpenWnnEvent.CANDIDATE_VIEW_TOUCH:
            boolean ret;
                ret = ((TextCandidatesViewManager)mCandidatesViewManager).onTouchSync();
            return ret;

        default:
            break;
		}

		dismissPopupKeyboard();
        KeyEvent keyEvent = ev.keyEvent;
        int keyCode = 0;
        if (keyEvent != null) {
            keyCode = keyEvent.getKeyCode();
        }
		if (mDirectInputMode) {
            if (ev.code == OpenWnnEvent.INPUT_SOFT_KEY && mInputConnection != null) {
                mInputConnection.sendKeyEvent(keyEvent);
				mInputConnection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP,
                                                           keyEvent.getKeyCode()));
            }
			return false;
		}

		if (ev.code == OpenWnnEvent.LIST_CANDIDATES_FULL) {
			mCandidatesViewManager.setViewType(CandidatesViewManager.VIEW_TYPE_FULL);
			return true;
		}

		boolean ret = false;
		switch (ev.code) {
		case OpenWnnEvent.INPUT_CHAR:
			EditorInfo edit = getCurrentInputEditorInfo();
			if( edit.inputType == EditorInfo.TYPE_CLASS_PHONE){
				commitText(new String(ev.chars));			
			}else{
				setSymbolMode(null);
				insertCharToComposingText(ev.chars);
				ret = true;
				mPreviousEventCode = ev.code;
			}
			break;

		case OpenWnnEvent.INPUT_KEY:
			keyCode = ev.keyEvent.getKeyCode();
			/* update shift/alt state */
			switch (keyCode) {
			case KeyEvent.KEYCODE_ALT_LEFT:
			case KeyEvent.KEYCODE_ALT_RIGHT:
				if (ev.keyEvent.getRepeatCount() == 0) {
					if (++mHardAlt > 2) { mHardAlt = 0; }
				}
                mAltPressing   = true;
				updateMetaKeyStateDisplay();
				return true;

			case KeyEvent.KEYCODE_SHIFT_LEFT:
			case KeyEvent.KEYCODE_SHIFT_RIGHT:
				if (ev.keyEvent.getRepeatCount() == 0) {
					if (++mHardShift > 2) { mHardShift = 0; }
				}
                mShiftPressing = true;
				updateMetaKeyStateDisplay();
				return true;
			}
			setSymbolMode(null);
			updateComposingText(1);
			/* handle other key event */
			ret = processKeyEvent(ev.keyEvent);
			mPreviousEventCode = ev.code;
			break;

		case OpenWnnEvent.INPUT_SOFT_KEY:
			setSymbolMode(null);
			updateComposingText(1);
			ret = processKeyEvent(ev.keyEvent);
			if (!ret) {
				mInputConnection.sendKeyEvent(ev.keyEvent);
				mInputConnection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, ev.keyEvent.getKeyCode()));
                ret = true;
			}
			mPreviousEventCode = ev.code;
			break;

		case OpenWnnEvent.SELECT_CANDIDATE:
            if (mSymbolMode) {
                commitText(ev.word, false);
            } else {
                if (mWordSeparators.contains(ev.word.candidate) && 
                    mPreviousEventCode == OpenWnnEvent.SELECT_CANDIDATE) {
                    mInputConnection.deleteSurroundingText(1, 0);
                }
                commitText(ev.word, true);
            }
            mComposingText.clear();
            mPreviousEventCode = ev.code;
            updateComposingText(1);
            break;

		case OpenWnnEvent.LIST_SYMBOLS:
			commitText(1);
			mComposingText.clear();
			setSymbolMode(SymbolList.SYMBOL_ENGLISH);
			updateComposingText(1);
			break;

		default:
			break;
		}

		if (mCandidatesViewManager.getViewType() == CandidatesViewManager.VIEW_TYPE_FULL) {
			mCandidatesViewManager.setViewType(CandidatesViewManager.VIEW_TYPE_NORMAL);
		}

		return ret;
	}

	/***********************************************************************
	 * OpenWnnEN
	 ***********************************************************************/
	/**
	 * Handling KeyEvent
	 * <br>
	 * This method is called from {@link #onEvent()}.
	 *
	 * @param ev   A key event
	 * @return		{@code true} if the event is processed in this method; {@code false} if the event is not processed in this method
	 */
	private boolean processKeyEvent(KeyEvent ev) {

		int key = ev.getKeyCode();
		EditorInfo edit = getCurrentInputEditorInfo();
		/* keys which produce a glyph */
		if (ev.isPrintingKey()) {
			/* do nothing if the character is not able to display or the character is dead key */
			if ((mHardShift > 0 && mHardAlt > 0) || (ev.isAltPressed() && ev.isShiftPressed())) {
				int charCode = ev.getUnicodeChar(MetaKeyKeyListener.META_SHIFT_ON | MetaKeyKeyListener.META_ALT_ON);
				if (charCode == 0 || (charCode & KeyCharacterMap.COMBINING_ACCENT) != 0 || charCode == PRIVATE_AREA_CODE) {
                    if(mHardShift == 1){
                        mShiftPressing = false;
                    }
                    if(mHardAlt == 1){
                        mAltPressing   = false;
                    }
	            	if(!ev.isAltPressed()){
	            		if (mHardAlt == 1) {
	            			mHardAlt = 0;
	            		}
	            	}
	            	if(!ev.isShiftPressed()){
	            		if (mHardShift == 1) {
	            			mHardShift = 0;
	            		}
	            	}
	            	if(!ev.isShiftPressed() && !ev.isAltPressed()){
	                    updateMetaKeyStateDisplay();
	            	}
					return true;
				}
			}

            /* get the key character */
			if (mHardShift== 0  && mHardAlt == 0) {
                /* no meta key is locked */
				int shift = (mAutoCaps) ? getShiftKeyState(edit) : 0;
				if (shift != mHardShift && (key >= KeyEvent.KEYCODE_A && key <= KeyEvent.KEYCODE_Z)) {
                    /* handling auto caps for a alphabet character */
                    insertCharToComposingText(ev.getUnicodeChar(MetaKeyKeyListener.META_SHIFT_ON));
                } else {
                    insertCharToComposingText(ev.getUnicodeChar());
                }
			} else {
                insertCharToComposingText(ev.getUnicodeChar(mShiftKeyToggle[mHardShift]
                                                            | mAltKeyToggle[mHardAlt]));
                if(mHardShift == 1){
                    mShiftPressing = false;
                }
                if(mHardAlt == 1){
                    mAltPressing   = false;
                }
                /* back to 0 (off) if 1 (on/not locked) */
            	if(!ev.isAltPressed()){
            		if (mHardAlt == 1) {
            			mHardAlt = 0;
            		}
            	}
            	if(!ev.isShiftPressed()){
            		if (mHardShift == 1) {
            			mHardShift = 0;
            		}
            	}
            	if(!ev.isShiftPressed() && !ev.isAltPressed()){
                    updateMetaKeyStateDisplay();
            	}
			}

            if (edit.inputType == EditorInfo.TYPE_CLASS_PHONE) {
				commitText(1);
				mComposingText.clear();
				return true;
			}
			return true;

		} else if (key == KeyEvent.KEYCODE_SPACE) {
			if (ev.isAltPressed()) {
                /* display the symbol list (G1 specific. same as KEYCODE_SYM) */
				commitText(1);
				mComposingText.clear();
				setSymbolMode(SymbolList.SYMBOL_ENGLISH);
				updateComposingText(1);		
				mHardAlt = 0;
                updateMetaKeyStateDisplay();
			} else {
				insertCharToComposingText(SPACE);	
			}
			return true;
        } else if (key == KeyEvent.KEYCODE_SYM) {
            /* display the symbol list */
            commitText(1);
            mComposingText.clear();
            setSymbolMode(SymbolList.SYMBOL_ENGLISH);
            updateComposingText(1);		
            mHardAlt = 0;
            updateMetaKeyStateDisplay();
		} 


		/* Functional key */
		if (mComposingText.size(1) > 0) {
			switch (key) {
			case KeyEvent.KEYCODE_DEL:
				mComposingText.delete(1, false);
				updateComposingText(1);
				return true;

			case KeyEvent.KEYCODE_BACK:
				if (mCandidatesViewManager.getViewType() == CandidatesViewManager.VIEW_TYPE_FULL) {
					mCandidatesViewManager.setViewType(CandidatesViewManager.VIEW_TYPE_NORMAL);
				} else {
					mComposingText.clear();
					updateComposingText(1);
				}
				return true;

			case KeyEvent.KEYCODE_DPAD_LEFT:
				mComposingText.moveCursor(1, -1);
				updateComposingText(1);
				return true;

			case KeyEvent.KEYCODE_DPAD_RIGHT:
				mComposingText.moveCursor(1, 1);
				updateComposingText(1);
				return true;

			case KeyEvent.KEYCODE_ENTER:
			case KeyEvent.KEYCODE_DPAD_CENTER:
				commitText(1);
				mComposingText.clear();
				return true;

			default:
				break;
			}
		}

		return false;
	}

	/**
	 * Runnable for a thread getting and displaying candidates.
	 */
	private final Runnable updatePredictionRunnable = new Runnable() {
		public void run() {
			int candidates = 0;
			if (mConverter != null) {
				/* normal prediction */
				candidates = mConverter.predict(mComposingText, 0, -1);
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
	 * Update the composing text.
	 *
	 * @param layer  {@link mComposingText}'s layer to display
	 */
	private void updateComposingText(int layer) {
		/* update the candidates view */
		if (!mOptPrediction) {
			commitText(1);
			mComposingText.clear();
            if (mSymbolMode) {
				mDelayUpdateHandler.removeCallbacks(updatePredictionRunnable);
				mDelayUpdateHandler.postDelayed(updatePredictionRunnable, 0);         
            }
		} else {
			if (mComposingText.size(1) != 0) {
				mDelayUpdateHandler.removeCallbacks(updatePredictionRunnable);
				mDelayUpdateHandler.postDelayed(updatePredictionRunnable, 250);
			} else {
				mDelayUpdateHandler.removeCallbacks(updatePredictionRunnable);
				mDelayUpdateHandler.postDelayed(updatePredictionRunnable, 0);         
			}

			/* notice to the input view */
			this.mInputViewManager.onUpdateState(this);

			/* set the candidates view to the normal size */
			if (mCandidatesViewManager.getViewType() != CandidatesViewManager.VIEW_TYPE_NORMAL) {
				mCandidatesViewManager.setViewType(CandidatesViewManager.VIEW_TYPE_NORMAL);
			}
			/* set the text for displaying as the composing text */
			SpannableStringBuilder disp = mDisplayText;
			disp.clear();
			disp.insert(0, mComposingText.toString(layer));

			/* add decoration to the text */
			int cursor = mComposingText.getCursor(layer);
			if (disp.length() != 0) {
				if (cursor > 0 && cursor < disp.length()) {
					disp.setSpan(SPAN_EXACT_BGCOLOR_HL, 0, cursor,
							Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
				}
				if (cursor < disp.length()) {
                    mDisplayText.setSpan(SPAN_REMAIN_BGCOLOR_HL, cursor, disp.length(),
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
				}
				
				disp.setSpan(SPAN_UNDERLINE, 0, disp.length(),
						Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
			
            int displayCursor = cursor;
            if (FIX_CURSOR_TEXT_END) {
                displayCursor = (cursor == 0) ?  0 : 1;
            } 
			/* update the composing text on the EditView */
			mInputConnection.setComposingText(disp, displayCursor);
		}
	}

	/**
	 * Commit the composing text.
	 *
	 * @param layer  {@link mComposingText}'s layer to commit.
	 */
	private void commitText(int layer) {
		String tmp = mComposingText.toString(layer);

		if (mOptLearning && mConverter != null && tmp.length() > 0) {
			WnnWord word = new WnnWord(tmp, tmp);
			mConverter.learn(word);
		}

        mInputConnection.commitText(tmp, (FIX_CURSOR_TEXT_END ? 1 : tmp.length()));
		mCandidatesViewManager.clearCandidates();
	}

	/**
	 * Commit a word
	 *
	 * @param word 		 	A word to commit
	 * @param withSpace		Append a space after the word if {@code true}.
	 */
	private void commitText(WnnWord word, boolean withSpace) {

		if (mOptLearning && mConverter != null) {
			mConverter.learn(word);
		}

        mInputConnection.commitText(word.candidate, (FIX_CURSOR_TEXT_END ? 1 : word.candidate.length()));

		if (withSpace) {
			commitText(" ");
		}		
	}

	/**
	 * Commit a string
     * <br>
     * The string is not registered into the learning dictionary.
	 *
	 * @param str  A string to commit
	 */
	private void commitText(String str) {
        mInputConnection.commitText(str, (FIX_CURSOR_TEXT_END ? 1 : str.length()));
		mCandidatesViewManager.clearCandidates();
	}

	/**
	 * Dismiss the pop-up keyboard
	 */
	protected void dismissPopupKeyboard() {
		DefaultSoftKeyboardEN kbd = (DefaultSoftKeyboardEN)mInputViewManager;
		if (kbd != null) {
			kbd.dismissPopupKeyboard();
		}
	}

	/**
	 * Display current meta-key state.
	 */
	private void updateMetaKeyStateDisplay() {
        int mode = 0;
        if(mHardShift == 0 && mHardAlt == 0){
            mode = DefaultSoftKeyboard.HARD_KEYMODE_SHIFT_OFF_ALT_OFF;
        }else if(mHardShift == 1 && mHardAlt == 0){
            mode = DefaultSoftKeyboard.HARD_KEYMODE_SHIFT_ON_ALT_OFF;
        }else if(mHardShift == 2  && mHardAlt == 0){
            mode = DefaultSoftKeyboard.HARD_KEYMODE_SHIFT_LOCK_ALT_OFF;
        }else if(mHardShift == 0 && mHardAlt == 1){
            mode = DefaultSoftKeyboard.HARD_KEYMODE_SHIFT_OFF_ALT_ON;
        }else if(mHardShift == 0 && mHardAlt == 2){
            mode = DefaultSoftKeyboard.HARD_KEYMODE_SHIFT_OFF_ALT_LOCK;
        }else if(mHardShift == 1 && mHardAlt == 1){
            mode = DefaultSoftKeyboard.HARD_KEYMODE_SHIFT_ON_ALT_ON;
        }else if(mHardShift == 1 && mHardAlt == 2){
            mode = DefaultSoftKeyboard.HARD_KEYMODE_SHIFT_ON_ALT_LOCK;
        }else if(mHardShift == 2 && mHardAlt == 1){
            mode = DefaultSoftKeyboard.HARD_KEYMODE_SHIFT_LOCK_ALT_ON;
        }else if(mHardShift == 2 && mHardAlt == 2){
            mode = DefaultSoftKeyboard.HARD_KEYMODE_SHIFT_LOCK_ALT_LOCK;
        }else{
            mode = DefaultSoftKeyboard.HARD_KEYMODE_SHIFT_OFF_ALT_OFF;
        }

        ((DefaultSoftKeyboard) mInputViewManager).updateIndicator(mode);
	}

	/**
	 * Handling KeyEvent(KEYUP)
	 * <br>
	 * This method is called from {@link #onEvent()}.
	 *
	 * @param ev   An up key event
	 */
    private void onKeyUpEvent(KeyEvent ev) {
        int key = ev.getKeyCode();
        if(!mShiftPressing){
            if(key == KeyEvent.KEYCODE_SHIFT_LEFT || key == KeyEvent.KEYCODE_SHIFT_RIGHT){
                mHardShift = 0;
                mShiftPressing = true;
                updateMetaKeyStateDisplay();
            }
        }
        if(!mAltPressing ){
            if(key == KeyEvent.KEYCODE_ALT_LEFT || key == KeyEvent.KEYCODE_ALT_RIGHT){
                mHardAlt = 0;
                mAltPressing   = true;
                updateMetaKeyStateDisplay();
            }
        }
    }
    /**
     * Fits an editor info.
     * 
     * @param preferences  The preference data.
     * @param info  		The editor info.
     */
    private void fitInputType(SharedPreferences preference, EditorInfo info) {
        if (info.inputType == EditorInfo.TYPE_NULL) {
            mDirectInputMode = true;
            return;
        }
    }
}









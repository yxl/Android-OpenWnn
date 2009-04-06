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

package jp.co.omronsoft.openwnn.ZH;

import jp.co.omronsoft.openwnn.*;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.inputmethodservice.Keyboard;
import android.util.Log;
import android.view.View;
import android.content.SharedPreferences;

/**
 * The default Software Keyboard class for Chinese IME.
 *
 * @author Copyright (C) 2009 OMRON SOFTWARE CO., LTD.  All Rights Reserved.
 */
public class DefaultSoftKeyboardZH extends DefaultSoftKeyboard {
    /** Enable English word prediction on half-width alphabet mode */
    private static final boolean USE_ENGLISH_PREDICT = true;

    /** Input mode toggle cycle table */
    private static final int[] CN_MODE_CYCLE_TABLE = {
        KEYMODE_CN_PINYIN, KEYMODE_CN_ALPHABET, KEYMODE_CN_HALF_NUMBER
    };

    /** Definition for {@code mInputType} (toggle) */
    private static final int INPUT_TYPE_TOGGLE = 1;
    /** Definition for {@code mInputType} (commit instantly) */
    private static final int INPUT_TYPE_INSTANT = 2;

    /** The constant for mFixedKeyMode. It means that input mode is not fixed. */
    private static final int INVALID_KEYMODE = -1;    

    /** Input mode that is not able to be changed. If ENABLE_CHANGE_KEYMODE is set, input mode can change. */
    private int mFixedKeyMode = INVALID_KEYMODE;

    /** Input mode that is given the first priority. If ENABLE_CHANGE_KEYMODE is set, input mode can change. */
    private int mPreferenceKeyMode = INVALID_KEYMODE;
    
    /** Definition of propagation keycodes */
    private static final int EM_DASH = 8212;
    private static final int THREE_DOT_LEADER = 8230;

    /** The last input type */
    private int mLastInputType = 0;

    /** Auto caps mode */
    private boolean mEnableAutoCaps = true;

    /**
     * Default constructor
     */
    public DefaultSoftKeyboardZH() {
        mCurrentLanguage     = LANG_CN;
        mCurrentKeyboardType = KEYBOARD_QWERTY;
        mShiftOn             = KEYBOARD_SHIFT_OFF;
        mCurrentKeyMode      = KEYMODE_CN_PINYIN;
    }
	

    /** @see jp.co.omronsoft.openwnn.DefaultSoftKeyboard#createKeyboards */
    @Override protected void createKeyboards(OpenWnn parent) {
        mKeyboard = new Keyboard[3][2][4][2][7][2];

        if (mHardKeyboardHidden) {
        	/* Create the suitable keyboard object */
        	if (mDisplayMode == DefaultSoftKeyboard.PORTRAIT) {
        		createKeyboardsPortrait(parent);
        	} else {
        	
        	}
        }
        mWnn.onEvent(new OpenWnnEvent(OpenWnnEvent.CHANGE_MODE,
        		OpenWnnJAJP.ENGINE_MODE_OPT_TYPE_QWERTY));
    }
    /**
     * Commit the pre-edit string for committing operation that is not explicit
     * (ex. when a candidate is selected)
     */
    private void commitText() {
        if (!mNoInput) {
            mWnn.onEvent(new OpenWnnEvent(OpenWnnEvent.COMMIT_COMPOSING_TEXT));
        }
    }

    /**
     * Change input mode
     * <br>
     * @param keyMode   The type of input mode
     */
    public void changeKeyMode(int keyMode) {
    	int targetMode = keyMode;
    	commitText();

    	if (mCapsLock) {
    		mWnn.onEvent(new OpenWnnEvent(OpenWnnEvent.INPUT_SOFT_KEY,
                                          new KeyEvent(KeyEvent.ACTION_UP,
                                                       KeyEvent.KEYCODE_SHIFT_LEFT)));
    		mCapsLock = false;
    	}
    	mShiftOn = KEYBOARD_SHIFT_OFF;

    	if (mFixedKeyMode != INVALID_KEYMODE) {
    		targetMode = mFixedKeyMode;
    	}

        if (!mHardKeyboardHidden) {
        	if (targetMode == KEYMODE_CN_HALF_NUMBER) {
        		targetMode = KEYMODE_CN_ALPHABET;
        	} else if (targetMode == KEYMODE_CN_FULL_NUMBER) {
        		targetMode = KEYMODE_CN_PINYIN;
        	}
        }
        Keyboard kbd = getModeChangeKeyboard(targetMode);
        mCurrentKeyMode = targetMode;
        
        int mode = OpenWnnEvent.Mode.DIRECT;
        
        switch (targetMode) {
        case KEYMODE_CN_PINYIN:
            mode = OpenWnnEvent.Mode.DEFAULT;
            break;
            
        case KEYMODE_CN_ALPHABET:
            if (USE_ENGLISH_PREDICT) {
                mode = OpenWnnEvent.Mode.NO_LV1_CONV;
            } else {
                mode = OpenWnnEvent.Mode.DIRECT;
            }
            break;
            
        case KEYMODE_CN_FULL_NUMBER:
            mode = OpenWnnEvent.Mode.DIRECT;
            break;
            
        case KEYMODE_CN_HALF_NUMBER:
            mode = OpenWnnEvent.Mode.DIRECT;
            break;
            
        default:
            break;
        }
        
        setStatusIcon();
        changeKeyboard(kbd);
        mWnn.onEvent(new OpenWnnEvent(OpenWnnEvent.CHANGE_MODE, mode));
    }

    /** @see jp.co.omronsoft.openwnn.DefaultSoftKeyboard#initView */
    @Override public View initView(OpenWnn parent, int width, int height) {
    	View view = super.initView(parent, width, height);
    	changeKeyboard(mKeyboard[mCurrentLanguage][mDisplayMode][mCurrentKeyboardType][mShiftOn][mCurrentKeyMode][0]);
    	return view;
    }

    /** @see jp.co.omronsoft.openwnn.DefaultSoftKeyboard#changeKeyboardType */
    @Override public void changeKeyboardType(int type) {
        commitText();
        super.changeKeyboardType(type);
    }

    /** @see jp.co.omronsoft.openwnn.DefaultSoftKeyboard#onKey */
    @Override public void onKey(int primaryCode, int[] keyCodes) {

        if (mDisableKeyInput) {
            return;
        }

        switch (primaryCode) {
        case KEYCODE_QWERTY_TOGGLE_MODE:
            nextKeyMode();
            break;

        case KEYCODE_QWERTY_BACKSPACE:
        case KEYCODE_JP12_BACKSPACE:
            mWnn.onEvent(new OpenWnnEvent(OpenWnnEvent.INPUT_SOFT_KEY,
                                          new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL)));
            break;

        case KEYCODE_QWERTY_SHIFT:
            toggleShiftLock();
            break;

        case KEYCODE_QWERTY_ALT:
            processAltKey();
            break;

        case KEYCODE_QWERTY_ENTER:
        case KEYCODE_JP12_ENTER:
            mWnn.onEvent(new OpenWnnEvent(OpenWnnEvent.INPUT_SOFT_KEY,
                                          new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER)));
            break;

        case KEYCODE_QWERTY_EMOJI:
        	commitText();
        	mWnn.onEvent(new OpenWnnEvent(OpenWnnEvent.CHANGE_MODE, OpenWnnZHCN.ENGINE_MODE_SYMBOL));
            break;

        case KEYCODE_QWERTY_HAN_ALPHA:
            this.changeKeyMode(KEYMODE_CN_ALPHABET);
            break;

        case KEYCODE_QWERTY_HAN_NUM:
            this.changeKeyMode(KEYMODE_CN_HALF_NUMBER);
            break;

        case KEYCODE_QWERTY_PINYIN:
            this.changeKeyMode(KEYMODE_CN_PINYIN);
            break;

        case KEYCODE_QWERTY_ZEN_NUM:
            this.changeKeyMode(KEYMODE_CN_FULL_NUMBER);
            break;


        case KEYCODE_JP12_LEFT:
            mWnn.onEvent(new OpenWnnEvent(OpenWnnEvent.INPUT_SOFT_KEY,
                                          new KeyEvent(KeyEvent.ACTION_DOWN,
                                                       KeyEvent.KEYCODE_DPAD_LEFT)));
            break;

        case KEYCODE_JP12_RIGHT:
            mWnn.onEvent(new OpenWnnEvent(OpenWnnEvent.INPUT_SOFT_KEY,
                                          new KeyEvent(KeyEvent.ACTION_DOWN,
                                                       KeyEvent.KEYCODE_DPAD_RIGHT)));
            break;
            
        case EM_DASH:
        case THREE_DOT_LEADER:
            mWnn.onEvent(new OpenWnnEvent(OpenWnnEvent.INPUT_CHAR, (char)primaryCode));
            mWnn.onEvent(new OpenWnnEvent(OpenWnnEvent.INPUT_CHAR, (char)primaryCode));
            break;
            
        default:
            if (primaryCode >= 0) {
                if (mKeyboardView.isShifted()) {
                    primaryCode = Character.toUpperCase(primaryCode);
                }
                mWnn.onEvent(new OpenWnnEvent(OpenWnnEvent.INPUT_CHAR, (char)primaryCode));
            }
            break;
        }

        if (!mCapsLock && (primaryCode != DefaultSoftKeyboard.KEYCODE_QWERTY_SHIFT)) {
            setShiftByEditorInfo();
        }
    }

    /** @see jp.co.omronsoft.openwnn.DefaultSoftKeyboard#setPreferences */
    @Override public void setPreferences(SharedPreferences pref, EditorInfo editor) {
        super.setPreferences(pref, editor);

        int inputType = editor.inputType;
        if (inputType == EditorInfo.TYPE_NULL) {
            return;
        }

        mEnableAutoCaps = pref.getBoolean("auto_caps", true);
        mFixedKeyMode = INVALID_KEYMODE;
        mPreferenceKeyMode = INVALID_KEYMODE;
        mNoInput = true;
        mDisableKeyInput = false;

        switch (inputType & EditorInfo.TYPE_MASK_CLASS) {

        case EditorInfo.TYPE_CLASS_NUMBER:
        case EditorInfo.TYPE_CLASS_DATETIME:
            mPreferenceKeyMode = KEYMODE_CN_HALF_NUMBER;
            break;

        case EditorInfo.TYPE_CLASS_PHONE:
            mFixedKeyMode = KEYMODE_CN_PHONE;
            break;

        case EditorInfo.TYPE_CLASS_TEXT:
            switch (inputType & EditorInfo.TYPE_MASK_VARIATION) {

            case EditorInfo.TYPE_TEXT_VARIATION_PASSWORD:
                mPreferenceKeyMode = KEYMODE_CN_ALPHABET;
                break;

            case EditorInfo.TYPE_TEXT_VARIATION_EMAIL_ADDRESS:
            case EditorInfo.TYPE_TEXT_VARIATION_URI:
                mPreferenceKeyMode = KEYMODE_CN_ALPHABET;
                break;

            default:
                break;
            }
            break;

        default:
            break;
        }

        if (inputType != mLastInputType) {
            setDefaultKeyboard();
            mLastInputType = inputType;
        }

        setShiftByEditorInfo();
    }

    /** @see jp.co.omronsoft.openwnn.DefaultSoftKeyboard#onUpdateState */
    @Override public void onUpdateState(OpenWnn parent) {
        super.onUpdateState(parent);
        if (!mCapsLock) {
            setShiftByEditorInfo();
        }
    }

    /**
     * Change the keyboard to default
     */
    public void setDefaultKeyboard() {
        int keymode = KEYMODE_CN_PINYIN;
        if (mPreferenceKeyMode != INVALID_KEYMODE) {
            keymode = mPreferenceKeyMode;
        }
        changeKeyMode(keymode);
    } 

    /**
     * Change to the next input mode
     */
    private void nextKeyMode() {
        boolean found = false;
        int index;
        for (index = 0; index < CN_MODE_CYCLE_TABLE.length; index++) {
            if (CN_MODE_CYCLE_TABLE[index] == mCurrentKeyMode) {
                found = true;
                break;
            }
        }

        if (!found) {
            setDefaultKeyboard();
        } else {
            index++;
            if (CN_MODE_CYCLE_TABLE.length <= index) {
                index = 0;
            }

            changeKeyMode(CN_MODE_CYCLE_TABLE[index]);
        }
    }

    /**
     * Create the keyboard for portrait mode
     * <br>
     * @param parent  The context
     */
    private void createKeyboardsPortrait(OpenWnn parent) {       
        Keyboard[][] keyList;
        /***********************************************************************
         * Chinese
         ***********************************************************************/
        /* qwerty shift_off */
        keyList = mKeyboard[LANG_CN][PORTRAIT][KEYBOARD_QWERTY][KEYBOARD_SHIFT_OFF];
        keyList[KEYMODE_CN_ALPHABET][0] = new Keyboard(parent, R.xml.default_cn_qwerty);
        keyList[KEYMODE_CN_HALF_NUMBER][0] = new Keyboard(parent, R.xml.default_cn_half_symbols);
        keyList[KEYMODE_CN_PHONE][0] = new Keyboard(parent, R.xml.keyboard_12key_phone);
        keyList[KEYMODE_CN_PINYIN][0] = new Keyboard(parent, R.xml.default_cn_qwerty_pinyin);
        keyList[KEYMODE_CN_FULL_NUMBER][0] = new Keyboard(parent, R.xml.default_cn_full_symbols);

        /* qwerty shift_on */
        keyList = mKeyboard[LANG_CN][PORTRAIT][KEYBOARD_QWERTY][KEYBOARD_SHIFT_ON];
        keyList[KEYMODE_CN_ALPHABET][0] = 
        	mKeyboard[LANG_CN][PORTRAIT][KEYBOARD_QWERTY][KEYBOARD_SHIFT_OFF][KEYMODE_CN_ALPHABET][0];
        keyList[KEYMODE_CN_HALF_NUMBER][0] = new Keyboard(parent, R.xml.default_cn_half_symbols_shift);
        keyList[KEYMODE_CN_PHONE][0] = new Keyboard(parent, R.xml.keyboard_12key_phone);
        keyList[KEYMODE_CN_PINYIN][0] = new Keyboard(parent, R.xml.default_cn_qwerty_pinyin_shift);
        keyList[KEYMODE_CN_FULL_NUMBER][0] = new Keyboard(parent, R.xml.default_cn_full_symbols_shift);
    }


    /**
     * Set the status icon that is appropriate in current mode
     */
    protected void setStatusIcon() {
        int icon = 0;

        switch (mCurrentKeyMode) {
        case KEYMODE_CN_PINYIN:
            icon = R.drawable.immodeic_chinese;
            break;
        case KEYMODE_CN_FULL_NUMBER:
            icon = R.drawable.immodeic_full_number;
            break;
        case KEYMODE_CN_ALPHABET:
            icon = R.drawable.immodeic_half_alphabet;
            break;
        case KEYMODE_CN_HALF_NUMBER:
        case KEYMODE_CN_PHONE:
            icon = R.drawable.immodeic_half_number;
            break;
        default:
        	break;
        }

        mWnn.showStatusIcon(icon);
    }

    /**
     * Get the shift key state from the editor.
     * <br>
     * @param editor	The editor information
     * @return			The state id of the shift key (0:off, 1:on)
     */
    protected int getShiftKeyState(EditorInfo editor) {
        int caps = mWnn.getCurrentInputConnection().getCursorCapsMode(editor.inputType);
        return (caps == 0) ? 0 : 1;
    }

    /**
     * Set the shift key state from {@link EditorInfo}.
     */
    private void setShiftByEditorInfo() {
        if (mEnableAutoCaps && (mCurrentKeyMode == KEYMODE_CN_ALPHABET)) {
            int shift = getShiftKeyState(mWnn.getCurrentInputEditorInfo());
            
            mShiftOn = shift;
            changeKeyboard(getShiftChangeKeyboard(shift));
        }
    }

}


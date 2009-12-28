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

import android.inputmethodservice.InputMethodService;
import android.view.WindowManager;
import android.content.Context;
import android.view.View;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import android.util.Log;
import android.os.*;
import android.view.inputmethod.*;
import android.content.res.Configuration;
import android.graphics.*;
import android.graphics.drawable.*;

/**
 * The OpenWnn IME's base class.
 *
 * @author Copyright (C) 2009 OMRON SOFTWARE CO., LTD.  All Rights Reserved.
 */
public class OpenWnn extends InputMethodService {

    /** Candidate view */
    protected CandidatesViewManager  mCandidatesViewManager = null;
    /** Input view (software keyboard) */
    protected InputViewManager  mInputViewManager = null;
    /** Conversion engine */
    protected WnnEngine  mConverter = null;
    /** Pre-converter (for Romaji-to-Kana input, Hangul input, etc.) */
    protected LetterConverter  mPreConverter = null;
    /** The inputing/editing string */
    protected ComposingText  mComposingText = null;
    /** The input connection */
    protected InputConnection mInputConnection = null;
    /** Auto hide candidate view */
    protected boolean mAutoHideMode = true;
    /** Direct input mode */
    protected boolean mDirectInputMode = true;
     
    /** Flag for checking if the previous down key event is consumed by OpenWnn  */
    private boolean mConsumeDownEvent;

    /**
     * Constructor
     */
    public OpenWnn() {
        super();
    }

    /***********************************************************************
     * InputMethodService 
     **********************************************************************/
    /** @see android.inputmethodservice.InputMethodService#onCreate */
    @Override public void onCreate() {
        super.onCreate();

        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);


        if (mConverter != null) { mConverter.init(); }
        if (mComposingText != null) { mComposingText.clear(); }
    }

    /** @see android.inputmethodservice.InputMethodService#onCreateCandidatesView */
    @Override public View onCreateCandidatesView() {
        if (mCandidatesViewManager != null) {
            WindowManager wm = (WindowManager)getSystemService(Context.WINDOW_SERVICE);
            View view = mCandidatesViewManager.initView(this,
                                                        wm.getDefaultDisplay().getWidth(),
                                                        wm.getDefaultDisplay().getHeight());
            mCandidatesViewManager.setViewType(CandidatesViewManager.VIEW_TYPE_NORMAL);
            return view;
        } else {
            return super.onCreateCandidatesView();
        }
    }

    /** @see android.inputmethodservice.InputMethodService#onCreateInputView */
    @Override public View onCreateInputView() {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);


        if (mInputViewManager != null) {
            WindowManager wm = (WindowManager)getSystemService(Context.WINDOW_SERVICE);
            return mInputViewManager.initView(this,
                                              wm.getDefaultDisplay().getWidth(),
                                              wm.getDefaultDisplay().getHeight());
        } else {
            return super.onCreateInputView();
        }
    }

    /** @see android.inputmethodservice.InputMethodService#onDestroy */
    @Override public void onDestroy() {
        super.onDestroy();

        close();
    }

    /** @see android.inputmethodservice.InputMethodService#onKeyDown */
    @Override public boolean onKeyDown(int keyCode, KeyEvent event) {
        mConsumeDownEvent = onEvent(new OpenWnnEvent(event));
        if (!mConsumeDownEvent) {
            return super.onKeyDown(keyCode, event);
        }
        return mConsumeDownEvent;
    }

    /** @see android.inputmethodservice.InputMethodService#onKeyUp */
    @Override public boolean onKeyUp(int keyCode, KeyEvent event) {
        boolean ret = mConsumeDownEvent;
        if (!ret) {
            ret = super.onKeyUp(keyCode, event);
        }else{
            onEvent(new OpenWnnEvent(event));
        }
        return ret;
    }
        
    /** @see android.inputmethodservice.InputMethodService#onStartInput */
    @Override public void onStartInput(EditorInfo attribute, boolean restarting) {
        super.onStartInput(attribute, restarting);
        mInputConnection = getCurrentInputConnection();
        if (!restarting && mComposingText != null) {
            mComposingText.clear();
        }
    }

    /** @see android.inputmethodservice.InputMethodService#onStartInputView */
    @Override public void onStartInputView(EditorInfo attribute, boolean restarting) {
        super.onStartInputView(attribute, restarting);
        mInputConnection = getCurrentInputConnection();

        setCandidatesViewShown(false);
        if (mInputConnection != null) {
            mDirectInputMode = false;
            if (mConverter != null) { mConverter.init(); }
        } else {
            mDirectInputMode = true;
        }
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        if (mCandidatesViewManager != null) { mCandidatesViewManager.setPreferences(pref);  }
        if (mInputViewManager != null) { mInputViewManager.setPreferences(pref, attribute);  }
        if (mPreConverter != null) { mPreConverter.setPreferences(pref);  }
        if (mConverter != null) { mConverter.setPreferences(pref);  }
    }

    /** @see android.inputmethodservice.InputMethodService#requestHideSelf */
    @Override public void requestHideSelf(int flag) {
        super.requestHideSelf(flag);
        if (mInputViewManager == null) {
            hideWindow();
        }
    }

    /** @see android.inputmethodservice.InputMethodService#setCandidatesViewShown */
    @Override public void setCandidatesViewShown(boolean shown) {
        super.setCandidatesViewShown(shown);
        if (shown) {
            showWindow(true);
        } else {
            if (mAutoHideMode && mInputViewManager == null) {
                hideWindow();
            }
        }
    }

    /** @see android.inputmethodservice.InputMethodService#hideWindow */
    @Override public void hideWindow() {
        super.hideWindow();
        mDirectInputMode = true;
        hideStatusIcon();
    }
    /** @see android.inputmethodservice.InputMethodService#onComputeInsets */
    @Override public void onComputeInsets(InputMethodService.Insets outInsets) {
        super.onComputeInsets(outInsets);
        outInsets.contentTopInsets = outInsets.visibleTopInsets;
    }


    /**********************************************************************
     * OpenWnn
     **********************************************************************/
    /**
     * Process an event.
     *
     * @param  ev  An event
     * @return  {@code true} if the event is processed in this method; {@code false} if not.
     */
    public boolean onEvent(OpenWnnEvent ev) {
        return false;
    }

    /**
     * Search a character for toggle input.
     *
     * @param prevChar     The character input previous
     * @param toggleTable  Toggle table
     * @param reverse      {@code false} if toggle direction is forward, {@code true} if toggle direction is backward
     * @return          A character ({@code null} if no character is found)
     */
    protected String searchToggleCharacter(String prevChar, String[] toggleTable, boolean reverse) {
        for (int i = 0; i < toggleTable.length; i++) {
            if (prevChar.equals(toggleTable[i])) {
                if (reverse) {
                    i--;
                    if (i < 0) {
                        return toggleTable[toggleTable.length - 1];
                    } else {
                        return toggleTable[i];
                    }
                } else {
                    i++;
                    if (i == toggleTable.length) {
                        return toggleTable[0];
                    } else {
                        return toggleTable[i];
                    }
                }
            }
        }
        return null;
    }

    /**
     * Processing of resource open when IME ends.
     */
    protected void close() {
        if (mConverter != null) { mConverter.close(); }
    }
}

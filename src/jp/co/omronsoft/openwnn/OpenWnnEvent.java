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

import android.view.KeyEvent;
import java.util.*;

/**
 * Definition of event message used by OpenWnn framework
 *
 * @author Copyright (C) 2009 OMRON SOFTWARE CO., LTD.  All Rights Reserved.
 */
public class OpenWnnEvent {
    /** Offset value for private events */
    public static final int PRIVATE_EVENT_OFFSET = 0xFF000000;

    /** Undefined */
    public static final int UNDEFINED = 0;

    /**
     * Reverse key.
     * <br>
     * This is used for multi-tap keyboard like 12-key.
     */
    public static final int TOGGLE_REVERSE_CHAR = 0xF0000001;

    /**
     * Convert.
     * <br>
     * This event makes <code>OpenWnn</code> to display conversion candidates from <code>ComposingText</code>.
     */
    public static final int CONVERT = 0xF0000002;

    /**
     * Predict.
     * <br>
     * This event makes <code>OpenWnn</code> to display prediction candidates from <code>ComposingText</code>.
     */
    public static final int PREDICT = 0xF0000008;

    /**
     * List candidates (normal view).
     * <br>
     * This event changes the candidates view's size
     */
    public static final int LIST_CANDIDATES_NORMAL = 0xF0000003;

    /**
     * List candidates (wide view).
     * <br>
     * This event changes the candidates view's size
     */
    public static final int LIST_CANDIDATES_FULL = 0xF0000004;

    /**
     * Close view
     */
    public static final int CLOSE_VIEW = 0xF0000005;

    /**
     * Insert character(s).
     * <br>
     * This event input specified character(s)(<code>chars</code>) into the cursor position.
     */
    public static final int INPUT_CHAR = 0xF0000006;

    /**
     * Toggle a character.
     * <br>
     * This event changes a character at cursor position with specified rule(<code>toggleMap</code>).
     * This is used for multi-tap keyboard.
     */
    public static final int TOGGLE_CHAR = 0xF000000C;

    /**
     * Replace a character at the cursor.
     */
    public static final int REPLACE_CHAR = 0xF000000D;

    /**
     * Input key.
     * <br>
     * This event processes a <code>keyEvent</code>.
     */
    public static final int INPUT_KEY  = 0xF0000007;

    /**
     * Input Soft key.
     * <br>
     * This event processes a <code>keyEvent</code>.
     * If the event is not processed in <code>OpenWnn</code>, the event is thrown to the IME's client.
     */
    public static final int INPUT_SOFT_KEY  = 0xF000000E;

    /**
     * Focus to the candidates view.
     */
    public static final int FOCUS_TO_CANDIDATE_VIEW  = 0xF0000009;

    /**
     * Focus out from the candidates view.
     */
    public static final int FOCUS_OUT_CANDIDATE_VIEW  = 0xF000000A;

    /**
     * Select a candidate
     */
    public static final int SELECT_CANDIDATE  = 0xF000000B;

    /**
     * Change Mode
     */
    public static final int CHANGE_MODE  = 0xF000000F;

    /**
     * Constant definition class of engine's mode
     */
    public static final class Mode {
        /** Default (use both of the letterConverter and the WnnEngine) */
        public static final int DEFAULT      = 0;
        /** Direct input (not use the letterConverter and the WnnEngine) */
        public static final int DIRECT       = 1;
        /** Do not use the letterConverter */
        public static final int NO_LV1_CONV  = 2;
        /** Do not use the WnnEngine */
        public static final int NO_LV2_CONV  = 3;
    }

    /**
     * Commit the composing text
     */
    public static final int COMMIT_COMPOSING_TEXT  = 0xF0000010;

    /**
     * List symbols
     */
    public static final int LIST_SYMBOLS  = 0xF0000011;

    /**
     * Switch Language
     */
    public static final int SWITCH_LANGUAGE  = 0xF0000012;

    /**
     * Initialize the user dictionary.
     */
    public static final int INITIALIZE_USER_DICTIONARY = 0xF0000013;

    /**
     * Initialize the learning dictionary.
     */
    public static final int INITIALIZE_LEARNING_DICTIONARY = 0xF0000014;

    /**
     * List words in the user dictionary.
     * <br>
     * To get words from the list, use <code>GET_WORD</code> event.
     */
    public static final int LIST_WORDS_IN_USER_DICTIONARY = 0xF0000015;

    /**
     * Get a word from the user dictionary.
     * <br>
     * Get a word from top of the list made by <code>LIST_WORDS_IN_USER_DICTIONARY</code>.
     */
    public static final int GET_WORD  = 0xF0000018;

    /**
     * Add word to the user dictionary.
     */
    public static final int ADD_WORD     = 0xF0000016;

    /**
     * Delete a word from the dictionary.
     */
    public static final int DELETE_WORD  = 0xF0000017;

    /**
     * Update the candidate view
     */
    public static final int UPDATE_CANDIDATE = 0xF0000019; 


    /** Event code */
    public int code = UNDEFINED;
    /** Detail mode of the event */
    public int mode = 0;
    /** Type of dictionary */
    public int dictionaryType = 0;
    /** Input character(s) */
    public char[] chars = null;
    /** Key event */
    public KeyEvent keyEvent = null;
    /** Mapping table for toggle input */
    public String[]  toggleTable = null;
    /** Mapping table for toggle input */
    public HashMap replaceTable = null;
    /** Word's information */
    public WnnWord  word = null;
    /** Error code */ 
    public int errorCode;
    
    /**
     * Generate OpenWnnEvent
     *
     * @param code      the code
     */
    public OpenWnnEvent(int code) {
        this.code = code;
    }
    /**
     * Generate OpenWnnEvent for changing the mode
     *
     * @param code      the code
     * @param mode      the mode
     */
    public OpenWnnEvent(int code, int mode) {
        this.code = code;       
        this.mode = mode;
    }
    /**
     * Generate OpenWnnEvent for a inputing character
     *
     * @param code      the code
     * @param c         the inputing character
     */
    public OpenWnnEvent(int code, char c) {
        this.code = code;       
        this.chars = new char[1];
        this.chars[0] = c;
     }
    /**
     * Generate OpenWnnEvent for inputing characters
     *
     * @param code      the code
     * @param c         the array of inputing character
     */
    public OpenWnnEvent(int code, char c[]) {
        this.code = code;       
        this.chars = c;
    }
    /**
     * Generate OpenWnnEvent for toggle inputing a character
     *
     * @param code          the code
     * @param toggleTable   the array of toggle inputing a character
     */
    public OpenWnnEvent(int code, String[] toggleTable) {
        this.code = code;
        this.toggleTable = toggleTable;
    }
    /**
     * Generate OpenWnnEvent for replacing a character
     *
     * @param code          the code
     * @param replaceTable  the replace table
     */
    public OpenWnnEvent(int code, HashMap replaceTable) {
        this.code = code;
        this.replaceTable = replaceTable;
    }
    /**
     * Generate OpenWnnEvent from KeyEvent
     * <br>
     * same as <code>OpenWnnEvent(INPUT_KEY, ev)</code>.
     *
     * @param ev    the key event
     */
    public OpenWnnEvent(KeyEvent ev) {
        this.code = INPUT_KEY;
        this.keyEvent = ev;
    }
    /**
     * Generate OpenWnnEvent from KeyEvent
     *
     * @param code      the code
     * @param ev        the key event
     */
    public OpenWnnEvent(int code, KeyEvent ev) {
        this.code = code;
        this.keyEvent = ev;
    }
    /**
     * Generate OpenWnnEvent for selecting a candidate
     *
     * @param code      the code
     * @param word      the selected candidate
     */
    public OpenWnnEvent(int code, WnnWord word) {
        this.code = code;       
        this.word = word;
    }

    /**
     * Generate OpenWnnEvent for dictionary management
     *
     * @param code      the code
     * @param dict      the type of dictionary
     * @param word      the selected candidate
     */
    public OpenWnnEvent(int code, int dict, WnnWord word) {
        this.code = code;
        this.dictionaryType = dict;
        this.word = word;
    }
}

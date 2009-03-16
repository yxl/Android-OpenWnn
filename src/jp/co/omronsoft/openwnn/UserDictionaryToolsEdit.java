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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

/**
 * User dictionary's word editor abstract class.
 *
 * @author Copyright (C) 2009, OMRON SOFTWARE CO., LTD.  All Rights Reserved.
 */
public abstract class UserDictionaryToolsEdit extends Activity implements View.OnClickListener {
    /** The class information for intent(Set this informations in the extend class) */
    protected String  mListViewName;
    /** The class information for intent(Set this informations in the extend class) */
    protected String  mPackageName;

    /** The operation mode (Unknown) */
    private static final int STATE_UNKNOWN = 0;
    /** The operation mode (Add the word) */
    private static final int STATE_INSERT = 1;
    /** The operation mode (Edit the word) */
    private static final int STATE_EDIT = 2;

    /** Maximum length of a word's string */
    private static final int MAX_TEXT_SIZE = 20;

    /** The error code (Already registered the same word) */
    private static final int RETURN_SAME_WORD = -11;
    /** The error code (Insufficient the free space of the user dictionary) */
    private static final int RETURN_USER_DICTIONARY_FULL = -12;

    /** The focus view and pair view */
    private static View sFocusingView = null;
    private static View sFocusingPairView = null;

    /** Widgets which constitute this screen of activity */
    private EditText mReadEditText;
    private EditText mCandidateEditText;
    private Button mEntryButton;

    /** The word information which contains the previous information */
    private WnnWord mBeforeEditWnnWord;
    /** The instance of word list activity */
    private UserDictionaryToolsList mListInstance;

    /** The constant for notifying dialog (Already exists the specified word) */
    private static final int DIALOG_CONTROL_WORDS_DUPLICATE = 0;
    /** The constant for notifying dialog (The length of specified stroke or candidate exceeds the limit) */
    private static final int DIALOG_CONTROL_OVER_MAX_TEXT_SIZE = 1;

    /** The operation mode of this activity */
    private int mRequestState;

    /**
     * Constructor
     */
    public UserDictionaryToolsEdit() {
        super();
    }

    /**
     * Constructor
     *
     * @param  focusView      The information of view
     * @param  focusPairView  The information of pair of view
     * @param  wordsCount     The count of registered words
     */
    public UserDictionaryToolsEdit(View focusView, View focusPairView, int wordsCount) {
        super();
        sFocusingView = focusView;
        sFocusingPairView = focusPairView;
    }
    
    /**
     * Send the specified event to IME
     *
     * @param ev    The event object
     * @return      <code>true</code> if this event is processed
     */
    protected abstract boolean sendEventToIME(OpenWnnEvent ev);

    /**
     * Create the screen of editing word
     *
     * @param  savedInstanceState      The instance
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {


        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.user_dictionary_tools_edit);

        findViewById(R.id.addButton).setOnClickListener(this);
        findViewById(R.id.cancelButton).setOnClickListener(this);

        mEntryButton = (Button)findViewById(R.id.addButton);

        mRequestState = STATE_UNKNOWN;
        Intent intent = getIntent();
        String action = intent.getAction();
        mReadEditText = (EditText)findViewById(R.id.editRead);
        mCandidateEditText = (EditText)findViewById(R.id.editCandidate);
        mReadEditText.setSingleLine();
        mCandidateEditText.setSingleLine();
        if (action.equals(Intent.ACTION_INSERT)) {
            mEntryButton.setEnabled(false);
            mRequestState = STATE_INSERT;
        } else if (action.equals(Intent.ACTION_EDIT)) {
            mBeforeEditWnnWord = new WnnWord();
            mEntryButton.setEnabled(true);
            mReadEditText.setText(((TextView)sFocusingView).getText());
            mBeforeEditWnnWord.stroke = ((TextView)sFocusingView).getText().toString();
            mCandidateEditText.setText(((TextView)sFocusingPairView).getText());
            mBeforeEditWnnWord.candidate = ((TextView)sFocusingPairView).getText().toString();
            mRequestState = STATE_EDIT;
        } else {
            Log.e("OpenWnn", "onCreate() : Invaled Get Intent. ID=" + intent);
            finish();
            return;
        }

        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE,
                                  R.layout.user_dictionary_tools_edit_header);

        setAddButtonControl();

    }

    /**
     * Process the event when some key is pressed
     *
     * @param  keyCode      The key code
     * @param  event        The event object
     * @return              always <code>False</code>
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {


        if (keyCode == event.KEYCODE_BACK) {
            screenTransition();
        }
        return false;
    }

    /**
     * Change the state of the "Add" button into the depending state of input area
     */
    public void setAddButtonControl() {


        mReadEditText.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
            }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            	if ((mReadEditText.getText().toString().length() != 0) && 
                    (mCandidateEditText.getText().toString().length() != 0)) {
            		mEntryButton.setEnabled(true);
                } else {
                	mEntryButton.setEnabled(false);
                }
            }
        });
        mCandidateEditText.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
            }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if ((mReadEditText.getText().toString().length() != 0) && 
                    (mCandidateEditText.getText().toString().length() != 0)) {
                    mEntryButton.setEnabled(true);
                } else {
                    mEntryButton.setEnabled(false);
                }
            }
        });

    }

    /**
     * Process the event when the button is clicked
     *
     * @param  v             The information of view
     */
    public void onClick(View v) {


    	switch (v.getId()) {
            case R.id.addButton:
                doSaveAction();
                break;
            case R.id.cancelButton:
                doRevertAction();
                break;
            default:
                Log.e("OpenWnn", "onClick: Get Invalid ButtonID. ID=" + v.getId());
                finish();
            	return;
        }
    }

    /**
     * Process the adding or editing action
     */
    private void doSaveAction() {
        boolean ret;


        switch (mRequestState) {
        case STATE_INSERT:
            if (inputDataCheck(mReadEditText) && inputDataCheck(mCandidateEditText)) {
                    if (addDictionary(mReadEditText, mCandidateEditText)) {
                        screenTransition();
                    }
                }
            break;
            
        case STATE_EDIT:
            if (inputDataCheck(mReadEditText) && inputDataCheck(mCandidateEditText)) {
                deleteDictionary(mBeforeEditWnnWord);
                    if (addDictionary(mReadEditText, mCandidateEditText)) {
                        screenTransition();
                    }
                }
            break;

        default:
            Log.e("OpenWnn", "doSaveAction: Invalid Add Status. Status=" + mRequestState);
            break;
        }
    }
    
    /**
     * Process the cancel action
     */
    private void doRevertAction() {


        screenTransition();


    }

    /**
     * Create the alert dialog for notifing the error
     *
     * @param  id        The dialog ID
     * @return           The information of the dialog
     */
    @Override
    protected Dialog onCreateDialog(int id) {

    	switch (id) {
            case DIALOG_CONTROL_WORDS_DUPLICATE:
                return new AlertDialog.Builder(UserDictionaryToolsEdit.this)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setMessage(R.string.user_dictionary_words_duplication_message)
                        .setPositiveButton(android.R.string.ok, null)
                        .setCancelable(true)
                        .create();
            case DIALOG_CONTROL_OVER_MAX_TEXT_SIZE:
                return new AlertDialog.Builder(UserDictionaryToolsEdit.this)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setMessage(R.string.user_dictionary_over_max_text_size_message)
                        .setPositiveButton(android.R.string.ok, null)
                        .setCancelable(true)
                        .create();
        }
        return super.onCreateDialog(id);

    }

    /**
     * Add the word
     *
     * @param  readView         The view of stroke
     * @param  candidateView    The view of candidate
     * @return                  <code>True</code> if success; <code>False</code> if fail
     */
    private boolean addDictionary(View readView, View candidateView) {

        boolean ret;


        SpannableStringBuilder readString = (SpannableStringBuilder)mReadEditText.getText();
        SpannableStringBuilder candidateString = (SpannableStringBuilder)mCandidateEditText.getText();
        WnnWord wnnWordAdd = new WnnWord();
        wnnWordAdd.stroke = readString.toString();
        wnnWordAdd.candidate = candidateString.toString();
        OpenWnnEvent event = new OpenWnnEvent(OpenWnnEvent.ADD_WORD,
                                  WnnEngine.DICTIONARY_TYPE_USER,
                                  wnnWordAdd);
        ret = sendEventToIME(event);
        if (ret == false) {
            int ret_code = event.errorCode;
            if (ret_code == RETURN_SAME_WORD) {
                showDialog(DIALOG_CONTROL_WORDS_DUPLICATE);
            }
        } else {
            mListInstance = createUserDictionaryToolsList();
        }
        return ret;
    }

    /**
     * Delete the word
     *
     * @param  word     The information of word
     */
    private void deleteDictionary(WnnWord word) {


        mListInstance = createUserDictionaryToolsList();
        boolean deleted = mListInstance.deleteWord(word);
        if (!deleted) {
            Toast.makeText(getApplicationContext(),
                           R.string.user_dictionary_delete_fail,
                           Toast.LENGTH_LONG).show();
        }

    }

    /**
     * Create the instance of UserDictionaryToolList object
     */
    protected abstract UserDictionaryToolsList createUserDictionaryToolsList();

    /**
     * Check the input string
     *
     * @param   v       The information of view
     * @return          <code>True</code> if success; <code>False</code> if fail
     */
    private boolean inputDataCheck(View v) {


        if ((((TextView)v).getTextSize()) > MAX_TEXT_SIZE) {
            showDialog(DIALOG_CONTROL_OVER_MAX_TEXT_SIZE);
            Log.e("OpenWnn", "inputDataCheck() : over max string length.");
            return false;
        }

        return true;
    }

    /**
     * Transit the new state
     */
    private void screenTransition() {
        finish();

        Intent intent = new Intent();
        intent.setClassName(mPackageName, mListViewName);
        startActivity(intent);

    }

}

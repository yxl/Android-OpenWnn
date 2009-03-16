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
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnTouchListener;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

/**
 * User dictionary tool abstract class
 *
 * @author Copyright (C) 2008, OMRON SOFTWARE CO., LTD.  All Rights Reserved.
 */
public abstract class UserDictionaryToolsList extends Activity
    implements View.OnClickListener, OnTouchListener, OnFocusChangeListener {

    /** The class name of the user dictionary tool */
    protected String  mListViewName;
    /** The class name of the user dictionary editor */
    protected String  mEditViewName;
    /** The package name of the user dictionary editor */
    protected String  mPackageName;

    /** ID of the menu item (add) */
    private final int MENU_ITEM_ADD = 0;
    /** ID of the menu item (edit) */
    private final int MENU_ITEM_EDIT = 1;
    /** ID of the menu item (delete) */
    private final int MENU_ITEM_DELETE = 2;
    /** ID of the menu item (initialize) */
    private final int MENU_ITEM_INIT = 3;
    
    /** ID of the dialog control (confirm deletion) */
    private final int DIALOG_CONTROL_DELETE_CONFIRM = 0;
    /** ID of the dialog control (confirm initialize) */
    private final int DIALOG_CONTROL_INIT_CONFIRM = 1;

    /** The size of font*/
    private final int WORD_TEXT_SIZE = 16;

    /** The color of background */
    private final int UNFOCUS_BACKGROUND_COLOR = 0xFF242424;
    private final int FOCUS_BACKGROUND_COLOR = 0xFFFF8500;

    /** The minimum count of registered words */
    private final int MIN_WORD_COUNT = 0;
    /** The maximum count of registered words */
    private final int MAX_WORD_COUNT = 100;
    /** Maximum word count to display */
    private final int MAX_LIST_WORD_COUNT = 50;
    /** Maximum word count to display (at the first step) */
    private final int MAX_LIST_WORD_DELAY_COUNT = 50;

    /** The threshold time of the double tapping */
    private final int DOUBLE_TAP_TIME = 300;

    /** Widgets which constitute this screen of activity */
    private Menu mMenu;
    private TableLayout mTableLayout;
    private UserDictionaryToolsListFocus mTextCandidateListFocus;
    private UserDictionaryToolsEdit mUserDictionaryToolsEdit;
    private static View sFocusingView = null;
    private static View sFocusingPairView = null;

    /** Objects which control state transitions */
    private Intent mIntent;
    private OpenWnnEvent mEvent;
    private Handler mDelayUpdateHandler;

    /** The number of the registered words */
    private int mWordCount = -1;

    /** The state of menu items */
    private boolean mAddMenuEnabled;
    private boolean mEditMenuEnabled;
    private boolean mDeleteMenuEnabled;
    private boolean mInitMenuEnabled;

    /** <code>true</code> if the menu option is initialized */
    private boolean mInitializedMenu = false;
    /** <code>true</code> if one of word is selected */
    private boolean mSelectedWords;
    /** The viewID which is selected */
    private int mSelectedViewID = -1;
    /** The viewID which was selected previously */
    private static int sBeforeSelectedViewID = -1;
    /** The time of previous action */
    private static long sJustBeforeActionTime = -1;

    private boolean mHasCreatedList = false;

    /**
     * Send the specified event to IME
     *
     * @param ev    The event object
     * @return      <code>true</code> if this event is processed
     */
    protected abstract boolean sendEventToIME(OpenWnnEvent ev);

    /**
     * Create the header
     */
    protected abstract void  headerCreate();

    /** @see android.app.Activity#onCreate */
    @Override protected void onCreate(Bundle savedInstanceState) {


        super.onCreate(savedInstanceState);

        mDelayUpdateHandler = new Handler();
        /* create XML layout */
        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.user_dictionary_tools_list);

        updateWordList();

    }

    /**
     * Set parameters of table
     *
     * @param  w        The width of the table
     * @param  h        The height of the table
     * @return          The information of the layout
     */
    private TableLayout.LayoutParams tableCreateParam(int w, int h) {
        return new TableLayout.LayoutParams(w, h);
    }

    /** @see android.app.Activity#onCreateOptionsMenu */
    @Override public boolean onCreateOptionsMenu(Menu menu) {


        /* initialize the menu */
        menu.clear();
        /* set the menu item enable/disable */
        setOptionsMenuEnabled();
        /* [menu] add a word */
        menu.add(0, MENU_ITEM_ADD, 0, R.string.user_dictionary_add)
            .setIcon(android.R.drawable.ic_menu_add)
            .setEnabled(mAddMenuEnabled);
        /* [menu] edit a word */
        menu.add(0, MENU_ITEM_EDIT, 0, R.string.user_dictionary_edit)
            .setIcon(android.R.drawable.ic_menu_edit)
            .setEnabled(mEditMenuEnabled);
        /* [menu] delete a word */
        menu.add(0, MENU_ITEM_DELETE, 0, R.string.user_dictionary_delete)
            .setIcon(android.R.drawable.ic_menu_delete)
            .setEnabled(mDeleteMenuEnabled);
        /* [menu] clear the dictionary */
        menu.add(1, MENU_ITEM_INIT, 0, R.string.user_dictionary_init)
            .setIcon(android.R.drawable.ic_menu_delete)
            .setEnabled(mInitMenuEnabled);

        mMenu = menu;
        mInitializedMenu = true;


        return super.onCreateOptionsMenu(menu);
    }

    /**
     * Change state of the option menus according to a current state of the list widget
     */
    private void setOptionsMenuEnabled() {


        /* [menu] add a word */
        if ((mWordCount >= MAX_WORD_COUNT) || !mHasCreatedList) {
            /* disable if the number of registered word exceeds MAX_WORD_COUNT */
            mAddMenuEnabled = false;
        } else {
            mAddMenuEnabled = true;
        }
        
        /* [menu] edit a word/delete a word */
        if (mWordCount <= MIN_WORD_COUNT /* no word */
            || mSelectedWords == false   /* no selected word */) {
            /* disable if no word is registered or no word is selected */
            mEditMenuEnabled = false;
            mDeleteMenuEnabled = false;
        } else {
            mEditMenuEnabled = true;
            mDeleteMenuEnabled = true;
        }
        
        /* [menu] clear the dictionary (always enabled) */
        mInitMenuEnabled = true;

    }

    /** @see android.app.Activity#onOptionsItemSelected */
    @Override public boolean onOptionsItemSelected(MenuItem item) {

        boolean ret;
        switch (item.getItemId()) {
        case MENU_ITEM_ADD:
            /* add a word */
            wordAdd();
            ret = true;
            break;

        case MENU_ITEM_EDIT:
            /* edit the word (show dialog) */
            wordEdit(sFocusingView, sFocusingPairView);
            ret = true;
            break;

        case MENU_ITEM_DELETE:
            /* delete the word (show dialog) */
            showDialog(DIALOG_CONTROL_DELETE_CONFIRM);
            ret = true;
            break;

        case MENU_ITEM_INIT:
            /* clear the dictionary (show dialog) */
            showDialog(DIALOG_CONTROL_INIT_CONFIRM);
            ret = true;
            break;

        default:
            ret = false;
        }

        return ret;
    }

    /** @see android.app.Activity#onKeyUp */
    @Override public boolean onKeyUp(int keyCode, KeyEvent event) {
        /* open the menu if KEYCODE_DPAD_CENTER is pressed */
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
            openOptionsMenu();
            return true;
        }
        return false;
    }

    /** @see android.app.Activity#onCreateDialog */
    @Override protected Dialog onCreateDialog(int id) {
        switch (id) {
        case DIALOG_CONTROL_DELETE_CONFIRM:
            return new AlertDialog.Builder(UserDictionaryToolsList.this)
                .setMessage(R.string.user_dictionary_delete_confirm)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, mDialogDeleteWords)
                .setCancelable(true)
                .create();

        case DIALOG_CONTROL_INIT_CONFIRM:
            return new AlertDialog.Builder(UserDictionaryToolsList.this)
                .setMessage(R.string.dialog_clear_user_dictionary_message)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, mDialogInitWords)
                .setCancelable(true)
                .create();

        default:
            Log.e("OpenWnn", "onCreateDialog : Invaled Get DialogID. ID=" + id);
            break;
        }


        return super.onCreateDialog(id);
    }

    /**
     * Process the event when the button on the "Delete word" dialog is pushed
     *
     * @param  dialog    The information of the dialog
     * @param  button    The button that is pushed
     */
    private DialogInterface.OnClickListener mDialogDeleteWords =
        new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int button) {

                CharSequence focusString = ((TextView)sFocusingView).getText();
                CharSequence focusPairString = ((TextView)sFocusingPairView).getText();
                WnnWord wnnWordSearch = new WnnWord();

                if (mSelectedViewID>=MAX_WORD_COUNT) {
                    wnnWordSearch.stroke = focusPairString.toString();
                    wnnWordSearch.candidate = focusString.toString();
                } else {
                    wnnWordSearch.stroke = focusString.toString();
                    wnnWordSearch.candidate = focusPairString.toString();
                }
                boolean deleted = deleteWord(wnnWordSearch);
                if (deleted) {
                    Toast.makeText(getApplicationContext(),
                                   R.string.user_dictionary_delete_complete,
                                   Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getApplicationContext(),
                                   R.string.user_dictionary_delete_fail,
                                   Toast.LENGTH_LONG).show();
                }
 
                int id = mSelectedViewID;
                View v = null;

                mTableLayout.removeView((View)sFocusingView.getParent());

                for (int i = id; i < MAX_WORD_COUNT; i++) {
                    v = mTableLayout.findViewById(i);
                    if (v != null) {
                        break;
                    }
                }

                if (v == null) {
                    for (int i = id; 0 <= i; i--) {
                        v = mTableLayout.findViewById(i);
                        if (v != null) {
                            break;
                        }
                    }
                }
            
                if (v != null) {
                    ((View)v.getParent()).requestFocus();
                }
                mWordCount--;

                TextView leftText = (TextView) findViewById(R.id.user_dictionary_tools_list_title_words_count);
                leftText.setText(mWordCount + "/" + MAX_WORD_COUNT);

                if (mInitializedMenu) {
                    onCreateOptionsMenu(mMenu);
                }
            }
        };
    
    /**
     * Process the event when the button on the "Initialize" dialog is pushed
     *
     * @param  dialog    The information of the dialog
     * @param  button    The button that is pushed
     */
    private DialogInterface.OnClickListener mDialogInitWords =
        new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int button) {

                /* clear the user dictionary */
                OpenWnnEvent ev = new OpenWnnEvent(OpenWnnEvent.INITIALIZE_USER_DICTIONARY, new WnnWord());

                sendEventToIME(ev);
                /* show the message */
                Toast.makeText(getApplicationContext(), R.string.dialog_clear_user_dictionary_done,
                               Toast.LENGTH_LONG).show();
                updateWordList();
            }
        };

    
    /** @see android view.View.OnClickListener#onClick */
    public void onClick(View arg0) {
    }

    /** @see android.view.View.OnTouchListener#onTouch */
    public boolean onTouch(View v, MotionEvent e) {


        mSelectedViewID = ((TextView)v).getId();
        switch (e.getAction()) {
        case MotionEvent.ACTION_DOWN:
            /* double tap handling */
            if (sBeforeSelectedViewID != ((TextView)v).getId()) {
                /* save the view id if the id is not same as previously selected one */
                sBeforeSelectedViewID = ((TextView)v).getId();
            } else {
                if ((e.getDownTime() - sJustBeforeActionTime) < DOUBLE_TAP_TIME) {
                    /* edit the word if double tapped */
                    sFocusingView = v;
                    sFocusingPairView = ((UserDictionaryToolsListFocus)v).getPairView();
                    wordEdit(sFocusingView, sFocusingPairView);
                }
            }
            /* save the action time */
            sJustBeforeActionTime = e.getDownTime();
            break;
        }

        return false;
    }

    /** @see android.view.View.onFocusChangeListener#onFocusChange */
    public void onFocusChange(View v, boolean hasFocus) {

        mSelectedViewID = ((TextView)v).getId();
        sFocusingView = v;
        sFocusingPairView = ((UserDictionaryToolsListFocus)v).getPairView();
        if (hasFocus) {
            ((TextView)v).setTextColor(Color.BLACK);
            v.setBackgroundColor(FOCUS_BACKGROUND_COLOR);
            ((TextView)sFocusingPairView).setTextColor(Color.BLACK);
            sFocusingPairView.setBackgroundColor(FOCUS_BACKGROUND_COLOR);
            mSelectedWords = true;
        } else {
            if (mSelectedViewID == 0) {
                mSelectedWords = false;
            }
            ((TextView)v).setTextColor(Color.LTGRAY);
            v.setBackgroundColor(UNFOCUS_BACKGROUND_COLOR);
            ((TextView)sFocusingPairView).setTextColor(Color.LTGRAY);
            sFocusingPairView.setBackgroundColor(UNFOCUS_BACKGROUND_COLOR);
        }
        if (mInitializedMenu) {
            onCreateOptionsMenu(mMenu);
        }
    }

    /**
     * Add the word
     */
    public void wordAdd() {
        screenTransition(Intent.ACTION_INSERT, mEditViewName);
    }

    /**
     * Edit the specified word
     *
     * @param  focusView       The information of view
     * @param  focusPairView   The information of pair of view
     */
    public void wordEdit(View focusView, View focusPairView) {
        if (mSelectedViewID >= MAX_WORD_COUNT) {
            mUserDictionaryToolsEdit = createUserDictionaryToolsEdit(focusPairView, focusView, mWordCount);
        } else {
            mUserDictionaryToolsEdit = createUserDictionaryToolsEdit(focusView, focusPairView, mWordCount);
        }
        screenTransition(Intent.ACTION_EDIT, mEditViewName);
    }

    /**
     * The internal process of editing the specified word
     *
     * @param  focusView        The information of view
     * @param  focusPairView    The information of pair of view
     * @param  wordCount        The count of the registered words
     */
    protected abstract UserDictionaryToolsEdit createUserDictionaryToolsEdit(View focusView, View focusPairView, int wordCount);

    /**
     * Delete the specified word
     *
     * @param  searchword       The information of searching
     * @return <code>true</code> if success; <code>false</code> if fail
     */
    public boolean deleteWord(WnnWord searchword) {
        OpenWnnEvent event = new OpenWnnEvent(OpenWnnEvent.LIST_WORDS_IN_USER_DICTIONARY,
                                              WnnEngine.DICTIONARY_TYPE_USER,
                                              searchword);

        boolean deleted = false;
        sendEventToIME(event);
        for( int i=0; i < MAX_WORD_COUNT; i++) {
            WnnWord getword = new WnnWord();
            event = new OpenWnnEvent(OpenWnnEvent.GET_WORD,
                                     getword);
            sendEventToIME(event);
            getword = event.word;
            int len = getword.candidate.length();
            if (len == 0) {
                break;
            }
            if (searchword.candidate.equals(getword.candidate)) {
                WnnWord delword = new WnnWord();
                delword.stroke = searchword.stroke;
                delword.candidate = searchword.candidate;
                delword.id = i;
                event = new OpenWnnEvent(OpenWnnEvent.DELETE_WORD,
                                         delword);
                deleted = sendEventToIME(event);
                break;
            }
        }
        
        if (mInitializedMenu) {
            onCreateOptionsMenu(mMenu);
        }

        return deleted;
    }


    /**
     * Processing the transition of screen
     *
     * @param  action       The string of action
     * @param  classname    The class name
     */
    private void screenTransition(String action, String classname) {

        if (action.equals("")) {
            mIntent = new Intent();
        } else {
            mIntent = new Intent(action);
        }
        mIntent.setClassName(mPackageName, classname);
        startActivity(mIntent);
        finish();
    }


    /**
     * Update the word list.
     */
    private void updateWordList() {
        mWordCount = 0;
        WnnWord wnnWordSearch = new WnnWord();
        mEvent = new OpenWnnEvent(OpenWnnEvent.LIST_WORDS_IN_USER_DICTIONARY,
                                  WnnEngine.DICTIONARY_TYPE_USER,
                                  wnnWordSearch);
        sendEventToIME(mEvent);

        mTableLayout = (TableLayout)findViewById(R.id.user_dictionary_tools_table);
        mTableLayout.removeAllViews();

        if (createWordList(mWordCount,MAX_LIST_WORD_COUNT, this)) {
            headerCreate();
            final TextView leftText = (TextView) findViewById(R.id.user_dictionary_tools_list_title_words_count);
            leftText.setText(R.string.user_dictionary_creating_wordlist);
            mDelayUpdateHandler.removeCallbacks(updateWordListRunnable);
            mDelayUpdateHandler.postDelayed(updateWordListRunnable, 0);
        } else {
            if (mSelectedViewID >= 0) {
                View v;
                v = mTableLayout.findViewById(mSelectedViewID);
                if (v != null) {
                    ((View)v.getParent()).requestFocus();
                } else {
                    mSelectedViewID--;
                    if (mSelectedViewID < 0) {
                        mSelectedViewID = 0;
                    }
                    v = mTableLayout.findViewById(mSelectedViewID);
                    if (v != null) {
                        ((View)v.getParent()).requestFocus();
                    }
                }
            }
            headerCreate();
            final TextView leftText = (TextView) findViewById(R.id.user_dictionary_tools_list_title_words_count);
            leftText.setText(mWordCount + "/" + MAX_WORD_COUNT);
            mHasCreatedList = true;
        }
    }

    /**
     * Handler for updating the word list.
     */
    private final Runnable updateWordListRunnable = new Runnable() {
            public void run() {
                UserDictionaryToolsList self = UserDictionaryToolsList.this;
                if (createWordList(mWordCount,mWordCount+MAX_LIST_WORD_DELAY_COUNT, self)) {
                    mDelayUpdateHandler.removeCallbacks(updateWordListRunnable);
                    mDelayUpdateHandler.postDelayed(updateWordListRunnable, 0);
                } else {
                    mDelayUpdateHandler.removeCallbacks(updateWordListRunnable);
                    headerCreate();
                    final TextView leftText = (TextView) findViewById(R.id.user_dictionary_tools_list_title_words_count);
                    leftText.setText(mWordCount + "/" + MAX_WORD_COUNT);
                    mHasCreatedList = true;
                    if (mInitializedMenu) {
                        onCreateOptionsMenu(mMenu);
                    }
                }
            }
        };
        
    /**
     * Create the list of words.
     *
     * @param  position start position to create the list
     * @param  max      maximum number of words to display
     * @param  self     UserDictionaryToolsList
     * @return <code>true</code> if more words undisplayed; <code>false</code> if no more.
     */
    private boolean createWordList(int position ,int max, UserDictionaryToolsList self) {
        boolean ret = true;
                
        if (position >= MAX_WORD_COUNT) {
            return false;
        }
        Window window = getWindow();
        WindowManager windowManager = window.getWindowManager();
        Display display = windowManager.getDefaultDisplay();
        int system_width = display.getWidth();
        WnnWord wnnWordGet = new WnnWord();
        mEvent = new OpenWnnEvent(OpenWnnEvent.GET_WORD, wnnWordGet);
        
        int i;
        for (i = position; i < max; i++) {
            if (!sendEventToIME(mEvent)) {
                ret =  false;
                break;
            }
            wnnWordGet = mEvent.word;
            int len_stroke = wnnWordGet.stroke.length();
            int len_candidate = wnnWordGet.candidate.length();
            if (len_stroke == 0 || len_candidate == 0) {
                ret =  false;
                break;
            }

            mWordCount++;
            TableRow row = new TableRow(self);
            UserDictionaryToolsListFocus stroke = new UserDictionaryToolsListFocus(self);
            stroke.setId(mWordCount);
            stroke.setText(wnnWordGet.stroke);
            stroke.setWidth(system_width/2);
            stroke.setTextSize(WORD_TEXT_SIZE);
            stroke.setTextColor(Color.LTGRAY);
            stroke.setBackgroundColor(UNFOCUS_BACKGROUND_COLOR);
            stroke.setSingleLine();
            stroke.setPadding(1,0,1,1);
            stroke.setEllipsize(TextUtils.TruncateAt.END);
            stroke.setClickable(true);
            stroke.setFocusable(true);
            stroke.setFocusableInTouchMode(true);
            stroke.setOnTouchListener(self);
            stroke.setOnFocusChangeListener(self);

            UserDictionaryToolsListFocus candidate = new UserDictionaryToolsListFocus(self);
            candidate.setId(mWordCount+MAX_WORD_COUNT);
            candidate.setText(wnnWordGet.candidate);
            candidate.setWidth(system_width/2);
            candidate.setTextSize(WORD_TEXT_SIZE);
            candidate.setTextColor(Color.LTGRAY);
            candidate.setBackgroundColor(UNFOCUS_BACKGROUND_COLOR);
            candidate.setSingleLine();
            candidate.setPadding(1,0,1,1);
            candidate.setEllipsize(TextUtils.TruncateAt.END);
            candidate.setClickable(true);
            candidate.setFocusable(true);
            candidate.setFocusableInTouchMode(true);
            candidate.setOnTouchListener(self);
            candidate.setOnFocusChangeListener(self);

            stroke.setPairView(candidate);
            candidate.setPairView(stroke);

            row.addView(stroke);
            row.addView(candidate);
            mTableLayout.addView(row, tableCreateParam(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }
        mTableLayout.requestLayout();
        return ret;
    }
}





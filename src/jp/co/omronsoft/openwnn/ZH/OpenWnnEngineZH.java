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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Arrays;

import jp.co.omronsoft.openwnn.*;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * The OpenWnn engine class for Chinese IME.
 * 
 * @author Copyright (C) 2009 OMRON SOFTWARE CO., LTD.  All Rights Reserved.
 */
public class OpenWnnEngineZH implements WnnEngine {
    /** Current dictionary type */
    private int mDictType = DIC_LANG_INIT;
    /** Dictionary type (default) */
    public static final int DIC_LANG_INIT = 0;
    /** Dictionary type (Chinese standard) */
    public static final int DIC_LANG_ZH = 0;
    /** Dictionary type (English standard) */
    public static final int DIC_LANG_EN = 1;
    /** Dictionary type (Chinese person's name) */
    public static final int DIC_LANG_ZH_PERSON_NAME = 2;
    /** Dictionary type (User dictionary) */
    public static final int DIC_USERDIC = 3;
    /** Dictionary type (Chinese EISU-KANA conversion) */
    public static final int DIC_LANG_ZH_EISUKANA = 4;
    /** Dictionary type (e-mail/URI) */
    public static final int DIC_LANG_EN_EMAIL_ADDRESS = 5;
    /** Dictionary type (Chinese postal address) */
    public static final int DIC_LANG_ZH_POSTAL_ADDRESS = 6;

    /** Type of the keyboard */
    private int mKeyboardType = KEYBOARD_UNDEF;
    /** Keyboard type (not defined) */
    public static final int KEYBOARD_UNDEF = 0;
    /** Keyboard type (12-keys) */
    public static final int KEYBOARD_KEYPAD12 = 1;
    /** Keyboard type (Qwerty) */
    public static final int KEYBOARD_QWERTY = 2;
    /** Maximum limit length of output */
    public static final int MAX_OUTPUT_LENGTH = 50;

    /** Score(frequency value) of word in the learning dictionary */
    public static final int FREQ_LEARN = 600;
    /** Score(frequency value) of word in the user dictionary */
    public static final int FREQ_USER = 500;

    /** OpenWnn dictionary */
    private WnnDictionary mDictionaryZH;

    /** Word list */
    private ArrayList<WnnWord> mConvResult;

    /** HashMap for checking duplicate word */
    private HashMap<String, WnnWord> mCandTable;

    /** Input string (PinYin) */
    private String mInputPinyin;
    
    /** Number of output candidates */
    private int mOutputNum;
    
    /**
     * Where to get the next candidates from.<br>
     * (0:prefix search from the dictionary, 1:single clause converter, 2:Kana converter)
     */
    private int mGetCandidateFrom;
    
    /** Previously selected word */
    private WnnWord mPreviousWord;

    /** Converter for single/consecutive clause conversion */
    private OpenWnnClauseConverterZH mClauseConverter;

    /** Whether exact match search or prefix match search */
    private boolean mExactMatchMode;

    /** Whether displaying single clause candidates or not */
    private boolean mSingleClauseMode;

    /** A result of consecutive clause conversion */
    private WnnSentence mConvertSentence;

    /** Consonant predict converter */
    protected ConsonantPrediction mConsonantPredictConverter;
    
    /** Length of the search key */
    private int mSearchLength;
    
    private HashMap<String,ArrayList<WnnWord>> mSearchCache;
    private ArrayList<WnnWord> mSearchCacheArray;
    private ArrayList<WnnWord> mNoWord;
    
    /** The candidate filter */
    private CandidateFilter mFilter;

    /**
     * Constructor
     *
     * @param dicLib   The dictionary library  
     * @param dicFilePath  The DB file for the user/learning dictionary
     */
    public OpenWnnEngineZH(String dicLib, String dicFilePath) {
        /* load Chinese dictionary library */
        mDictionaryZH = new OpenWnnDictionaryImpl("/data/data/jp.co.omronsoft.openwnn/lib/" + dicLib, dicFilePath);
        if (!mDictionaryZH.isActive()) {
        	mDictionaryZH = new OpenWnnDictionaryImpl("/system/lib/" + dicLib, dicFilePath);
        }

        /* clear dictionary settings */
        mDictionaryZH.clearDictionary();
        mDictionaryZH.clearApproxPattern();
        mDictionaryZH.setInUseState(false);

        /* work buffers */
        mConvResult = new ArrayList<WnnWord>();
        mCandTable = new HashMap<String, WnnWord>();
        mSearchCache = new HashMap<String, ArrayList<WnnWord>>();
        mNoWord = new ArrayList<WnnWord>();
        
        /* converters */
        mClauseConverter = new OpenWnnClauseConverterZH();
        mConsonantPredictConverter = new ConsonantPrediction();
        mFilter = new CandidateFilter();
    }

    /**
     * Set dictionary for prediction.
     * 
     * @param strlen  Length of input string
     */
    private void setDictionaryForPrediction(int strlen) {
        WnnDictionary dict = mDictionaryZH;

        dict.clearDictionary();
        dict.clearApproxPattern();
        if (strlen == 0) {
        	dict.setDictionary(3, 300, 400);
        	dict.setDictionary(4, 100, 200);
            dict.setDictionary(WnnDictionary.INDEX_LEARN_DICTIONARY, FREQ_LEARN, FREQ_LEARN);
        } else {
        	dict.setDictionary(0, 300, 400);
        	dict.setDictionary(1, 300, 400);
        	if (strlen <= PinyinParser.PINYIN_MAX_LENGTH) {
        		dict.setDictionary(2, 400, 500); /* single Kanji dictionary */
        	}
            dict.setDictionary(WnnDictionary.INDEX_USER_DICTIONARY, FREQ_USER, FREQ_USER);
            dict.setDictionary(WnnDictionary.INDEX_LEARN_DICTIONARY, FREQ_LEARN, FREQ_LEARN);
            dict.setApproxPattern(WnnDictionary.APPROX_PATTERN_EN_TOUPPER);
        }
    }

    /**
     * Set the candidate filter
     * 
     * @param filter	The candidate filter
     */
    public void setFilter(CandidateFilter filter) {
    	mFilter = filter;
    	mClauseConverter.setFilter(filter);
    }

    /**
     * Get a candidate.
     *
     * @param index		Index of a candidate.
     * @return			The candidate; {@code null} if there is no candidate.
     */
    private WnnWord getCandidate(int index) {
        WnnWord word;

        if (mGetCandidateFrom == 0) {
            if (mSingleClauseMode) {
                /* single clause conversion */
                Iterator<?> convResult = mClauseConverter.convert(mInputPinyin);
                if (convResult != null) {
                	while (convResult.hasNext()) {
                		addCandidate((WnnWord)convResult.next());
                	}
                }	
                /* end of candidates by single clause conversion */
                mGetCandidateFrom = -1;
            } else {
                /* get prefix matching words from the dictionaries */
                while (index >= mConvResult.size()) {
                    if ((word = mDictionaryZH.getNextWord()) == null) {
                    	if (!mExactMatchMode && mSearchLength > 1) {
                    		mGetCandidateFrom = 1;
                    		break;
                    	} else {
                    		mGetCandidateFrom = 2;
                    		break;
                    	}
                    }
                    if (mSearchLength == word.stroke.length()
                        || (!mExactMatchMode && (mSearchLength == mInputPinyin.length()))) {
                        addCandidate(word);
                    }
                }
            }
        }
        
        if (mGetCandidateFrom == 1) {
            /* get common prefix matching words from the dictionaries */
            while (index >= mConvResult.size()) {
                if ((word = mDictionaryZH.getNextWord()) == null) {
                	if (--mSearchLength > 0) {
                		String input = mInputPinyin.substring(0, mSearchLength);
                		if (mSearchLength == PinyinParser.PINYIN_MAX_LENGTH) {
                			/* if length of the key is less than PinyinParser.PINYIN_MAX_LENGTH,
                			 * use the single Kanji dictionary.
                			 */
                			mDictionaryZH.setDictionary(2, 400, 500); /* single Kanji dictionary */
                		}
                		
                		ArrayList<WnnWord> cache = mSearchCache.get(input);
                		if (cache != null) {
                			if (cache != mNoWord) {
                				Iterator<WnnWord> cachei = cache.iterator();
                				while (cachei.hasNext()) {
                					addCandidate(cachei.next());
                				}
                				mSearchCacheArray = cache;
                				mDictionaryZH.searchWord(WnnDictionary.SEARCH_PREFIX, WnnDictionary.ORDER_BY_FREQUENCY, input);
                			}
                		} else {
                			if (PinyinParser.isPinyin(input)
                					&& mDictionaryZH.searchWord(WnnDictionary.SEARCH_PREFIX, WnnDictionary.ORDER_BY_FREQUENCY, input) > 0) {
                    			mSearchCacheArray = new ArrayList<WnnWord>();
                    		} else {
                    			mSearchCacheArray = mNoWord;                    			
                    		}
                			mSearchCache.put(input, mSearchCacheArray);
                		}
                		continue;
                	} else {
                		mGetCandidateFrom = 2;
                		break;
                	}
                }
                if (mSearchLength == word.stroke.length()
                    || (!mExactMatchMode && (mSearchLength == mInputPinyin.length()))) {
                    if (addCandidate(word)) {
                    	mSearchCacheArray.add(word);
                    }                    
                }
            }
        }
        
        if (mGetCandidateFrom == 2) {
        	/* get a candidate from mConsonantPredictConverter. */
    		word = mConsonantPredictConverter.convert(mInputPinyin, mExactMatchMode);
    	
    		if (word == null) {
    			mGetCandidateFrom = -1;
    		} else {
    			mGetCandidateFrom = 3;
    			addCandidate(word);
    		}
        }

        if (mGetCandidateFrom == 3) {
        	/* get all candidates from mConsonantPredictConverter. */
        	while (index >= mConvResult.size()) {
        		if ((word = mConsonantPredictConverter.nextCandidate()) == null) {
        			mGetCandidateFrom = -1;
        			break;
        		} else {
        			addCandidate(word);
        		}
        	}
        }

        if (index >= mConvResult.size()) {
            return null;
        }
        return (WnnWord) mConvResult.get(index);
    }

    /**
     * Add a candidate to the conversion result buffer.
     * <br>
     * This method adds a word to the result buffer if there is not
     * the same one in the buffer and the length of the candidate
     * string is not longer than {@code MAX_OUTPUT_LENGTH}.
     *
     * @param word		The word to be added
     * @return			{@code true} if the word added; {@code false} if not.
     */
    protected boolean addCandidate(WnnWord word) {
        if (word.candidate == null || mCandTable.containsKey(word.candidate)
                || word.candidate.length() > MAX_OUTPUT_LENGTH) {
            return false;
        }
        if (mFilter != null && !mFilter.isAllowed(word)) {
        	return false;
        }
        mCandTable.put(word.candidate, word);
        mConvResult.add(word);
        return true;
    }

    /**
     * Clear work area that hold candidates information.
     */
    protected void clearCandidates() {
        mConvResult.clear();
        mCandTable.clear();
        mOutputNum = 0;
        mInputPinyin = null;
        mGetCandidateFrom = 0;
        mSingleClauseMode = false;
    }

    /**
     * Set dictionary type.
     *
     * @param type		Type of dictionary
     * @return			{@code true} if the dictionary is changed; {@code false} if not.
     */
    public boolean setDictionary(int type) {
        mDictType = type;
        mSearchCache.clear();
        return true;
    }

    /**
     * Set the search key and the search mode from {@link ComposingText}.
     *
     * @param text		Input text
     * @param maxLen	Maximum length to convert
     * @return			Length of the search key.
     */
    protected int setSearchKey(ComposingText text, int maxLen) {
        String input = text.toString(ComposingText.LAYER1);
        if (0 <= maxLen && maxLen <= input.length()) {
            input = input.substring(0, maxLen);
            mExactMatchMode = true;
        } else {
            mExactMatchMode = false;
        }

        if (input.length() == 0) {
            mInputPinyin = "";
            return 0;
        }

        mInputPinyin = input;

        return input.length();
    }

    /**
     * Clear the previous word's information.
     */
    public void clearPreviousWord() {
        mPreviousWord = null;
    }

    /**
     * Set keyboard type.
     * 
     * @param keyboardType		Type of keyboard
     */
    public void setKeyboardType(int keyboardType) {
        mKeyboardType = keyboardType;
    }

    /***********************************************************************
     * WnnEngine's interface
     **********************************************************************/
    /** @see jp.co.omronsoft.openwnn.WnnEngine#init */
    public void init() {
        clearPreviousWord();
        mClauseConverter.setDictionary(mDictionaryZH);
        mConsonantPredictConverter.setDictionary(mDictionaryZH);
    }

    /** @see jp.co.omronsoft.openwnn.WnnEngine#close */
    public void close() {
        mDictionaryZH.setInUseState(false);   	
    }

    /** @see jp.co.omronsoft.openwnn.WnnEngine#predict */
    public int predict(ComposingText text, int minLen, int maxLen) {
        clearCandidates();
        if (text == null) { return 0; }

        /* set mInputPinyin and mInputRomaji */
        int len = setSearchKey(text, maxLen);

        /* set dictionaries by the length of input */
        setDictionaryForPrediction(len);
        
        /* search dictionaries */
        mDictionaryZH.setInUseState( true );
        
        mSearchLength = len;

        if (len == 0) {
            /* search by previously selected word */
            return mDictionaryZH.searchWord(WnnDictionary.SEARCH_LINK, WnnDictionary.ORDER_BY_FREQUENCY,
                                            mInputPinyin, mPreviousWord);
        } else {
        	mDictionaryZH.searchWord(WnnDictionary.SEARCH_PREFIX, WnnDictionary.ORDER_BY_FREQUENCY,
        			mInputPinyin);
            return 1;
        }
    }

    /** @see jp.co.omronsoft.openwnn.WnnEngine#convert */
    public int convert(ComposingText text) {
        clearCandidates();

        if (text == null) {
            return 0;
        }

        mDictionaryZH.setInUseState(true);

        int cursor = text.getCursor(ComposingText.LAYER1);
        String input;
        WnnClause head = null;
        if (cursor > 0) {
            /* convert previous part from cursor */
            input = text.toString(ComposingText.LAYER1, 0, cursor - 1);
            Iterator<?> headCandidates = mClauseConverter.convert(input);
            if ((headCandidates == null) || (!headCandidates.hasNext())) {
                return 0;
            }
            WnnWord wd = (WnnWord)headCandidates.next();
            head = new WnnClause(wd.stroke, wd);

            /* set the rest of input string */
            input = text.toString(ComposingText.LAYER1, cursor, text.size(ComposingText.LAYER1) - 1);
        } else {
            /* set whole of input string */
            input = text.toString(ComposingText.LAYER1);
        }

        WnnSentence sentence = null;
        if (input.length() != 0) {
            sentence = mClauseConverter.consecutiveClauseConvert(input);
        }
        if (head != null) {
            sentence = new WnnSentence(head, sentence);
        }
        if (sentence == null) {
            return 0;
        }

        StrSegmentClause[] ss = new StrSegmentClause[sentence.elements.size()];
        int pos = 0;
        int idx = 0;
        Iterator<WnnClause> it = sentence.elements.iterator();
        while(it.hasNext()) {
            WnnClause clause = (WnnClause)it.next();
            int len = clause.stroke.length();
            ss[idx] = new StrSegmentClause(clause, pos, pos + len - 1);
            pos += len;
            idx += 1;
        }
        text.setCursor(ComposingText.LAYER2, text.size(ComposingText.LAYER2));
        text.replaceStrSegment(ComposingText.LAYER2, ss, 
                               text.getCursor(ComposingText.LAYER2));
        mConvertSentence = sentence;

        return 0;
    }
    
    /** @see jp.co.omronsoft.openwnn.WnnEngine#searchWords */
    public int searchWords(String key) {
        clearCandidates();
        return 0;
    }

    /** @see jp.co.omronsoft.openwnn.WnnEngine#searchWords */
    public int searchWords(WnnWord word) {
        clearCandidates();
        return 0;
    }

    /** @see jp.co.omronsoft.openwnn.WnnEngine#getNextCandidate */
    public WnnWord getNextCandidate() {
        if (mInputPinyin == null) {
            return null;
        }
        WnnWord word = getCandidate(mOutputNum);
        if (word != null) {
            mOutputNum++;
        }
        return word;
    }

    /** @see jp.co.omronsoft.openwnn.WnnEngine#learn */
    public boolean learn(WnnWord word) {
        int ret = -1;
        if (word.partOfSpeech.right == 0) {
            word.partOfSpeech = mDictionaryZH.getPOS(WnnDictionary.POS_TYPE_MEISI);
        }

        WnnDictionary dict = mDictionaryZH;
        if (word instanceof WnnSentence) {
            Iterator<WnnClause> clauses = ((WnnSentence)word).elements.iterator();
            while (clauses.hasNext()) {
                WnnWord wd = clauses.next();
                if (mPreviousWord != null) {
                    ret = dict.learnWord(wd, mPreviousWord);
                } else {
                    ret = dict.learnWord(wd);
                }
                mPreviousWord = wd;
                if (ret != 0) {
                    break;
                }
            }
        } else {
            if (mPreviousWord != null) {
                ret = dict.learnWord(word, mPreviousWord);
            } else {
                ret = dict.learnWord(word);
            }
            mPreviousWord = word;
            mClauseConverter.setDictionary(dict);
            mConsonantPredictConverter.setDictionary(dict);
            mSearchCache.clear();
        }

        return (ret == 0);
    }

    /** @see jp.co.omronsoft.openwnn.WnnEngine#addWord */
    public int addWord(WnnWord word) {
        mDictionaryZH.setInUseState( true );
        mDictionaryZH.addWordToUserDictionary(word);
        mDictionaryZH.setInUseState( false );
        return 0;
    }

    /** @see jp.co.omronsoft.openwnn.WnnEngine#deleteWord */
    public boolean deleteWord(WnnWord word) {
        mDictionaryZH.setInUseState( true );
        mDictionaryZH.removeWordFromUserDictionary(word);
        mDictionaryZH.setInUseState( false );
        return false;
    }

    /** @see jp.co.omronsoft.openwnn.WnnEngine#setPreferences */
    public void setPreferences(SharedPreferences pref) {}

    /** @see jp.co.omronsoft.openwnn.WnnEngine#breakSequence */
    public void breakSequence()  {
        clearPreviousWord();
    }

    /** @see jp.co.omronsoft.openwnn.WnnEngine#makeCandidateListOf */
    public int makeCandidateListOf(int clausePosition)  {
        clearCandidates();

        if ((mConvertSentence == null) || (mConvertSentence.elements.size() <= clausePosition)) {
            return 0;
        }
        mSingleClauseMode = true;
        WnnClause clause = mConvertSentence.elements.get(clausePosition);
        mInputPinyin = clause.stroke;

        return 1;
    }

    /** @see jp.co.omronsoft.openwnn.WnnEngine#initializeDictionary */
    public boolean initializeDictionary(int dictionary)  {
        switch( dictionary ) {
        case WnnEngine.DICTIONARY_TYPE_LEARN:
            mDictionaryZH.setInUseState( true );
            mDictionaryZH.clearLearnDictionary();
            mDictionaryZH.setInUseState( false );
            return true;

        case WnnEngine.DICTIONARY_TYPE_USER:
            mDictionaryZH.setInUseState( true );
            mDictionaryZH.clearUserDictionary();
            mDictionaryZH.setInUseState( false );
            return true;
        }
        return false;
    }

    /** @see jp.co.omronsoft.openwnn.WnnEngine#initializeDictionary */
    public boolean initializeDictionary(int dictionary, int type) {
    	return initializeDictionary(dictionary);
    }
    
    /** @see jp.co.omronsoft.openwnn.WnnEngine#getUserDictionaryWords */
    public WnnWord[] getUserDictionaryWords( ) {
        /* get words in the user dictionary */
        mDictionaryZH.setInUseState(true);
        WnnWord[] result = mDictionaryZH.getUserDictionaryWords( );
        mDictionaryZH.setInUseState(false);

        /* sort the array of words */
        Arrays.sort(result, new WnnWordComparator());

        return result;
    }

    /* {@link WnnWord} comparator for listing up words in the user dictionary */
    private class WnnWordComparator implements java.util.Comparator {
        public int compare(Object object1, Object object2) {
            WnnWord wnnWord1 = (WnnWord) object1;
            WnnWord wnnWord2 = (WnnWord) object2;
            return wnnWord1.stroke.compareTo(wnnWord2.stroke);
        }
    }
}

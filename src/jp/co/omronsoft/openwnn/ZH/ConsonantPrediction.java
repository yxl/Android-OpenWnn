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
import java.util.List;
import jp.co.omronsoft.openwnn.*;
import android.util.Log;

/**
 * Consonant predict converter for Chinese IME
 * 
 * @author Copyright (C) 2009, OMRON SOFTWARE CO., LTD. All Rights Reserved.
 */
public class ConsonantPrediction {
    /** Score(frequency value) of a word in the learning dictionary */
    private static final int FREQ_LEARN = 600;
    /** Score(frequency value) of a word in the user dictionary */
    private static final int FREQ_USER = 500;
    /** Maximum length of Kanji string to predict */
    private static final int MAX_KANJI_LENGTH = 20;

    /** Dictionary */
    private WnnDictionary mDictionary;
    /** HashMap for checking candidate duplication */
    private HashMap<String, WnnWord> mCheckDuplication;
    /** List of candidates */
    private List<WnnWord> mCandidateList;
    /** Input string's pinyin list */
    private List<String> mInputPinyinList;
    /** Whether exact match search or not */
    private boolean mExactMatchMode = false;
    
    /** Search cache */
    private ArrayList<WnnWord> mSearchCache[];
    /** Number of words fetched from the dictionary */
    private int mFetchNumFromDict;
    /** Number of words in the cache */
    private int mCacheNum;
    /** Search key */
    private String mSearchKey;
    /** Iterator of candidates from cache */
    private Iterator<WnnWord> mCacheIt;
    
    /**
     * Constructor
     */
    public ConsonantPrediction() {
    	mCheckDuplication = new HashMap<String, WnnWord>();
        mCandidateList = new ArrayList<WnnWord>();
        mInputPinyinList = new ArrayList<String>();
        mSearchCache = new ArrayList[MAX_KANJI_LENGTH];
        clearCache();
    }

    /**
     * Set dictionaries to convert.
     */
    public void setDictionary(WnnDictionary dict) {
        mDictionary = dict;
        clearCache();
    }
    
    /**
     * Clear the work buffers to convert.
     */
    private void clearBuffers() {
    	mCheckDuplication.clear();
    	mCandidateList.clear();
    	mInputPinyinList.clear();
    }
    
    private void clearCache() {
    	mSearchKey = "";
    	ArrayList<WnnWord>[] cache = mSearchCache;
    	for (int i = 0; i < cache.length; i++) {
    		cache[i] = null;
    	}
        mCacheNum = 0;
        mCacheIt = null;
    }

    /**
     * Convert from incomplete PinYin.
     * <br>
     * This method converts incomplete PinYin string to Kanji (For example, "nh" to "你好"(NiHao)).
     * 
     * @param input  The input string
     * @return The first candidate; {@code null} if fail.
     */
    public WnnWord convert(String input, boolean exact) {
        clearBuffers();
        mExactMatchMode = exact;

        if (input == null || input.length() == 0) {
            return null;
        }
        
        List<String> pinyinList = mInputPinyinList = PinyinParser.getPinyinList(input);
        if (pinyinList.size() < 2) {
        	return null;
        }
        
        WnnDictionary dict = mDictionary;
        dict.clearDictionary();
        dict.clearApproxPattern();
        dict.setDictionary(0, 300, 400);
        dict.setDictionary(WnnDictionary.INDEX_USER_DICTIONARY, FREQ_USER, FREQ_USER);
        dict.setDictionary(WnnDictionary.INDEX_LEARN_DICTIONARY, FREQ_LEARN, FREQ_LEARN);
        dict.setApproxPattern(WnnDictionary.APPROX_PATTERN_EN_TOUPPER);

        String searchKey = pinyinList.get(0);
        if (searchKey.equals(mSearchKey)) {
            int pinyinLen = pinyinList.size();
            ArrayList<WnnWord> cache = (mSearchCache.length > pinyinLen) ? mSearchCache[pinyinLen] : null;
            if (cache == null) {
            	mCacheIt = null;
            } else {
            	mCacheIt = cache.iterator();
            }
        } else {
        	clearCache();
        	mSearchKey = searchKey;
        }
        if (mCacheNum < 0) {
        	/* If all the matched words from the dictionary is fetched already,
        	 * use the cache.
        	 */
        	return nextCandidate();
        } else if (dict.searchWord(WnnDictionary.SEARCH_PREFIX, WnnDictionary.ORDER_BY_FREQUENCY,
                            searchKey) > 0) {
            mFetchNumFromDict = 0;
        	return nextCandidate();
        }
        mCacheNum = -1;
        return null;
    }
    
    /**
     * Try to add a word to the candidate list.
     * 
     * @param word   A word
     * @return  {@code true} if success; {@code false} if the same word is already in the list.
     */
    private boolean addCandidate(WnnWord word) {
    	if (mCheckDuplication.containsKey(word.candidate)) {
    		return false;
    	} else {
			mCheckDuplication.put(word.candidate, word);
			return true;
    	}
    }
    
    /** 	     
     * Get the next candidate.
     * @return The next candidate; {@code null} if there is no more candidate.
     */
    public WnnWord nextCandidate() {      
        List<String> pinyinList = mInputPinyinList;
        
        /* use cache if there is */
        if (mCacheIt != null) {
        	while (mCacheIt.hasNext()) {
        		 WnnWord word = mCacheIt.next();
        		 List<String> pinyinList2 = PinyinParser.getPinyinList(word.stroke);
        		 if (matchPinyin(pinyinList, pinyinList2, mExactMatchMode)) {
        			 return word;
        		 }
        	}
        	return null;
        }
        if (mCacheNum < 0) {
        	/* End of matched word */
        	return null;
        }
        
        WnnDictionary dict = mDictionary;  
        while (mFetchNumFromDict < mCacheNum) {
        	if (dict.getNextWord() == null) {
        		mFetchNumFromDict = -1;
        		return null;
        	}
        	mFetchNumFromDict++;
        }

        
        WnnWord word;
    	while ((word = dict.getNextWord()) != null) {
    		mFetchNumFromDict++;
    		
    		/* store to the cache */
    		List<String> pinyinList2 = PinyinParser.getPinyinList(word.stroke);
    		int len = pinyinList2.size();
    		if (len >= mSearchCache.length) {
    			len = mSearchCache.length - 1;
    		}
    		for (int i = 1; i <= len; i++) {
    			ArrayList<WnnWord> cache = mSearchCache[i];
    			if (cache == null) {
    				cache = new ArrayList<WnnWord>();
    				mSearchCache[i] = cache;
    			}
    			cache.add(word);
    		}
    		mCacheNum++;
    			
    		if (matchPinyin(pinyinList, pinyinList2, mExactMatchMode)) {
    			if (addCandidate(word)) {
                    return word;  				
    			}
    		}
    	}
    	mCacheNum = -1;
    	return null;
    }
    
    private boolean matchPinyin(List<String> pinyin1, List<String> pinyin2, boolean exact) {
    	int len1 = pinyin1.size();
    	int len2 = pinyin2.size();
    	
		if (exact) {
			if (len1 != len2) {
				return false;
			}
		} else {
			if (len1 > len2) {
				return false;
			}
		}
		int i;
		for (i = 0; i < len1; i++) {
			String p1 = pinyin1.get(i);
			String p2 = pinyin2.get(i);
			if (p1.length() > p2.length()
                || !p1.equals(p2.substring(0, p1.length()))) {
				break;
			}
		}
		
		if (i == len1) {
			return true;
		}
		return false;
    }
}

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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The filter class for candidates.
 * This class is used for filtering candidates by {link WnnEngine}.
 * 
 * @author Copyright (C) 2009 OMRON SOFTWARE CO., LTD.  All Rights Reserved.
 *
 */
public class CandidateFilter {
    /** Filtering pattern (No filter) */
	public static final int FILTER_NONE = 0x0;
    /** Filtering pattern (Emoji filter) */
	public static final int FILTER_EMOJI = 0x1;

    /** Regular expression pattern for emoji */
    private static final Pattern PATTERN_EMOJI = Pattern.compile("[\uDBB8\uDBB9\uDBBA\uDBBB]");

    /** Current filter type */
	private int mFilter = 0;

	/**
	 * Set specified filter type.
	 * 
	 * @param filter	The filter type
	 * @see jp.co.omronsoft.openwnn.CandidateFilter#FILTER_NONE
	 * @see jp.co.omronsoft.openwnn.CandidateFilter#FILTER_EMOJI
	 */
	public void setFilter(int filter) {
		mFilter = filter;
	}
	
	/**
	 * Checking whether a specified word is filtered.
	 * 
	 * @param word		A word
	 * @return			{@code true} if the word is allowed; {@code false} if the word is denied.
	 */
	public boolean isAllowed(WnnWord word) {
		if (mFilter == 0) {
			return true;
		}
		if ((mFilter & FILTER_EMOJI) != 0) {
			Matcher m = PATTERN_EMOJI.matcher(word.candidate);
			if (m.matches()) {
				return false;
			}
		}		
		return true;
	}
}

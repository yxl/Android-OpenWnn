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

package name.yxl.openwnn.JAJP;

import name.yxl.openwnn.R;
import android.view.View;
import android.view.Window;
import java.util.Comparator;

import name.yxl.openwnn.*;

/**
 * The user dictionary tool class for Japanese IME.
 *
 * @author Copyright (C) 2009 OMRON SOFTWARE CO., LTD.  All Rights Reserved.
 */
public class UserDictionaryToolsListJAJP extends UserDictionaryToolsList {
    /**
     * Constructor
     */
    public UserDictionaryToolsListJAJP() {
        mListViewName = "name.yxl.openwnn.JAJP.UserDictionaryToolsListJAJP";
        mEditViewName = "name.yxl.openwnn.JAJP.UserDictionaryToolsEditJAJP";
        mPackageName  = "name.yxl.openwnn";
    }

    /** @see name.yxl.openwnn.UserDictionaryToolsList#headerCreate */
    @Override protected void headerCreate() {
      getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE,
    		  R.layout.user_dictionary_tools_list_header_ja);
    }

    /** @see name.yxl.openwnn.UserDictionaryToolsList#createUserDictionaryToolsEdit */
    @Override protected UserDictionaryToolsEdit createUserDictionaryToolsEdit(View view1, View view2) {
        return new UserDictionaryToolsEditJAJP(view1, view2);
    }

    /** @see name.yxl.openwnn.UserDictionaryToolsList#sendEventToIME */
    @Override protected boolean sendEventToIME(OpenWnnEvent ev) {
        try {
            return OpenWnnJAJP.getInstance().onEvent(ev);
        } catch (Exception ex) {
            /* do nothing if an error occurs */
        }
        return false;
    }

    /** @see name.yxl.openwnn.UserDictionaryToolsList#getComparator */
    @Override protected Comparator<WnnWord> getComparator() {
    	return new ListComparatorJAJP();
    }

    /** Comparator class for sorting the list of Japanese user dictionary */
    protected class ListComparatorJAJP implements Comparator<WnnWord>{
        public int compare(WnnWord word1, WnnWord word2) {
            return word1.stroke.compareTo(word2.stroke);
        };
    }
}

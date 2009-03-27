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

package jp.co.omronsoft.openwnn.EN;

import jp.co.omronsoft.openwnn.*;
import android.content.Context;
import android.preference.ListPreference;
import android.util.AttributeSet;

/**
 * The preference class of keyboard image list for English IME.
 * This class notices to {@code OpenWnnEN} that the keyboard image is changed.
 * 
 * @author Copyright (C) 2009 OMRON SOFTWARE CO., LTD.
 */
public class KeyboardListPreferenceEN extends ListPreference{
    public KeyboardListPreferenceEN(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    
    public KeyboardListPreferenceEN(Context context) {
        this(context, null);
    }

    /** see android.preference.DialogPreference#onDialogClosed */
    @Override protected void onDialogClosed(boolean positiveResult) {
    	super.onDialogClosed(positiveResult);

    	if (positiveResult) {
        	OpenWnnEN wnn = OpenWnnEN.getInstance();
        	int code = OpenWnnEvent.CHANGE_INPUT_VIEW;
        	OpenWnnEvent ev = new OpenWnnEvent(code);
        	try {
        		wnn.onEvent(ev);
        	} catch (Exception ex) {
        	}   		
    	}
    }
}

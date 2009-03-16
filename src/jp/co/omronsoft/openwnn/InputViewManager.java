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

import android.view.View;
import android.content.SharedPreferences;
import android.view.inputmethod.EditorInfo;

/**
 * The interface of input view manager used by OpenWnn
 *
 * @author Copyright (C) 2009, OMRON SOFTWARE CO., LTD.  All Rights Reserved.
 */
public interface InputViewManager {
    /**
     * Initialize the view
     *
     * @param parent    The OpenWnn object
     * @param width   The width of the display
     * @param height  The height of the display
     *
     * @return The view created in the initialize process; null if cannot create a view.
     */
    public View initView(OpenWnn parent, int width, int height);

    /**
     * Get the view being used currently.
     *
     * @return The view; null if no view is used currently.
     */
    public View getCurrentView();

    /**
     * Notification of updating parent's state
     *
     * @param parent    The OpenWnn object using this manager
     */
    public void onUpdateState(OpenWnn parent);

    /**
     * Set preferences
     *
     * @param pref    The preferences
     * @param editor  The information about the editor
     */
    public void setPreferences(SharedPreferences pref, EditorInfo editor);


    /**
     * Non-display internal views.
     */
    public void closing();
}

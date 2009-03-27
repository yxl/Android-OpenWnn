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

/**
 * The interface of candidates view manager used by {@link OpenWnn}.
 *
 * @author Copyright (C) 2008, 2009 OMRON SOFTWARE CO., LTD.  All Rights Reserved.
 */
public interface CandidatesViewManager {
    /** Size of candidates view (normal) */
    public static final int VIEW_TYPE_NORMAL = 0;
    /** Size of candidates view (full) */
    public static final int VIEW_TYPE_FULL   = 1;
    /** Size of candidates view (close/non-display) */
    public static final int VIEW_TYPE_CLOSE  = 2;

    /**
     * Attribute of a word (no attribute)
     * @see jp.co.omronsoft.openwnn.WnnWord
     */
    public static final int ATTRIBUTE_NONE    = 0;
    /**
     * Attribute of a word (a candidate in the history list)
     * @see jp.co.omronsoft.openwnn.WnnWord
     */
    public static final int ATTRIBUTE_HISTORY = 1;
    /**
     * Attribute of a word (the best candidate)
     * @see jp.co.omronsoft.openwnn.WnnWord
     */
    public static final int ATTRIBUTE_BEST    = 2;
    /**
     * Attribute of a word (auto generated/not in the dictionary)
     * @see jp.co.omronsoft.openwnn.WnnWord
     */
    public static final int ATTRIBUTE_AUTO_GENERATED  = 4;

    /**
     * Initialize the candidates view.
     *
     * @param parent    The OpenWnn object
     * @param width     The width of the display
     * @param height    The height of the display
     *
     * @return The candidates view created in the initialize process; {@code null} if cannot create a candidates view.
     */
    public View initView(OpenWnn parent, int width, int height);

    /**
     * Get the candidates view being used currently.
     *
     * @return The candidates view; {@code null} if no candidates view is used currently.
     */
    public View getCurrentView();

    /**
     * Set the candidates view type.
     *
     * @param type  The candidate view type
     */
    public void setViewType(int type);

    /**
     * Get the candidates view type.
     *
     * @return      The view type
     */
    public int getViewType();

    /**
     * Display candidates.
     *
     * @param converter  The {@link WnnEngine} from which {@link CandidatesViewManager} gets the candidates
     *
     * @see jp.co.omronsoft.openwnn.WnnEngine#getNextCandidate
     */
    public void displayCandidates(WnnEngine converter);

    /**
     * Clear and hide the candidates view.
     */
    public void clearCandidates();

    /**
     * Reflect the preferences in the candidates view.
     *
     * @param pref    The preferences
     */
    public void setPreferences(SharedPreferences pref);
}

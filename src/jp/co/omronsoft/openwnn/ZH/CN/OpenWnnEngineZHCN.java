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

package jp.co.omronsoft.openwnn.ZH.CN;

import jp.co.omronsoft.openwnn.ZH.OpenWnnEngineZH;

/**
 * The OpenWnn engine class for Chinese IME.
 * 
 * @author Copyright (C) 2009 OMRON SOFTWARE CO., LTD.  All Rights Reserved.
 */
public class OpenWnnEngineZHCN extends OpenWnnEngineZH {
    /** Dictionary type (Chinese standard) */
    public static final int DIC_LANG_ZHCN = DIC_LANG_ZH;
    /** Dictionary type (Chinese person's name) */
    public static final int DIC_LANG_ZHCN_PERSON_NAME = DIC_LANG_ZH_PERSON_NAME;
    /** Dictionary type (Chinese EISU-KANA conversion) */
    public static final int DIC_LANG_ZHCN_EISUKANA = DIC_LANG_ZH_EISUKANA;
    /** Dictionary type (Chinese postal address) */
    public static final int DIC_LANG_ZHCN_POSTAL_ADDRESS = DIC_LANG_ZH_POSTAL_ADDRESS;

    /**
     * Constructor
     * 
     * @param dicLib  The dictionary library file name
     * @param dicFilePath The path name of writable dictionary(null if not use)
     */
    public OpenWnnEngineZHCN(String dicLib, String dicFilePath) {
        super(dicLib, dicFilePath);
    }
}


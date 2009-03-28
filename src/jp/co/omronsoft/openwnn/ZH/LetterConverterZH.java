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

import java.util.HashMap;
import jp.co.omronsoft.openwnn.ComposingText;
import jp.co.omronsoft.openwnn.LetterConverter;
import jp.co.omronsoft.openwnn.StrSegment;
import android.content.SharedPreferences;

/**
 * The Half-width symbol to Full-width symbol converter class for Chinese IME.
 *
 * @author Copyright (C) 2009 OMRON SOFTWARE CO., LTD.  All Rights Reserved.
 */
public class LetterConverterZH implements LetterConverter {
    /** HashMap for symbol conversion (Chinese mode) */
    private static final HashMap<String, String> convTable = new HashMap<String, String>() {{
        put("-", "\u30fc"); put(".", "\u3002"); put(",", "\uff0c"); put("?", "\uff1f"); put("/", "\u3001");
        put("@", "\uff20"); put("#", "\uff03"); put("%", "\uff05"); put("&", "\uff06"); put("*", "\u00d7");
        put("+", "\uff0b"); put("=", "\u2014"); put("(", "\uff08"); put(")", "\uff09");
        put("~", "\uff5e"); put("\"", "\u201c"); put("'", "\u2018"); put(":", "\uff1a"); put(";", "\uff1b");
        put("!", "\uff01"); put("^", "\u2026\u2026"); put("\u00a5", "\uffe5"); put("$", "\uff04"); put("[", "\u3010");
        put("]", "\u3011"); put("_", "\u2014\u2014"); put("{", "\u3014"); put("}", "\u3015");
        put("`", "\u00b7"); put("<", "\u300a"); put(">", "\u300b"); put("\\", "\u3001"); put("|", "\u30fb");
        put("1", "\uff11"); put("2", "\uff12"); put("3", "\uff13"); put("4", "\uff14"); put("5", "\uff15");
        put("6", "\uff16"); put("7", "\uff17"); put("8", "\uff18"); put("9", "\uff19"); put("0", "\uff10");        
    }};

    /** HashMap for symbol conversion(when the shift key is pressed) (Chinese mode) */
    private static final HashMap<String, String> convTableShifted = new HashMap<String, String>() {{
        put(".", "\uff0e"); put(",", "\uff0c");
        put("-", "\u30fc"); put("?", "\uff1f"); put("/", "\u3001");
        put("@", "\uff20"); put("#", "\uff03"); put("%", "\uff05"); put("&", "\uff06"); put("*", "\u00d7");
        put("+", "\uff0b"); put("=", "\uff0d"); put("(", "\uff08"); put(")", "\uff09");
        put("~", "\u301c"); put("\"", "\u201d"); put("'", "\u2019"); put(":", "\uff1a"); put(";", "\uff1b");
        put("!", "\uff01"); put("^", "\u2026\u2026"); put("\u00a5", "\uffe5"); put("$", "\uff04"); put("[", "\uff3b");
        put("]", "\uff3d"); put("_", "\u2014\u2014"); put("{", "\u3014"); put("}", "\u3015");
        put("`", "\uff40"); put("<", "\u3008"); put(">", "\u3009"); put("\\", "\u3001"); put("|", "\u30fb");
        put("1", "\uff11"); put("2", "\uff12"); put("3", "\uff13"); put("4", "\uff14"); put("5", "\uff15");
        put("6", "\uff16"); put("7", "\uff17"); put("8", "\uff18"); put("9", "\uff19"); put("0", "\uff10");
    }};

    /***********************************************************************
     * LetterConverter's interface
     ***********************************************************************/
    /**
     * Convert key input to a string in accord with the shift key state.
     * <br>
     * This method is same as {@code convert()},
     *  but changes its behavior by the shift key state.
     *  
     *  @param text		The input text
     *  @param shift	The shift key state. (0:not pressed, 1:otherwise)
     *  @return		{@code true} if success to convert; {@code false} if fail.
     */
    public boolean convert(ComposingText text, int shift) {
        int cursor = text.getCursor(1);
        String match;
        if (cursor <= 0) {
            return false;
        }

        StrSegment[] str = new StrSegment[3];
        int start = 2;
        str[2] = text.getStrSegment(1, cursor - 1);
        if (cursor >= 2) {
            str[1] = text.getStrSegment(1, cursor - 2);
            start = 1;
            if (cursor >= 3) {
                str[0] = text.getStrSegment(1, cursor - 3);
                start = 0;
            }
        }

        StringBuffer key = new StringBuffer();
        while (start < 3) {
            for (int i = start; i < 3; i++) {
                key.append(str[i].string);
            }
            if (shift == 0) {
                match = (String) LetterConverterZH.convTable.get(key.toString().toLowerCase());
            } else {
                match = (String) LetterConverterZH.convTableShifted.get(key.toString().toLowerCase());
            }
            if (match != null) {
                StrSegment[] out;
                if (match.length() == 1) {
                    out = new StrSegment[1];
                    out[0] = new StrSegment(match, str[start].from, str[2].to);
                    text.replaceStrSegment(1, out, 3 - start);
                } else {
                    out = new StrSegment[2];
                    out[0] = new StrSegment(match.substring(0, match.length() - 1), str[start].from, str[2].to - 1);
                    out[1] = new StrSegment(match.substring(match.length() - 1), str[2].to, str[2].to);
                    text.replaceStrSegment(1, out, 3 - start);
                }
                return true;
            }
            start++;
            key.delete(0, key.length());
        }
        return false;
    }

    /** @see jp.co.omronsoft.openwnn.LetterConverter#setPreferences */
    public void setPreferences(SharedPreferences pref) {}

    /** @see jp.co.omronsoft.openwnn.LetterConverter#convert */
    public boolean convert(ComposingText text) {
        return convert(text, 0);
    }
}

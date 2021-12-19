/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.pranav.ide.dx.dex.code;

import com.pranav.ide.dx.dex.DexOptions;
import com.pranav.ide.dx.dex.code.form.Form10t;
import com.pranav.ide.dx.dex.code.form.Form10x;
import com.pranav.ide.dx.dex.code.form.Form11n;
import com.pranav.ide.dx.dex.code.form.Form11x;
import com.pranav.ide.dx.dex.code.form.Form12x;
import com.pranav.ide.dx.dex.code.form.Form20t;
import com.pranav.ide.dx.dex.code.form.Form21c;
import com.pranav.ide.dx.dex.code.form.Form21h;
import com.pranav.ide.dx.dex.code.form.Form21s;
import com.pranav.ide.dx.dex.code.form.Form21t;
import com.pranav.ide.dx.dex.code.form.Form22b;
import com.pranav.ide.dx.dex.code.form.Form22c;
import com.pranav.ide.dx.dex.code.form.Form22s;
import com.pranav.ide.dx.dex.code.form.Form22t;
import com.pranav.ide.dx.dex.code.form.Form22x;
import com.pranav.ide.dx.dex.code.form.Form23x;
import com.pranav.ide.dx.dex.code.form.Form30t;
import com.pranav.ide.dx.dex.code.form.Form31c;
import com.pranav.ide.dx.dex.code.form.Form31i;
import com.pranav.ide.dx.dex.code.form.Form31t;
import com.pranav.ide.dx.dex.code.form.Form32x;
import com.pranav.ide.dx.dex.code.form.Form35c;
import com.pranav.ide.dx.dex.code.form.Form3rc;
import com.pranav.ide.dx.dex.code.form.Form45cc;
import com.pranav.ide.dx.dex.code.form.Form4rcc;
import com.pranav.ide.dx.dex.code.form.Form51l;
import com.pranav.ide.dx.dex.code.form.SpecialFormat;
import com.pranav.ide.dx.io.Opcodes;

/**
 * Standard instances of {@link com.pranav.ide.dx.dex.code.Dop} and utility methods for getting
 * them.
 */
public final class Dops {
    /**
     * pseudo-opcode used for nonstandard formatted "instructions"
     * (which are mostly not actually instructions, though they do
     * appear in instruction lists). TODO: Retire the usage of this
     * constant.
     */
    public static final com.pranav.ide.dx.dex.code.Dop SPECIAL_FORMAT =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.SPECIAL_FORMAT, com.pranav.ide.dx.io.Opcodes.SPECIAL_FORMAT,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, SpecialFormat.THE_ONE, false);
    // BEGIN(dops); GENERATED AUTOMATICALLY BY opcode-gen
    public static final com.pranav.ide.dx.dex.code.Dop NOP =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.NOP, com.pranav.ide.dx.io.Opcodes.NOP,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form10x.THE_ONE, false);
    public static final com.pranav.ide.dx.dex.code.Dop MOVE =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.MOVE, com.pranav.ide.dx.io.Opcodes.MOVE,
                    com.pranav.ide.dx.io.Opcodes.MOVE_FROM16, com.pranav.ide.dx.dex.code.form.Form12x.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop MOVE_FROM16 =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.MOVE_FROM16, com.pranav.ide.dx.io.Opcodes.MOVE,
                    com.pranav.ide.dx.io.Opcodes.MOVE_16, com.pranav.ide.dx.dex.code.form.Form22x.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop MOVE_16 =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.MOVE_16, com.pranav.ide.dx.io.Opcodes.MOVE,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form32x.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop MOVE_WIDE =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.MOVE_WIDE, com.pranav.ide.dx.io.Opcodes.MOVE_WIDE,
                    com.pranav.ide.dx.io.Opcodes.MOVE_WIDE_FROM16, com.pranav.ide.dx.dex.code.form.Form12x.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop MOVE_WIDE_FROM16 =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.MOVE_WIDE_FROM16, com.pranav.ide.dx.io.Opcodes.MOVE_WIDE,
                    com.pranav.ide.dx.io.Opcodes.MOVE_WIDE_16, com.pranav.ide.dx.dex.code.form.Form22x.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop MOVE_WIDE_16 =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.MOVE_WIDE_16, com.pranav.ide.dx.io.Opcodes.MOVE_WIDE,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form32x.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop MOVE_OBJECT =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.MOVE_OBJECT, com.pranav.ide.dx.io.Opcodes.MOVE_OBJECT,
                    com.pranav.ide.dx.io.Opcodes.MOVE_OBJECT_FROM16, com.pranav.ide.dx.dex.code.form.Form12x.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop MOVE_OBJECT_FROM16 =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.MOVE_OBJECT_FROM16, com.pranav.ide.dx.io.Opcodes.MOVE_OBJECT,
                    com.pranav.ide.dx.io.Opcodes.MOVE_OBJECT_16, Form22x.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop MOVE_OBJECT_16 =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.MOVE_OBJECT_16, com.pranav.ide.dx.io.Opcodes.MOVE_OBJECT,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, Form32x.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop MOVE_RESULT =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.MOVE_RESULT, com.pranav.ide.dx.io.Opcodes.MOVE_RESULT,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form11x.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop MOVE_RESULT_WIDE =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.MOVE_RESULT_WIDE, com.pranav.ide.dx.io.Opcodes.MOVE_RESULT_WIDE,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form11x.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop MOVE_RESULT_OBJECT =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.MOVE_RESULT_OBJECT, com.pranav.ide.dx.io.Opcodes.MOVE_RESULT_OBJECT,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form11x.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop MOVE_EXCEPTION =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.MOVE_EXCEPTION, com.pranav.ide.dx.io.Opcodes.MOVE_EXCEPTION,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form11x.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop RETURN_VOID =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.RETURN_VOID, com.pranav.ide.dx.io.Opcodes.RETURN_VOID,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, Form10x.THE_ONE, false);
    public static final com.pranav.ide.dx.dex.code.Dop RETURN =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.RETURN, com.pranav.ide.dx.io.Opcodes.RETURN,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form11x.THE_ONE, false);
    public static final com.pranav.ide.dx.dex.code.Dop RETURN_WIDE =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.RETURN_WIDE, com.pranav.ide.dx.io.Opcodes.RETURN_WIDE,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form11x.THE_ONE, false);
    public static final com.pranav.ide.dx.dex.code.Dop RETURN_OBJECT =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.RETURN_OBJECT, com.pranav.ide.dx.io.Opcodes.RETURN_OBJECT,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form11x.THE_ONE, false);
    public static final com.pranav.ide.dx.dex.code.Dop CONST_4 =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.CONST_4, com.pranav.ide.dx.io.Opcodes.CONST,
                    com.pranav.ide.dx.io.Opcodes.CONST_16, Form11n.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop CONST_16 =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.CONST_16, com.pranav.ide.dx.io.Opcodes.CONST,
                    com.pranav.ide.dx.io.Opcodes.CONST_HIGH16, com.pranav.ide.dx.dex.code.form.Form21s.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop CONST =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.CONST, com.pranav.ide.dx.io.Opcodes.CONST,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form31i.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop CONST_HIGH16 =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.CONST_HIGH16, com.pranav.ide.dx.io.Opcodes.CONST,
                    com.pranav.ide.dx.io.Opcodes.CONST, com.pranav.ide.dx.dex.code.form.Form21h.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop CONST_WIDE_16 =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.CONST_WIDE_16, com.pranav.ide.dx.io.Opcodes.CONST_WIDE,
                    com.pranav.ide.dx.io.Opcodes.CONST_WIDE_HIGH16, Form21s.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop CONST_WIDE_32 =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.CONST_WIDE_32, com.pranav.ide.dx.io.Opcodes.CONST_WIDE,
                    com.pranav.ide.dx.io.Opcodes.CONST_WIDE, Form31i.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop CONST_WIDE =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.CONST_WIDE, com.pranav.ide.dx.io.Opcodes.CONST_WIDE,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, Form51l.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop CONST_WIDE_HIGH16 =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.CONST_WIDE_HIGH16, com.pranav.ide.dx.io.Opcodes.CONST_WIDE,
                    com.pranav.ide.dx.io.Opcodes.CONST_WIDE_32, Form21h.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop CONST_STRING =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.CONST_STRING, com.pranav.ide.dx.io.Opcodes.CONST_STRING,
                    com.pranav.ide.dx.io.Opcodes.CONST_STRING_JUMBO, com.pranav.ide.dx.dex.code.form.Form21c.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop CONST_STRING_JUMBO =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.CONST_STRING_JUMBO, com.pranav.ide.dx.io.Opcodes.CONST_STRING,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, Form31c.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop CONST_CLASS =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.CONST_CLASS, com.pranav.ide.dx.io.Opcodes.CONST_CLASS,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form21c.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop MONITOR_ENTER =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.MONITOR_ENTER, com.pranav.ide.dx.io.Opcodes.MONITOR_ENTER,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form11x.THE_ONE, false);
    public static final com.pranav.ide.dx.dex.code.Dop MONITOR_EXIT =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.MONITOR_EXIT, com.pranav.ide.dx.io.Opcodes.MONITOR_EXIT,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form11x.THE_ONE, false);
    public static final com.pranav.ide.dx.dex.code.Dop CHECK_CAST =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.CHECK_CAST, com.pranav.ide.dx.io.Opcodes.CHECK_CAST,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form21c.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop INSTANCE_OF =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.INSTANCE_OF, com.pranav.ide.dx.io.Opcodes.INSTANCE_OF,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form22c.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop ARRAY_LENGTH =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.ARRAY_LENGTH, com.pranav.ide.dx.io.Opcodes.ARRAY_LENGTH,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form12x.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop NEW_INSTANCE =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.NEW_INSTANCE, com.pranav.ide.dx.io.Opcodes.NEW_INSTANCE,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form21c.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop NEW_ARRAY =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.NEW_ARRAY, com.pranav.ide.dx.io.Opcodes.NEW_ARRAY,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form22c.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop FILLED_NEW_ARRAY =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.FILLED_NEW_ARRAY, com.pranav.ide.dx.io.Opcodes.FILLED_NEW_ARRAY,
                    com.pranav.ide.dx.io.Opcodes.FILLED_NEW_ARRAY_RANGE, com.pranav.ide.dx.dex.code.form.Form35c.THE_ONE, false);
    public static final com.pranav.ide.dx.dex.code.Dop FILLED_NEW_ARRAY_RANGE =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.FILLED_NEW_ARRAY_RANGE, com.pranav.ide.dx.io.Opcodes.FILLED_NEW_ARRAY,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form3rc.THE_ONE, false);
    public static final com.pranav.ide.dx.dex.code.Dop FILL_ARRAY_DATA =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.FILL_ARRAY_DATA, com.pranav.ide.dx.io.Opcodes.FILL_ARRAY_DATA,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form31t.THE_ONE, false);
    public static final com.pranav.ide.dx.dex.code.Dop THROW =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.THROW, com.pranav.ide.dx.io.Opcodes.THROW,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, Form11x.THE_ONE, false);
    public static final com.pranav.ide.dx.dex.code.Dop GOTO =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.GOTO, com.pranav.ide.dx.io.Opcodes.GOTO,
                    com.pranav.ide.dx.io.Opcodes.GOTO_16, Form10t.THE_ONE, false);
    public static final com.pranav.ide.dx.dex.code.Dop GOTO_16 =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.GOTO_16, com.pranav.ide.dx.io.Opcodes.GOTO,
                    com.pranav.ide.dx.io.Opcodes.GOTO_32, Form20t.THE_ONE, false);
    public static final com.pranav.ide.dx.dex.code.Dop GOTO_32 =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.GOTO_32, com.pranav.ide.dx.io.Opcodes.GOTO,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, Form30t.THE_ONE, false);
    public static final com.pranav.ide.dx.dex.code.Dop PACKED_SWITCH =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.PACKED_SWITCH, com.pranav.ide.dx.io.Opcodes.PACKED_SWITCH,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form31t.THE_ONE, false);
    public static final com.pranav.ide.dx.dex.code.Dop SPARSE_SWITCH =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.SPARSE_SWITCH, com.pranav.ide.dx.io.Opcodes.SPARSE_SWITCH,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, Form31t.THE_ONE, false);
    public static final com.pranav.ide.dx.dex.code.Dop CMPL_FLOAT =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.CMPL_FLOAT, com.pranav.ide.dx.io.Opcodes.CMPL_FLOAT,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form23x.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop CMPG_FLOAT =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.CMPG_FLOAT, com.pranav.ide.dx.io.Opcodes.CMPG_FLOAT,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form23x.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop CMPL_DOUBLE =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.CMPL_DOUBLE, com.pranav.ide.dx.io.Opcodes.CMPL_DOUBLE,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form23x.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop CMPG_DOUBLE =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.CMPG_DOUBLE, com.pranav.ide.dx.io.Opcodes.CMPG_DOUBLE,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form23x.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop CMP_LONG =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.CMP_LONG, com.pranav.ide.dx.io.Opcodes.CMP_LONG,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form23x.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop IF_EQ =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.IF_EQ, com.pranav.ide.dx.io.Opcodes.IF_EQ,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form22t.THE_ONE, false);
    public static final com.pranav.ide.dx.dex.code.Dop IF_NE =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.IF_NE, com.pranav.ide.dx.io.Opcodes.IF_NE,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form22t.THE_ONE, false);
    public static final com.pranav.ide.dx.dex.code.Dop IF_LT =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.IF_LT, com.pranav.ide.dx.io.Opcodes.IF_LT,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form22t.THE_ONE, false);
    public static final com.pranav.ide.dx.dex.code.Dop IF_GE =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.IF_GE, com.pranav.ide.dx.io.Opcodes.IF_GE,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form22t.THE_ONE, false);
    public static final com.pranav.ide.dx.dex.code.Dop IF_GT =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.IF_GT, com.pranav.ide.dx.io.Opcodes.IF_GT,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form22t.THE_ONE, false);
    public static final com.pranav.ide.dx.dex.code.Dop IF_LE =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.IF_LE, com.pranav.ide.dx.io.Opcodes.IF_LE,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, Form22t.THE_ONE, false);
    public static final com.pranav.ide.dx.dex.code.Dop IF_EQZ =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.IF_EQZ, com.pranav.ide.dx.io.Opcodes.IF_EQZ,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form21t.THE_ONE, false);
    public static final com.pranav.ide.dx.dex.code.Dop IF_NEZ =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.IF_NEZ, com.pranav.ide.dx.io.Opcodes.IF_NEZ,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form21t.THE_ONE, false);
    public static final com.pranav.ide.dx.dex.code.Dop IF_LTZ =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.IF_LTZ, com.pranav.ide.dx.io.Opcodes.IF_LTZ,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form21t.THE_ONE, false);
    public static final com.pranav.ide.dx.dex.code.Dop IF_GEZ =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.IF_GEZ, com.pranav.ide.dx.io.Opcodes.IF_GEZ,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form21t.THE_ONE, false);
    public static final com.pranav.ide.dx.dex.code.Dop IF_GTZ =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.IF_GTZ, com.pranav.ide.dx.io.Opcodes.IF_GTZ,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form21t.THE_ONE, false);
    public static final com.pranav.ide.dx.dex.code.Dop IF_LEZ =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.IF_LEZ, com.pranav.ide.dx.io.Opcodes.IF_LEZ,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, Form21t.THE_ONE, false);
    public static final com.pranav.ide.dx.dex.code.Dop AGET =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.AGET, com.pranav.ide.dx.io.Opcodes.AGET,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form23x.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop AGET_WIDE =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.AGET_WIDE, com.pranav.ide.dx.io.Opcodes.AGET_WIDE,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form23x.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop AGET_OBJECT =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.AGET_OBJECT, com.pranav.ide.dx.io.Opcodes.AGET_OBJECT,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form23x.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop AGET_BOOLEAN =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.AGET_BOOLEAN, com.pranav.ide.dx.io.Opcodes.AGET_BOOLEAN,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form23x.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop AGET_BYTE =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.AGET_BYTE, com.pranav.ide.dx.io.Opcodes.AGET_BYTE,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form23x.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop AGET_CHAR =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.AGET_CHAR, com.pranav.ide.dx.io.Opcodes.AGET_CHAR,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form23x.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop AGET_SHORT =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.AGET_SHORT, com.pranav.ide.dx.io.Opcodes.AGET_SHORT,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form23x.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop APUT =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.APUT, com.pranav.ide.dx.io.Opcodes.APUT,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form23x.THE_ONE, false);
    public static final com.pranav.ide.dx.dex.code.Dop APUT_WIDE =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.APUT_WIDE, com.pranav.ide.dx.io.Opcodes.APUT_WIDE,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form23x.THE_ONE, false);
    public static final com.pranav.ide.dx.dex.code.Dop APUT_OBJECT =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.APUT_OBJECT, com.pranav.ide.dx.io.Opcodes.APUT_OBJECT,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form23x.THE_ONE, false);
    public static final com.pranav.ide.dx.dex.code.Dop APUT_BOOLEAN =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.APUT_BOOLEAN, com.pranav.ide.dx.io.Opcodes.APUT_BOOLEAN,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form23x.THE_ONE, false);
    public static final com.pranav.ide.dx.dex.code.Dop APUT_BYTE =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.APUT_BYTE, com.pranav.ide.dx.io.Opcodes.APUT_BYTE,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form23x.THE_ONE, false);
    public static final com.pranav.ide.dx.dex.code.Dop APUT_CHAR =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.APUT_CHAR, com.pranav.ide.dx.io.Opcodes.APUT_CHAR,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form23x.THE_ONE, false);
    public static final com.pranav.ide.dx.dex.code.Dop APUT_SHORT =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.APUT_SHORT, com.pranav.ide.dx.io.Opcodes.APUT_SHORT,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form23x.THE_ONE, false);
    public static final com.pranav.ide.dx.dex.code.Dop IGET =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.IGET, com.pranav.ide.dx.io.Opcodes.IGET,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form22c.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop IGET_WIDE =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.IGET_WIDE, com.pranav.ide.dx.io.Opcodes.IGET_WIDE,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form22c.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop IGET_OBJECT =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.IGET_OBJECT, com.pranav.ide.dx.io.Opcodes.IGET_OBJECT,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form22c.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop IGET_BOOLEAN =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.IGET_BOOLEAN, com.pranav.ide.dx.io.Opcodes.IGET_BOOLEAN,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form22c.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop IGET_BYTE =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.IGET_BYTE, com.pranav.ide.dx.io.Opcodes.IGET_BYTE,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form22c.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop IGET_CHAR =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.IGET_CHAR, com.pranav.ide.dx.io.Opcodes.IGET_CHAR,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form22c.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop IGET_SHORT =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.IGET_SHORT, com.pranav.ide.dx.io.Opcodes.IGET_SHORT,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form22c.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop IPUT =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.IPUT, com.pranav.ide.dx.io.Opcodes.IPUT,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form22c.THE_ONE, false);
    public static final com.pranav.ide.dx.dex.code.Dop IPUT_WIDE =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.IPUT_WIDE, com.pranav.ide.dx.io.Opcodes.IPUT_WIDE,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form22c.THE_ONE, false);
    public static final com.pranav.ide.dx.dex.code.Dop IPUT_OBJECT =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.IPUT_OBJECT, com.pranav.ide.dx.io.Opcodes.IPUT_OBJECT,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form22c.THE_ONE, false);
    public static final com.pranav.ide.dx.dex.code.Dop IPUT_BOOLEAN =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.IPUT_BOOLEAN, com.pranav.ide.dx.io.Opcodes.IPUT_BOOLEAN,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form22c.THE_ONE, false);
    public static final com.pranav.ide.dx.dex.code.Dop IPUT_BYTE =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.IPUT_BYTE, com.pranav.ide.dx.io.Opcodes.IPUT_BYTE,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form22c.THE_ONE, false);
    public static final com.pranav.ide.dx.dex.code.Dop IPUT_CHAR =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.IPUT_CHAR, com.pranav.ide.dx.io.Opcodes.IPUT_CHAR,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form22c.THE_ONE, false);
    public static final com.pranav.ide.dx.dex.code.Dop IPUT_SHORT =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.IPUT_SHORT, com.pranav.ide.dx.io.Opcodes.IPUT_SHORT,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, Form22c.THE_ONE, false);
    public static final com.pranav.ide.dx.dex.code.Dop SGET =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.SGET, com.pranav.ide.dx.io.Opcodes.SGET,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form21c.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop SGET_WIDE =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.SGET_WIDE, com.pranav.ide.dx.io.Opcodes.SGET_WIDE,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form21c.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop SGET_OBJECT =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.SGET_OBJECT, com.pranav.ide.dx.io.Opcodes.SGET_OBJECT,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form21c.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop SGET_BOOLEAN =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.SGET_BOOLEAN, com.pranav.ide.dx.io.Opcodes.SGET_BOOLEAN,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form21c.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop SGET_BYTE =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.SGET_BYTE, com.pranav.ide.dx.io.Opcodes.SGET_BYTE,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form21c.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop SGET_CHAR =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.SGET_CHAR, com.pranav.ide.dx.io.Opcodes.SGET_CHAR,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form21c.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop SGET_SHORT =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.SGET_SHORT, com.pranav.ide.dx.io.Opcodes.SGET_SHORT,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form21c.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop SPUT =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.SPUT, com.pranav.ide.dx.io.Opcodes.SPUT,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form21c.THE_ONE, false);
    public static final com.pranav.ide.dx.dex.code.Dop SPUT_WIDE =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.SPUT_WIDE, com.pranav.ide.dx.io.Opcodes.SPUT_WIDE,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form21c.THE_ONE, false);
    public static final com.pranav.ide.dx.dex.code.Dop SPUT_OBJECT =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.SPUT_OBJECT, com.pranav.ide.dx.io.Opcodes.SPUT_OBJECT,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form21c.THE_ONE, false);
    public static final com.pranav.ide.dx.dex.code.Dop SPUT_BOOLEAN =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.SPUT_BOOLEAN, com.pranav.ide.dx.io.Opcodes.SPUT_BOOLEAN,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form21c.THE_ONE, false);
    public static final com.pranav.ide.dx.dex.code.Dop SPUT_BYTE =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.SPUT_BYTE, com.pranav.ide.dx.io.Opcodes.SPUT_BYTE,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form21c.THE_ONE, false);
    public static final com.pranav.ide.dx.dex.code.Dop SPUT_CHAR =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.SPUT_CHAR, com.pranav.ide.dx.io.Opcodes.SPUT_CHAR,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form21c.THE_ONE, false);
    public static final com.pranav.ide.dx.dex.code.Dop SPUT_SHORT =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.SPUT_SHORT, com.pranav.ide.dx.io.Opcodes.SPUT_SHORT,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form21c.THE_ONE, false);
    public static final com.pranav.ide.dx.dex.code.Dop INVOKE_VIRTUAL =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.INVOKE_VIRTUAL, com.pranav.ide.dx.io.Opcodes.INVOKE_VIRTUAL,
                    com.pranav.ide.dx.io.Opcodes.INVOKE_VIRTUAL_RANGE, com.pranav.ide.dx.dex.code.form.Form35c.THE_ONE, false);
    public static final com.pranav.ide.dx.dex.code.Dop INVOKE_SUPER =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.INVOKE_SUPER, com.pranav.ide.dx.io.Opcodes.INVOKE_SUPER,
                    com.pranav.ide.dx.io.Opcodes.INVOKE_SUPER_RANGE, com.pranav.ide.dx.dex.code.form.Form35c.THE_ONE, false);
    public static final com.pranav.ide.dx.dex.code.Dop INVOKE_DIRECT =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.INVOKE_DIRECT, com.pranav.ide.dx.io.Opcodes.INVOKE_DIRECT,
                    com.pranav.ide.dx.io.Opcodes.INVOKE_DIRECT_RANGE, com.pranav.ide.dx.dex.code.form.Form35c.THE_ONE, false);
    public static final com.pranav.ide.dx.dex.code.Dop INVOKE_STATIC =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.INVOKE_STATIC, com.pranav.ide.dx.io.Opcodes.INVOKE_STATIC,
                    com.pranav.ide.dx.io.Opcodes.INVOKE_STATIC_RANGE, com.pranav.ide.dx.dex.code.form.Form35c.THE_ONE, false);
    public static final com.pranav.ide.dx.dex.code.Dop INVOKE_INTERFACE =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.INVOKE_INTERFACE, com.pranav.ide.dx.io.Opcodes.INVOKE_INTERFACE,
                    com.pranav.ide.dx.io.Opcodes.INVOKE_INTERFACE_RANGE, com.pranav.ide.dx.dex.code.form.Form35c.THE_ONE, false);
    public static final com.pranav.ide.dx.dex.code.Dop INVOKE_VIRTUAL_RANGE =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.INVOKE_VIRTUAL_RANGE, com.pranav.ide.dx.io.Opcodes.INVOKE_VIRTUAL,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form3rc.THE_ONE, false);
    public static final com.pranav.ide.dx.dex.code.Dop INVOKE_SUPER_RANGE =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.INVOKE_SUPER_RANGE, com.pranav.ide.dx.io.Opcodes.INVOKE_SUPER,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form3rc.THE_ONE, false);
    public static final com.pranav.ide.dx.dex.code.Dop INVOKE_DIRECT_RANGE =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.INVOKE_DIRECT_RANGE, com.pranav.ide.dx.io.Opcodes.INVOKE_DIRECT,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form3rc.THE_ONE, false);
    public static final com.pranav.ide.dx.dex.code.Dop INVOKE_STATIC_RANGE =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.INVOKE_STATIC_RANGE, com.pranav.ide.dx.io.Opcodes.INVOKE_STATIC,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form3rc.THE_ONE, false);
    public static final com.pranav.ide.dx.dex.code.Dop INVOKE_INTERFACE_RANGE =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.INVOKE_INTERFACE_RANGE, com.pranav.ide.dx.io.Opcodes.INVOKE_INTERFACE,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form3rc.THE_ONE, false);
    public static final com.pranav.ide.dx.dex.code.Dop NEG_INT =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.NEG_INT, com.pranav.ide.dx.io.Opcodes.NEG_INT,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form12x.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop NOT_INT =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.NOT_INT, com.pranav.ide.dx.io.Opcodes.NOT_INT,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form12x.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop NEG_LONG =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.NEG_LONG, com.pranav.ide.dx.io.Opcodes.NEG_LONG,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form12x.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop NOT_LONG =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.NOT_LONG, com.pranav.ide.dx.io.Opcodes.NOT_LONG,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form12x.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop NEG_FLOAT =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.NEG_FLOAT, com.pranav.ide.dx.io.Opcodes.NEG_FLOAT,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form12x.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop NEG_DOUBLE =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.NEG_DOUBLE, com.pranav.ide.dx.io.Opcodes.NEG_DOUBLE,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form12x.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop INT_TO_LONG =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.INT_TO_LONG, com.pranav.ide.dx.io.Opcodes.INT_TO_LONG,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form12x.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop INT_TO_FLOAT =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.INT_TO_FLOAT, com.pranav.ide.dx.io.Opcodes.INT_TO_FLOAT,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form12x.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop INT_TO_DOUBLE =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.INT_TO_DOUBLE, com.pranav.ide.dx.io.Opcodes.INT_TO_DOUBLE,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form12x.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop LONG_TO_INT =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.LONG_TO_INT, com.pranav.ide.dx.io.Opcodes.LONG_TO_INT,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form12x.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop LONG_TO_FLOAT =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.LONG_TO_FLOAT, com.pranav.ide.dx.io.Opcodes.LONG_TO_FLOAT,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form12x.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop LONG_TO_DOUBLE =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.LONG_TO_DOUBLE, com.pranav.ide.dx.io.Opcodes.LONG_TO_DOUBLE,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form12x.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop FLOAT_TO_INT =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.FLOAT_TO_INT, com.pranav.ide.dx.io.Opcodes.FLOAT_TO_INT,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form12x.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop FLOAT_TO_LONG =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.FLOAT_TO_LONG, com.pranav.ide.dx.io.Opcodes.FLOAT_TO_LONG,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form12x.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop FLOAT_TO_DOUBLE =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.FLOAT_TO_DOUBLE, com.pranav.ide.dx.io.Opcodes.FLOAT_TO_DOUBLE,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form12x.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop DOUBLE_TO_INT =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.DOUBLE_TO_INT, com.pranav.ide.dx.io.Opcodes.DOUBLE_TO_INT,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form12x.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop DOUBLE_TO_LONG =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.DOUBLE_TO_LONG, com.pranav.ide.dx.io.Opcodes.DOUBLE_TO_LONG,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form12x.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop DOUBLE_TO_FLOAT =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.DOUBLE_TO_FLOAT, com.pranav.ide.dx.io.Opcodes.DOUBLE_TO_FLOAT,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form12x.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop INT_TO_BYTE =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.INT_TO_BYTE, com.pranav.ide.dx.io.Opcodes.INT_TO_BYTE,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form12x.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop INT_TO_CHAR =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.INT_TO_CHAR, com.pranav.ide.dx.io.Opcodes.INT_TO_CHAR,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form12x.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop INT_TO_SHORT =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.INT_TO_SHORT, com.pranav.ide.dx.io.Opcodes.INT_TO_SHORT,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form12x.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop ADD_INT =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.ADD_INT, com.pranav.ide.dx.io.Opcodes.ADD_INT,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form23x.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop SUB_INT =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.SUB_INT, com.pranav.ide.dx.io.Opcodes.SUB_INT,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form23x.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop MUL_INT =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.MUL_INT, com.pranav.ide.dx.io.Opcodes.MUL_INT,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form23x.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop DIV_INT =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.DIV_INT, com.pranav.ide.dx.io.Opcodes.DIV_INT,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form23x.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop REM_INT =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.REM_INT, com.pranav.ide.dx.io.Opcodes.REM_INT,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form23x.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop AND_INT =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.AND_INT, com.pranav.ide.dx.io.Opcodes.AND_INT,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form23x.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop OR_INT =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.OR_INT, com.pranav.ide.dx.io.Opcodes.OR_INT,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form23x.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop XOR_INT =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.XOR_INT, com.pranav.ide.dx.io.Opcodes.XOR_INT,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form23x.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop SHL_INT =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.SHL_INT, com.pranav.ide.dx.io.Opcodes.SHL_INT,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form23x.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop SHR_INT =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.SHR_INT, com.pranav.ide.dx.io.Opcodes.SHR_INT,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form23x.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop USHR_INT =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.USHR_INT, com.pranav.ide.dx.io.Opcodes.USHR_INT,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form23x.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop ADD_LONG =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.ADD_LONG, com.pranav.ide.dx.io.Opcodes.ADD_LONG,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form23x.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop SUB_LONG =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.SUB_LONG, com.pranav.ide.dx.io.Opcodes.SUB_LONG,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form23x.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop MUL_LONG =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.MUL_LONG, com.pranav.ide.dx.io.Opcodes.MUL_LONG,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form23x.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop DIV_LONG =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.DIV_LONG, com.pranav.ide.dx.io.Opcodes.DIV_LONG,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form23x.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop REM_LONG =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.REM_LONG, com.pranav.ide.dx.io.Opcodes.REM_LONG,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form23x.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop AND_LONG =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.AND_LONG, com.pranav.ide.dx.io.Opcodes.AND_LONG,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form23x.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop OR_LONG =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.OR_LONG, com.pranav.ide.dx.io.Opcodes.OR_LONG,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form23x.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop XOR_LONG =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.XOR_LONG, com.pranav.ide.dx.io.Opcodes.XOR_LONG,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form23x.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop SHL_LONG =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.SHL_LONG, com.pranav.ide.dx.io.Opcodes.SHL_LONG,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form23x.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop SHR_LONG =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.SHR_LONG, com.pranav.ide.dx.io.Opcodes.SHR_LONG,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form23x.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop USHR_LONG =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.USHR_LONG, com.pranav.ide.dx.io.Opcodes.USHR_LONG,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form23x.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop ADD_FLOAT =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.ADD_FLOAT, com.pranav.ide.dx.io.Opcodes.ADD_FLOAT,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form23x.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop SUB_FLOAT =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.SUB_FLOAT, com.pranav.ide.dx.io.Opcodes.SUB_FLOAT,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form23x.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop MUL_FLOAT =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.MUL_FLOAT, com.pranav.ide.dx.io.Opcodes.MUL_FLOAT,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form23x.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop DIV_FLOAT =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.DIV_FLOAT, com.pranav.ide.dx.io.Opcodes.DIV_FLOAT,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form23x.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop REM_FLOAT =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.REM_FLOAT, com.pranav.ide.dx.io.Opcodes.REM_FLOAT,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form23x.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop ADD_DOUBLE =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.ADD_DOUBLE, com.pranav.ide.dx.io.Opcodes.ADD_DOUBLE,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form23x.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop SUB_DOUBLE =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.SUB_DOUBLE, com.pranav.ide.dx.io.Opcodes.SUB_DOUBLE,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form23x.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop MUL_DOUBLE =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.MUL_DOUBLE, com.pranav.ide.dx.io.Opcodes.MUL_DOUBLE,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form23x.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop DIV_DOUBLE =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.DIV_DOUBLE, com.pranav.ide.dx.io.Opcodes.DIV_DOUBLE,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form23x.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop REM_DOUBLE =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.REM_DOUBLE, com.pranav.ide.dx.io.Opcodes.REM_DOUBLE,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, Form23x.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop ADD_INT_2ADDR =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.ADD_INT_2ADDR, com.pranav.ide.dx.io.Opcodes.ADD_INT,
                    com.pranav.ide.dx.io.Opcodes.ADD_INT, com.pranav.ide.dx.dex.code.form.Form12x.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop SUB_INT_2ADDR =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.SUB_INT_2ADDR, com.pranav.ide.dx.io.Opcodes.SUB_INT,
                    com.pranav.ide.dx.io.Opcodes.SUB_INT, com.pranav.ide.dx.dex.code.form.Form12x.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop MUL_INT_2ADDR =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.MUL_INT_2ADDR, com.pranav.ide.dx.io.Opcodes.MUL_INT,
                    com.pranav.ide.dx.io.Opcodes.MUL_INT, com.pranav.ide.dx.dex.code.form.Form12x.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop DIV_INT_2ADDR =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.DIV_INT_2ADDR, com.pranav.ide.dx.io.Opcodes.DIV_INT,
                    com.pranav.ide.dx.io.Opcodes.DIV_INT, com.pranav.ide.dx.dex.code.form.Form12x.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop REM_INT_2ADDR =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.REM_INT_2ADDR, com.pranav.ide.dx.io.Opcodes.REM_INT,
                    com.pranav.ide.dx.io.Opcodes.REM_INT, com.pranav.ide.dx.dex.code.form.Form12x.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop AND_INT_2ADDR =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.AND_INT_2ADDR, com.pranav.ide.dx.io.Opcodes.AND_INT,
                    com.pranav.ide.dx.io.Opcodes.AND_INT, com.pranav.ide.dx.dex.code.form.Form12x.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop OR_INT_2ADDR =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.OR_INT_2ADDR, com.pranav.ide.dx.io.Opcodes.OR_INT,
                    com.pranav.ide.dx.io.Opcodes.OR_INT, com.pranav.ide.dx.dex.code.form.Form12x.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop XOR_INT_2ADDR =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.XOR_INT_2ADDR, com.pranav.ide.dx.io.Opcodes.XOR_INT,
                    com.pranav.ide.dx.io.Opcodes.XOR_INT, com.pranav.ide.dx.dex.code.form.Form12x.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop SHL_INT_2ADDR =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.SHL_INT_2ADDR, com.pranav.ide.dx.io.Opcodes.SHL_INT,
                    com.pranav.ide.dx.io.Opcodes.SHL_INT, com.pranav.ide.dx.dex.code.form.Form12x.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop SHR_INT_2ADDR =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.SHR_INT_2ADDR, com.pranav.ide.dx.io.Opcodes.SHR_INT,
                    com.pranav.ide.dx.io.Opcodes.SHR_INT, com.pranav.ide.dx.dex.code.form.Form12x.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop USHR_INT_2ADDR =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.USHR_INT_2ADDR, com.pranav.ide.dx.io.Opcodes.USHR_INT,
                    com.pranav.ide.dx.io.Opcodes.USHR_INT, com.pranav.ide.dx.dex.code.form.Form12x.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop ADD_LONG_2ADDR =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.ADD_LONG_2ADDR, com.pranav.ide.dx.io.Opcodes.ADD_LONG,
                    com.pranav.ide.dx.io.Opcodes.ADD_LONG, com.pranav.ide.dx.dex.code.form.Form12x.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop SUB_LONG_2ADDR =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.SUB_LONG_2ADDR, com.pranav.ide.dx.io.Opcodes.SUB_LONG,
                    com.pranav.ide.dx.io.Opcodes.SUB_LONG, com.pranav.ide.dx.dex.code.form.Form12x.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop MUL_LONG_2ADDR =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.MUL_LONG_2ADDR, com.pranav.ide.dx.io.Opcodes.MUL_LONG,
                    com.pranav.ide.dx.io.Opcodes.MUL_LONG, com.pranav.ide.dx.dex.code.form.Form12x.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop DIV_LONG_2ADDR =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.DIV_LONG_2ADDR, com.pranav.ide.dx.io.Opcodes.DIV_LONG,
                    com.pranav.ide.dx.io.Opcodes.DIV_LONG, com.pranav.ide.dx.dex.code.form.Form12x.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop REM_LONG_2ADDR =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.REM_LONG_2ADDR, com.pranav.ide.dx.io.Opcodes.REM_LONG,
                    com.pranav.ide.dx.io.Opcodes.REM_LONG, com.pranav.ide.dx.dex.code.form.Form12x.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop AND_LONG_2ADDR =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.AND_LONG_2ADDR, com.pranav.ide.dx.io.Opcodes.AND_LONG,
                    com.pranav.ide.dx.io.Opcodes.AND_LONG, com.pranav.ide.dx.dex.code.form.Form12x.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop OR_LONG_2ADDR =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.OR_LONG_2ADDR, com.pranav.ide.dx.io.Opcodes.OR_LONG,
                    com.pranav.ide.dx.io.Opcodes.OR_LONG, com.pranav.ide.dx.dex.code.form.Form12x.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop XOR_LONG_2ADDR =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.XOR_LONG_2ADDR, com.pranav.ide.dx.io.Opcodes.XOR_LONG,
                    com.pranav.ide.dx.io.Opcodes.XOR_LONG, com.pranav.ide.dx.dex.code.form.Form12x.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop SHL_LONG_2ADDR =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.SHL_LONG_2ADDR, com.pranav.ide.dx.io.Opcodes.SHL_LONG,
                    com.pranav.ide.dx.io.Opcodes.SHL_LONG, com.pranav.ide.dx.dex.code.form.Form12x.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop SHR_LONG_2ADDR =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.SHR_LONG_2ADDR, com.pranav.ide.dx.io.Opcodes.SHR_LONG,
                    com.pranav.ide.dx.io.Opcodes.SHR_LONG, com.pranav.ide.dx.dex.code.form.Form12x.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop USHR_LONG_2ADDR =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.USHR_LONG_2ADDR, com.pranav.ide.dx.io.Opcodes.USHR_LONG,
                    com.pranav.ide.dx.io.Opcodes.USHR_LONG, com.pranav.ide.dx.dex.code.form.Form12x.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop ADD_FLOAT_2ADDR =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.ADD_FLOAT_2ADDR, com.pranav.ide.dx.io.Opcodes.ADD_FLOAT,
                    com.pranav.ide.dx.io.Opcodes.ADD_FLOAT, com.pranav.ide.dx.dex.code.form.Form12x.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop SUB_FLOAT_2ADDR =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.SUB_FLOAT_2ADDR, com.pranav.ide.dx.io.Opcodes.SUB_FLOAT,
                    com.pranav.ide.dx.io.Opcodes.SUB_FLOAT, com.pranav.ide.dx.dex.code.form.Form12x.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop MUL_FLOAT_2ADDR =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.MUL_FLOAT_2ADDR, com.pranav.ide.dx.io.Opcodes.MUL_FLOAT,
                    com.pranav.ide.dx.io.Opcodes.MUL_FLOAT, com.pranav.ide.dx.dex.code.form.Form12x.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop DIV_FLOAT_2ADDR =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.DIV_FLOAT_2ADDR, com.pranav.ide.dx.io.Opcodes.DIV_FLOAT,
                    com.pranav.ide.dx.io.Opcodes.DIV_FLOAT, com.pranav.ide.dx.dex.code.form.Form12x.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop REM_FLOAT_2ADDR =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.REM_FLOAT_2ADDR, com.pranav.ide.dx.io.Opcodes.REM_FLOAT,
                    com.pranav.ide.dx.io.Opcodes.REM_FLOAT, com.pranav.ide.dx.dex.code.form.Form12x.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop ADD_DOUBLE_2ADDR =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.ADD_DOUBLE_2ADDR, com.pranav.ide.dx.io.Opcodes.ADD_DOUBLE,
                    com.pranav.ide.dx.io.Opcodes.ADD_DOUBLE, com.pranav.ide.dx.dex.code.form.Form12x.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop SUB_DOUBLE_2ADDR =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.SUB_DOUBLE_2ADDR, com.pranav.ide.dx.io.Opcodes.SUB_DOUBLE,
                    com.pranav.ide.dx.io.Opcodes.SUB_DOUBLE, com.pranav.ide.dx.dex.code.form.Form12x.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop MUL_DOUBLE_2ADDR =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.MUL_DOUBLE_2ADDR, com.pranav.ide.dx.io.Opcodes.MUL_DOUBLE,
                    com.pranav.ide.dx.io.Opcodes.MUL_DOUBLE, com.pranav.ide.dx.dex.code.form.Form12x.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop DIV_DOUBLE_2ADDR =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.DIV_DOUBLE_2ADDR, com.pranav.ide.dx.io.Opcodes.DIV_DOUBLE,
                    com.pranav.ide.dx.io.Opcodes.DIV_DOUBLE, com.pranav.ide.dx.dex.code.form.Form12x.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop REM_DOUBLE_2ADDR =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.REM_DOUBLE_2ADDR, com.pranav.ide.dx.io.Opcodes.REM_DOUBLE,
                    com.pranav.ide.dx.io.Opcodes.REM_DOUBLE, Form12x.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop ADD_INT_LIT16 =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.ADD_INT_LIT16, com.pranav.ide.dx.io.Opcodes.ADD_INT,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form22s.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop RSUB_INT =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.RSUB_INT, com.pranav.ide.dx.io.Opcodes.RSUB_INT,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form22s.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop MUL_INT_LIT16 =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.MUL_INT_LIT16, com.pranav.ide.dx.io.Opcodes.MUL_INT,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form22s.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop DIV_INT_LIT16 =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.DIV_INT_LIT16, com.pranav.ide.dx.io.Opcodes.DIV_INT,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form22s.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop REM_INT_LIT16 =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.REM_INT_LIT16, com.pranav.ide.dx.io.Opcodes.REM_INT,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form22s.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop AND_INT_LIT16 =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.AND_INT_LIT16, com.pranav.ide.dx.io.Opcodes.AND_INT,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form22s.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop OR_INT_LIT16 =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.OR_INT_LIT16, com.pranav.ide.dx.io.Opcodes.OR_INT,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form22s.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop XOR_INT_LIT16 =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.XOR_INT_LIT16, com.pranav.ide.dx.io.Opcodes.XOR_INT,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, Form22s.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop ADD_INT_LIT8 =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.ADD_INT_LIT8, com.pranav.ide.dx.io.Opcodes.ADD_INT,
                    com.pranav.ide.dx.io.Opcodes.ADD_INT_LIT16, com.pranav.ide.dx.dex.code.form.Form22b.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop RSUB_INT_LIT8 =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.RSUB_INT_LIT8, com.pranav.ide.dx.io.Opcodes.RSUB_INT,
                    com.pranav.ide.dx.io.Opcodes.RSUB_INT, com.pranav.ide.dx.dex.code.form.Form22b.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop MUL_INT_LIT8 =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.MUL_INT_LIT8, com.pranav.ide.dx.io.Opcodes.MUL_INT,
                    com.pranav.ide.dx.io.Opcodes.MUL_INT_LIT16, com.pranav.ide.dx.dex.code.form.Form22b.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop DIV_INT_LIT8 =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.DIV_INT_LIT8, com.pranav.ide.dx.io.Opcodes.DIV_INT,
                    com.pranav.ide.dx.io.Opcodes.DIV_INT_LIT16, com.pranav.ide.dx.dex.code.form.Form22b.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop REM_INT_LIT8 =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.REM_INT_LIT8, com.pranav.ide.dx.io.Opcodes.REM_INT,
                    com.pranav.ide.dx.io.Opcodes.REM_INT_LIT16, com.pranav.ide.dx.dex.code.form.Form22b.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop AND_INT_LIT8 =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.AND_INT_LIT8, com.pranav.ide.dx.io.Opcodes.AND_INT,
                    com.pranav.ide.dx.io.Opcodes.AND_INT_LIT16, com.pranav.ide.dx.dex.code.form.Form22b.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop OR_INT_LIT8 =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.OR_INT_LIT8, com.pranav.ide.dx.io.Opcodes.OR_INT,
                    com.pranav.ide.dx.io.Opcodes.OR_INT_LIT16, com.pranav.ide.dx.dex.code.form.Form22b.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop XOR_INT_LIT8 =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.XOR_INT_LIT8, com.pranav.ide.dx.io.Opcodes.XOR_INT,
                    com.pranav.ide.dx.io.Opcodes.XOR_INT_LIT16, com.pranav.ide.dx.dex.code.form.Form22b.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop SHL_INT_LIT8 =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.SHL_INT_LIT8, com.pranav.ide.dx.io.Opcodes.SHL_INT,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form22b.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop SHR_INT_LIT8 =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.SHR_INT_LIT8, com.pranav.ide.dx.io.Opcodes.SHR_INT,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form22b.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop USHR_INT_LIT8 =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.USHR_INT_LIT8, com.pranav.ide.dx.io.Opcodes.USHR_INT,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, Form22b.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop INVOKE_POLYMORPHIC =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.INVOKE_POLYMORPHIC, com.pranav.ide.dx.io.Opcodes.INVOKE_POLYMORPHIC,
                    com.pranav.ide.dx.io.Opcodes.INVOKE_POLYMORPHIC_RANGE, Form45cc.THE_ONE, false);
    public static final com.pranav.ide.dx.dex.code.Dop INVOKE_POLYMORPHIC_RANGE =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.INVOKE_POLYMORPHIC_RANGE, com.pranav.ide.dx.io.Opcodes.INVOKE_POLYMORPHIC,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, Form4rcc.THE_ONE, false);
    public static final com.pranav.ide.dx.dex.code.Dop INVOKE_CUSTOM =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.INVOKE_CUSTOM, com.pranav.ide.dx.io.Opcodes.INVOKE_CUSTOM,
                    com.pranav.ide.dx.io.Opcodes.INVOKE_CUSTOM_RANGE, Form35c.THE_ONE, false);
    public static final com.pranav.ide.dx.dex.code.Dop INVOKE_CUSTOM_RANGE =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.INVOKE_CUSTOM_RANGE, com.pranav.ide.dx.io.Opcodes.INVOKE_CUSTOM,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, Form3rc.THE_ONE, false);
    public static final com.pranav.ide.dx.dex.code.Dop CONST_METHOD_HANDLE =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.CONST_METHOD_HANDLE, com.pranav.ide.dx.io.Opcodes.CONST_METHOD_HANDLE,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, com.pranav.ide.dx.dex.code.form.Form21c.THE_ONE, true);
    public static final com.pranav.ide.dx.dex.code.Dop CONST_METHOD_TYPE =
            new com.pranav.ide.dx.dex.code.Dop(com.pranav.ide.dx.io.Opcodes.CONST_METHOD_TYPE, com.pranav.ide.dx.io.Opcodes.CONST_METHOD_TYPE,
                    com.pranav.ide.dx.io.Opcodes.NO_NEXT, Form21c.THE_ONE, true);
    /**
     * {@code non-null;} array containing all the standard instances
     */
    private static final com.pranav.ide.dx.dex.code.Dop[] DOPS;

    // END(dops)

    // Static initialization.
    static {
        DOPS = new com.pranav.ide.dx.dex.code.Dop[com.pranav.ide.dx.io.Opcodes.MAX_VALUE - com.pranav.ide.dx.io.Opcodes.MIN_VALUE + 1];

        set(SPECIAL_FORMAT);

        // BEGIN(dops-init); GENERATED AUTOMATICALLY BY opcode-gen
        set(NOP);
        set(MOVE);
        set(MOVE_FROM16);
        set(MOVE_16);
        set(MOVE_WIDE);
        set(MOVE_WIDE_FROM16);
        set(MOVE_WIDE_16);
        set(MOVE_OBJECT);
        set(MOVE_OBJECT_FROM16);
        set(MOVE_OBJECT_16);
        set(MOVE_RESULT);
        set(MOVE_RESULT_WIDE);
        set(MOVE_RESULT_OBJECT);
        set(MOVE_EXCEPTION);
        set(RETURN_VOID);
        set(RETURN);
        set(RETURN_WIDE);
        set(RETURN_OBJECT);
        set(CONST_4);
        set(CONST_16);
        set(CONST);
        set(CONST_HIGH16);
        set(CONST_WIDE_16);
        set(CONST_WIDE_32);
        set(CONST_WIDE);
        set(CONST_WIDE_HIGH16);
        set(CONST_STRING);
        set(CONST_STRING_JUMBO);
        set(CONST_CLASS);
        set(MONITOR_ENTER);
        set(MONITOR_EXIT);
        set(CHECK_CAST);
        set(INSTANCE_OF);
        set(ARRAY_LENGTH);
        set(NEW_INSTANCE);
        set(NEW_ARRAY);
        set(FILLED_NEW_ARRAY);
        set(FILLED_NEW_ARRAY_RANGE);
        set(FILL_ARRAY_DATA);
        set(THROW);
        set(GOTO);
        set(GOTO_16);
        set(GOTO_32);
        set(PACKED_SWITCH);
        set(SPARSE_SWITCH);
        set(CMPL_FLOAT);
        set(CMPG_FLOAT);
        set(CMPL_DOUBLE);
        set(CMPG_DOUBLE);
        set(CMP_LONG);
        set(IF_EQ);
        set(IF_NE);
        set(IF_LT);
        set(IF_GE);
        set(IF_GT);
        set(IF_LE);
        set(IF_EQZ);
        set(IF_NEZ);
        set(IF_LTZ);
        set(IF_GEZ);
        set(IF_GTZ);
        set(IF_LEZ);
        set(AGET);
        set(AGET_WIDE);
        set(AGET_OBJECT);
        set(AGET_BOOLEAN);
        set(AGET_BYTE);
        set(AGET_CHAR);
        set(AGET_SHORT);
        set(APUT);
        set(APUT_WIDE);
        set(APUT_OBJECT);
        set(APUT_BOOLEAN);
        set(APUT_BYTE);
        set(APUT_CHAR);
        set(APUT_SHORT);
        set(IGET);
        set(IGET_WIDE);
        set(IGET_OBJECT);
        set(IGET_BOOLEAN);
        set(IGET_BYTE);
        set(IGET_CHAR);
        set(IGET_SHORT);
        set(IPUT);
        set(IPUT_WIDE);
        set(IPUT_OBJECT);
        set(IPUT_BOOLEAN);
        set(IPUT_BYTE);
        set(IPUT_CHAR);
        set(IPUT_SHORT);
        set(SGET);
        set(SGET_WIDE);
        set(SGET_OBJECT);
        set(SGET_BOOLEAN);
        set(SGET_BYTE);
        set(SGET_CHAR);
        set(SGET_SHORT);
        set(SPUT);
        set(SPUT_WIDE);
        set(SPUT_OBJECT);
        set(SPUT_BOOLEAN);
        set(SPUT_BYTE);
        set(SPUT_CHAR);
        set(SPUT_SHORT);
        set(INVOKE_VIRTUAL);
        set(INVOKE_SUPER);
        set(INVOKE_DIRECT);
        set(INVOKE_STATIC);
        set(INVOKE_INTERFACE);
        set(INVOKE_VIRTUAL_RANGE);
        set(INVOKE_SUPER_RANGE);
        set(INVOKE_DIRECT_RANGE);
        set(INVOKE_STATIC_RANGE);
        set(INVOKE_INTERFACE_RANGE);
        set(NEG_INT);
        set(NOT_INT);
        set(NEG_LONG);
        set(NOT_LONG);
        set(NEG_FLOAT);
        set(NEG_DOUBLE);
        set(INT_TO_LONG);
        set(INT_TO_FLOAT);
        set(INT_TO_DOUBLE);
        set(LONG_TO_INT);
        set(LONG_TO_FLOAT);
        set(LONG_TO_DOUBLE);
        set(FLOAT_TO_INT);
        set(FLOAT_TO_LONG);
        set(FLOAT_TO_DOUBLE);
        set(DOUBLE_TO_INT);
        set(DOUBLE_TO_LONG);
        set(DOUBLE_TO_FLOAT);
        set(INT_TO_BYTE);
        set(INT_TO_CHAR);
        set(INT_TO_SHORT);
        set(ADD_INT);
        set(SUB_INT);
        set(MUL_INT);
        set(DIV_INT);
        set(REM_INT);
        set(AND_INT);
        set(OR_INT);
        set(XOR_INT);
        set(SHL_INT);
        set(SHR_INT);
        set(USHR_INT);
        set(ADD_LONG);
        set(SUB_LONG);
        set(MUL_LONG);
        set(DIV_LONG);
        set(REM_LONG);
        set(AND_LONG);
        set(OR_LONG);
        set(XOR_LONG);
        set(SHL_LONG);
        set(SHR_LONG);
        set(USHR_LONG);
        set(ADD_FLOAT);
        set(SUB_FLOAT);
        set(MUL_FLOAT);
        set(DIV_FLOAT);
        set(REM_FLOAT);
        set(ADD_DOUBLE);
        set(SUB_DOUBLE);
        set(MUL_DOUBLE);
        set(DIV_DOUBLE);
        set(REM_DOUBLE);
        set(ADD_INT_2ADDR);
        set(SUB_INT_2ADDR);
        set(MUL_INT_2ADDR);
        set(DIV_INT_2ADDR);
        set(REM_INT_2ADDR);
        set(AND_INT_2ADDR);
        set(OR_INT_2ADDR);
        set(XOR_INT_2ADDR);
        set(SHL_INT_2ADDR);
        set(SHR_INT_2ADDR);
        set(USHR_INT_2ADDR);
        set(ADD_LONG_2ADDR);
        set(SUB_LONG_2ADDR);
        set(MUL_LONG_2ADDR);
        set(DIV_LONG_2ADDR);
        set(REM_LONG_2ADDR);
        set(AND_LONG_2ADDR);
        set(OR_LONG_2ADDR);
        set(XOR_LONG_2ADDR);
        set(SHL_LONG_2ADDR);
        set(SHR_LONG_2ADDR);
        set(USHR_LONG_2ADDR);
        set(ADD_FLOAT_2ADDR);
        set(SUB_FLOAT_2ADDR);
        set(MUL_FLOAT_2ADDR);
        set(DIV_FLOAT_2ADDR);
        set(REM_FLOAT_2ADDR);
        set(ADD_DOUBLE_2ADDR);
        set(SUB_DOUBLE_2ADDR);
        set(MUL_DOUBLE_2ADDR);
        set(DIV_DOUBLE_2ADDR);
        set(REM_DOUBLE_2ADDR);
        set(ADD_INT_LIT16);
        set(RSUB_INT);
        set(MUL_INT_LIT16);
        set(DIV_INT_LIT16);
        set(REM_INT_LIT16);
        set(AND_INT_LIT16);
        set(OR_INT_LIT16);
        set(XOR_INT_LIT16);
        set(ADD_INT_LIT8);
        set(RSUB_INT_LIT8);
        set(MUL_INT_LIT8);
        set(DIV_INT_LIT8);
        set(REM_INT_LIT8);
        set(AND_INT_LIT8);
        set(OR_INT_LIT8);
        set(XOR_INT_LIT8);
        set(SHL_INT_LIT8);
        set(SHR_INT_LIT8);
        set(USHR_INT_LIT8);
        set(INVOKE_POLYMORPHIC);
        set(INVOKE_POLYMORPHIC_RANGE);
        set(INVOKE_CUSTOM);
        set(INVOKE_CUSTOM_RANGE);
        set(CONST_METHOD_HANDLE);
        set(CONST_METHOD_TYPE);
        // END(dops-init)
    }

    /**
     * This class is uninstantiable.
     */
    private Dops() {
        // This space intentionally left blank.
    }

    /**
     * Gets the {@link com.pranav.ide.dx.dex.code.Dop} for the given opcode value.
     *
     * @param opcode {@code Opcodes.MIN_VALUE..Opcodes.MAX_VALUE;} the
     *               opcode value
     * @return {@code non-null;} the associated opcode instance
     */
    public static com.pranav.ide.dx.dex.code.Dop get(int opcode) {
        int idx = opcode - com.pranav.ide.dx.io.Opcodes.MIN_VALUE;

        try {
            com.pranav.ide.dx.dex.code.Dop result = DOPS[idx];
            if (result != null) {
                return result;
            }
        } catch (ArrayIndexOutOfBoundsException ex) {
            // Fall through.
        }

        throw new IllegalArgumentException("bogus opcode");
    }

    /**
     * Gets the next {@link com.pranav.ide.dx.dex.code.Dop} in the instruction fitting chain after the
     * given instance, if any.
     *
     * @param opcode  {@code non-null;} the opcode
     * @param options {@code non-null;} options, used to determine
     *                which opcodes are potentially off-limits
     * @return {@code null-ok;} the next opcode in the same family, in the
     * chain of opcodes to try, or {@code null} if the given opcode is
     * the last in its chain
     */
    public static com.pranav.ide.dx.dex.code.Dop getNextOrNull(com.pranav.ide.dx.dex.code.Dop opcode, DexOptions options) {
        int nextOpcode = opcode.getNextOpcode();

        if (nextOpcode == com.pranav.ide.dx.io.Opcodes.NO_NEXT) {
            return null;
        }

        opcode = get(nextOpcode);

        return opcode;
    }

    /**
     * Puts the given opcode into the table of all ops.
     *
     * @param opcode {@code non-null;} the opcode
     */
    private static void set(Dop opcode) {
        int idx = opcode.getOpcode() - Opcodes.MIN_VALUE;
        DOPS[idx] = opcode;
    }
}

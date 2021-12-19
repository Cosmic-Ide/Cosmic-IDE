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

import com.pranav.ide.dx.rop.code.RegisterSpecList;
import com.pranav.ide.dx.rop.code.SourcePosition;

/**
 * Instruction which has no extra info beyond the basics provided for in
 * the base class.
 */
public final class SimpleInsn extends FixedSizeInsn {
    /**
     * Constructs an instance. The output address of this instance is initially
     * unknown ({@code -1}).
     *
     * @param opcode    the opcode; one of the constants from {@link Dops}
     * @param position  {@code non-null;} source position
     * @param registers {@code non-null;} register list, including a
     *                  result register if appropriate (that is, registers may be either
     *                  ins or outs)
     */
    public SimpleInsn(com.pranav.ide.dx.dex.code.Dop opcode, SourcePosition position,
                      com.pranav.ide.dx.rop.code.RegisterSpecList registers) {
        super(opcode, position, registers);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public com.pranav.ide.dx.dex.code.DalvInsn withOpcode(Dop opcode) {
        return new SimpleInsn(opcode, getPosition(), getRegisters());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DalvInsn withRegisters(RegisterSpecList registers) {
        return new SimpleInsn(getOpcode(), getPosition(), registers);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String argString() {
        return null;
    }
}

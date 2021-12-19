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

package com.pranav.ide.dx.cf.code;

import com.pranav.ide.dx.rop.cst.CstCallSiteRef;
import com.pranav.ide.dx.rop.cst.CstType;
import com.pranav.ide.dx.rop.type.Prototype;
import com.pranav.ide.dx.rop.type.Type;
import com.pranav.ide.dx.rop.type.TypeBearer;
import com.pranav.ide.dx.util.Hex;

/**
 * {@link Machine} which keeps track of known values but does not do
 * smart/realistic reference type calculations.
 */
public class ValueAwareMachine extends BaseMachine {
    /**
     * Constructs an instance.
     *
     * @param prototype {@code non-null;} the prototype for the associated
     *                  method
     */
    public ValueAwareMachine(Prototype prototype) {
        super(prototype);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run(Frame frame, int offset, int opcode) {
        switch (opcode) {
            case com.pranav.ide.dx.cf.code.ByteOps.NOP:
            case com.pranav.ide.dx.cf.code.ByteOps.IASTORE:
            case com.pranav.ide.dx.cf.code.ByteOps.POP:
            case com.pranav.ide.dx.cf.code.ByteOps.POP2:
            case com.pranav.ide.dx.cf.code.ByteOps.IFEQ:
            case com.pranav.ide.dx.cf.code.ByteOps.IFNE:
            case com.pranav.ide.dx.cf.code.ByteOps.IFLT:
            case com.pranav.ide.dx.cf.code.ByteOps.IFGE:
            case com.pranav.ide.dx.cf.code.ByteOps.IFGT:
            case com.pranav.ide.dx.cf.code.ByteOps.IFLE:
            case com.pranav.ide.dx.cf.code.ByteOps.IF_ICMPEQ:
            case com.pranav.ide.dx.cf.code.ByteOps.IF_ICMPNE:
            case com.pranav.ide.dx.cf.code.ByteOps.IF_ICMPLT:
            case com.pranav.ide.dx.cf.code.ByteOps.IF_ICMPGE:
            case com.pranav.ide.dx.cf.code.ByteOps.IF_ICMPGT:
            case com.pranav.ide.dx.cf.code.ByteOps.IF_ICMPLE:
            case com.pranav.ide.dx.cf.code.ByteOps.IF_ACMPEQ:
            case com.pranav.ide.dx.cf.code.ByteOps.IF_ACMPNE:
            case com.pranav.ide.dx.cf.code.ByteOps.GOTO:
            case com.pranav.ide.dx.cf.code.ByteOps.RET:
            case com.pranav.ide.dx.cf.code.ByteOps.LOOKUPSWITCH:
            case com.pranav.ide.dx.cf.code.ByteOps.IRETURN:
            case com.pranav.ide.dx.cf.code.ByteOps.RETURN:
            case com.pranav.ide.dx.cf.code.ByteOps.PUTSTATIC:
            case com.pranav.ide.dx.cf.code.ByteOps.PUTFIELD:
            case com.pranav.ide.dx.cf.code.ByteOps.ATHROW:
            case com.pranav.ide.dx.cf.code.ByteOps.MONITORENTER:
            case com.pranav.ide.dx.cf.code.ByteOps.MONITOREXIT:
            case com.pranav.ide.dx.cf.code.ByteOps.IFNULL:
            case com.pranav.ide.dx.cf.code.ByteOps.IFNONNULL: {
                // Nothing to do for these ops in this class.
                clearResult();
                break;
            }
            case com.pranav.ide.dx.cf.code.ByteOps.LDC:
            case com.pranav.ide.dx.cf.code.ByteOps.LDC2_W: {
                setResult((com.pranav.ide.dx.rop.type.TypeBearer) getAuxCst());
                break;
            }
            case com.pranav.ide.dx.cf.code.ByteOps.ILOAD:
            case com.pranav.ide.dx.cf.code.ByteOps.ISTORE: {
                setResult(arg(0));
                break;
            }
            case com.pranav.ide.dx.cf.code.ByteOps.IALOAD:
            case com.pranav.ide.dx.cf.code.ByteOps.IADD:
            case com.pranav.ide.dx.cf.code.ByteOps.ISUB:
            case com.pranav.ide.dx.cf.code.ByteOps.IMUL:
            case com.pranav.ide.dx.cf.code.ByteOps.IDIV:
            case com.pranav.ide.dx.cf.code.ByteOps.IREM:
            case com.pranav.ide.dx.cf.code.ByteOps.INEG:
            case com.pranav.ide.dx.cf.code.ByteOps.ISHL:
            case com.pranav.ide.dx.cf.code.ByteOps.ISHR:
            case com.pranav.ide.dx.cf.code.ByteOps.IUSHR:
            case com.pranav.ide.dx.cf.code.ByteOps.IAND:
            case com.pranav.ide.dx.cf.code.ByteOps.IOR:
            case com.pranav.ide.dx.cf.code.ByteOps.IXOR:
            case com.pranav.ide.dx.cf.code.ByteOps.IINC:
            case com.pranav.ide.dx.cf.code.ByteOps.I2L:
            case com.pranav.ide.dx.cf.code.ByteOps.I2F:
            case com.pranav.ide.dx.cf.code.ByteOps.I2D:
            case com.pranav.ide.dx.cf.code.ByteOps.L2I:
            case com.pranav.ide.dx.cf.code.ByteOps.L2F:
            case com.pranav.ide.dx.cf.code.ByteOps.L2D:
            case com.pranav.ide.dx.cf.code.ByteOps.F2I:
            case com.pranav.ide.dx.cf.code.ByteOps.F2L:
            case com.pranav.ide.dx.cf.code.ByteOps.F2D:
            case com.pranav.ide.dx.cf.code.ByteOps.D2I:
            case com.pranav.ide.dx.cf.code.ByteOps.D2L:
            case com.pranav.ide.dx.cf.code.ByteOps.D2F:
            case com.pranav.ide.dx.cf.code.ByteOps.I2B:
            case com.pranav.ide.dx.cf.code.ByteOps.I2C:
            case com.pranav.ide.dx.cf.code.ByteOps.I2S:
            case com.pranav.ide.dx.cf.code.ByteOps.LCMP:
            case com.pranav.ide.dx.cf.code.ByteOps.FCMPL:
            case com.pranav.ide.dx.cf.code.ByteOps.FCMPG:
            case com.pranav.ide.dx.cf.code.ByteOps.DCMPL:
            case com.pranav.ide.dx.cf.code.ByteOps.DCMPG:
            case com.pranav.ide.dx.cf.code.ByteOps.ARRAYLENGTH: {
                setResult(getAuxType());
                break;
            }
            case com.pranav.ide.dx.cf.code.ByteOps.DUP:
            case com.pranav.ide.dx.cf.code.ByteOps.DUP_X1:
            case com.pranav.ide.dx.cf.code.ByteOps.DUP_X2:
            case com.pranav.ide.dx.cf.code.ByteOps.DUP2:
            case com.pranav.ide.dx.cf.code.ByteOps.DUP2_X1:
            case com.pranav.ide.dx.cf.code.ByteOps.DUP2_X2:
            case com.pranav.ide.dx.cf.code.ByteOps.SWAP: {
                clearResult();
                for (int pattern = getAuxInt(); pattern != 0; pattern >>= 4) {
                    int which = (pattern & 0x0f) - 1;
                    addResult(arg(which));
                }
                break;
            }

            case com.pranav.ide.dx.cf.code.ByteOps.JSR: {
                setResult(new ReturnAddress(getAuxTarget()));
                break;
            }
            case com.pranav.ide.dx.cf.code.ByteOps.GETSTATIC:
            case com.pranav.ide.dx.cf.code.ByteOps.GETFIELD:
            case com.pranav.ide.dx.cf.code.ByteOps.INVOKEVIRTUAL:
            case com.pranav.ide.dx.cf.code.ByteOps.INVOKESTATIC:
            case com.pranav.ide.dx.cf.code.ByteOps.INVOKEINTERFACE: {
                com.pranav.ide.dx.rop.type.Type type = ((com.pranav.ide.dx.rop.type.TypeBearer) getAuxCst()).getType();
                if (type == com.pranav.ide.dx.rop.type.Type.VOID) {
                    clearResult();
                } else {
                    setResult(type);
                }
                break;
            }
            case com.pranav.ide.dx.cf.code.ByteOps.INVOKESPECIAL: {
                com.pranav.ide.dx.rop.type.Type thisType = arg(0).getType();
                if (thisType.isUninitialized()) {
                    frame.makeInitialized(thisType);
                }
                com.pranav.ide.dx.rop.type.Type type = ((TypeBearer) getAuxCst()).getType();
                if (type == com.pranav.ide.dx.rop.type.Type.VOID) {
                    clearResult();
                } else {
                    setResult(type);
                }
                break;
            }
            case com.pranav.ide.dx.cf.code.ByteOps.INVOKEDYNAMIC: {
                com.pranav.ide.dx.rop.type.Type type = ((CstCallSiteRef) getAuxCst()).getReturnType();
                if (type == com.pranav.ide.dx.rop.type.Type.VOID) {
                    clearResult();
                } else {
                    setResult(type);
                }
                break;
            }
            case com.pranav.ide.dx.cf.code.ByteOps.NEW: {
                com.pranav.ide.dx.rop.type.Type type = ((com.pranav.ide.dx.rop.cst.CstType) getAuxCst()).getClassType();
                setResult(type.asUninitialized(offset));
                break;
            }
            case com.pranav.ide.dx.cf.code.ByteOps.NEWARRAY:
            case com.pranav.ide.dx.cf.code.ByteOps.CHECKCAST:
            case com.pranav.ide.dx.cf.code.ByteOps.MULTIANEWARRAY: {
                com.pranav.ide.dx.rop.type.Type type = ((com.pranav.ide.dx.rop.cst.CstType) getAuxCst()).getClassType();
                setResult(type);
                break;
            }
            case com.pranav.ide.dx.cf.code.ByteOps.ANEWARRAY: {
                com.pranav.ide.dx.rop.type.Type type = ((CstType) getAuxCst()).getClassType();
                setResult(type.getArrayType());
                break;
            }
            case ByteOps.INSTANCEOF: {
                setResult(Type.INT);
                break;
            }
            default: {
                throw new RuntimeException("shouldn't happen: " +
                        Hex.u1(opcode));
            }
        }

        storeResults(frame);
    }
}

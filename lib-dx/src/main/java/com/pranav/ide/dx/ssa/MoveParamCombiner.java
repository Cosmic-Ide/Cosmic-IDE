/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.pranav.ide.dx.ssa;

import com.pranav.ide.dx.rop.code.CstInsn;
import com.pranav.ide.dx.rop.code.LocalItem;
import com.pranav.ide.dx.rop.code.RegOps;
import com.pranav.ide.dx.rop.code.RegisterSpec;
import com.pranav.ide.dx.rop.cst.CstInteger;

import java.util.HashSet;
import java.util.List;

/**
 * Combine identical move-param insns, which may result from Ropper's
 * handling of synchronized methods.
 */
public class MoveParamCombiner {

    /**
     * method to process
     */
    private final com.pranav.ide.dx.ssa.SsaMethod ssaMeth;

    private MoveParamCombiner(SsaMethod ssaMeth) {
        this.ssaMeth = ssaMeth;
    }

    /**
     * Processes a method with this optimization step.
     *
     * @param ssaMethod method to process
     */
    public static void process(com.pranav.ide.dx.ssa.SsaMethod ssaMethod) {
        new MoveParamCombiner(ssaMethod).run();
    }

    /**
     * Runs this optimization step.
     */
    private void run() {
        // This will contain the definition specs for each parameter
        final com.pranav.ide.dx.rop.code.RegisterSpec[] paramSpecs
                = new com.pranav.ide.dx.rop.code.RegisterSpec[ssaMeth.getParamWidth()];

        // Insns to delete when all done
        final HashSet<com.pranav.ide.dx.ssa.SsaInsn> deletedInsns = new HashSet<>();

        ssaMeth.forEachInsn(new com.pranav.ide.dx.ssa.SsaInsn.Visitor() {
            @Override
            public void visitMoveInsn(NormalSsaInsn insn) {
            }

            @Override
            public void visitPhiInsn(PhiInsn phi) {
            }

            @Override
            public void visitNonMoveInsn(NormalSsaInsn insn) {
                if (insn.getOpcode().getOpcode() != RegOps.MOVE_PARAM) {
                    return;
                }

                int param = getParamIndex(insn);

                if (paramSpecs[param] == null) {
                    paramSpecs[param] = insn.getResult();
                } else {
                    final com.pranav.ide.dx.rop.code.RegisterSpec specA = paramSpecs[param];
                    final com.pranav.ide.dx.rop.code.RegisterSpec specB = insn.getResult();
                    com.pranav.ide.dx.rop.code.LocalItem localA = specA.getLocalItem();
                    com.pranav.ide.dx.rop.code.LocalItem localB = specB.getLocalItem();
                    LocalItem newLocal;

                    /*
                     * Is there local information to preserve?
                     */

                    if (localA == null) {
                        newLocal = localB;
                    } else if (localB == null) {
                        newLocal = localA;
                    } else if (localA.equals(localB)) {
                        newLocal = localA;
                    } else {
                        /*
                         * Oddly, these two identical move-params have distinct
                         * debug info. We'll just keep them distinct.
                         */
                        return;
                    }

                    ssaMeth.getDefinitionForRegister(specA.getReg())
                            .setResultLocal(newLocal);

                    /*
                     * Map all uses of specB to specA
                     */

                    com.pranav.ide.dx.ssa.RegisterMapper mapper = new RegisterMapper() {
                        /** {@inheritDoc} */
                        @Override
                        public int getNewRegisterCount() {
                            return ssaMeth.getRegCount();
                        }

                        /** {@inheritDoc} */
                        @Override
                        public com.pranav.ide.dx.rop.code.RegisterSpec map(RegisterSpec registerSpec) {
                            if (registerSpec.getReg() == specB.getReg()) {
                                return specA;
                            }

                            return registerSpec;
                        }
                    };

                    List<com.pranav.ide.dx.ssa.SsaInsn> uses
                            = ssaMeth.getUseListForRegister(specB.getReg());

                    // Use list is modified by mapSourceRegisters
                    for (int i = uses.size() - 1; i >= 0; i--) {
                        SsaInsn use = uses.get(i);
                        use.mapSourceRegisters(mapper);
                    }

                    deletedInsns.add(insn);
                }

            }
        });

        ssaMeth.deleteInsns(deletedInsns);
    }

    /**
     * Returns the parameter index associated with a move-param insn. Does
     * not verify that the insn is a move-param insn.
     *
     * @param insn {@code non-null;} a move-param insn
     * @return {@code >=0;} parameter index
     */
    private int getParamIndex(NormalSsaInsn insn) {
        com.pranav.ide.dx.rop.code.CstInsn cstInsn = (CstInsn) (insn.getOriginalRopInsn());

        int param = ((CstInteger) cstInsn.getConstant()).getValue();
        return param;
    }

}

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

package com.pranav.ide.dx.ssa;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;

/**
 * This class computes dominator and post-dominator information using the
 * Lengauer-Tarjan method.
 * <p>
 * See A Fast Algorithm for Finding Dominators in a Flowgraph
 * T. Lengauer &amp; R. Tarjan, ACM TOPLAS July 1979, pgs 121-141.
 * <p>
 * This implementation runs in time O(n log n).  The time bound
 * could be changed to O(n * ack(n)) with a small change to the link and eval,
 * and an addition of a child field to the DFS info. In reality, the constant
 * overheads are high enough that the current method is faster in all but the
 * strangest artificially constructed examples.
 * <p>
 * The basic idea behind this algorithm is to perform a DFS walk, keeping track
 * of various info about parents.  We then use this info to calculate the
 * dominators, using union-find structures to link together the DFS info,
 * then finally evaluate the union-find results to get the dominators.
 * This implementation is m log n because it does not perform union by
 * rank to keep the union-find tree balanced.
 */
public final class Dominators {
    /* postdom is true if we want post dominators */
    private final boolean postdom;

    /* {@code non-null;} method being processed */
    private final com.pranav.ide.dx.ssa.SsaMethod meth;

    /* Method's basic blocks. */
    private final ArrayList<com.pranav.ide.dx.ssa.SsaBasicBlock> blocks;

    /**
     * indexed by basic block index
     */
    private final DFSInfo[] info;

    private final ArrayList<com.pranav.ide.dx.ssa.SsaBasicBlock> vertex;

    /**
     * {@code non-null;} the raw dominator info
     */
    private final com.pranav.ide.dx.ssa.DomFront.DomInfo[] domInfos;

    /**
     * Constructs an instance.
     *
     * @param meth     {@code non-null;} method to process
     * @param domInfos {@code non-null;} the raw dominator info
     * @param postdom  true for postdom information, false for normal dom info
     */
    private Dominators(com.pranav.ide.dx.ssa.SsaMethod meth, com.pranav.ide.dx.ssa.DomFront.DomInfo[] domInfos,
                       boolean postdom) {
        this.meth = meth;
        this.domInfos = domInfos;
        this.postdom = postdom;
        this.blocks = meth.getBlocks();
        this.info = new DFSInfo[blocks.size() + 2];
        this.vertex = new ArrayList<com.pranav.ide.dx.ssa.SsaBasicBlock>();
    }

    /**
     * Constructs a fully-initialized instance. (This method exists so as
     * to avoid calling a large amount of code in the constructor.)
     *
     * @param meth     {@code non-null;} method to process
     * @param domInfos {@code non-null;} the raw dominator info
     * @param postdom  true for postdom information, false for normal dom info
     */
    public static Dominators make(SsaMethod meth, DomFront.DomInfo[] domInfos,
                                  boolean postdom) {
        Dominators result = new Dominators(meth, domInfos, postdom);

        result.run();
        return result;
    }

    private BitSet getSuccs(com.pranav.ide.dx.ssa.SsaBasicBlock block) {
        if (postdom) {
            return block.getPredecessors();
        } else {
            return block.getSuccessors();
        }
    }

    private BitSet getPreds(com.pranav.ide.dx.ssa.SsaBasicBlock block) {
        if (postdom) {
            return block.getSuccessors();
        } else {
            return block.getPredecessors();
        }
    }

    /**
     * Performs path compress on the DFS info.
     *
     * @param in Basic block whose DFS info we are path compressing.
     */
    private void compress(com.pranav.ide.dx.ssa.SsaBasicBlock in) {
        DFSInfo bbInfo = info[in.getIndex()];
        DFSInfo ancestorbbInfo = info[bbInfo.ancestor.getIndex()];

        if (ancestorbbInfo.ancestor != null) {
            ArrayList<com.pranav.ide.dx.ssa.SsaBasicBlock> worklist = new ArrayList<com.pranav.ide.dx.ssa.SsaBasicBlock>();
            HashSet<com.pranav.ide.dx.ssa.SsaBasicBlock> visited = new HashSet<com.pranav.ide.dx.ssa.SsaBasicBlock>();
            worklist.add(in);

            while (!worklist.isEmpty()) {
                int wsize = worklist.size();
                com.pranav.ide.dx.ssa.SsaBasicBlock v = worklist.get(wsize - 1);
                DFSInfo vbbInfo = info[v.getIndex()];
                com.pranav.ide.dx.ssa.SsaBasicBlock vAncestor = vbbInfo.ancestor;
                DFSInfo vabbInfo = info[vAncestor.getIndex()];

                // Make sure we process our ancestor before ourselves.
                if (visited.add(vAncestor) && vabbInfo.ancestor != null) {
                    worklist.add(vAncestor);
                    continue;
                }
                worklist.remove(wsize - 1);

                // Update based on ancestor info.
                if (vabbInfo.ancestor == null) {
                    continue;
                }
                com.pranav.ide.dx.ssa.SsaBasicBlock vAncestorRep = vabbInfo.rep;
                com.pranav.ide.dx.ssa.SsaBasicBlock vRep = vbbInfo.rep;
                if (info[vAncestorRep.getIndex()].semidom
                        < info[vRep.getIndex()].semidom) {
                    vbbInfo.rep = vAncestorRep;
                }
                vbbInfo.ancestor = vabbInfo.ancestor;
            }
        }
    }

    private com.pranav.ide.dx.ssa.SsaBasicBlock eval(com.pranav.ide.dx.ssa.SsaBasicBlock v) {
        DFSInfo bbInfo = info[v.getIndex()];

        if (bbInfo.ancestor == null) {
            return v;
        }

        compress(v);
        return bbInfo.rep;
    }

    /**
     * Performs dominator/post-dominator calculation for the control
     * flow graph.
     *
     * @param meth {@code non-null;} method to analyze
     */
    private void run() {
        com.pranav.ide.dx.ssa.SsaBasicBlock root = postdom
                ? meth.getExitBlock() : meth.getEntryBlock();

        if (root != null) {
            vertex.add(root);
            domInfos[root.getIndex()].idom = root.getIndex();
        }

        /*
         * First we perform a DFS numbering of the blocks, by
         * numbering the dfs tree roots.
         */

        DfsWalker walker = new DfsWalker();
        meth.forEachBlockDepthFirst(postdom, walker);

        // the largest semidom number assigned
        int dfsMax = vertex.size() - 1;

        // Now calculate semidominators.
        for (int i = dfsMax; i >= 2; --i) {
            com.pranav.ide.dx.ssa.SsaBasicBlock w = vertex.get(i);
            DFSInfo wInfo = info[w.getIndex()];

            BitSet preds = getPreds(w);
            for (int j = preds.nextSetBit(0);
                 j >= 0;
                 j = preds.nextSetBit(j + 1)) {
                com.pranav.ide.dx.ssa.SsaBasicBlock predBlock = blocks.get(j);
                DFSInfo predInfo = info[predBlock.getIndex()];

                /*
                 * PredInfo may not exist in case the predecessor is
                 * not reachable.
                 */
                if (predInfo != null) {
                    int predSemidom = info[eval(predBlock).getIndex()].semidom;
                    if (predSemidom < wInfo.semidom) {
                        wInfo.semidom = predSemidom;
                    }
                }
            }
            info[vertex.get(wInfo.semidom).getIndex()].bucket.add(w);

            /*
             * Normally we would call link here, but in our O(m log n)
             * implementation this is equivalent to the following
             * single line.
             */
            wInfo.ancestor = wInfo.parent;

            // Implicity define idom for each vertex.
            ArrayList<com.pranav.ide.dx.ssa.SsaBasicBlock> wParentBucket;
            wParentBucket = info[wInfo.parent.getIndex()].bucket;

            while (!wParentBucket.isEmpty()) {
                int lastItem = wParentBucket.size() - 1;
                com.pranav.ide.dx.ssa.SsaBasicBlock last = wParentBucket.remove(lastItem);
                com.pranav.ide.dx.ssa.SsaBasicBlock U = eval(last);
                if (info[U.getIndex()].semidom
                        < info[last.getIndex()].semidom) {
                    domInfos[last.getIndex()].idom = U.getIndex();
                } else {
                    domInfos[last.getIndex()].idom = wInfo.parent.getIndex();
                }
            }
        }

        // Now explicitly define the immediate dominator of each vertex
        for (int i = 2; i <= dfsMax; ++i) {
            com.pranav.ide.dx.ssa.SsaBasicBlock w = vertex.get(i);
            if (domInfos[w.getIndex()].idom
                    != vertex.get(info[w.getIndex()].semidom).getIndex()) {
                domInfos[w.getIndex()].idom
                        = domInfos[domInfos[w.getIndex()].idom].idom;
            }
        }
    }

    private static final class DFSInfo {
        public int semidom;
        public com.pranav.ide.dx.ssa.SsaBasicBlock parent;

        /**
         * rep(resentative) is known as "label" in the paper. It is the node
         * that our block's DFS info has been unioned to.
         */
        public com.pranav.ide.dx.ssa.SsaBasicBlock rep;

        public com.pranav.ide.dx.ssa.SsaBasicBlock ancestor;
        public ArrayList<com.pranav.ide.dx.ssa.SsaBasicBlock> bucket;

        public DFSInfo() {
            bucket = new ArrayList<SsaBasicBlock>();
        }
    }

    /**
     * Callback for depth-first walk through control flow graph (either
     * from the entry block or the exit block). Records the traversal order
     * in the {@code info}list.
     */
    private class DfsWalker implements com.pranav.ide.dx.ssa.SsaBasicBlock.Visitor {
        private int dfsNum = 0;

        @Override
        public void visitBlock(com.pranav.ide.dx.ssa.SsaBasicBlock v, com.pranav.ide.dx.ssa.SsaBasicBlock parent) {
            DFSInfo bbInfo = new DFSInfo();
            bbInfo.semidom = ++dfsNum;
            bbInfo.rep = v;
            bbInfo.parent = parent;
            vertex.add(v);
            info[v.getIndex()] = bbInfo;
        }
    }
}

/*******************************************************************************
 * Copyright (c) 2000, 2019 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.core.util;

/**
 * Description of a stack map frame as specified in the JVM specifications.
 *
 * This interface may be implemented by clients.
 *
 * @since 3.2
 */
public interface IStackMapFrame {

	/**
	 * Answer back the frame type for this entry.
	 * <table>
	 * <tr>
	 * <th>Type</th>
	 * <th>Range</th>
	 * </tr>
	 * <tr>
	 * <td>SAME</td>
	 * <td>0-63</td>
	 * </tr>
	 * <tr>
	 * <td>SAME_LOCALS_1_STACK_ITEM</td>
	 * <td>64-127</td>
	 * </tr>
	 * <tr>
	 * <td>SAME_LOCALS_1_STACK_ITEM_EXTENDED</td>
	 * <td>247</td>
	 * </tr>
	 * <tr>
	 * <td>CHOP</td>
	 * <td>248-250</td>
	 * </tr>
	 * <tr>
	 * <td>SAME_FRAME_EXTENDED</td>
	 * <td>251</td>
	 * </tr>
	 * <tr>
	 * <td>APPEND</td>
	 * <td>252-254</td>
	 * </tr>
	 * <tr>
	 * <td>FULL_FRAME</td>
	 * <td>255</td>
	 * </tr>
	 * </table>
	 *
	 * @return the frame type for this entry
	 */
	int getFrameType();

	/**
	 * Answer back the offset delta.
	 * <p>This is not defined only for the frame types SAME and SAME_LOCALS_1_STACK_ITEM.</p>
	 *
	 * @return the offset delta
	 */
	int getOffsetDelta();

	/**
	 * Answer back the number of locals.
	 * <p>This is defined only for the frame type FULL_FRAME.</p>
	 *
	 * @return the number of locals
	 */
	int getNumberOfLocals();

	/**
	 * Answer back verification infos for the defined locals.
	 * <p>This is defined only for frame types APPEND and FULL_FRAME.
	 *
	 * @return verification infos for the defined locals
	 */
	IVerificationTypeInfo[] getLocals();

	/**
	 * Answer back the number of stack items
	 * <p>This is defined only for the frame types SAME_LOCALS_1_STACK_ITEM, SAME_LOCALS_1_STACK_ITEM_EXTENDED and FULL_FRAME.
	 * For SAME_LOCALS_1_STACK_ITEM and SAME_LOCALS_1_STACK_ITEM_EXTENDED, the answer is implicitely 1.</p>
	 *
	 * @return the number of stack items
	 */
	int getNumberOfStackItems();

	/**
	 * Answer back the verification infos for the stack items.
	 *
	 * @return the verification infos for the stack items
	 */
	IVerificationTypeInfo[] getStackItems();
}

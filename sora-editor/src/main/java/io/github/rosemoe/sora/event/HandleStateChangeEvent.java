/*
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2022  Rosemoe
 *
 *     This library is free software; you can redistribute it and/or
 *     modify it under the terms of the GNU Lesser General Public
 *     License as published by the Free Software Foundation; either
 *     version 2.1 of the License, or (at your option) any later version.
 *
 *     This library is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *     Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public
 *     License along with this library; if not, write to the Free Software
 *     Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 *     USA
 *
 *     Please contact Rosemoe by email 2073412493@qq.com if you need
 *     additional information or have any questions
 */
package io.github.rosemoe.sora.event;

import io.github.rosemoe.sora.widget.CodeEditor;

/** Notifies a selection handle's touch state has changed */
public class HandleStateChangeEvent extends Event {

    public static final int HANDLE_TYPE_INSERT = 0;
    public static final int HANDLE_TYPE_LEFT = 1;
    public static final int HANDLE_TYPE_RIGHT = 2;
    private final int mWhich;
    private final boolean isHeld;

    public HandleStateChangeEvent(CodeEditor editor, int which, boolean heldState) {
        super(editor);
        mWhich = which;
        isHeld = heldState;
    }

    public int getHandleType() {
        return mWhich;
    }

    public boolean isHeld() {
        return isHeld;
    }
}

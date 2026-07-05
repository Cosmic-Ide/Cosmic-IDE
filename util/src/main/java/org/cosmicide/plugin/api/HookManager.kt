/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.plugin.api

import android.content.Context
import top.canyie.pine.Pine
import top.canyie.pine.callback.MethodHook
import java.lang.ref.WeakReference
import java.lang.reflect.Member

object HookManager {

    @JvmStatic
    lateinit var context: WeakReference<Context>

    @JvmStatic
    fun registerHook(hook: Hook) =
        Pine.hook(
            hook.type.getDeclaredMethod(hook.method, *hook.argTypes),
            object : MethodHook() {
                override fun beforeCall(callFrame: Pine.CallFrame) {
                    hook.before(callFrame)
                }

                override fun afterCall(callFrame: Pine.CallFrame) {
                    hook.after(callFrame)
                }
            }
        )

    @JvmStatic
    fun invokeOriginal(method: Member, obj: Any?, vararg args: Any) =
        Pine.invokeOriginalMethod(method, obj, *args)

    @JvmStatic
    fun isHooked(method: Member) = Pine.isHooked(method)

    fun disableHiddenApiRestrictions() = Pine.disableHiddenApiPolicy(true, true)

    fun disableProfileSaver() = Pine.disableProfileSaver()
}

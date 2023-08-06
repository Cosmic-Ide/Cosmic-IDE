/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.rewrite.plugin.api

import android.content.Context
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import java.lang.ref.WeakReference
import java.lang.reflect.Member

object HookManager {

    @JvmStatic
    lateinit var context: WeakReference<Context>

    @JvmStatic
    fun registerHook(hook: Hook) =
        XposedBridge.hookMethod(
            hook.type.getDeclaredMethod(hook.method, *hook.argTypes),
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    hook.before(param)
                }

                override fun afterHookedMethod(param: MethodHookParam) {
                    hook.after(param)
                }
            })

    @JvmStatic
    fun invokeOriginal(method: Member, obj: Any?, vararg args: Any?) =
        XposedBridge.invokeOriginalMethod(method, obj, args)

    @JvmStatic
    fun isHooked(method: Member) = XposedBridge.isHooked(method)

    @JvmStatic
    fun hookAllConstructors(clazz: Class<*>, callback: XC_MethodHook) =
        XposedBridge.hookAllConstructors(clazz, callback)

    @JvmStatic
    fun hookAllMethods(clazz: Class<*>, methodName: String, callback: XC_MethodHook) =
        XposedBridge.hookAllMethods(clazz, methodName, callback)

    fun deoptimizeMethod(member: Member) = XposedBridge.deoptimizeMethod(member)

    fun disableHiddenApiRestrictions() = XposedBridge.disableHiddenApiRestrictions()

    fun disableProfileSaver() = XposedBridge.disableProfileSaver()

    fun makeClassInheritable(clazz: Class<*>) = XposedBridge.makeClassInheritable(clazz)
}

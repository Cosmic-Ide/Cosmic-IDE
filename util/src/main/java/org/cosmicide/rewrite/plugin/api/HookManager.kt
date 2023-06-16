/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.rewrite.plugin.api

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import java.lang.reflect.Member

object HookManager {
    fun registerHook(hook: Hook) {
        XposedBridge.hookMethod(hook.type.getDeclaredMethod(hook.method), object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                hook.before(param)
            }

            override fun afterHookedMethod(param: MethodHookParam) {
                hook.after(param)
            }
        })
    }


    fun invokeOriginal(method: Member, obj: Any?, vararg args: Any?): Any? {
        return XposedBridge.invokeOriginalMethod(method, obj, args)
    }

    fun isHooked(method: Member): Boolean {
        return XposedBridge.isHooked(method)
    }

    fun hookAllConstructors(clazz: Class<*>, callback: XC_MethodHook) {
        XposedBridge.hookAllConstructors(clazz, callback)
    }

    fun hookAllMethods(clazz: Class<*>, methodName: String, callback: XC_MethodHook) {
        XposedBridge.hookAllMethods(clazz, methodName, callback)
    }
}

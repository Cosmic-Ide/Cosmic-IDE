/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.rewrite.plugin.api

import de.robv.android.xposed.XC_MethodHook.MethodHookParam
import org.cosmicide.rewrite.util.MultipleDexClassLoader

/**
 * A hook that can be registered with the HookManager.
 * @param method The name of the method to hook.
 * @param argTypes The types of the arguments of the method to hook.
 * @param type The class that contains the method to hook.
 */
open class Hook(
    open val method: String,
    open vararg val argTypes: Class<*>,
    open val type: Class<*>
) {

    /**
     * @param methodName The name of the method to hook.
     * @param args The types of the arguments of the method to hook.
     * @param clazz The class that contains the method to hook.
     * @param useSharedClassLoader Whether to use the shared ClassLoader that loads all plugins or not. This could be useful if you wanna change the behaviour of other plugins. Make sure to check if the plugin is loaded before using this.
     */
    constructor(
        methodName: String,
        vararg args: Class<*>,
        clazz: String,
        useSharedClassLoader: Boolean = false
    ) : this(
        method = methodName,
        argTypes = args,
        type = if (useSharedClassLoader) MultipleDexClassLoader.INSTANCE.loader.loadClass(clazz) else Class.forName(
            clazz
        )
    )

    open fun before(param: MethodHookParam) = run {}
    open fun after(param: MethodHookParam) = run {}
}

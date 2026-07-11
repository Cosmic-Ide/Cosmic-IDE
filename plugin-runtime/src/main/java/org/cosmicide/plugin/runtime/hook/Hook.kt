/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.plugin.runtime.hook

import org.cosmicide.plugin.runtime.loading.SharedPluginClassLoader
import top.canyie.pine.Pine

open class Hook(
    open val method: String,
    open vararg val argTypes: Class<*>,
    open val type: Class<*>
) {

    constructor(
        methodName: String,
        vararg args: Class<*>,
        clazz: String,
        useSharedPluginClassLoader: Boolean = false
    ) : this(
        method = methodName,
        argTypes = args,
        type = if (useSharedPluginClassLoader) {
            SharedPluginClassLoader.loader.loadClass(clazz)
        } else {
            Class.forName(clazz)
        }
    )

    open fun before(param: Pine.CallFrame) = Unit

    open fun after(param: Pine.CallFrame) = Unit
}

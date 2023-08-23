/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.compile.ssvm.native

import dev.xdark.ssvm.VirtualMachine
import dev.xdark.ssvm.api.MethodInvoker
import dev.xdark.ssvm.mirror.InstanceJavaClass
import org.objectweb.asm.Type
import java.io.FileDescriptor

class SunFileKeyNatives : NativeInitializer {
    override fun init(vm: VirtualMachine) {
        val fileKey = vm.findBootstrapClass("sun/nio/ch/FileKey") as InstanceJavaClass

        vm.`interface`.setInvoker(
            fileKey,
            "initIDs",
            Type.getMethodDescriptor(Type.VOID_TYPE),
            MethodInvoker.noop()
        )

        vm.`interface`.setInvoker(
            fileKey,
            "init",
            Type.getMethodDescriptor(
                Type.VOID_TYPE,
                Type.getType(FileDescriptor::class.java)
            ),
            MethodInvoker.noop()
        )
    }
}
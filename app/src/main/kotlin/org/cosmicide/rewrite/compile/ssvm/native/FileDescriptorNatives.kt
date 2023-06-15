/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.rewrite.compile.ssvm.native

import dev.xdark.ssvm.VirtualMachine
import dev.xdark.ssvm.api.MethodInvoker
import dev.xdark.ssvm.mirror.InstanceJavaClass
import org.objectweb.asm.Type

class FileDescriptorNatives : NativeInitializer {
    override fun init(vm: VirtualMachine) {
        val fileDescriptor = vm.findBootstrapClass("java/io/FileDescriptor") as InstanceJavaClass

        vm.`interface`.setInvoker(
            fileDescriptor,
            "init",
            Type.getMethodDescriptor(Type.VOID_TYPE),
            MethodInvoker.noop()
        )
    }
}
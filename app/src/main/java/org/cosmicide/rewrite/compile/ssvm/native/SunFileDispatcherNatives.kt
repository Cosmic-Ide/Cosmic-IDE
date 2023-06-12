/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.rewrite.compile.ssvm.native


import dev.xdark.ssvm.VirtualMachine
import dev.xdark.ssvm.api.MethodInvoker
import dev.xdark.ssvm.execution.Result
import dev.xdark.ssvm.mirror.InstanceJavaClass
import dev.xdark.ssvm.value.IntValue
import org.objectweb.asm.Type
import java.io.FileDescriptor

class SunFileDispatcherNatives : NativeInitializer {
    override fun init(vm: VirtualMachine) {
        val fileDispatcher =
            vm.findBootstrapClass("sun/nio/ch/FileDispatcherImpl") as InstanceJavaClass

        // no-op, handled in real vm
        vm.`interface`.setInvoker(
            fileDispatcher,
            "init",
            Type.getMethodDescriptor(Type.VOID_TYPE),
            MethodInvoker.noop()
        )

        vm.`interface`.setInvoker(
            fileDispatcher,
            "release0",
            Type.getMethodDescriptor(
                Type.VOID_TYPE,
                Type.getType(FileDescriptor::class.java),
                Type.LONG_TYPE,
                Type.LONG_TYPE
            )
        ) {
            // Not sure what to delegate here
            return@setInvoker Result.ABORT
        }

        vm.`interface`.setInvoker(
            fileDispatcher,
            "lock0",
            Type.getMethodDescriptor(
                Type.INT_TYPE,
                Type.getType(FileDescriptor::class.java),
                Type.BOOLEAN_TYPE,
                Type.LONG_TYPE,
                Type.LONG_TYPE,
                Type.BOOLEAN_TYPE
            )
        ) {
            it.result = IntValue.of(0)
            return@setInvoker Result.ABORT
        }
    }
}
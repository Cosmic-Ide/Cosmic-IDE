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
import dev.xdark.ssvm.value.LongValue
import org.objectweb.asm.Type

class SunNativeThreadNatives : NativeInitializer {
    override fun init(vm: VirtualMachine) {
        val nativeThread = vm.findBootstrapClass("sun/nio/ch/NativeThread") as InstanceJavaClass

        vm.`interface`.setInvoker(
            nativeThread,
            "init",
            Type.getMethodDescriptor(Type.VOID_TYPE),
            MethodInvoker.noop()
        )

        vm.`interface`.setInvoker(
            nativeThread,
            "current",
            Type.getMethodDescriptor(Type.LONG_TYPE)
        ) {
            // TODO: not sure what value to return here
            it.result = LongValue.of(0)
            return@setInvoker Result.ABORT
        }
    }
}
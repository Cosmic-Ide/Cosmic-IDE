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

class SunIOUtilNatives : NativeInitializer {
    override fun init(vm: VirtualMachine) {
        val ioUtils = vm.findBootstrapClass("sun/nio/ch/IOUtil") as InstanceJavaClass

        // no-op, already handled in the real vm
        vm.`interface`.setInvoker(
            ioUtils,
            "initIDs",
            Type.getMethodDescriptor(Type.VOID_TYPE),
            MethodInvoker.noop()
        )

        vm.`interface`.setInvoker(
            ioUtils,
            "iovMax",
            Type.getMethodDescriptor(Type.INT_TYPE)
        ) {
            // Not sure if its correct to return a random value here
            it.result = IntValue.of(0)
            return@setInvoker Result.ABORT
        }
    }
}
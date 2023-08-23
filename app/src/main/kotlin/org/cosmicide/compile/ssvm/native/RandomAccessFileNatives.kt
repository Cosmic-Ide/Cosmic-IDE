/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.compile.ssvm.native


import dev.xdark.ssvm.VirtualMachine
import dev.xdark.ssvm.api.MethodInvoker
import dev.xdark.ssvm.execution.Result
import dev.xdark.ssvm.mirror.InstanceJavaClass
import dev.xdark.ssvm.value.IntValue
import dev.xdark.ssvm.value.Value
import org.objectweb.asm.Type

class RandomAccessFileNatives : NativeInitializer {
    override fun init(vm: VirtualMachine) {
        val randomAccessFile =
            vm.findBootstrapClass("java/io/RandomAccessFile") as InstanceJavaClass

        vm.`interface`.setInvoker(
            randomAccessFile,
            "initIDs",
            Type.getMethodDescriptor(Type.VOID_TYPE),
            MethodInvoker.noop()
        )

        vm.`interface`.setInvoker(
            randomAccessFile,
            "open",
            Type.getMethodDescriptor(
                Type.VOID_TYPE,
                Type.getType(String::class.java),
                Type.INT_TYPE
            )
        ) {
            val value = it.locals.load<Value>(1)
            val path: String = vm.helper.readUtf8(value)
            val modeValue = it.locals.load<IntValue>(2)
            vm.fileDescriptorManager.open(path, modeValue.asInt())
            return@setInvoker Result.ABORT
        }

        // no-op, handled in the open() method
        vm.`interface`.setInvoker(
            randomAccessFile,
            "open0",
            Type.getMethodDescriptor(Type.VOID_TYPE, Type.INT_TYPE),
            MethodInvoker.noop()
        )

        vm.`interface`.setInvoker(
            randomAccessFile,
            "close0",
            Type.getMethodDescriptor(Type.VOID_TYPE)
        ) {
            // TODO: read the path field on the random access file and close that
            return@setInvoker Result.ABORT
        }
    }
}
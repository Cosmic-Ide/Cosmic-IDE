/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.rewrite.compile.ssvm.native


import dev.xdark.ssvm.VirtualMachine
import dev.xdark.ssvm.execution.Result
import dev.xdark.ssvm.mirror.InstanceJavaClass
import dev.xdark.ssvm.value.InstanceValue
import dev.xdark.ssvm.value.IntValue
import dev.xdark.ssvm.value.Value
import org.objectweb.asm.Type
import java.io.File

class OldFileSystemNativesEx : NativeInitializer {
    override fun init(vm: VirtualMachine) {
        val fs: InstanceJavaClass = (vm.findBootstrapClass("java/io/WinNTFileSystem")
            ?: vm.findBootstrapClass("java/io/UnixFileSystem")) as InstanceJavaClass

        vm.`interface`.setInvoker(
            fs,
            "checkAccess",
            Type.getMethodDescriptor(
                Type.BOOLEAN_TYPE,
                Type.getType(File::class.java),
                Type.INT_TYPE
            )
        ) {
            val value = it.locals.load<Value>(1)
            val path: String =
                vm.helper.readUtf8((value as InstanceValue).getValue("path", "Ljava/lang/String;"))
            val result = if (File(path).canRead()) 1 else 0
            it.result = IntValue.of(result)
            return@setInvoker Result.ABORT
        }

        vm.`interface`.setInvoker(
            fs,
            "delete0",
            Type.getMethodDescriptor(Type.BOOLEAN_TYPE, Type.getType(File::class.java))
        ) {
            val value = it.locals.load<Value>(1)
            val path: String =
                vm.helper.readUtf8((value as InstanceValue).getValue("path", "Ljava/lang/String;"))
            val result = if (File(path).delete()) 1 else 0
            it.result = IntValue.of(result)
            return@setInvoker Result.ABORT
        }

        vm.`interface`.setInvoker(
            fs, "createDirectory", Type.getMethodDescriptor(
                Type.BOOLEAN_TYPE, Type.getType(File::class.java)
            )
        ) {
            val value = it.locals.load<Value>(1)
            val path: String =
                vm.helper.readUtf8((value as InstanceValue).getValue("path", "Ljava/lang/String;"))
            val result = if (File(path).mkdir()) 1 else 0
            it.result = IntValue.of(result)
            return@setInvoker Result.ABORT
        }
    }


}
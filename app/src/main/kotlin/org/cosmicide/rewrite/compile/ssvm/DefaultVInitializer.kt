/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.rewrite.compile.ssvm

import dev.xdark.ssvm.VMInitializer
import dev.xdark.ssvm.VirtualMachine
import org.cosmicide.rewrite.compile.ssvm.native.FileChannelImplNatives
import org.cosmicide.rewrite.compile.ssvm.native.FileDescriptorNatives
import org.cosmicide.rewrite.compile.ssvm.native.OldFileSystemNativesEx
import org.cosmicide.rewrite.compile.ssvm.native.RandomAccessFileNatives
import org.cosmicide.rewrite.compile.ssvm.native.SunFileDispatcherNatives
import org.cosmicide.rewrite.compile.ssvm.native.SunFileKeyNatives
import org.cosmicide.rewrite.compile.ssvm.native.SunIOUtilNatives
import org.cosmicide.rewrite.compile.ssvm.native.SunNativeThreadNatives
import org.cosmicide.rewrite.compile.ssvm.native.SunSharedFileLockTableNatives

class DefaultVMInitializer : VMInitializer {

    private val nativeInitializers = setOf(
        FileChannelImplNatives(),
        FileDescriptorNatives(),
        RandomAccessFileNatives(),
        OldFileSystemNativesEx(),
        SunFileDispatcherNatives(),
        SunFileKeyNatives(),
        SunSharedFileLockTableNatives(),
        SunIOUtilNatives(),
        SunNativeThreadNatives()
    )

    override fun initBegin(vm: VirtualMachine) {
        vm.properties.apply {
            setProperty("sun.stderr.encoding", "UTF-8")
            setProperty("sun.stdout.encoding", "UTF-8")
            setProperty("sun.jnu.encoding", "UTF-8")
            setProperty("line.separator", "\n")
            setProperty("path.separator", ":")
            setProperty("file.separator", "/")
            setProperty("user.dir", "/home/mike")
            setProperty("user.name", "mike")
            setProperty("os.version", "10.0")
            setProperty("os.arch", "arm64")
            setProperty("os.name", "Linux")
            setProperty("file.encoding", "UTF-8")
        }

        // Contains android stuffs
        vm.getenv().clear()
    }

    override fun nativeInit(vm: VirtualMachine) {
        nativeInitializers.forEach {
            it.init(vm)
        }
    }
}
/************************************************************************************
 * This file is part of Java Language Server (https://github.com/itsaky/java-language-server)
 *
 * Copyright (C) 2021 Akash Yadav
 *
 * Java Language Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Java Language Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Java Language Server.  If not, see <https://www.gnu.org/licenses/>.
 *
 **************************************************************************************/

package org.javacs;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

// Read the classfile format defined in https://docs.oracle.com/javase/specs/jvms/se11/html/jvms-4.html
class ClassHeader {

    private static final int ACC_PUBLIC = 0x0001; // Declared public; may be accessed from outside its package.
    private static final int ACC_FINAL = 0x0010; // Declared final; no subclasses allowed.
    private static final int ACC_SUPER =
            0x0020; // Treat superclass methods specially when invoked by the invokespecial instruction.
    private static final int ACC_INTERFACE = 0x0200; // Is an interface, not a class.
    private static final int ACC_ABSTRACT = 0x0400; // Declared abstract; must not be instantiated.
    private static final int ACC_SYNTHETIC = 0x1000; // Declared synthetic; not present in the source code.
    private static final int ACC_ANNOTATION = 0x2000; // Declared as an annotation type.
    private static final int ACC_ENUM = 0x4000; // Declared as an enum type.
    private static final int ACC_MODULE = 0x8000; // Is a module, not a class or interface.
    private static final int CONSTANT_Class = 7;
    private static final int CONSTANT_Fieldref = 9;
    private static final int CONSTANT_Methodref = 10;
    private static final int CONSTANT_InterfaceMethodref = 11;
    private static final int CONSTANT_String = 8;
    private static final int CONSTANT_Integer = 3;
    private static final int CONSTANT_Float = 4;
    private static final int CONSTANT_Long = 5;
    private static final int CONSTANT_Double = 6;
    private static final int CONSTANT_NameAndType = 12;
    private static final int CONSTANT_Utf8 = 1;
    private static final int CONSTANT_MethodHandle = 15;
    private static final int CONSTANT_MethodType = 16;
    private static final int CONSTANT_InvokeDynamic = 18;
    private static final int CONSTANT_Module = 19;
    private static final int CONSTANT_Package = 20;
    final boolean isPublic, isFinal, isInterface, isAbstract, isAnnotation, isEnum, isModule;
    private ClassHeader(DataInputStream in) {
        try {
            // u4             magic;
            // u2             minor_version;
            // u2             major_version;
            // u2             constant_pool_count;
            // cp_info        constant_pool[constant_pool_count-1];
            // u2             access_flags;
            var magic = in.readNBytes(4);
            var minorVersion = in.readUnsignedShort();
            var majorVersion = in.readUnsignedShort();
            var constantPoolCount = in.readUnsignedShort();
            var constants = new Constant[constantPoolCount];
            var i = 0;
            while (i < constantPoolCount - 1) {
                constants[i] = readConstant(in);
                i += slots(constants[i]);
            }
            var accessFlags = in.readUnsignedShort();

            this.isPublic = (accessFlags & ACC_PUBLIC) != 0;
            this.isFinal = (accessFlags & ACC_FINAL) != 0;
            this.isInterface = (accessFlags & ACC_INTERFACE) != 0;
            this.isAbstract = (accessFlags & ACC_ABSTRACT) != 0;
            this.isAnnotation = (accessFlags & ACC_ANNOTATION) != 0;
            this.isEnum = (accessFlags & ACC_ENUM) != 0;
            this.isModule = (accessFlags & ACC_MODULE) != 0;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static ClassHeader of(InputStream in) {
        return new ClassHeader(new DataInputStream(in));
    }

    private Constant readConstant(DataInputStream in) throws IOException {
        var tag = (short) in.readByte();
        switch (tag) {
            case CONSTANT_Class:
            case CONSTANT_String:
            case CONSTANT_MethodType: {
                var info = in.readNBytes(2);
                return new Constant(tag, info);
            }
            case CONSTANT_Fieldref:
            case CONSTANT_Methodref:
            case CONSTANT_InterfaceMethodref:
            case CONSTANT_Integer:
            case CONSTANT_Float:
            case CONSTANT_NameAndType:
            case CONSTANT_InvokeDynamic: {
                var info = in.readNBytes(4);
                return new Constant(tag, info);
            }
            case CONSTANT_Long:
            case CONSTANT_Double: {
                var info = in.readNBytes(8);
                return new Constant(tag, info);
            }
            case CONSTANT_Utf8: {
                var length = in.readUnsignedShort();
                var string = in.readNBytes(length);
                return new Constant(tag, string);
            }
            case CONSTANT_MethodHandle:
            case CONSTANT_Module:
            case CONSTANT_Package: {
                var info = in.readNBytes(3);
                return new Constant(tag, info);
            }
            default:
                throw new RuntimeException("Don't know what to do with " + tag);
        }
    }

    private int slots(Constant c) {
        switch (c.tag) {
            case CONSTANT_Long:
            case CONSTANT_Double:
                return 2;
            default:
                return 1;
        }
    }

    private static class Constant {
        final short tag;
        final byte[] info;

        Constant(short tag, byte[] info) {
            this.tag = tag;
            this.info = info;
        }
    }
}

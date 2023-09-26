/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package sun.misc;

import jdk.internal.vm.annotation.ForceInline;
import jdk.internal.misc.VM;
import jdk.internal.reflect.CallerSensitive;
import jdk.internal.reflect.Reflection;

import java.lang.reflect.Field;
import java.util.Set;

public final class Unsafe {

    private Unsafe() {}

    private static final Unsafe theUnsafe = new Unsafe();
    private static final jdk.internal.misc.Unsafe theInternalUnsafe = jdk.internal.misc.Unsafe.getUnsafe();

    @CallerSensitive
    public static Unsafe getUnsafe() {
        return theUnsafe;
    }

    @ForceInline
    public int getInt(Object o, long offset) {
        return theInternalUnsafe.getInt(o, offset);
    }

    @ForceInline
    public void putInt(Object o, long offset, int x) {
        theInternalUnsafe.putInt(o, offset, x);
    }

    @ForceInline
    public Object getObject(Object o, long offset) {
        return theInternalUnsafe.getReference(o, offset);
    }

    @ForceInline
    public void putObject(Object o, long offset, Object x) {
        theInternalUnsafe.putReference(o, offset, x);
    }

    @ForceInline
    public boolean getBoolean(Object o, long offset) {
        return theInternalUnsafe.getBoolean(o, offset);
    }

    @ForceInline
    public void putBoolean(Object o, long offset, boolean x) {
        theInternalUnsafe.putBoolean(o, offset, x);
    }

    @ForceInline
    public byte getByte(Object o, long offset) {
        return theInternalUnsafe.getByte(o, offset);
    }

    @ForceInline
    public void putByte(Object o, long offset, byte x) {
        theInternalUnsafe.putByte(o, offset, x);
    }

    @ForceInline
    public short getShort(Object o, long offset) {
        return theInternalUnsafe.getShort(o, offset);
    }

    @ForceInline
    public void putShort(Object o, long offset, short x) {
        theInternalUnsafe.putShort(o, offset, x);
    }

    @ForceInline
    public char getChar(Object o, long offset) {
        return theInternalUnsafe.getChar(o, offset);
    }

    @ForceInline
    public void putChar(Object o, long offset, char x) {
        theInternalUnsafe.putChar(o, offset, x);
    }

    @ForceInline
    public long getLong(Object o, long offset) {
        return theInternalUnsafe.getLong(o, offset);
    }

    @ForceInline
    public void putLong(Object o, long offset, long x) {
        theInternalUnsafe.putLong(o, offset, x);
    }

    @ForceInline
    public float getFloat(Object o, long offset) {
        return theInternalUnsafe.getFloat(o, offset);
    }

    @ForceInline
    public void putFloat(Object o, long offset, float x) {
        theInternalUnsafe.putFloat(o, offset, x);
    }

    @ForceInline
    public double getDouble(Object o, long offset) {
        return theInternalUnsafe.getDouble(o, offset);
    }

    @ForceInline
    public void putDouble(Object o, long offset, double x) {
        theInternalUnsafe.putDouble(o, offset, x);
    }

    // These work on values in the C heap.

    
    @ForceInline
    public byte getByte(long address) {
        return theInternalUnsafe.getByte(address);
    }

    
    @ForceInline
    public void putByte(long address, byte x) {
        theInternalUnsafe.putByte(address, x);
    }

    
    @ForceInline
    public short getShort(long address) {
        return theInternalUnsafe.getShort(address);
    }

    
    @ForceInline
    public void putShort(long address, short x) {
        theInternalUnsafe.putShort(address, x);
    }

    
    @ForceInline
    public char getChar(long address) {
        return theInternalUnsafe.getChar(address);
    }

    
    @ForceInline
    public void putChar(long address, char x) {
        theInternalUnsafe.putChar(address, x);
    }

    
    @ForceInline
    public int getInt(long address) {
        return theInternalUnsafe.getInt(address);
    }

    
    @ForceInline
    public void putInt(long address, int x) {
        theInternalUnsafe.putInt(address, x);
    }

    
    @ForceInline
    public long getLong(long address) {
        return theInternalUnsafe.getLong(address);
    }

    
    @ForceInline
    public void putLong(long address, long x) {
        theInternalUnsafe.putLong(address, x);
    }

    
    @ForceInline
    public float getFloat(long address) {
        return theInternalUnsafe.getFloat(address);
    }

    
    @ForceInline
    public void putFloat(long address, float x) {
        theInternalUnsafe.putFloat(address, x);
    }

    
    @ForceInline
    public double getDouble(long address) {
        return theInternalUnsafe.getDouble(address);
    }

    
    @ForceInline
    public void putDouble(long address, double x) {
        theInternalUnsafe.putDouble(address, x);
    }


    
    @ForceInline
    public long getAddress(long address) {
        return theInternalUnsafe.getAddress(address);
    }

    
    @ForceInline
    public void putAddress(long address, long x) {
        theInternalUnsafe.putAddress(address, x);
    }


    /// wrappers for malloc, realloc, free:

    
    @ForceInline
    public long allocateMemory(long bytes) {
        return theInternalUnsafe.allocateMemory(bytes);
    }

    
    @ForceInline
    public long reallocateMemory(long address, long bytes) {
        return theInternalUnsafe.reallocateMemory(address, bytes);
    }

    
    @ForceInline
    public void setMemory(Object o, long offset, long bytes, byte value) {
        theInternalUnsafe.setMemory(o, offset, bytes, value);
    }

    
    @ForceInline
    public void setMemory(long address, long bytes, byte value) {
        theInternalUnsafe.setMemory(address, bytes, value);
    }

    
    @ForceInline
    public void copyMemory(Object srcBase, long srcOffset,
                           Object destBase, long destOffset,
                           long bytes) {
        theInternalUnsafe.copyMemory(srcBase, srcOffset, destBase, destOffset, bytes);
    }

    
    @ForceInline
    public void copyMemory(long srcAddress, long destAddress, long bytes) {
        theInternalUnsafe.copyMemory(srcAddress, destAddress, bytes);
    }

    
    @ForceInline
    public void freeMemory(long address) {
        theInternalUnsafe.freeMemory(address);
    }

    /// random queries

    
    public static final int INVALID_FIELD_OFFSET = jdk.internal.misc.Unsafe.INVALID_FIELD_OFFSET;

    
    @ForceInline
    public long objectFieldOffset(Field f) {
        return 0L;
    }

    
    @ForceInline
    public long staticFieldOffset(Field f) {
        return 0L;
    }

    
    @ForceInline
    public Object staticFieldBase(Field f) {
        return 0L;
    }

    @ForceInline
    public boolean shouldBeInitialized(Class<?> c) {
        return theInternalUnsafe.shouldBeInitialized(c);
    }

    @ForceInline
    public void ensureClassInitialized(Class<?> c) {
        theInternalUnsafe.ensureClassInitialized(c);
    }

    @ForceInline
    public int arrayBaseOffset(Class<?> arrayClass) {
        return theInternalUnsafe.arrayBaseOffset(arrayClass);
    }

    public static final int ARRAY_BOOLEAN_BASE_OFFSET = jdk.internal.misc.Unsafe.ARRAY_BOOLEAN_BASE_OFFSET;

    public static final int ARRAY_BYTE_BASE_OFFSET = jdk.internal.misc.Unsafe.ARRAY_BYTE_BASE_OFFSET;

    public static final int ARRAY_SHORT_BASE_OFFSET = jdk.internal.misc.Unsafe.ARRAY_SHORT_BASE_OFFSET;

    public static final int ARRAY_CHAR_BASE_OFFSET = jdk.internal.misc.Unsafe.ARRAY_CHAR_BASE_OFFSET;

    public static final int ARRAY_INT_BASE_OFFSET = jdk.internal.misc.Unsafe.ARRAY_INT_BASE_OFFSET;

    public static final int ARRAY_LONG_BASE_OFFSET = jdk.internal.misc.Unsafe.ARRAY_LONG_BASE_OFFSET;

    public static final int ARRAY_FLOAT_BASE_OFFSET = jdk.internal.misc.Unsafe.ARRAY_FLOAT_BASE_OFFSET;

    public static final int ARRAY_DOUBLE_BASE_OFFSET = jdk.internal.misc.Unsafe.ARRAY_DOUBLE_BASE_OFFSET;

    public static final int ARRAY_OBJECT_BASE_OFFSET = jdk.internal.misc.Unsafe.ARRAY_OBJECT_BASE_OFFSET;

    @ForceInline
    public int arrayIndexScale(Class<?> arrayClass) {
        return theInternalUnsafe.arrayIndexScale(arrayClass);
    }

    public static final int ARRAY_BOOLEAN_INDEX_SCALE = jdk.internal.misc.Unsafe.ARRAY_BOOLEAN_INDEX_SCALE;

    public static final int ARRAY_BYTE_INDEX_SCALE = jdk.internal.misc.Unsafe.ARRAY_BYTE_INDEX_SCALE;

    public static final int ARRAY_SHORT_INDEX_SCALE = jdk.internal.misc.Unsafe.ARRAY_SHORT_INDEX_SCALE;

    public static final int ARRAY_CHAR_INDEX_SCALE = jdk.internal.misc.Unsafe.ARRAY_CHAR_INDEX_SCALE;

    public static final int ARRAY_INT_INDEX_SCALE = jdk.internal.misc.Unsafe.ARRAY_INT_INDEX_SCALE;

    public static final int ARRAY_LONG_INDEX_SCALE = jdk.internal.misc.Unsafe.ARRAY_LONG_INDEX_SCALE;

    public static final int ARRAY_FLOAT_INDEX_SCALE = jdk.internal.misc.Unsafe.ARRAY_FLOAT_INDEX_SCALE;

    public static final int ARRAY_DOUBLE_INDEX_SCALE = jdk.internal.misc.Unsafe.ARRAY_DOUBLE_INDEX_SCALE;

    public static final int ARRAY_OBJECT_INDEX_SCALE = jdk.internal.misc.Unsafe.ARRAY_OBJECT_INDEX_SCALE;

    @ForceInline
    public int addressSize() {
        return theInternalUnsafe.addressSize();
    }

    public static final int ADDRESS_SIZE = theInternalUnsafe.addressSize();

    @ForceInline
    public int pageSize() {
        return theInternalUnsafe.pageSize();
    }


    @ForceInline
    public Object allocateInstance(Class<?> cls)
            throws InstantiationException {
        return theInternalUnsafe.allocateInstance(cls);
    }

    @ForceInline
    public void throwException(Throwable ee) {
        theInternalUnsafe.throwException(ee);
    }

    @ForceInline
    public final boolean compareAndSwapObject(Object o, long offset,
                                              Object expected,
                                              Object x) {
        return theInternalUnsafe.compareAndSetReference(o, offset, expected, x);
    }

    @ForceInline
    public final boolean compareAndSwapInt(Object o, long offset,
                                           int expected,
                                           int x) {
        return theInternalUnsafe.compareAndSetInt(o, offset, expected, x);
    }

    @ForceInline
    public final boolean compareAndSwapLong(Object o, long offset,
                                            long expected,
                                            long x) {
        return theInternalUnsafe.compareAndSetLong(o, offset, expected, x);
    }

    @ForceInline
    public Object getObjectVolatile(Object o, long offset) {
        return theInternalUnsafe.getReferenceVolatile(o, offset);
    }

    
    @ForceInline
    public void putObjectVolatile(Object o, long offset, Object x) {
        theInternalUnsafe.putReferenceVolatile(o, offset, x);
    }

    
    @ForceInline
    public int getIntVolatile(Object o, long offset) {
        return theInternalUnsafe.getIntVolatile(o, offset);
    }

    
    @ForceInline
    public void putIntVolatile(Object o, long offset, int x) {
        theInternalUnsafe.putIntVolatile(o, offset, x);
    }

    
    @ForceInline
    public boolean getBooleanVolatile(Object o, long offset) {
        return theInternalUnsafe.getBooleanVolatile(o, offset);
    }

    
    @ForceInline
    public void putBooleanVolatile(Object o, long offset, boolean x) {
        theInternalUnsafe.putBooleanVolatile(o, offset, x);
    }

    
    @ForceInline
    public byte getByteVolatile(Object o, long offset) {
        return theInternalUnsafe.getByteVolatile(o, offset);
    }

    
    @ForceInline
    public void putByteVolatile(Object o, long offset, byte x) {
        theInternalUnsafe.putByteVolatile(o, offset, x);
    }

    
    @ForceInline
    public short getShortVolatile(Object o, long offset) {
        return theInternalUnsafe.getShortVolatile(o, offset);
    }

    
    @ForceInline
    public void putShortVolatile(Object o, long offset, short x) {
        theInternalUnsafe.putShortVolatile(o, offset, x);
    }

    
    @ForceInline
    public char getCharVolatile(Object o, long offset) {
        return theInternalUnsafe.getCharVolatile(o, offset);
    }

    
    @ForceInline
    public void putCharVolatile(Object o, long offset, char x) {
        theInternalUnsafe.putCharVolatile(o, offset, x);
    }

    
    @ForceInline
    public long getLongVolatile(Object o, long offset) {
        return theInternalUnsafe.getLongVolatile(o, offset);
    }

    
    @ForceInline
    public void putLongVolatile(Object o, long offset, long x) {
        theInternalUnsafe.putLongVolatile(o, offset, x);
    }

    
    @ForceInline
    public float getFloatVolatile(Object o, long offset) {
        return theInternalUnsafe.getFloatVolatile(o, offset);
    }

    
    @ForceInline
    public void putFloatVolatile(Object o, long offset, float x) {
        theInternalUnsafe.putFloatVolatile(o, offset, x);
    }

    
    @ForceInline
    public double getDoubleVolatile(Object o, long offset) {
        return theInternalUnsafe.getDoubleVolatile(o, offset);
    }

    
    @ForceInline
    public void putDoubleVolatile(Object o, long offset, double x) {
        theInternalUnsafe.putDoubleVolatile(o, offset, x);
    }

    
    @ForceInline
    public void putOrderedObject(Object o, long offset, Object x) {
        theInternalUnsafe.putReferenceRelease(o, offset, x);
    }

    
    @ForceInline
    public void putOrderedInt(Object o, long offset, int x) {
        theInternalUnsafe.putIntRelease(o, offset, x);
    }

    
    @ForceInline
    public void putOrderedLong(Object o, long offset, long x) {
        theInternalUnsafe.putLongRelease(o, offset, x);
    }

    
    @ForceInline
    public void unpark(Object thread) {
        theInternalUnsafe.unpark(thread);
    }

    
    @ForceInline
    public void park(boolean isAbsolute, long time) {
        theInternalUnsafe.park(isAbsolute, time);
    }

    
    @ForceInline
    public int getLoadAverage(double[] loadavg, int nelems) {
        return theInternalUnsafe.getLoadAverage(loadavg, nelems);
    }

    // The following contain CAS-based Java implementations used on
    // platforms not supporting native instructions

    
    @ForceInline
    public final int getAndAddInt(Object o, long offset, int delta) {
        return 0;
    }

    
    @ForceInline
    public final long getAndAddLong(Object o, long offset, long delta) {
        return 0;
    }

    
    @ForceInline
    public final int getAndSetInt(Object o, long offset, int newValue) {
        return 0;
    }

    
    @ForceInline
    public final long getAndSetLong(Object o, long offset, long newValue) {
        return 0;
    }

    
    @ForceInline
    public final Object getAndSetObject(Object o, long offset, Object newValue) {
        return 0;
    }


    
    @ForceInline
    public void loadFence() {
    }

    
    @ForceInline
    public void storeFence() {

    }

    
    @ForceInline
    public void fullFence() {

    }

    
    public void invokeCleaner(java.nio.ByteBuffer directBuffer) {
        if (!directBuffer.isDirect())
            throw new IllegalArgumentException("buffer is non-direct");

        theInternalUnsafe.invokeCleaner(directBuffer);
    }
}

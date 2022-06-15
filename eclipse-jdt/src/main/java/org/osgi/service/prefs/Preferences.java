/*******************************************************************************
 * Copyright (c) Contributors to the Eclipse Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0 
 *******************************************************************************/

package org.osgi.service.prefs;

/**
 * A node in a hierarchical collection of preference data.
 * 
 * <p>
 * This interface allows applications to store and retrieve user and system
 * preference data. This data is stored persistently in an
 * implementation-dependent backing store. Typical implementations include flat
 * files, OS-specific registries, directory servers and SQL databases.
 * 
 * <p>
 * For each bundle, there is a separate tree of nodes for each user, and one for
 * system preferences. The precise description of "user" and "system" will vary
 * from one bundle to another. Typical information stored in the user preference
 * tree might include font choice, and color choice for a bundle which interacts
 * with the user via a servlet. Typical information stored in the system
 * preference tree might include installation data, or things like high score
 * information for a game program.
 * 
 * <p>
 * Nodes in a preference tree are named in a similar fashion to directories in a
 * hierarchical file system. Every node in a preference tree has a <i>node name
 * </i> (which is not necessarily unique), a unique <i>absolute path name </i>,
 * and a path name <i>relative </i> to each ancestor including itself.
 * 
 * <p>
 * The root node has a node name of the empty {@code String} object (""). Every
 * other node has an arbitrary node name, specified at the time it is created.
 * The only restrictions on this name are that it cannot be the empty string,
 * and it cannot contain the slash character ('/').
 * 
 * <p>
 * The root node has an absolute path name of {@code "/"}. Children of the root
 * node have absolute path names of {@code "/" + } <i>&lt;node name&gt; </i>.
 * All other nodes have absolute path names of <i>&lt;parent's absolute path
 * name&gt; </i> {@code  + "/" + } <i>&lt;node name&gt; </i>. Note that all
 * absolute path names begin with the slash character.
 * 
 * <p>
 * A node <i>n </i>'s path name relative to its ancestor <i>a </i> is simply the
 * string that must be appended to <i>a </i>'s absolute path name in order to
 * form <i>n </i>'s absolute path name, with the initial slash character (if
 * present) removed. Note that:
 * <ul>
 * <li>No relative path names begin with the slash character.</li>
 * <li>Every node's path name relative to itself is the empty string.</li>
 * <li>Every node's path name relative to its parent is its node name (except
 * for the root node, which does not have a parent).</li>
 * <li>Every node's path name relative to the root is its absolute path name
 * with the initial slash character removed.</li>
 * </ul>
 * 
 * <p>
 * Note finally that:
 * <ul>
 * <li>No path name contains multiple consecutive slash characters.</li>
 * <li>No path name with the exception of the root's absolute path name end in
 * the slash character.</li>
 * <li>Any string that conforms to these two rules is a valid path name.</li>
 * </ul>
 * 
 * <p>
 * Each {@code Preference} node has zero or more properties associated with it,
 * where a property consists of a name and a value. The bundle writer is free to
 * choose any appropriate names for properties. Their values can be of type
 * {@code String},{@code long},{@code int},{@code boolean}, {@code byte[]},
 * {@code float}, or {@code double} but they can always be accessed as if they
 * were {@code String} objects.
 * 
 * <p>
 * All node name and property name comparisons are case-sensitive.
 * 
 * <p>
 * All of the methods that modify preference data are permitted to operate
 * asynchronously; they may return immediately, and changes will eventually
 * propagate to the persistent backing store, with an implementation-dependent
 * delay. The {@code flush} method may be used to synchronously force updates to
 * the backing store.
 * 
 * <p>
 * Implementations must automatically attempt to flush to the backing store any
 * pending updates for a bundle's preferences when the bundle is stopped or
 * otherwise ungets the Preferences Service.
 * 
 * <p>
 * The methods in this class may be invoked concurrently by multiple threads in
 * a single Java Virtual Machine (JVM) without the need for external
 * synchronization, and the results will be equivalent to some serial execution.
 * If this class is used concurrently <i>by multiple JVMs </i> that store their
 * preference data in the same backing store, the data store will not be
 * corrupted, but no other guarantees are made concerning the consistency of the
 * preference data.
 * 
 * @noimplement
 * @author $Id$
 */
public interface Preferences {
	/**
	 * Associates the specified value with the specified key in this node.
	 * 
	 * @param key key with which the specified value is to be associated.
	 * @param value value to be associated with the specified key.
	 * @throws NullPointerException if {@code key} or {@code value} is
	 *         {@code null}.
	 * @throws IllegalStateException if this node (or an ancestor) has been
	 *         removed with the {@link #removeNode()} method.
	 */
	public void put(String key, String value);

	/**
	 * Returns the value associated with the specified {@code key} in this node.
	 * Returns the specified default if there is no value associated with the
	 * {@code key}, or the backing store is inaccessible.
	 * 
	 * @param key key whose associated value is to be returned.
	 * @param def the value to be returned in the event that this node has no
	 *        value associated with {@code key} or the backing store is
	 *        inaccessible.
	 * @return the value associated with {@code key}, or {@code def} if no value
	 *         is associated with {@code key}.
	 * @throws IllegalStateException if this node (or an ancestor) has been
	 *         removed with the {@link #removeNode()} method.
	 * @throws NullPointerException if {@code key} is {@code null}. (A
	 *         {@code null} default <i>is </i> permitted.)
	 */
	public String get(String key, String def);

	/**
	 * Removes the value associated with the specified {@code key} in this node,
	 * if any.
	 * 
	 * @param key key whose mapping is to be removed from this node.
	 * @see #get(String,String)
	 * @throws IllegalStateException if this node (or an ancestor) has been
	 *         removed with the {@link #removeNode()} method.
	 */
	public void remove(String key);

	/**
	 * Removes all of the properties (key-value associations) in this node. This
	 * call has no effect on any descendants of this node.
	 * 
	 * @throws BackingStoreException if this operation cannot be completed due
	 *         to a failure in the backing store, or inability to communicate
	 *         with it.
	 * @throws IllegalStateException if this node (or an ancestor) has been
	 *         removed with the {@link #removeNode()} method.
	 * @see #remove(String)
	 */
	public void clear() throws BackingStoreException;

	/**
	 * Associates a {@code String} object representing the specified {@code int}
	 * value with the specified {@code key} in this node. The associated string
	 * is the one that would be returned if the {@code int} value were passed to
	 * {@code Integer.toString(int)}. This method is intended for use in
	 * conjunction with {@link #getInt(String, int)} method.
	 * 
	 * <p>
	 * Implementor's note: it is <i>not </i> necessary that the property value
	 * be represented by a {@code String} object in the backing store. If the
	 * backing store supports integer values, it is not unreasonable to use
	 * them. This implementation detail is not visible through the
	 * {@code Preferences} API, which allows the value to be read as an
	 * {@code int} (with {@code getInt} or a {@code String} (with {@code get})
	 * type.
	 * 
	 * @param key key with which the string form of value is to be associated.
	 * @param value {@code value} whose string form is to be associated with
	 *        {@code key}.
	 * @throws NullPointerException if {@code key} is {@code null}.
	 * @throws IllegalStateException if this node (or an ancestor) has been
	 *         removed with the {@link #removeNode()} method.
	 * @see #getInt(String,int)
	 */
	public void putInt(String key, int value);

	/**
	 * Returns the {@code int} value represented by the {@code String} object
	 * associated with the specified {@code key} in this node. The
	 * {@code String} object is converted to an {@code int} as by
	 * {@code Integer.parseInt(String)}. Returns the specified default if there
	 * is no value associated with the {@code key}, the backing store is
	 * inaccessible, or if {@code Integer.parseInt(String)} would throw a
	 * {@code NumberFormatException} if the associated {@code value} were
	 * passed. This method is intended for use in conjunction with the
	 * {@link #putInt(String, int)} method.
	 * 
	 * @param key key whose associated value is to be returned as an {@code int}
	 *        .
	 * @param def the value to be returned in the event that this node has no
	 *        value associated with {@code key} or the associated value cannot
	 *        be interpreted as an {@code int} or the backing store is
	 *        inaccessible.
	 * @return the {@code int} value represented by the {@code String} object
	 *         associated with {@code key} in this node, or {@code def} if the
	 *         associated value does not exist or cannot be interpreted as an
	 *         {@code int} type.
	 * @throws NullPointerException if {@code key} is {@code null}.
	 * @throws IllegalStateException if this node (or an ancestor) has been
	 *         removed with the {@link #removeNode()} method.
	 * @see #putInt(String,int)
	 * @see #get(String,String)
	 */
	public int getInt(String key, int def);

	/**
	 * Associates a {@code String} object representing the specified
	 * {@code long} value with the specified {@code key} in this node. The
	 * associated {@code String} object is the one that would be returned if the
	 * {@code long} value were passed to {@code Long.toString(long)}. This
	 * method is intended for use in conjunction with the
	 * {@link #getLong(String, long)} method.
	 * 
	 * <p>
	 * Implementor's note: it is <i>not </i> necessary that the {@code value} be
	 * represented by a {@code String} type in the backing store. If the backing
	 * store supports {@code long} values, it is not unreasonable to use them.
	 * This implementation detail is not visible through the {@code  Preferences}
	 * API, which allows the value to be read as a {@code long} (with
	 * {@code getLong} or a {@code String} (with {@code get}) type.
	 * 
	 * @param key {@code key} with which the string form of {@code value} is to
	 *        be associated.
	 * @param value {@code value} whose string form is to be associated with
	 *        {@code key}.
	 * @throws NullPointerException if {@code key} is {@code null}.
	 * @throws IllegalStateException if this node (or an ancestor) has been
	 *         removed with the {@link #removeNode()} method.
	 * @see #getLong(String,long)
	 */
	public void putLong(String key, long value);

	/**
	 * Returns the {@code long} value represented by the {@code String} object
	 * associated with the specified {@code key} in this node. The
	 * {@code String} object is converted to a {@code long} as by
	 * {@code Long.parseLong(String)}. Returns the specified default if there is
	 * no value associated with the {@code key}, the backing store is
	 * inaccessible, or if {@code Long.parseLong(String)} would throw a
	 * {@code NumberFormatException} if the associated {@code value} were
	 * passed. This method is intended for use in conjunction with the
	 * {@link #putLong(String, long)} method.
	 * 
	 * @param key {@code key} whose associated value is to be returned as a
	 *        {@code long} value.
	 * @param def the value to be returned in the event that this node has no
	 *        value associated with {@code key} or the associated value cannot
	 *        be interpreted as a {@code long} type or the backing store is
	 *        inaccessible.
	 * @return the {@code long} value represented by the {@code String} object
	 *         associated with {@code key} in this node, or {@code def} if the
	 *         associated value does not exist or cannot be interpreted as a
	 *         {@code long} type.
	 * @throws NullPointerException if {@code key} is {@code null}.
	 * @throws IllegalStateException if this node (or an ancestor) has been
	 *         removed with the {@link #removeNode()} method.
	 * @see #putLong(String,long)
	 * @see #get(String,String)
	 */
	public long getLong(String key, long def);

	/**
	 * Associates a {@code String} object representing the specified
	 * {@code boolean} value with the specified key in this node. The associated
	 * string is "true" if the value is {@code true}, and "false" if it is
	 * {@code false}. This method is intended for use in conjunction with the
	 * {@link #getBoolean(String, boolean)} method.
	 * 
	 * <p>
	 * Implementor's note: it is <i>not </i> necessary that the value be
	 * represented by a string in the backing store. If the backing store
	 * supports {@code boolean} values, it is not unreasonable to use them. This
	 * implementation detail is not visible through the {@code Preferences
	 * } API, which
	 * allows the value to be read as a {@code boolean} (with {@code getBoolean}
	 * ) or a {@code String} (with {@code get}) type.
	 * 
	 * @param key {@code key} with which the string form of value is to be
	 *        associated.
	 * @param value value whose string form is to be associated with {@code key}
	 *        .
	 * @throws NullPointerException if {@code key} is {@code null}.
	 * @throws IllegalStateException if this node (or an ancestor) has been
	 *         removed with the {@link #removeNode()} method.
	 * @see #getBoolean(String,boolean)
	 * @see #get(String,String)
	 */
	public void putBoolean(String key, boolean value);

	/**
	 * Returns the {@code boolean} value represented by the {@code String}
	 * object associated with the specified {@code key} in this node. Valid
	 * strings are "true", which represents {@code true}, and "false", which
	 * represents {@code false}. Case is ignored, so, for example, "TRUE" and
	 * "False" are also valid. This method is intended for use in conjunction
	 * with the {@link #putBoolean(String, boolean)} method.
	 * 
	 * <p>
	 * Returns the specified default if there is no value associated with the
	 * {@code key}, the backing store is inaccessible, or if the associated
	 * value is something other than "true" or "false", ignoring case.
	 * 
	 * @param key {@code key} whose associated value is to be returned as a
	 *        {@code boolean}.
	 * @param def the value to be returned in the event that this node has no
	 *        value associated with {@code key} or the associated value cannot
	 *        be interpreted as a {@code boolean} or the backing store is
	 *        inaccessible.
	 * @return the {@code boolean} value represented by the {@code String}
	 *         object associated with {@code key} in this node, or {@code null}
	 *         if the associated value does not exist or cannot be interpreted
	 *         as a {@code boolean}.
	 * @throws NullPointerException if {@code key} is {@code null}.
	 * @throws IllegalStateException if this node (or an ancestor) has been
	 *         removed with the {@link #removeNode()} method.
	 * @see #get(String,String)
	 * @see #putBoolean(String,boolean)
	 */
	public boolean getBoolean(String key, boolean def);

	/**
	 * Associates a {@code String} object representing the specified
	 * {@code float} value with the specified {@code key} in this node. The
	 * associated {@code String} object is the one that would be returned if the
	 * {@code float} value were passed to {@code Float.toString(float)}. This
	 * method is intended for use in conjunction with the
	 * {@link #getFloat(String, float)} method.
	 * 
	 * <p>
	 * Implementor's note: it is <i>not </i> necessary that the value be
	 * represented by a string in the backing store. If the backing store
	 * supports {@code float} values, it is not unreasonable to use them. This
	 * implementation detail is not visible through the {@code Preferences
	 * } API, which
	 * allows the value to be read as a {@code float} (with {@code getFloat}) or
	 * a {@code String} (with {@code get}) type.
	 * 
	 * @param key {@code key} with which the string form of value is to be
	 *        associated.
	 * @param value value whose string form is to be associated with {@code key}
	 *        .
	 * @throws NullPointerException if {@code key} is {@code null}.
	 * @throws IllegalStateException if this node (or an ancestor) has been
	 *         removed with the {@link #removeNode()} method.
	 * @see #getFloat(String,float)
	 */
	public void putFloat(String key, float value);

	/**
	 * Returns the float {@code value} represented by the {@code String} object
	 * associated with the specified {@code key} in this node. The
	 * {@code String} object is converted to a {@code float} value as by
	 * {@code Float.parseFloat(String)}. Returns the specified default if there
	 * is no value associated with the {@code key}, the backing store is
	 * inaccessible, or if {@code Float.parseFloat(String)} would throw a
	 * {@code NumberFormatException} if the associated value were passed. This
	 * method is intended for use in conjunction with the
	 * {@link #putFloat(String, float)} method.
	 * 
	 * @param key {@code key} whose associated value is to be returned as a
	 *        {@code float} value.
	 * @param def the value to be returned in the event that this node has no
	 *        value associated with {@code key} or the associated value cannot
	 *        be interpreted as a {@code float} type or the backing store is
	 *        inaccessible.
	 * @return the {@code float} value represented by the string associated with
	 *         {@code key} in this node, or {@code def} if the associated value
	 *         does not exist or cannot be interpreted as a {@code float} type.
	 * @throws IllegalStateException if this node (or an ancestor) has been
	 *         removed with the {@link #removeNode()} method.
	 * @throws NullPointerException if {@code key} is {@code null}.
	 * @see #putFloat(String,float)
	 * @see #get(String,String)
	 */
	public float getFloat(String key, float def);

	/**
	 * Associates a {@code String} object representing the specified
	 * {@code double} value with the specified {@code key} in this node. The
	 * associated {@code String} object is the one that would be returned if the
	 * {@code double} value were passed to {@code Double.toString(double)}. This
	 * method is intended for use in conjunction with the
	 * {@link #getDouble(String, double)} method
	 * 
	 * <p>
	 * Implementor's note: it is <i>not </i> necessary that the value be
	 * represented by a string in the backing store. If the backing store
	 * supports {@code double} values, it is not unreasonable to use them. This
	 * implementation detail is not visible through the {@code Preferences
	 * } API, which
	 * allows the value to be read as a {@code double} (with {@code getDouble})
	 * or a {@code String} (with {@code get}) type.
	 * 
	 * @param key {@code key} with which the string form of value is to be
	 *        associated.
	 * @param value value whose string form is to be associated with {@code key}
	 *        .
	 * @throws NullPointerException if {@code key} is {@code null}.
	 * @throws IllegalStateException if this node (or an ancestor) has been
	 *         removed with the {@link #removeNode()} method.
	 * @see #getDouble(String,double)
	 */
	public void putDouble(String key, double value);

	/**
	 * Returns the {@code double} value represented by the {@code String} object
	 * associated with the specified {@code key} in this node. The
	 * {@code String} object is converted to a {@code double} value as by
	 * {@code Double.parseDouble(String)}. Returns the specified default if
	 * there is no value associated with the {@code key}, the backing store is
	 * inaccessible, or if {@code Double.parseDouble(String)} would throw a
	 * {@code NumberFormatException} if the associated value were passed. This
	 * method is intended for use in conjunction with the {@link #putDouble}
	 * method.
	 * 
	 * @param key {@code key} whose associated value is to be returned as a
	 *        {@code double} value.
	 * @param def the value to be returned in the event that this node has no
	 *        value associated with {@code key} or the associated value cannot
	 *        be interpreted as a {@code double} type or the backing store is
	 *        inaccessible.
	 * @return the {@code double} value represented by the {@code String} object
	 *         associated with {@code key} in this node, or {@code def} if the
	 *         associated value does not exist or cannot be interpreted as a
	 *         {@code double} type.
	 * @throws IllegalStateException if this node (or an ancestor) has been
	 *         removed with the {@link #removeNode()} method.
	 * @throws NullPointerException if {@code key} is {@code null}.
	 * @see #putDouble(String,double)
	 * @see #get(String,String)
	 */
	public double getDouble(String key, double def);

	/**
	 * Associates a {@code String} object representing the specified
	 * {@code byte[]} with the specified {@code key} in this node. The
	 * associated {@code String} object the <i>Base64 </i> encoding of the
	 * {@code byte[]}, as defined in <a
	 * href="http://www.ietf.org/rfc/rfc2045.txt">RFC 2045 </a>, Section 6.8,
	 * with one minor change: the string will consist solely of characters from
	 * the <i>Base64 Alphabet </i>; it will not contain any newline characters.
	 * This method is intended for use in conjunction with the
	 * {@link #getByteArray(String, byte[])} method.
	 * 
	 * <p>
	 * Implementor's note: it is <i>not </i> necessary that the value be
	 * represented by a {@code String} type in the backing store. If the backing
	 * store supports {@code byte[]} values, it is not unreasonable to use them.
	 * This implementation detail is not visible through the {@code  Preferences}
	 * API, which allows the value to be read as an a {@code byte[]} object
	 * (with {@code getByteArray}) or a {@code String} object (with {@code get}
	 * ).
	 * 
	 * @param key {@code key} with which the string form of {@code value} is to
	 *        be associated.
	 * @param value {@code value} whose string form is to be associated with
	 *        {@code key}.
	 * @throws NullPointerException if {@code key} or {@code value} is
	 *         {@code null}.
	 * @throws IllegalStateException if this node (or an ancestor) has been
	 *         removed with the {@link #removeNode()} method.
	 * @see #getByteArray(String,byte[])
	 * @see #get(String,String)
	 */
	public void putByteArray(String key, byte[] value);

	/**
	 * Returns the {@code byte[]} value represented by the {@code String} object
	 * associated with the specified {@code key} in this node. Valid
	 * {@code String} objects are <i>Base64 </i> encoded binary data, as defined
	 * in <a href="http://www.ietf.org/rfc/rfc2045.txt">RFC 2045 </a>, Section
	 * 6.8, with one minor change: the string must consist solely of characters
	 * from the <i>Base64 Alphabet </i>; no newline characters or extraneous
	 * characters are permitted. This method is intended for use in conjunction
	 * with the {@link #putByteArray(String, byte[])} method.
	 * 
	 * <p>
	 * Returns the specified default if there is no value associated with the
	 * {@code key}, the backing store is inaccessible, or if the associated
	 * value is not a valid Base64 encoded byte array (as defined above).
	 * 
	 * @param key {@code key} whose associated value is to be returned as a
	 *        {@code byte[]} object.
	 * @param def the value to be returned in the event that this node has no
	 *        value associated with {@code key} or the associated value cannot
	 *        be interpreted as a {@code byte[]} type, or the backing store is
	 *        inaccessible.
	 * @return the {@code byte[]} value represented by the {@code String} object
	 *         associated with {@code key} in this node, or {@code def} if the
	 *         associated value does not exist or cannot be interpreted as a
	 *         {@code byte[]}.
	 * @throws NullPointerException if {@code key} is {@code null}. (A
	 *         {@code null} value for {@code def} <i>is </i> permitted.)
	 * @throws IllegalStateException if this node (or an ancestor) has been
	 *         removed with the {@link #removeNode()} method.
	 * @see #get(String,String)
	 * @see #putByteArray(String,byte[])
	 */
	public byte[] getByteArray(String key, byte[] def);

	/**
	 * Returns all of the keys that have an associated value in this node. (The
	 * returned array will be of size zero if this node has no preferences and
	 * not {@code null}!)
	 * 
	 * @return an array of the keys that have an associated value in this node.
	 * @throws BackingStoreException if this operation cannot be completed due
	 *         to a failure in the backing store, or inability to communicate
	 *         with it.
	 * @throws IllegalStateException if this node (or an ancestor) has been
	 *         removed with the {@link #removeNode()} method.
	 */
	public String[] keys() throws BackingStoreException;

	/**
	 * Returns the names of the children of this node. (The returned array will
	 * be of size zero if this node has no children and not {@code null}!)
	 * 
	 * @return the names of the children of this node.
	 * @throws BackingStoreException if this operation cannot be completed due
	 *         to a failure in the backing store, or inability to communicate
	 *         with it.
	 * @throws IllegalStateException if this node (or an ancestor) has been
	 *         removed with the {@link #removeNode()} method.
	 */
	public String[] childrenNames() throws BackingStoreException;

	/**
	 * Returns the parent of this node, or {@code null} if this is the root.
	 * 
	 * @return the parent of this node.
	 * @throws IllegalStateException if this node (or an ancestor) has been
	 *         removed with the {@link #removeNode()} method.
	 */
	public Preferences parent();

	/**
	 * Returns a named {@code Preferences} object (node), creating it and any of
	 * its ancestors if they do not already exist. Accepts a relative or
	 * absolute pathname. Absolute pathnames (which begin with {@code '/'}) are
	 * interpreted relative to the root of this node. Relative pathnames (which
	 * begin with any character other than {@code '/'}) are interpreted relative
	 * to this node itself. The empty string ({@code ""}) is a valid relative
	 * pathname, referring to this node itself.
	 * 
	 * <p>
	 * If the returned node did not exist prior to this call, this node and any
	 * ancestors that were created by this call are not guaranteed to become
	 * persistent until the {@code flush} method is called on the returned node
	 * (or one of its descendants).
	 * 
	 * @param pathName the path name of the {@code Preferences} object to
	 *        return.
	 * @return the specified {@code Preferences} object.
	 * @throws IllegalArgumentException if the path name is invalid.
	 * @throws IllegalStateException if this node (or an ancestor) has been
	 *         removed with the {@link #removeNode()} method.
	 * @throws NullPointerException if path name is {@code null}.
	 * @see #flush()
	 */
	public Preferences node(String pathName);

	/**
	 * Returns true if the named node exists. Accepts a relative or absolute
	 * pathname. Absolute pathnames (which begin with {@code '/'}) are
	 * interpreted relative to the root of this node. Relative pathnames (which
	 * begin with any character other than {@code '/'}) are interpreted relative
	 * to this node itself. The pathname {@code ""} is valid, and refers to this
	 * node itself.
	 * 
	 * <p>
	 * If this node (or an ancestor) has already been removed with the
	 * {@link #removeNode()} method, it <i>is </i> legal to invoke this method,
	 * but only with the pathname {@code ""}; the invocation will return
	 * {@code false}. Thus, the idiom {@code p.nodeExists("")} may be used to
	 * test whether {@code p} has been removed.
	 * 
	 * @param pathName the path name of the node whose existence is to be
	 *        checked.
	 * @return true if the specified node exists.
	 * @throws BackingStoreException if this operation cannot be completed due
	 *         to a failure in the backing store, or inability to communicate
	 *         with it.
	 * @throws IllegalStateException if this node (or an ancestor) has been
	 *         removed with the {@link #removeNode()} method and
	 *         {@code pathname} is not the empty string ({@code ""}).
	 * @throws IllegalArgumentException if the path name is invalid (i.e., it
	 *         contains multiple consecutive slash characters, or ends with a
	 *         slash character and is more than one character long).
	 */
	public boolean nodeExists(String pathName) throws BackingStoreException;

	/**
	 * Removes this node and all of its descendants, invalidating any properties
	 * contained in the removed nodes. Once a node has been removed, attempting
	 * any method other than {@code name()},{@code absolutePath()} or
	 * {@code nodeExists("")} on the corresponding {@code Preferences} instance
	 * will fail with an {@code IllegalStateException}. (The methods defined on
	 * {@code Object} can still be invoked on a node after it has been removed;
	 * they will not throw {@code IllegalStateException}.)
	 * 
	 * <p>
	 * The removal is not guaranteed to be persistent until the {@code flush}
	 * method is called on the parent of this node.
	 * 
	 * @throws IllegalStateException if this node (or an ancestor) has already
	 *         been removed with the {@link #removeNode()} method.
	 * @throws BackingStoreException if this operation cannot be completed due
	 *         to a failure in the backing store, or inability to communicate
	 *         with it.
	 * @see #flush()
	 */
	public void removeNode() throws BackingStoreException;

	/**
	 * Returns this node's name, relative to its parent.
	 * 
	 * @return this node's name, relative to its parent.
	 */
	public String name();

	/**
	 * Returns this node's absolute path name. Note that:
	 * <ul>
	 * <li>Root node - The path name of the root node is {@code "/"}.</li>
	 * <li>Slash at end - Path names other than that of the root node may not
	 * end in slash ({@code '/'}).</li>
	 * <li>Unusual names -{@code "."} and {@code ".."} have <i>no</i> special
	 * significance in path names.</li>
	 * <li>Illegal names - The only illegal path names are those that contain
	 * multiple consecutive slashes, or that end in slash and are not the root.</li>
	 * </ul>
	 * 
	 * @return this node's absolute path name.
	 */
	public String absolutePath();

	/**
	 * Forces any changes in the contents of this node and its descendants to
	 * the persistent store.
	 * 
	 * <p>
	 * Once this method returns successfully, it is safe to assume that all
	 * changes made in the subtree rooted at this node prior to the method
	 * invocation have become permanent.
	 * 
	 * <p>
	 * Implementations are free to flush changes into the persistent store at
	 * any time. They do not need to wait for this method to be called.
	 * 
	 * <p>
	 * When a flush occurs on a newly created node, it is made persistent, as
	 * are any ancestors (and descendants) that have yet to be made persistent.
	 * Note however that any properties value changes in ancestors are <i>not
	 * </i> guaranteed to be made persistent.
	 * 
	 * @throws BackingStoreException if this operation cannot be completed due
	 *         to a failure in the backing store, or inability to communicate
	 *         with it.
	 * @throws IllegalStateException if this node (or an ancestor) has been
	 *         removed with the {@link #removeNode()} method.
	 * @see #sync()
	 */
	public void flush() throws BackingStoreException;

	/**
	 * Ensures that future reads from this node and its descendants reflect any
	 * changes that were committed to the persistent store (from any VM) prior
	 * to the {@code sync} invocation. As a side-effect, forces any changes in
	 * the contents of this node and its descendants to the persistent store, as
	 * if the {@code flush} method had been invoked on this node.
	 * 
	 * @throws BackingStoreException if this operation cannot be completed due
	 *         to a failure in the backing store, or inability to communicate
	 *         with it.
	 * @throws IllegalStateException if this node (or an ancestor) has been
	 *         removed with the {@link #removeNode()} method.
	 * @see #flush()
	 */
	public void sync() throws BackingStoreException;
}

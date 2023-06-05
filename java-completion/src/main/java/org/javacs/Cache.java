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

import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Cache maps a file + an arbitrary key to a value. When the file is modified, the mapping expires.
 */
class Cache<K, V> {
    private final Map<Key, Value> map = new HashMap<>();

    boolean has(Path file, K k) {
        return !needs(file, k);
    }

    boolean needs(Path file, K k) {
        // If key is not in map, it needs to be loaded
        var key = new Key<K>(file, k);
        if (!map.containsKey(key)) return true;

        // If key was loaded before file was last modified, it needs to be reloaded
        var value = map.get(key);
        var modified = FileStore.modified(file);
        // TODO remove all keys associated with file when file changes
        return value.created.isBefore(modified);
    }

    void load(Path file, K k, V v) {
        // TODO limit total size of cache
        var key = new Key<K>(file, k);
        var value = new Value(v);
        map.put(key, value);
    }

    V get(Path file, K k) {
        var key = new Key<K>(file, k);
        if (!map.containsKey(key)) {
            throw new IllegalArgumentException(k + " is not in map " + map);
        }
        return map.get(key).value;
    }

    private static class Key<K> {
        final Path file;
        final K key;

        Key(Path file, K key) {
            this.file = file;
            this.key = key;
        }

        @Override
        public boolean equals(Object other) {
            if (other.getClass() != Key.class) return false;
            var that = (Key) other;
            return Objects.equals(this.key, that.key) && Objects.equals(this.file, that.file);
        }

        @Override
        public int hashCode() {
            return Objects.hash(file, key);
        }
    }

    private class Value {
        final V value;
        final Instant created = Instant.now();

        Value(V value) {
            this.value = value;
        }
    }
}

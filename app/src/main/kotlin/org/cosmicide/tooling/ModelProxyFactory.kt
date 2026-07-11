package org.cosmicide.tooling

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import org.gradle.tooling.model.DomainObjectSet
import java.io.File
import java.lang.reflect.GenericArrayType
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Proxy
import java.lang.reflect.Type
import java.lang.reflect.TypeVariable
import java.lang.reflect.WildcardType
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Wraps JSON produced by the server's reflection-based model serializer back
 * into Tooling API model interfaces using java.lang.reflect.Proxy.
 */
object ModelProxyFactory {

    fun wrap(json: JsonElement?, type: Type): Any? {
        if (json == null || json is JsonNull) {
            return null
        }

        val resolvedType = resolveType(type)
        val rawClass = rawClassOf(resolvedType)

        return when {
            rawClass == Any::class.java ->
                wrapUnknown(json)

            rawClass == String::class.java && json is JsonPrimitive ->
                json.asString

            rawClass == Char::class.java ||
                    rawClass == Char::class.javaObjectType -> {
                json.asString.firstOrNull() ?: '\u0000'
            }

            rawClass == File::class.java && json is JsonPrimitive ->
                File(json.asString)

            rawClass == Path::class.java && json is JsonPrimitive ->
                Paths.get(json.asString)

            rawClass == Boolean::class.java ||
                    rawClass == Boolean::class.javaObjectType -> {
                json.asBoolean
            }

            rawClass == Byte::class.java ||
                    rawClass == Byte::class.javaObjectType -> {
                json.asByte
            }

            rawClass == Short::class.java ||
                    rawClass == Short::class.javaObjectType -> {
                json.asShort
            }

            rawClass == Int::class.java ||
                    rawClass == Int::class.javaObjectType -> {
                json.asInt
            }

            rawClass == Long::class.java ||
                    rawClass == Long::class.javaObjectType -> {
                json.asLong
            }

            rawClass == Float::class.java ||
                    rawClass == Float::class.javaObjectType -> {
                json.asFloat
            }

            rawClass == Double::class.java ||
                    rawClass == Double::class.javaObjectType -> {
                json.asDouble
            }

            Number::class.java.isAssignableFrom(rawClass) &&
                    json is JsonPrimitive -> {
                json.asNumber
            }

            rawClass.isEnum && json is JsonPrimitive ->
                enumValue(rawClass, json.asString)

            json is JsonArray &&
                    DomainObjectSet::class.java.isAssignableFrom(rawClass) -> {
                val elementType = elementTypeOf(resolvedType)

                RemoteDomainObjectSet(
                    json.map { element ->
                        wrap(element, elementType)
                    }
                )
            }

            json is JsonArray &&
                    Set::class.java.isAssignableFrom(rawClass) -> {
                val elementType = elementTypeOf(resolvedType)

                LinkedHashSet(
                    json.map { element ->
                        wrap(element, elementType)
                    }
                )
            }

            json is JsonArray &&
                    Collection::class.java.isAssignableFrom(rawClass) -> {
                val elementType = elementTypeOf(resolvedType)

                json.map { element ->
                    wrap(element, elementType)
                }
            }

            json is JsonArray &&
                    Iterable::class.java.isAssignableFrom(rawClass) -> {
                val elementType = elementTypeOf(resolvedType)

                json.map { element ->
                    wrap(element, elementType)
                }
            }

            json is JsonArray && rawClass.isArray -> {
                val componentType = rawClass.componentType

                java.lang.reflect.Array.newInstance(
                    componentType,
                    json.size()
                ).also { array ->
                    json.forEachIndexed { index, element ->
                        java.lang.reflect.Array.set(
                            array,
                            index,
                            wrap(element, componentType)
                        )
                    }
                }
            }

            json is JsonObject && rawClass.isInterface -> {
                Proxy.newProxyInstance(
                    rawClass.classLoader
                        ?: ModelProxyFactory::class.java.classLoader,
                    arrayOf(rawClass),
                    ModelDataHandler(json)
                )
            }

            json is JsonPrimitive ->
                primitiveValue(json)

            json is JsonArray ->
                json.map { wrapUnknown(it) }

            json is JsonObject ->
                json.entrySet().associate { entry ->
                    entry.key to wrapUnknown(entry.value)
                }

            else ->
                json.toString()
        }
    }

    internal fun rawClassOf(type: Type): Class<*> {
        return when (val resolved = resolveType(type)) {
            is Class<*> ->
                resolved

            is ParameterizedType ->
                resolved.rawType as? Class<*> ?: Any::class.java

            is GenericArrayType -> {
                val componentClass = rawClassOf(resolved.genericComponentType)

                java.lang.reflect.Array
                    .newInstance(componentClass, 0)
                    .javaClass
            }

            else ->
                Any::class.java
        }
    }

    private fun resolveType(type: Type): Type {
        return when (type) {
            is WildcardType ->
                type.upperBounds.firstOrNull()
                    ?: type.lowerBounds.firstOrNull()
                    ?: Any::class.java

            is TypeVariable<*> ->
                type.bounds.firstOrNull() ?: Any::class.java

            else ->
                type
        }
    }

    private fun elementTypeOf(type: Type): Type {
        val resolved = resolveType(type)

        if (resolved !is ParameterizedType) {
            return Any::class.java
        }

        return resolved.actualTypeArguments
            .firstOrNull()
            ?.let(::resolveType)
            ?: Any::class.java
    }

    private fun wrapUnknown(json: JsonElement): Any? {
        return when {
            json is JsonNull ->
                null

            json is JsonPrimitive ->
                primitiveValue(json)

            json is JsonArray ->
                json.map(::wrapUnknown)

            json is JsonObject ->
                json.entrySet().associate { entry ->
                    entry.key to wrapUnknown(entry.value)
                }

            else ->
                json.toString()
        }
    }

    private fun primitiveValue(json: JsonPrimitive): Any {
        return when {
            json.isBoolean ->
                json.asBoolean

            json.isNumber ->
                json.asNumber

            else ->
                json.asString
        }
    }

    private fun enumValue(
        enumClass: Class<*>,
        name: String
    ): Any {
        return enumClass.enumConstants
            ?.firstOrNull { constant ->
                (constant as Enum<*>).name == name
            }
            ?: throw IllegalArgumentException(
                "Unknown ${enumClass.name} enum value: $name"
            )
    }
}

/**
 * Local immutable implementation of Gradle's DomainObjectSet.
 *
 * A plain List or Set cannot be returned from a proxy method whose declared
 * return type is DomainObjectSet.
 */
private class RemoteDomainObjectSet<T>(
    values: List<T>
) : java.util.AbstractSet<T>(), DomainObjectSet<T> {

    private val elements = values.toList()

    override val size: Int
        get() = elements.size

    override fun iterator(): MutableIterator<T> {
        val iterator = elements.iterator()

        return object : MutableIterator<T> {
            override fun hasNext(): Boolean =
                iterator.hasNext()

            override fun next(): T =
                iterator.next()

            override fun remove() {
                throw UnsupportedOperationException(
                    "Remote Tooling API models are immutable"
                )
            }
        }
    }

    override fun getAll(): MutableList<T> =
        elements.toMutableList()

    override fun getAt(index: Int): T =
        elements[index]
}

/**
 * Backs one wrapped Tooling API model instance.
 */
private class ModelDataHandler(
    private val data: JsonObject
) : InvocationHandler {

    override fun invoke(
        proxy: Any,
        method: Method,
        args: Array<out Any?>?
    ): Any? {
        val name = method.name

        return when {
            name == "toString" ->
                "RemoteModel($data)"

            name == "hashCode" ->
                data.hashCode()

            name == "equals" ->
                proxy === args?.getOrNull(0)

            name.startsWith("get") && name.length > 3 ->
                readField(
                    field = decapitalize(name.substring(3)),
                    method = method
                )

            name.startsWith("is") && name.length > 2 ->
                readField(
                    field = decapitalize(name.substring(2)),
                    method = method
                )

            else ->
                throw UnsupportedOperationException(
                    "$name has no wire data on this model"
                )
        }
    }

    private fun readField(
        field: String,
        method: Method
    ): Any? {
        val element = data.get(field)

        if (element == null || element is JsonNull) {
            return defaultValue(method.returnType)
        }

        return ModelProxyFactory.wrap(
            json = element,
            type = method.genericReturnType
        )
    }

    private fun defaultValue(type: Class<*>): Any? {
        if (!type.isPrimitive) {
            return null
        }

        return when (type) {
            Boolean::class.javaPrimitiveType -> false
            Byte::class.javaPrimitiveType -> 0.toByte()
            Short::class.javaPrimitiveType -> 0.toShort()
            Int::class.javaPrimitiveType -> 0
            Long::class.javaPrimitiveType -> 0L
            Float::class.javaPrimitiveType -> 0f
            Double::class.javaPrimitiveType -> 0.0
            Char::class.javaPrimitiveType -> '\u0000'
            else -> null
        }
    }

    private fun decapitalize(value: String): String {
        if (value.isEmpty()) {
            return value
        }

        // JavaBeans behavior: getURL() maps to "URL", not "uRL".
        if (
            value.length > 1 &&
            value[0].isUpperCase() &&
            value[1].isUpperCase()
        ) {
            return value
        }

        return value.replaceFirstChar { character ->
            character.lowercaseChar()
        }
    }
}
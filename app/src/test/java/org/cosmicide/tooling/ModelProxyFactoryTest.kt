package org.cosmicide.tooling

import com.google.gson.JsonParser
import org.gradle.tooling.model.DomainObjectSet
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.lang.reflect.ParameterizedType
import java.nio.file.Path

class ModelProxyFactoryTest {
    @Test
    fun `wrap converts scalar filesystem enum and unknown values`() {
        assertEquals("text", ModelProxyFactory.wrap(json("\"text\""), String::class.java))
        assertEquals(7, ModelProxyFactory.wrap(json("7"), Int::class.java))
        assertEquals(true, ModelProxyFactory.wrap(json("true"), Boolean::class.java))
        assertEquals('x', ModelProxyFactory.wrap(json("\"x\""), Char::class.java))
        assertEquals(
            File("/tmp/file"),
            ModelProxyFactory.wrap(json("\"/tmp/file\""), File::class.java)
        )
        assertEquals(
            Path.of("/tmp/file"),
            ModelProxyFactory.wrap(json("\"/tmp/file\""), Path::class.java)
        )
        assertEquals(Mode.FAST, ModelProxyFactory.wrap(json("\"FAST\""), Mode::class.java))
        val unknown = ModelProxyFactory.wrap(json("[\"one\",2,true]"), Any::class.java) as List<*>
        assertEquals("one", unknown[0])
        assertEquals(2L, (unknown[1] as Number).toLong())
        assertEquals(true, unknown[2])
    }

    @Test
    fun `wrapped interface reads nested generic data and primitive defaults`() {
        val model = ModelProxyFactory.wrap(
            json(
                """{
                  "name":"root",
                  "URL":"https://example.test",
                  "files":["/one","/two"],
                  "mode":"SAFE",
                  "child":{"name":"child"}
                }""".trimIndent()
            ),
            SampleModel::class.java
        ) as SampleModel

        assertEquals("root", model.name)
        assertEquals("https://example.test", model.URL)
        assertEquals(listOf(File("/one"), File("/two")), model.files)
        assertEquals(Mode.SAFE, model.mode)
        assertEquals("child", model.child?.name)
        assertFalse(model.enabled)
        assertEquals(0, model.count)
        assertNull(model.missing)
        assertTrue(model.toString().startsWith("RemoteModel("))
        assertEquals(model, model)
        assertNotEquals(model, ModelProxyFactory.wrap(json("{}"), SampleModel::class.java))
    }

    @Test
    fun `collections sets domain sets and arrays retain declared element types`() {
        val filesType = SampleModel::class.java.getMethod("getFiles").genericReturnType
        val domainType = SampleModel::class.java.getMethod("getItems").genericReturnType
        val setType = SampleModel::class.java.getMethod("getModes").genericReturnType

        assertEquals(
            listOf(File("/a"), File("/b")),
            ModelProxyFactory.wrap(json("[\"/a\",\"/b\"]"), filesType)
        )
        val domain = ModelProxyFactory.wrap(json("[\"a\",\"b\",\"a\"]"), domainType)
                as DomainObjectSet<*>
        assertEquals(3, domain.size)
        assertEquals("b", domain.getAt(1))
        assertEquals(listOf("a", "b", "a"), domain.all)
        assertEquals(
            setOf(Mode.FAST, Mode.SAFE),
            ModelProxyFactory.wrap(json("[\"FAST\",\"SAFE\",\"FAST\"]"), setType)
        )

        val ints = ModelProxyFactory.wrap(json("[1,2,3]"), IntArray::class.java) as IntArray
        assertArrayEquals(intArrayOf(1, 2, 3), ints)
    }

    @Test
    fun `invalid enum values and unsupported proxy methods fail clearly`() {
        val enumError = assertFails<IllegalArgumentException> {
            ModelProxyFactory.wrap(json("\"UNKNOWN\""), Mode::class.java)
        }
        assertTrue(enumError.message.orEmpty().contains("Unknown"))

        val model = ModelProxyFactory.wrap(json("{}"), SampleModel::class.java) as SampleModel
        val methodError = assertFails<UnsupportedOperationException> { model.execute() }
        assertTrue(methodError.message.orEmpty().contains("no wire data"))
    }

    @Test
    fun `raw class resolves parameterized generic array wildcard and type variable forms`() {
        val listType = SampleModel::class.java.getMethod("getFiles").genericReturnType
        assertEquals(List::class.java, ModelProxyFactory.rawClassOf(listType))

        val genericArray = GenericTypes::class.java.getMethod("array").genericReturnType
        assertTrue(ModelProxyFactory.rawClassOf(genericArray).isArray)

        val wildcard =
            (SampleModel::class.java.getMethod("getNumbers").genericReturnType as ParameterizedType)
                .actualTypeArguments.single()
        assertEquals(Number::class.java, ModelProxyFactory.rawClassOf(wildcard))

        val variable = GenericTypes::class.java.typeParameters.single()
        assertEquals(CharSequence::class.java, ModelProxyFactory.rawClassOf(variable))
    }

    private fun json(value: String) = JsonParser.parseString(value)

    enum class Mode { FAST, SAFE }

    interface SampleModel {
        val name: String?
        val URL: String?
        val enabled: Boolean
        val count: Int
        val missing: String?
        val files: List<File>
        val mode: Mode?
        val modes: Set<Mode>
        val child: SampleModel?
        val items: DomainObjectSet<String>
        val numbers: List<Number>
        fun execute()
    }

    interface GenericTypes<T : CharSequence> {
        fun array(): Array<T>
    }
}

private inline fun <reified T : Throwable> assertFails(block: () -> Unit): T {
    try {
        block()
    } catch (error: Throwable) {
        if (error is T) return error
        throw AssertionError("Expected ${T::class.java.name}, got ${error::class.java.name}", error)
    }
    throw AssertionError("Expected ${T::class.java.name}")
}

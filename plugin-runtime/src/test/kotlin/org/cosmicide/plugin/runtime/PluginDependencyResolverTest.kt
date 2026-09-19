package org.cosmicide.plugin.runtime

import org.cosmicide.plugin.api.PluginDependency
import org.cosmicide.plugin.api.PluginDescriptor
import org.junit.Assert.*
import org.junit.Test

class PluginDependencyResolverTest {
    private fun plugin(id: String, vararg dependencies: PluginDependency) =
        PluginDescriptor(id, id, "1.0.0", "Plugin", dependencies = dependencies.toList())

    @Test
    fun `strict semver follows the specification precedence chain`() {
        val chain = listOf(
            "1.0.0-alpha", "1.0.0-alpha.1", "1.0.0-alpha.beta", "1.0.0-beta",
            "1.0.0-beta.2", "1.0.0-beta.11", "1.0.0-rc.1", "1.0.0"
        )
        chain.zipWithNext().forEach { (a, b) -> assertTrue(SemVer.parse(a)!! < SemVer.parse(b)!!) }
        assertEquals(0, SemVer.parse("1.0.0+one")!!.compareTo(SemVer.parse("1.0.0+two")!!))
        assertTrue(SemVer.atLeast("999999999999999999999.0.0", "2.0.0"))
    }

    @Test
    fun `strict semver rejects loose versions and numeric prerelease leading zeros`() {
        listOf(
            "1",
            "v1.0.0",
            "01.0.0",
            "1.0.0-01",
            "1.0.0-",
            "1.0.0+",
            " 1.0.0",
            "1.0.0\n",
            "1.0.0-a..b"
        ).forEach {
            assertNull(it, SemVer.parse(it))
        }
        assertNotNull(SemVer.parse("1.0.0-01a+001"))
    }

    @Test
    fun `legacy unconstrained versions remain valid but constraints require semver`() {
        val provider = plugin("b").copy(version = "nightly")
        assertEquals(listOf("b", "a"), plan(plugin("a", PluginDependency("b")), provider))
        expect(PluginFailureKind.VERSION, plugin("a", PluginDependency("b", "1.0.0")), provider)
        expect(PluginFailureKind.VERSION, plugin("a", PluginDependency("b", "latest")), plugin("b"))
    }

    @Test
    fun `missing disabled duplicate and cyclic dependencies have distinct failures`() {
        expect(PluginFailureKind.MISSING, plugin("a", PluginDependency("b")))
        expect(
            PluginFailureKind.DISABLED,
            plugin("a", PluginDependency("b")),
            plugin("b").copy(enabledByDefault = false)
        )
        expect(
            PluginFailureKind.DUPLICATE,
            plugin("a", PluginDependency("b")),
            plugin("b"),
            plugin("b")
        )
        expect(
            PluginFailureKind.CYCLE,
            plugin("a", PluginDependency("b")),
            plugin("b", PluginDependency("a"))
        )
        expect(PluginFailureKind.CYCLE, plugin("a", PluginDependency("a")))
    }

    @Test
    fun `diamond ordering is deterministic across discovery and manifest order`() {
        val a = plugin("a", PluginDependency("c"), PluginDependency("b"))
        val b = plugin("b", PluginDependency("d"))
        val c = plugin("c", PluginDependency("d"))
        val d = plugin("d")
        assertEquals(listOf("d", "b", "c", "a"), plan(a, c, d, b))
        assertEquals(
            plan(a, c, d, b),
            plan(a.copy(dependencies = a.dependencies.reversed()), b, c, d)
        )
    }

    @Test
    fun `optional absent incompatible disabled and cyclic subtrees are best effort`() {
        val a = plugin(
            "a", PluginDependency("missing", optional = true), PluginDependency("b", "2.0.0", true),
            PluginDependency("c", optional = true), PluginDependency("d", optional = true)
        )
        assertEquals(
            listOf("a"), plan(
                a, plugin("b"), plugin("c").copy(enabledByDefault = false),
                plugin("d", PluginDependency("a"))
            )
        )
        assertEquals(
            listOf("b", "a"), plan(
                plugin("a", PluginDependency("b", optional = true)),
                plugin("b", PluginDependency("a", optional = true))
            )
        )
    }

    private fun plan(root: PluginDescriptor, vararg others: PluginDescriptor) =
        PluginDependencyResolver.plan(root.id, listOf(root) + others).map { it.id }

    private fun expect(
        kind: PluginFailureKind,
        root: PluginDescriptor,
        vararg others: PluginDescriptor
    ) {
        val error = assertThrows(PluginLifecycleException::class.java) { plan(root, *others) }
        assertEquals(kind, error.kind)
    }
}

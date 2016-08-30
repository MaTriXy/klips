package org.klips.engine

import org.junit.Test
import org.klips.dsl.Facet
import org.klips.dsl.Facet.FacetRef
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull


class PatternMatcherTest {

    val aid = ref<ActorId>("aid")
    val kind = ref<ActorKind>("kind")
    val pid = ref<PlayerId>("pid")
    val nrgy = ref<Level>("nrgy")
    val hlth = ref<Level>("hlth")
    val state = ref<State>("state")

    @Test
    fun asymmetricBind() {
        val fact1 = Actor(aid, pid, kind, nrgy, hlth, Facet.ConstFacet(State.OnMarch))
        val fact2 = Actor(aid, pid, kind, nrgy, hlth, state)
        val b1 = PatternMatcher(fact2).bind(fact1)
        println("$b1")
        val b2 = PatternMatcher(fact1).bind(fact2)
        println("$b2")
    }

    @Test
    fun basicMatcher(){
        val patt1 = PatternMatcher(Adjacent(const(CellId(0)), ref("aid1")))
        val patt2 = PatternMatcher(Adjacent(const(CellId(1)), ref("aid1")))
        val patt3 = PatternMatcher(Adjacent(const(CellId(0)), const(CellId(1))))

        assertNotNull(patt1.bind(Adjacent(0, 1)))
        assertNotNull(patt3.bind(Adjacent(0, 1)))
        assertNull(patt3.bind(patt1.pattern))
        assertNotNull(patt1.bind(Adjacent(0, 2)))
        assertNotNull(patt1.bind(patt1.pattern))
        assertNull(patt1.bind(Adjacent(1, 1)))
        assertNull(patt1.bind(patt2.pattern))

        assertEquals(const(CellId(1)), patt1.bind(Adjacent(0, 1))!![ref<Int>("aid1")])
        assertEquals(const(CellId(2)), patt1.bind(Adjacent(0, 2))!![ref<Int>("aid1")])
    }

    @Test
    fun sameMatcher(){
        val patt1 = PatternMatcher(Adjacent(ref("aid1"), ref("aid1")))

        assertNull(patt1.bind(Adjacent(0, 1)))
        assertNull(patt1.bind(Adjacent(0, 2)))
        assertNull(patt1.bind(Adjacent(ref("id1"), ref("id2"))))
        assertNotNull(patt1.bind(Adjacent(1, 1)))
        assertNotNull(patt1.bind(Adjacent(ref("id1"), ref("id1"))))

        assertEquals(ref<Int>("id1"), patt1.bind(Adjacent(ref("id1"), ref("id1")))!![ref<Int>("aid1")])

    }

    fun <T : Comparable<T>> ref(id:String) = FacetRef<T>(id)
    fun <T : Comparable<T>> const(v:T) = Facet.ConstFacet(v)
}
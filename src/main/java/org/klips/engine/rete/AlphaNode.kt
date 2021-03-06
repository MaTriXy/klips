package org.klips.engine.rete

import org.klips.dsl.Facet.FacetRef
import org.klips.dsl.Fact
import org.klips.engine.Binding
import org.klips.engine.Modification
import org.klips.engine.PatternMatcher
import org.klips.engine.util.Log
import org.klips.engine.util.activationFailed
import org.klips.engine.util.activationHappen


abstract class AlphaNode(log: Log, val pattern: Fact) : Node(log) {

    val matcher = PatternMatcher(pattern)

    override val refs: Set<FacetRef<*>>
        get() = matcher.refs

    fun accept(mdf: Modification<out Fact>): Boolean {
        matcher.bind(mdf.arg)?.let { b0 ->
            val modified = modifyCache(mdf.inherit(b0)) { b1 ->
                log.reteEvent {
                    activationHappen()
                    "ACCEPT HAPPEN: $mdf, $this"
                }
                notifyConsumers(mdf.inherit(b1))
            }
            if (!modified) log.reteEvent {
                activationFailed()
                "ACCEPT IDLE: $mdf, $this"
            }
            return true
        }
        return false
    }

    abstract protected fun modifyCache(mdf: Modification<out Binding>, hookModify: (Binding) -> Unit): Boolean

    ////////////////////////////////////////////////////////
//
//    override fun equals(other: Both?): Boolean {
//        if(other !is AlphaNode) return false
//
//        return pattern.equals(other.pattern)
//    }
//
//    override fun hashCode(): Int {
//        return pattern.hashCode()
//    }
//
    override fun toString() = "A-Node($pattern) [${System.identityHashCode(this)}]"
}
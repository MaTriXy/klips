package org.klips.dsl

import org.klips.PatternNotConnectedException
import org.klips.dsl.ActivationFilter.AssertOnly
import org.klips.dsl.Guard.Junction
import org.klips.dsl.Guard.Junction.And
import org.klips.dsl.Guard.MultiJunction
import org.klips.engine.Binding
import org.klips.engine.Modification
import org.klips.engine.Modification.Assert
import org.klips.engine.Modification.Retire
import org.klips.engine.rete.builder.RuleClause
import org.klips.engine.rete.builder.Trigger
import kotlin.properties.Delegates.notNull

class Rule(val group: String, val priority: Double) : FacetBuilder(), Asserter by AsserterTrait({}) {

    private val guards = MultiJunction(And)
    private var rhs by notNull<RHS>()

    val refs: Set<Facet.FacetRef<*>> by lazy {
        mutableSetOf<Facet.FacetRef<*>>().apply {
            asserted.forEach { fact -> addAll(fact.refs) }
            retired.forEach { fact -> addAll(fact.refs) }
        }
    }

    fun effect(occur: Occur? = null,
               activation: ActivationFilter = AssertOnly,
               init: RHS.(Modification<Binding>) -> Unit): RHS {
        rhs = RHS(this, occur, activation, init)
        checkConnectedByRef(asserted.union(retired))
        return rhs
    }

    private fun checkConnectedByRef(pattern: Set<Fact>) {
        val first  = pattern.first()
        val refs   = first.refs
        val others = pattern.minus(first)
        val reminder = checkConnectedByRef(refs.toSet(), others)
        if (reminder.size > 0)
            throw PatternNotConnectedException(pattern.minus(reminder), reminder)
    }
    private fun checkConnectedByRef(refs: Set<Facet<*>>, pattern: Set<Fact>) : Set<Fact> {
        pattern.find { fact -> fact.refs.intersect(refs).size > 0 }?.let {
            return checkConnectedByRef(refs.union(it.refs), pattern.minus(it))
        }

        return pattern
    }

    fun guard(vararg g: Guard) = guard(And, *g)

    fun guard(j: Junction, vararg g: Guard) =
            guards.juncts.add(MultiJunction(j, *g))

    fun guard(l: (Modification<Binding>) -> Boolean) {
        guards.juncts.add(Guard.LambdaGuard(l))
    }

    fun onceBy(vararg facets:Facet<*>) {
        guard(Guard.OnceBy(group, *facets))
    }

    fun toInternal(): RuleClause {
        val pattern = mutableSetOf<Fact>().apply {
            addAll(retired)
            addAll(asserted)
        }
        return RuleClause(pattern, object : Trigger {
            override fun fire(cache: MutableMap<Any,Any>,
                              solution: Modification<Binding>,
                              addEffect: (Modification<Fact>) -> Unit) {
                if (guards.eval(cache, solution)) {
                    rhs.init(solution)

                    retired.forEach { addEffect(Retire(it.substitute(solution.arg))) }
                    rhs.retired.forEach { addEffect(Retire(it.substitute(solution.arg))) }
                    rhs.asserted.forEach { addEffect(Assert(it.substitute(solution.arg))) }
                }
            }
        }, group, priority)
    }
}


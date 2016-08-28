package org.klips.dsl

import org.klips.engine.Binding
import org.klips.dsl.ActivationFilter
import org.klips.engine.Modification
import org.klips.dsl.ActivationFilter.*


class RHS(private val filter : ActivationFilter?, private val initBlock: RHS.(Modification<Binding>) -> Unit) : AsserterTrait() {

    fun init(solution: Modification<Binding>) {
        asserted.clear()
        retired.clear()
        when (filter) {
            Both -> initBlock(solution)
            AssertOnly -> if (solution is Modification.Assert) initBlock(solution)
            RetireOnly -> if (solution is Modification.Retire) initBlock(solution)
        }
    }

}
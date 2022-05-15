package com.martingoe.cubelang.backend.instructionselection

import com.martingoe.cubelang.backend.Utils
import com.martingoe.cubelang.common.ASMEmitter
import com.martingoe.cubelang.common.Expression

/**
 * The service used to accomplish instruction selection, emits the IR-Values needed for the given expressions
 */
class ASTToIRService(var asmEmitter: ASMEmitter) {
    private val astGetSymbol = ASTGetSymbol()
    private val rules = getRules()
    private val expressionMatchingTrie = ExpressionMatchingTrie(rules, astGetSymbol)

    /**
     * Uses the given [[ASMEmitter]] to emit the rules needed for the expression. This method both finds and executes the necessary rules.
     *
     * @return Returns the [[Expression]] returned from the [[Rule.constructString]] method.
     */
    fun emitCodeForExpression(expression: Expression): Expression {
        expressionMatchingTrie.visit(expression)
        val rule = expression.matchedResults['r'] ?:
        TODO("NO RULE for ${expression::class}")

        emitSubRuleReductions(rules[rule].expression, expression)
        return rules[rule].constructString(expression, asmEmitter, this)
    }

    private fun emitSubRuleReductions(rule: Expression, actual: Expression) {
        // Expects rule and actual to have the same arity
        val ruleChildren = Utils.getChildren(rule)
        val actualChildren = Utils.getChildren(actual)

        for (i in ruleChildren.indices) {
            if (ruleChildren[i]::class != actualChildren[i]::class) {
                // Update new child
                val ruleToApply = actualChildren[i].matchedResults[astGetSymbol.evaluate(ruleChildren[i])]!!

                emitSubRuleReductions(rules[ruleToApply].expression, actualChildren[i])
                val newExpression = rules[ruleToApply].constructString(actualChildren[i], asmEmitter, this)
                Utils.setNthChild(i, newExpression, actual)
            } else {
                emitSubRuleReductions(ruleChildren[i], actualChildren[i])
            }
        }
    }
}
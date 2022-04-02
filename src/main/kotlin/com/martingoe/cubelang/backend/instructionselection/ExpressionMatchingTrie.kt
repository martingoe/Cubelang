package com.martingoe.cubelang.backend.instructionselection

import com.martingoe.cubelang.common.Expression
import java.util.*


/**
 * The trie used to match expressions to immediate representation values.
 */
class ExpressionMatchingTrie(private val rules: Array<Rule>, private val astGetSymbol: ASTGetSymbol) {
    var trieEntries: MutableList<TrieEntry> = ArrayList()

    /**
     * Initialize the new trie by building it from the rules
      */
    init {
        trieEntries.add(TrieEntry(' ', 0))
        buildTrieFromRules(rules)
    }

    private fun buildTrieFromRules(rules: Array<Rule>) {
        for (i in rules.indices) {
            buildTrieFromExpression(rules[i].expression, i)
        }
        buildFailureFunctions()
    }

    private fun buildFailureFunctions() {
        val queue: PriorityQueue<Int> = PriorityQueue()
        queue.addAll(trieEntries[0].next)
        while (queue.isNotEmpty()) {
            val r = queue.poll()

            for (s in trieEntries[r].next) {
                queue.add(s)
                // state = f(r)?
                var state = trieEntries[r].failureState
                while (trieEntries[state].next.none { trieEntries[it].value == trieEntries[s].value } && state != 0)
                    state = trieEntries[state].failureState
                val newFailureState = trieEntries[state].next.firstOrNull { trieEntries[it].value == trieEntries[s].value } ?: 0
                trieEntries[s].failureState = newFailureState
                trieEntries[s].isAccepting = combineAcceptingStates(trieEntries[s].isAccepting, trieEntries[newFailureState].isAccepting)
            }

        }
    }

    private fun combineAcceptingStates(accepting: Array<Pair<Boolean, Int>>, accepting1: Array<Pair<Boolean, Int>>): Array<Pair<Boolean, Int>> {
        val newArray: Array<Pair<Boolean, Int>> = Array(accepting.size) { Pair(false, 0) }
        for (i in accepting.indices) {
            if (accepting[i].first)
                newArray[i] = accepting[i]
            if (accepting1[i].first)
                newArray[i] = accepting1[i]
        }
        return newArray

    }

    private fun buildTrieFromExpression(expression: Expression, ruleIndex: Int, currentState: Int = 0) {
        val ruleChar = astGetSymbol.evaluate(expression)
        val newState = generateTrieEntryIfNeeded(currentState, ruleChar)
        val children = Utils.getChildren(expression)
        if (children.isEmpty())
            trieEntries[newState].isAccepting[ruleIndex] = Pair(true, trieEntries[newState].length)

        children.forEachIndexed { index, child ->
            val indexState = generateTrieEntryIfNeeded(newState, index.toChar())
            buildTrieFromExpression(child, ruleIndex, indexState)
        }
    }

    private fun generateTrieEntryIfNeeded(currentState: Int, newChar: Char): Int {
        return try {
            trieEntries[currentState].next.first { trieEntries[it].value == newChar }
        } catch (e: NoSuchElementException) {
            val originalLength = trieEntries.size
            val newTrieEntry = TrieEntry(newChar, trieEntries[currentState].length + 1)
            trieEntries[currentState].next.add(originalLength)
            trieEntries.add(newTrieEntry)
            originalLength
        }
    }

    /**
     * Visit a given expression and annotate it with which rule to apply etc.
     */
    internal fun visit(expression: Expression, previous: Int = 0, index: Int = -1) {
        if (index != -1) {
            val indexState = succ(previous, index.toChar())
            val finalState = succ(indexState, astGetSymbol.evaluate(expression))
            expression.state = finalState
        } else {
            // Expression is the root
            val newState = succ(previous, astGetSymbol.evaluate(expression))
            expression.state = newState
        }

        val children = Utils.getChildren(expression)
        children.forEachIndexed { childIndex, it -> visit(it, expression.state, childIndex) }
        postProcess(expression, children, previous, index)
    }

    private fun postProcess(expression: Expression, children: List<Expression>, previousState: Int = 0, index: Int = -1) {
        setPartial(expression, expression.state)
        if (children.isNotEmpty()) {
            for (i in rules.indices) {
                var product = children[0].b[i] / 2
                for (child in children.subList(1, children.size)) {
                    product = product and child.b[i] / 2
                }

                expression.b[i] = expression.b[i] or product
            }
        }
        doReduce(expression, previousState, index)
    }

    private fun doReduce(expression: Expression, previousState: Int = 0, index: Int = -1) {
        for (i in rules.indices) {
            // If there is a rule matching exactly
            if (expression.b[i] % 2 == 1) {
                val possibleNewCost = rules[i].getCost(expression, astGetSymbol, rules)
                if (possibleNewCost < (expression.cost[rules[i].resultSymbol] ?: Int.MAX_VALUE)) {
                    expression.cost[rules[i].resultSymbol] = possibleNewCost
                    expression.match[rules[i].resultSymbol] = i

                    // Expression is root
                    val x: Int = if (index == -1) {
                        succ(0, rules[i].resultSymbol)
                    } else {
                        succ(succ(previousState, index.toChar()), rules[i].resultSymbol)
                    }
                    setPartial(expression, x)
                }
            }
        }
    }

    private fun setPartial(expression: Expression, state: Int) {
        for (rule in rules.indices) {
            if (trieEntries[state].isAccepting[rule].first) {
                expression.b[rule] = expression.b[rule] or twoToThePowerOf(getTreeLength(trieEntries[state].isAccepting[rule].second))
            }
        }
    }


    private fun twoToThePowerOf(n: Int): Int {
        val base = 2
        var result = 1
        var temp = n

        while (temp != 0) {
            result *= base
            --temp
        }
        return result
    }

    private fun getTreeLength(pathStringLength: Int): Int = (pathStringLength - 1) / 2

    private fun succ(from: Int, to_value: Char): Int {
        return try {
            trieEntries[from].next.first { trieEntries[it].value == to_value }
        } catch (e: NoSuchElementException) {
            if (trieEntries[from].failureState == from)
                return 0
            val newState = succ(trieEntries[from].failureState, to_value)
            newState
        }
    }

    class TrieEntry(val value: Char, val length: Int) {
        var next: MutableList<Int> = ArrayList()
        var isAccepting: Array<Pair<Boolean, Int>> = Array(Rule.RULE_COUNT) { Pair(false, 0) }
        var failureState: Int = 0
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as TrieEntry

            if (value != other.value) return false
            if (length != other.length) return false
            if (next != other.next) return false
            if (!isAccepting.contentEquals(other.isAccepting)) return false
            if (failureState != other.failureState) return false

            return true
        }

        override fun hashCode(): Int {
            var result = value.hashCode()
            result = 31 * result + length
            result = 31 * result + next.hashCode()
            result = 31 * result + isAccepting.contentHashCode()
            result = 31 * result + failureState
            return result
        }


    }
}

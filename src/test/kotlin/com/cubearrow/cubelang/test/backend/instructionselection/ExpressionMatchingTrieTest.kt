package com.cubearrow.cubelang.test.backend.instructionselection

import com.cubearrow.cubelang.backend.instructionselection.ASTGetSymbol
import com.cubearrow.cubelang.backend.instructionselection.ASTToIRService
import com.cubearrow.cubelang.backend.instructionselection.ExpressionMatchingTrie
import com.cubearrow.cubelang.backend.instructionselection.Rule
import com.cubearrow.cubelang.common.ASMEmitter
import com.cubearrow.cubelang.common.Expression
import com.cubearrow.cubelang.common.tokens.Token
import com.cubearrow.cubelang.common.tokens.TokenType
import org.junit.Test
import kotlin.test.assertEquals

private class TestAddRule: Rule() {
    override val expression = Expression.Operation(Expression.Register(), Token("+", TokenType.PLUSMINUS), Expression.Literal(null))
    override val resultSymbol = 'r'

    override fun getCost(expression: Expression, astGetSymbol: ASTGetSymbol, rules: Array<Rule>): Int {
        TODO("Not yet implemented")
    }

    override fun constructString(expression: Expression, emitter: ASMEmitter, astToIRService: ASTToIRService): Expression {
        TODO("Not yet implemented")
    }
}
private class TestLiteralRule: Rule() {
    override val expression = Expression.Literal(null)
    override val resultSymbol = 'r'

    override fun getCost(expression: Expression, astGetSymbol: ASTGetSymbol, rules: Array<Rule>): Int {
        TODO("Not yet implemented")
    }

    override fun constructString(expression: Expression, emitter: ASMEmitter, astToIRService: ASTToIRService): Expression {
        TODO("Not yet implemented")
    }
}
class ExpressionMatchingTrieTest {
    @Test
    internal fun simpleTrieCreationTest() {
        val rules = arrayOf<Rule>(
            TestAddRule()
        )
        val expressionMatchingTrie = ExpressionMatchingTrie(rules, ASTGetSymbol())

        // Expected trie entries
        val expectedTrieEntries = listOf(
            ExpressionMatchingTrie.TrieEntry(' ', 0),
            ExpressionMatchingTrie.TrieEntry('+', 1),
            ExpressionMatchingTrie.TrieEntry(0.toChar(), 2),
            ExpressionMatchingTrie.TrieEntry('r', 3),
            ExpressionMatchingTrie.TrieEntry(1.toChar(), 2),
            ExpressionMatchingTrie.TrieEntry('l', 3)
        )
        expectedTrieEntries[0].next.add(1)
        expectedTrieEntries[1].next.add(2)
        expectedTrieEntries[1].next.add(4)
        expectedTrieEntries[2].next.add(3)
        expectedTrieEntries[4].next.add(5)

        expectedTrieEntries[5].isAccepting[0] = Pair(true, 3)
        expectedTrieEntries[3].isAccepting[0] = Pair(true, 3)


        assertEquals(expectedTrieEntries, expressionMatchingTrie.trieEntries, "The tree creation process has failed.")
    }
    @Test
    internal fun failureFunctionTrieCreationTest() {
        val rules = arrayOf(
            TestAddRule(),
            TestLiteralRule(),
        )
        val expressionMatchingTrie = ExpressionMatchingTrie(rules, ASTGetSymbol())

        // Expected trie entries
        val expectedTrieEntries = listOf(
            ExpressionMatchingTrie.TrieEntry(' ', 0),
            ExpressionMatchingTrie.TrieEntry('+', 1),
            ExpressionMatchingTrie.TrieEntry(0.toChar(), 2),
            ExpressionMatchingTrie.TrieEntry('r', 3),
            ExpressionMatchingTrie.TrieEntry(1.toChar(), 2),
            ExpressionMatchingTrie.TrieEntry('l', 3),
            ExpressionMatchingTrie.TrieEntry('l', 1)
        )
        expectedTrieEntries[0].next.add(1)
        expectedTrieEntries[0].next.add(6)
        expectedTrieEntries[1].next.add(2)
        expectedTrieEntries[1].next.add(4)
        expectedTrieEntries[2].next.add(3)
        expectedTrieEntries[4].next.add(5)

        expectedTrieEntries[5].isAccepting[0] = Pair(true, 3)
        expectedTrieEntries[5].isAccepting[1] = Pair(true, 1)
        expectedTrieEntries[3].isAccepting[0] = Pair(true, 3)
        expectedTrieEntries[6].isAccepting[1] = Pair(true, 1)

        expectedTrieEntries[5].failureState = 6

        assertEquals(expectedTrieEntries, expressionMatchingTrie.trieEntries, "The tree creation process has failed because of failure state creation")
    }
}
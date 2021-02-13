package com.cubearrow.cubelang.utils

import com.cubearrow.cubelang.lexer.Token
import com.cubearrow.cubelang.lexer.TokenType
import com.cubearrow.cubelang.parser.Expression
import com.cubearrow.cubelang.utils.Type.Companion.getType
import org.junit.jupiter.api.Test

class ExpressionUtilsTest {
    @Test
    internal fun getType(){
        assert(getType(NormalType("i32"), 2) == NormalType("i32"))

        assert(getType(null, 4) == NormalType("i32"))
        assert(getType(null, "Hello, World") == NormalType("string"))
        assert(getType(null, 'i') == NormalType("char"))
        assert(getType(null, null) == NormalType("any"))
    }

    @Test
    internal fun mapArgumentDefinitions(){
        val argumentDefinitions = listOf(
            Expression.ArgumentDefinition(Token("name1", TokenType.IDENTIFIER, -1, -1), NormalType("type1")),
            Expression.ArgumentDefinition(Token("name2", TokenType.IDENTIFIER, -1, -1), NormalType("type2")),
        )
        val expected = mapOf("name1" to NormalType("type1"), "name2" to NormalType("type2"))
        assert(ExpressionUtils.mapArgumentDefinitions(argumentDefinitions) == expected)
    }
}
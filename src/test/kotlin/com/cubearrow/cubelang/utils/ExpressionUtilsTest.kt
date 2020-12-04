package com.cubearrow.cubelang.utils

import com.cubearrow.cubelang.lexer.Token
import com.cubearrow.cubelang.lexer.TokenType
import com.cubearrow.cubelang.parser.Expression
import com.cubearrow.cubelang.utils.ExpressionUtils.Companion.getType
import org.junit.jupiter.api.Test

class ExpressionUtilsTest {
    @Test
    internal fun getType(){
        assert(getType("int", 2) == "int")

        assert(getType(null, 4) == "int")
        assert(getType(null, "Hello, World") == "string")
        assert(getType(null, 'i') == "char")
        assert(getType(null, null) == "any")
    }

    @Test
    internal fun mapArgumentDefinitions(){
        val argumentDefinitions = listOf(
            Expression.ArgumentDefinition(Token("name1", TokenType.IDENTIFIER, -1, -1), Token("type1", TokenType.IDENTIFIER, -1, -1)),
            Expression.ArgumentDefinition(Token("name2", TokenType.IDENTIFIER, -1, -1), Token("type2", TokenType.IDENTIFIER, -1, -1)),
        )
        val expected = mapOf("name1" to "type1", "name2" to "type2")
        assert(ExpressionUtils.mapArgumentDefinitions(argumentDefinitions) == expected)
    }
}
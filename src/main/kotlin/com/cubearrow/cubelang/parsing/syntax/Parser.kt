package com.cubearrow.cubelang.parsing.syntax

import com.cubearrow.cubelang.bnf.BnfParser
import com.cubearrow.cubelang.main.Main
import com.cubearrow.cubelang.parsing.tokenization.Token
import com.cubearrow.cubelang.parsing.tokenization.TokenType

class Parser(var tokens: List<Token>) {
    companion object{
        val unidentifiableTokenTypes = listOf(TokenType.IDENTIFIER, TokenType.NUMBER)
    }

    private var current = -1
    fun parse() {
        while (current < tokens.size - 1) {
            println(nextExpression(null))
        }
    }

    private fun nextExpression(previousToken: Token?) : Expression?{
        current++
        val currentToken = tokens[current]
        if(currentToken.tokenType == TokenType.SEMICOLON){
            if(previousToken?.tokenType?.equals(TokenType.NUMBER)!!){
                return Expression.Literal(previousToken)
            }
        }

        if (previousToken == null && unidentifiableTokenTypes.contains(currentToken.tokenType)){
            return nextExpression(currentToken)
        }
        else if(currentToken.tokenType == TokenType.EQUALS && tokens[current + 1].tokenType != TokenType.EQUALS){
            return Expression.Assignment(previousToken as Token, currentToken, nextExpression(null) as Expression)
        }
        Main.error(currentToken.line, currentToken.index, null, "Unexpected token \"${currentToken.substring}\"")
        return null
    }
}
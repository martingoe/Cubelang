package com.cubearrow.cubelang.lexer

data class Token(var substring: String, var tokenType: TokenType, var line: Int, var index: Int)

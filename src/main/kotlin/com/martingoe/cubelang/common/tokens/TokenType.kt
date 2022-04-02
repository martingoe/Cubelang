package com.martingoe.cubelang.common.tokens

/**
 * The separate tokens in enum form. The same ones can be found in the grammar file.
 */
enum class TokenType {
    IDENTIFIER, CURLYR, CURLYL, IF, ELSE, RETURN, FUN, WHILE, BRCKTL, BRCKTR, SEMICOLON, SLASH,
    COMPARATOR, EQUALS, COMMA, STRING, FOR, VAR, STRUCT, DOT, COLON, NULLVALUE, NUMBER, CHAR, EOF, OR, AND, EQUALITY,
    PLUSMINUS, BANG, CLOSEDL, CLOSEDR, IMPORT, POINTER, STAR;
}
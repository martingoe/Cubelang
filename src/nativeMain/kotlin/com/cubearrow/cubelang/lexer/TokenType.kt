package com.cubearrow.cubelang.lexer

/**
 * The separate tokens in enum form. The same ones can be found in the grammar file.
 */
enum class TokenType {
    IDENTIFIER, CURLYR, CURLYL, LINE_COMMENT, IF, ELSE, RETURN, FUN, WHILE, BRCKTL, BRCKTR, SEMICOLON, OPERATOR,
    COMPARATOR, EQUALS, NOT_FOUND, COMMA, STRING, FOR, VAR, CLASS, DOT, COLON, NULLVALUE, NUMBER, CHAR, EOF, OR, AND, EQUALITY,
    PLUSMINUS, BANG, CLOSEDL, CLOSEDR, IMPORT;
}
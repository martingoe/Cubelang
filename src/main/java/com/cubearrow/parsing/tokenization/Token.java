package com.cubearrow.parsing.tokenization;

import org.json.JSONObject;

import java.util.Map;

public enum Token {
    IDENTIFIER,
    IF,
    RETURN,
    FUN,
    WHILE,
    BRCKTL,
    BRCKTR,
    SEMICOLON,
    ADD,
    SUB,
    EXP,
    DIV,
    MULT,
    MOD,
    EQEQ,
    EXCLEQ,
    AND_GATE,
    OR_GATE,
    INT_LITERAL,
    DOUBLE_LITERAL,
    CHAR_LITERAL,
    INT_TYPE,
    DOUBLE_TYPE,
    CHAR_TYPE,
    MULTLN_START,
    MULTLN_STOP,
    NOT_FOUND;

    public static Token fromString(String string, JSONObject grammarMap){
        Map<String, Object> stringObjectMap = grammarMap.toMap();
        for(Map.Entry<String, Object> grammarEntry : stringObjectMap.entrySet()) {
            if (string.matches((String) grammarEntry.getValue())) {
                return Token.valueOf((String) grammarEntry.getValue());
            }
        }
        return Token.NOT_FOUND;
    }
}

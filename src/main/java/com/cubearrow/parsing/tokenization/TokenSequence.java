package com.cubearrow.parsing.tokenization;

import java.util.HashMap;
import java.util.Map;

public class TokenSequence {
    Map<String, Token> tokenSequence = new HashMap<>();
    TokenGrammar tokenGrammar;

    TokenSequence(String line) {
        String splittingRegEx = String.format("\\s|%S|%S", tokenGrammar.getString("BRCKTL"), tokenGrammar.getString("BRCKTR"));
        loadTokenSequence(line, splittingRegEx);
    }

    private void loadTokenSequence(String line, String splittingRegEx) {
        String[] splitLine = line.split(splittingRegEx);

        for (String s : splitLine) {
            tokenSequence.put(s, Token.fromString(s, tokenGrammar));
        }
    }
}

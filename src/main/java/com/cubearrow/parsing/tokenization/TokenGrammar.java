package com.cubearrow.parsing.tokenization;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class TokenGrammar extends JSONObject{
    public TokenGrammar(File tokenGrammarFile) throws IOException {
        super(new String(Files.readAllBytes(tokenGrammarFile.toPath())));
    }
}

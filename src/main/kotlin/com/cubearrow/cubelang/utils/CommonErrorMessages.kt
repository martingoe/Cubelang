package com.cubearrow.cubelang.utils

import com.cubearrow.cubelang.compiler.CompilerContext
import com.cubearrow.cubelang.lexer.Token
import com.cubearrow.cubelang.main.Main

class CommonErrorMessages {
    companion object {
        fun onlyNumberError(token: Token, fileName: String) =
                Main.error(token.line, token.index, "This operation \"${token.substring}\" can only be executed on numbers.", fileName)

        fun xNotFound(notFoundThing: String, token: Token, context: CompilerContext) =
                context.error(token.line, token.index, "Could not find the $notFoundThing.")
    }
}
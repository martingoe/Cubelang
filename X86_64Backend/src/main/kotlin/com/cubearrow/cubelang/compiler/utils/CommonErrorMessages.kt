package com.cubearrow.cubelang.compiler.utils

import com.cubearrow.cubelang.common.tokens.Token
import com.cubearrow.cubelang.compiler.CompilerContext

class CommonErrorMessages {
    companion object {
        fun xNotFound(notFoundThing: String, token: Token, context: CompilerContext) =
                context.error(token.line, token.index, "Could not find the $notFoundThing.")
    }
}
package com.cubearrow.cubelang.utils

import com.cubearrow.cubelang.lexer.Token
import Main

class UsualErrorMessages {
    companion object {
        fun onlyNumberError(token: Token) =
                Main.error(token.line, token.index, null, "This operation \"${token.substring}\" can only be executed on numbers.")

        fun xNotFound(notFoundThing: String, token: Token) =
                Main.error(token.line, token.index, null, "Could not find the $notFoundThing.")
    }
}
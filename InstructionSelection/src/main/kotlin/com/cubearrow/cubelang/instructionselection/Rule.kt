package com.cubearrow.cubelang.instructionselection

import com.cubearrow.cubelang.common.*
import com.cubearrow.cubelang.common.ir.*
import com.cubearrow.cubelang.common.ASMEmitter
import com.cubearrow.cubelang.common.tokens.Token
import com.cubearrow.cubelang.common.tokens.TokenType

abstract class Rule {
    abstract val expression: Expression
    abstract val resultSymbol: Char

    abstract fun getCost(expression: Expression, astGetSymbol: ASTGetSymbol, rules: List<Rule>): Int
    abstract fun constructString(expression: Expression, emitter: ASMEmitter, trie: ExpressionMatchingTrie): Expression

    companion object {
        const val RULE_COUNT = 25
    }
}

private fun emitMulRegLiteral(
    right: Literal,
    left: TemporaryRegister,
    emitter: ASMEmitter,
    resultType: Type
) {
    when (right.value) {
        "2" -> {
            emitter.emit(IRValue(IRType.PLUS_OP, left, left, resultType))
        }
        "4" -> {
            emitter.emit(IRValue(IRType.SAL, left, Literal("2"), resultType))
        }
        "8" -> {
            emitter.emit(IRValue(IRType.SAL, left, Literal("3"), resultType))
        }
        else -> emitter.emit(IRValue(IRType.MUL_OP, left, right, resultType))
    }
}

class PlusOperationRegReg : Rule() {
    override val expression: Expression
        get() = Expression.Operation(Expression.Register(), Token("+", TokenType.PLUSMINUS), Expression.Register())
    override val resultSymbol: Char
        get() = 'r'

    override fun getCost(expression: Expression, astGetSymbol: ASTGetSymbol, rules: List<Rule>): Int {
        return 2 + calculateSubCosts(this.expression, expression, astGetSymbol, rules)
    }


    override fun constructString(expression: Expression, emitter: ASMEmitter, trie: ExpressionMatchingTrie): Expression {
        val castExpression = expression as Expression.Operation
        emitter.emit(
            IRValue(
                IRType.PLUS_OP, TemporaryRegister((castExpression.leftExpression as Expression.Register).index),
                TemporaryRegister((castExpression.rightExpression as Expression.Register).index), expression.resultType
            )
        )
        return castExpression.leftExpression
    }
}

class PlusOperationRegLit : Rule() {
    override val expression: Expression
        get() = Expression.Operation(Expression.Register(), Token("+", TokenType.PLUSMINUS), Expression.Literal(null))
    override val resultSymbol: Char
        get() = 'r'

    override fun getCost(expression: Expression, astGetSymbol: ASTGetSymbol, rules: List<Rule>): Int {
        return 2 + calculateSubCosts(this.expression, expression, astGetSymbol, rules)
    }

    override fun constructString(expression: Expression, emitter: ASMEmitter, trie: ExpressionMatchingTrie): Expression {
        val castExpression = expression as Expression.Operation
        val reg = castExpression.leftExpression as Expression.Register
        val temporaryRegister = TemporaryRegister(reg.index)
        val value = getLiteralValue((castExpression.rightExpression as Expression.Literal).value)
        if (value == 1)
//            emitter.emit("inc $reg")
            emitter.emit(IRValue(IRType.INC, temporaryRegister, null, expression.resultType))
        else {
//            emitter.emit("add $reg, $value")
            emitter.emit(IRValue(IRType.PLUS_OP, temporaryRegister, Literal(value.toString()), expression.resultType))
        }
        return castExpression.leftExpression
    }
}

class PlusOperationLitReg : Rule() {
    override val expression: Expression
        get() = Expression.Operation(Expression.Literal(null), Token("+", TokenType.PLUSMINUS), Expression.Register())
    override val resultSymbol: Char
        get() = 'r'

    override fun getCost(expression: Expression, astGetSymbol: ASTGetSymbol, rules: List<Rule>): Int {
        return 2 + calculateSubCosts(this.expression, expression, astGetSymbol, rules)
    }

    override fun constructString(expression: Expression, emitter: ASMEmitter, trie: ExpressionMatchingTrie): Expression {
        val castExpression = expression as Expression.Operation
        val reg = castExpression.rightExpression as Expression.Register
        val temporaryRegister = TemporaryRegister(reg.index)
        val value = getLiteralValue((castExpression.leftExpression as Expression.Literal).value)

        if (value == 1)
            emitter.emit(IRValue(IRType.INC, temporaryRegister, null, expression.resultType))
        else {
            emitter.emit(IRValue(IRType.PLUS_OP, temporaryRegister, Literal(value.toString()), expression.resultType))
        }
        return castExpression.rightExpression
    }
}

class SubOperationRegLit : Rule() {
    override val expression: Expression
        get() = Expression.Operation(Expression.Register(), Token("-", TokenType.PLUSMINUS), Expression.Literal(null))
    override val resultSymbol: Char
        get() = 'r'

    override fun getCost(expression: Expression, astGetSymbol: ASTGetSymbol, rules: List<Rule>): Int {
        return 2 + calculateSubCosts(this.expression, expression, astGetSymbol, rules)
    }

    override fun constructString(expression: Expression, emitter: ASMEmitter, trie: ExpressionMatchingTrie): Expression {
        val castExpression = expression as Expression.Operation
        val reg = castExpression.leftExpression as Expression.Register
        val temporaryRegister = TemporaryRegister(reg.index)
        val value = getLiteralValue((castExpression.rightExpression as Expression.Literal).value)

        if (value == 1) {
            emitter.emit(IRValue(IRType.DEC, temporaryRegister, null, expression.resultType))
        } else {
            emitter.emit(IRValue(IRType.MINUS_OP, temporaryRegister, Literal(value.toString()), expression.resultType))
        }
        return castExpression.leftExpression
    }
}

class ExtendTo64BitsRule : Rule() {
    override val expression: Expression
        get() = Expression.ExtendTo64Bit(Expression.Register())
    override val resultSymbol: Char
        get() = 'r'

    override fun getCost(expression: Expression, astGetSymbol: ASTGetSymbol, rules: List<Rule>): Int {
        return 1
    }

    override fun constructString(expression: Expression, emitter: ASMEmitter, trie: ExpressionMatchingTrie): Expression {
        val reg = (expression as Expression.ExtendTo64Bit).expression as Expression.Register
        emitter.emit(IRValue(IRType.EXTEND_TO_64BITS, TemporaryRegister(reg.index), null, expression.resultType))

        reg.resultType = NormalType(NormalTypes.I64)
        return reg
    }
}

class SubOperationRegReg : Rule() {
    override val expression: Expression
        get() = Expression.Operation(Expression.Register(), Token("-", TokenType.PLUSMINUS), Expression.Register())
    override val resultSymbol: Char
        get() = 'r'

    override fun getCost(expression: Expression, astGetSymbol: ASTGetSymbol, rules: List<Rule>): Int {
        return 2 + calculateSubCosts(this.expression, expression, astGetSymbol, rules)
    }

    override fun constructString(expression: Expression, emitter: ASMEmitter, trie: ExpressionMatchingTrie): Expression {
        val castExpression = expression as Expression.Operation
        val reg = castExpression.leftExpression as Expression.Register
        val temporaryRegister = TemporaryRegister(reg.index)
        val rightRegister = TemporaryRegister((castExpression.rightExpression as Expression.Register).index)
        emitter.emit(IRValue(IRType.MINUS_OP, temporaryRegister, rightRegister, expression.resultType))
        return castExpression.leftExpression
    }
}

class DivOperationRegReg : Rule() {
    override val expression: Expression
        get() = Expression.Operation(Expression.Register(), Token("/", TokenType.SLASH), Expression.Register())
    override val resultSymbol: Char
        get() = 'r'

    override fun getCost(expression: Expression, astGetSymbol: ASTGetSymbol, rules: List<Rule>): Int {
        return 3 + calculateSubCosts(this.expression, expression, astGetSymbol, rules)
    }

    override fun constructString(expression: Expression, emitter: ASMEmitter, trie: ExpressionMatchingTrie): Expression {
        val castExpression = expression as Expression.Operation
        val reg = castExpression.leftExpression as Expression.Register
        val temporaryRegister = TemporaryRegister(reg.index)
        val rightRegister = TemporaryRegister((castExpression.rightExpression as Expression.Register).index)
        emitter.emit(IRValue(IRType.DIV_OP, temporaryRegister, rightRegister, expression.resultType))
        return castExpression.leftExpression
    }
}

class MulOperationRegReg : Rule() {
    override val expression: Expression
        get() = Expression.Operation(Expression.Register(), Token("*", TokenType.STAR), Expression.Register())
    override val resultSymbol: Char
        get() = 'r'

    override fun getCost(expression: Expression, astGetSymbol: ASTGetSymbol, rules: List<Rule>): Int {
        return 2 + calculateSubCosts(this.expression, expression, astGetSymbol, rules)
    }

    override fun constructString(expression: Expression, emitter: ASMEmitter, trie: ExpressionMatchingTrie): Expression {
        val castExpression = expression as Expression.Operation
        val reg = castExpression.leftExpression as Expression.Register
        val temporaryRegister = TemporaryRegister(reg.index)
        val rightRegister = TemporaryRegister((castExpression.rightExpression as Expression.Register).index)
        emitter.emit(IRValue(IRType.MUL_OP, temporaryRegister, rightRegister, expression.resultType))
        return castExpression.leftExpression

    }
}

class MulOperationRegLit : Rule() {
    override val expression: Expression
        get() = Expression.Operation(Expression.Register(), Token("*", TokenType.STAR), Expression.Literal(null))
    override val resultSymbol: Char
        get() = 'r'

    override fun getCost(expression: Expression, astGetSymbol: ASTGetSymbol, rules: List<Rule>): Int {
        return 3 + calculateSubCosts(this.expression, expression, astGetSymbol, rules)
    }

    override fun constructString(expression: Expression, emitter: ASMEmitter, trie: ExpressionMatchingTrie): Expression {
        val castExpression = expression as Expression.Operation
        val left = TemporaryRegister((castExpression.leftExpression as Expression.Register).index)
        val right = Literal(getLiteralValue((castExpression.rightExpression as Expression.Literal).value).toString())
        emitMulRegLiteral(right, left, emitter, expression.resultType)
        return expression.leftExpression
    }
}

fun getLiteralValue(value: Any?): Int {
    return when (value) {
        is Char -> value.code
        is Int -> value
        is Short -> value.toInt()

        else -> error("${value!!::class}")
    }
}

class MulOperationLitReg : Rule() {
    override val expression: Expression
        get() = Expression.Operation(Expression.Literal(null), Token("*", TokenType.STAR), Expression.Register())
    override val resultSymbol: Char
        get() = 'r'

    override fun getCost(expression: Expression, astGetSymbol: ASTGetSymbol, rules: List<Rule>): Int {
        return 3 + calculateSubCosts(this.expression, expression, astGetSymbol, rules)
    }

    override fun constructString(expression: Expression, emitter: ASMEmitter, trie: ExpressionMatchingTrie): Expression {
        val castExpression = expression as Expression.Operation
        val left = TemporaryRegister((castExpression.rightExpression as Expression.Register).index)
        val right = Literal(getLiteralValue((castExpression.leftExpression as Expression.Literal).value).toString())
        emitMulRegLiteral(right, left, emitter, expression.resultType)
        return expression.rightExpression
    }
}

class LiteralToReg : Rule() {
    override val expression: Expression
        get() = Expression.Literal(null)
    override val resultSymbol: Char
        get() = 'r'

    override fun getCost(expression: Expression, astGetSymbol: ASTGetSymbol, rules: List<Rule>): Int {
        return 1
    }

    override fun constructString(expression: Expression, emitter: ASMEmitter, trie: ExpressionMatchingTrie): Expression {
        val reg = getReg(expression.resultType)
        val literal = Literal(
            getLiteralValue(
                (expression as Expression.Literal).value
            ).toString()
        )
        emitter.emit(IRValue(IRType.COPY, TemporaryRegister(reg.index), literal, expression.resultType))
        return reg
    }
}

class MovRegToReg : Rule() {
    override val expression: Expression
        get() = Expression.Assignment(Expression.Register(), Expression.Register(), Token("=", TokenType.EQUALS))
    override val resultSymbol: Char
        get() = 'r'

    override fun getCost(expression: Expression, astGetSymbol: ASTGetSymbol, rules: List<Rule>): Int {
        return 1 + calculateSubCosts(this.expression, expression, astGetSymbol, rules)
    }

    override fun constructString(expression: Expression, emitter: ASMEmitter, trie: ExpressionMatchingTrie): Expression {
        val castExpression = expression as Expression.Assignment
        val valueRegister = (castExpression.valueExpression) as Expression.Register
        val leftReg = (castExpression.leftSide) as Expression.Register
        emitter.emit(IRValue(IRType.COPY, TemporaryRegister(leftReg.index), TemporaryRegister(valueRegister.index), expression.resultType))
        return castExpression.leftSide
    }


}

class NegateRule : Rule() {
    override val expression: Expression
        get() = Expression.Unary(Token("-", TokenType.PLUSMINUS), Expression.Register())
    override val resultSymbol: Char
        get() = 'r'

    override fun getCost(expression: Expression, astGetSymbol: ASTGetSymbol, rules: List<Rule>): Int {
        return 1
    }

    override fun constructString(expression: Expression, emitter: ASMEmitter, trie: ExpressionMatchingTrie): Expression {

        val reg = (expression as Expression.Unary).expression as Expression.Register
        emitter.emit(IRValue(IRType.NEG_UNARY, TemporaryRegister(reg.index), null, expression.resultType))
        return reg
    }
}

class MovPointerRegToReg : Rule() {
    override val expression: Expression
        get() = Expression.Assignment(
            Expression.ValueFromPointer(Expression.Register(), Token("*", TokenType.STAR)),
            Expression.Register(),
            Token("=", TokenType.EQUALS)
        )
    override val resultSymbol: Char
        get() = 'r'

    override fun getCost(expression: Expression, astGetSymbol: ASTGetSymbol, rules: List<Rule>): Int {
        return 2 + calculateSubCosts(this.expression, expression, astGetSymbol, rules)
    }

    override fun constructString(expression: Expression, emitter: ASMEmitter, trie: ExpressionMatchingTrie): Expression {
        val castExpression = expression as Expression.Assignment
        val valueRegister = (castExpression.valueExpression) as Expression.Register
        val valueFromPointer = castExpression.leftSide as Expression.ValueFromPointer
        emitter.emit(
            IRValue(
                IRType.COPY_TO_DEREF,
                TemporaryRegister((valueFromPointer.expression as Expression.Register).index),
                TemporaryRegister(valueRegister.index),
                expression.resultType
            )
        )

        return valueFromPointer.expression
    }
}

class MovOffsetToReg : Rule() {
    override val expression: Expression
        get() = Expression.Assignment(
            Expression.ValueFromPointer(
                Expression.Operation(
                    Expression.FramePointer(),
                    Token("-", TokenType.PLUSMINUS),
                    Expression.Literal(null)
                ), Token("*", TokenType.STAR)
            ), Expression.Register(), Token("=", TokenType.EQUALS)
        )
    override val resultSymbol: Char
        get() = 'r'

    override fun getCost(expression: Expression, astGetSymbol: ASTGetSymbol, rules: List<Rule>): Int {
        return 2 + calculateSubCosts(this.expression, expression, astGetSymbol, rules)
    }

    override fun constructString(expression: Expression, emitter: ASMEmitter, trie: ExpressionMatchingTrie): Expression {
        val castExpression = expression as Expression.Assignment
        val valueRegister = (castExpression.valueExpression) as Expression.Register
        val valueFromPointer = castExpression.leftSide as Expression.ValueFromPointer
        val operation = valueFromPointer.expression as Expression.Operation
        emitter.emit(
            IRValue(
                IRType.COPY_TO_FP_OFFSET,
                Literal(getLiteralValue((operation.rightExpression as Expression.Literal).value).toString()),
                TemporaryRegister(valueRegister.index),
                expression.resultType
            )
        )
        return valueFromPointer.expression
    }
}

class MovFromFPOffset : Rule() {
    override val expression: Expression
        get() = Expression.ValueFromPointer(
            Expression.Operation(
                Expression.FramePointer(),
                Token("-", TokenType.PLUSMINUS),
                Expression.Literal(null)
            ), Token("*", TokenType.STAR)
        )
    override val resultSymbol: Char
        get() = 'r'

    override fun getCost(expression: Expression, astGetSymbol: ASTGetSymbol, rules: List<Rule>): Int {
        return 3
    }

    override fun constructString(expression: Expression, emitter: ASMEmitter, trie: ExpressionMatchingTrie): Expression {
        val reg = getReg(expression.resultType)
        val castExpression = expression as Expression.ValueFromPointer
        val operation = castExpression.expression as Expression.Operation

        emitter.emit(
            IRValue(
                IRType.COPY_FROM_FP_OFFSET,
                TemporaryRegister(reg.index),
                Literal(getLiteralValue((operation.rightExpression as Expression.Literal).value).toString()),
                expression.resultType
            )
        )
        return reg
    }
}

class MovFromRegOffset : Rule() {
    override val expression: Expression
        get() = Expression.ValueFromPointer(
            Expression.Operation(
                Expression.Register(),
                Token("-", TokenType.PLUSMINUS),
                Expression.Literal(null)
            ), Token("*", TokenType.STAR)
        )
    override val resultSymbol: Char
        get() = 'r'

    override fun getCost(expression: Expression, astGetSymbol: ASTGetSymbol, rules: List<Rule>): Int {
        return 3
    }

    override fun constructString(expression: Expression, emitter: ASMEmitter, trie: ExpressionMatchingTrie): Expression {
        val reg = getReg(expression.resultType)
        val castExpression = expression as Expression.ValueFromPointer
        val operation = castExpression.expression as Expression.Operation

        emitter.emit(
            IRValue(
                IRType.COPY_FROM_REG_OFFSET,
                RegOffset(
                    TemporaryRegister((operation.leftExpression as Expression.Register).index),
                    ((operation.rightExpression as Expression.Literal).value as Int).toString()
                ),
                TemporaryRegister(reg.index),
                expression.resultType
            )
        )

        return reg
    }
}


class MovFromRegOffsetWithAdder : Rule() {
    override val expression: Expression
        get() = Expression.ValueFromPointer(
            Expression.Operation(
                Expression.Operation(
                    Expression.FramePointer(),
                    Token("-", TokenType.PLUSMINUS),
                    Expression.Literal(null)
                ), Token("+", TokenType.PLUSMINUS),

                Expression.ExtendTo64Bit(Expression.Operation(Expression.Register(), Token("*", TokenType.STAR), Expression.Literal(null)))
            ), Token("*", TokenType.STAR)
        )
    override val resultSymbol: Char
        get() = 'r'

    override fun getCost(expression: Expression, astGetSymbol: ASTGetSymbol, rules: List<Rule>): Int {
        return 3
    }

    override fun constructString(expression: Expression, emitter: ASMEmitter, trie: ExpressionMatchingTrie): Expression {
        val reg = getReg(expression.resultType)
        val castExpression = expression as Expression.ValueFromPointer
        val operation = castExpression.expression as Expression.Operation
        val leftOperation = operation.leftExpression as Expression.Operation
        val rightOperation = (operation.rightExpression as Expression.ExtendTo64Bit).expression as Expression.Operation

        emitter.emit(
            IRValue(
                IRType.EXTEND_TO_64BITS,
                TemporaryRegister((rightOperation.leftExpression as Expression.Register).index),
                null,
                rightOperation.resultType
            )
        )

        emitter.emit(
            IRValue(
                IRType.COPY_FROM_FP_OFFSET,

                TemporaryRegister(reg.index),
                FramePointerOffset(
                    ((leftOperation.rightExpression as Expression.Literal).value as Int).toString(),
                    TemporaryRegister((rightOperation.leftExpression as Expression.Register).index),
                    ((rightOperation.rightExpression as Expression.Literal).value as Int).toString(),
                ),
                expression.resultType
            )
        )

        return reg
    }
}

class MovOffsetToOffset : Rule() {
    override val expression: Expression
        get() = Expression.Assignment(
            Expression.ValueFromPointer(
                Expression.Operation(Expression.FramePointer(), Token("-", TokenType.PLUSMINUS), Expression.Literal(null)),
                Token("*", TokenType.STAR)
            ),
            Expression.ValueFromPointer(
                Expression.Operation(Expression.FramePointer(), Token("-", TokenType.PLUSMINUS), Expression.Literal(null)),
                Token("*", TokenType.STAR)
            ), Token("=", TokenType.EQUALS)
        )
    override val resultSymbol: Char
        get() = 'r'

    override fun getCost(expression: Expression, astGetSymbol: ASTGetSymbol, rules: List<Rule>): Int {
        return 2
    }

    override fun constructString(expression: Expression, emitter: ASMEmitter, trie: ExpressionMatchingTrie): Expression {
        expression as Expression.Assignment
        var rightOffset =
            getLiteralValue((((expression.valueExpression as Expression.ValueFromPointer).expression as Expression.Operation).rightExpression as Expression.Literal).value)
        var leftOffset =
            getLiteralValue((((expression.leftSide as Expression.ValueFromPointer).expression as Expression.Operation).rightExpression as Expression.Literal).value)

        if (expression.valueExpression.resultType is StructType) {
            val lengths = Utils.splitStruct(expression.valueExpression.resultType.getLength())

            lengths.forEach {
                val intTypeForLength = getIntTypeForLength(it)
                val reg = getReg(intTypeForLength)
                emitter.emit(
                    IRValue(
                        IRType.COPY_FROM_FP_OFFSET,
                        TemporaryRegister(reg.index),
                        Literal(rightOffset.toString()),
                        intTypeForLength
                    )
                )
                emitter.emit(IRValue(IRType.COPY_TO_FP_OFFSET, Literal(leftOffset.toString()), TemporaryRegister(reg.index), intTypeForLength))

                rightOffset -= it
                leftOffset -= it

                currentRegister--
            }
            return getReg(expression.valueExpression.resultType)

        }

        val reg = getReg(expression.valueExpression.resultType)
        emitter.emit(IRValue(IRType.COPY_FROM_FP_OFFSET, TemporaryRegister(reg.index), Literal(rightOffset.toString()), expression.resultType))
        emitter.emit(IRValue(IRType.COPY_TO_FP_OFFSET, Literal(leftOffset.toString()), TemporaryRegister(reg.index), expression.resultType))

        return reg

    }
}

private fun getIntTypeForLength(length: Int): Type {
    return when (length) {
        1 -> NormalType(NormalTypes.I8)
        2 -> NormalType(NormalTypes.I16)
        4 -> NormalType(NormalTypes.I32)
        8 -> NormalType(NormalTypes.I64)

        else -> error("Unknown size")
    }
}


class MovOffsetToValueFromPointerOffset : Rule() {
    override val expression: Expression
        get() = Expression.Assignment(
            Expression.ValueFromPointer(
                Expression.Operation(Expression.FramePointer(), Token("-", TokenType.PLUSMINUS), Expression.Literal(null)),
                Token("*", TokenType.STAR)
            ),
            Expression.ValueFromPointer(
                Expression.ValueFromPointer(
                    Expression.Operation(
                        Expression.FramePointer(),
                        Token("-", TokenType.PLUSMINUS),
                        Expression.Literal(null)
                    ), Token("*", TokenType.STAR)
                ), Token("*", TokenType.STAR)
            ), Token("=", TokenType.EQUALS)
        )
    override val resultSymbol: Char
        get() = 'r'

    override fun getCost(expression: Expression, astGetSymbol: ASTGetSymbol, rules: List<Rule>): Int {
        return 2
    }

    override fun constructString(expression: Expression, emitter: ASMEmitter, trie: ExpressionMatchingTrie): Expression {
        expression as Expression.Assignment
        val varCallOffset =
            getLiteralValue(((((expression.valueExpression as Expression.ValueFromPointer).expression as Expression.ValueFromPointer).expression as Expression.Operation).rightExpression as Expression.Literal).value)


        val reg = getReg(NormalType(NormalTypes.I64))
        emitter.emit(
            IRValue(
                IRType.COPY_FROM_FP_OFFSET,
                TemporaryRegister(reg.index),
                Literal(varCallOffset.toString()),
                NormalType(NormalTypes.I64)
            )
        )

        var rightOffset = 0
        var leftOffset =
            getLiteralValue((((expression.leftSide as Expression.ValueFromPointer).expression as Expression.Operation).rightExpression as Expression.Literal).value)
        val lengths = Utils.splitStruct(expression.valueExpression.resultType.getLength())

        lengths.forEach {
            val intTypeForLength = getIntTypeForLength(it)
            val innerReg = getReg(intTypeForLength)
            emitter.emit(
                IRValue(
                    IRType.COPY_FROM_REG_OFFSET,
                    RegOffset(TemporaryRegister(reg.index), (rightOffset).toString()),
                    TemporaryRegister(innerReg.index),
                    intTypeForLength
                )
            )
            emitter.emit(IRValue(IRType.COPY_TO_FP_OFFSET, Literal(leftOffset.toString()), TemporaryRegister(innerReg.index), intTypeForLength))

            rightOffset -= it
            leftOffset -= it

            currentRegister--
        }
        return getReg(expression.leftSide.resultType)
    }
}

fun getReg(type: Type): Expression.Register {
    return Expression.Register(currentRegister++, type)
}

class ValueFromPointer : Rule() {
    override val expression: Expression
        get() = Expression.ValueFromPointer(Expression.Register(), Token("*", TokenType.STAR))
    override val resultSymbol: Char
        get() = 'r'

    override fun getCost(expression: Expression, astGetSymbol: ASTGetSymbol, rules: List<Rule>): Int {
        return 2 + calculateSubCosts(this.expression, expression, astGetSymbol, rules)
    }

    override fun constructString(expression: Expression, emitter: ASMEmitter, trie: ExpressionMatchingTrie): Expression {
        val castExpression = expression as Expression.ValueFromPointer
        val valueRegister = castExpression.expression as Expression.Register
        val reg = getReg(valueRegister.type)
        emitter.emit(IRValue(IRType.COPY_FROM_DEREF, TemporaryRegister(valueRegister.index), TemporaryRegister(reg.index), expression.resultType))

        return reg
    }
}

class PointerGet : Rule() {
    override val expression: Expression
        get() = Expression.PointerGet(
            Expression.ValueFromPointer(
                Expression.Operation(Expression.FramePointer(), Token("-", TokenType.PLUSMINUS), Expression.Literal(null)),
                Token("*", TokenType.STAR)
            )
        )
    override val resultSymbol: Char
        get() = 'r'

    override fun getCost(expression: Expression, astGetSymbol: ASTGetSymbol, rules: List<Rule>): Int {
        return 2 + calculateSubCosts(this.expression, expression, astGetSymbol, rules)
    }

    override fun constructString(expression: Expression, emitter: ASMEmitter, trie: ExpressionMatchingTrie): Expression {
        val castExpression = (expression as Expression.PointerGet).expression as Expression.ValueFromPointer
        val reg = getReg(expression.resultType)
        val operation = castExpression.expression as Expression.Operation
        val literal = (operation.rightExpression as Expression.Literal).value
        emitter.emit(IRValue(IRType.COPY_FROM_REF, Literal(literal.toString()), TemporaryRegister(reg.index), expression.resultType))

        return reg
    }
}

class FramePointerRule : Rule() {
    override val expression: Expression
        get() = Expression.FramePointer()
    override val resultSymbol: Char
        get() = 'r'

    override fun getCost(expression: Expression, astGetSymbol: ASTGetSymbol, rules: List<Rule>): Int {
        return 1
    }

    override fun constructString(expression: Expression, emitter: ASMEmitter, trie: ExpressionMatchingTrie): Expression {
        val reg = getReg(NormalType(NormalTypes.I64))
        emitter.emit(IRValue(IRType.COPY, TemporaryRegister(reg.index), FramePointer(), NormalType(NormalTypes.I64)))
        return reg
    }
}

class CallRule : Rule() {
    override val expression: Expression
        get() = Expression.Call(Expression.VarCall(Token("", TokenType.IDENTIFIER)), listOf(), Token("", TokenType.BRCKTL))
    override val resultSymbol: Char
        get() = 'r'

    override fun getCost(expression: Expression, astGetSymbol: ASTGetSymbol, rules: List<Rule>): Int {
        return 1
    }

    override fun constructString(expression: Expression, emitter: ASMEmitter, trie: ExpressionMatchingTrie): Expression {
        expression as Expression.Call
        for (arg in expression.arguments) {
            val result = trie.emitCodeForExpression(arg)
            val tempReg = TemporaryRegister((result as Expression.Register).index)
            emitter.emit(IRValue(IRType.PUSH_ARG, tempReg, null, arg.resultType))
        }

        val reg = if (expression.resultType !is NoneType) getReg(expression.resultType) else null
        emitter.emit(
            IRValue(
                IRType.CALL,
                FunctionLabel(expression.callee.varName.substring),
                reg?.index?.let { TemporaryRegister(it) },
                expression.resultType
            )
        )
        return reg ?: getReg(expression.resultType)
    }
}

class ComparisonRegReg : Rule() {
    override val expression: Expression
        get() = Expression.Comparison(Expression.Register(), Token("", TokenType.COMPARATOR), Expression.Register())
    override val resultSymbol: Char
        get() = 'r'

    override fun getCost(expression: Expression, astGetSymbol: ASTGetSymbol, rules: List<Rule>): Int {
        return 1
    }

    override fun constructString(expression: Expression, emitter: ASMEmitter, trie: ExpressionMatchingTrie): Expression {
        expression as Expression.Comparison
        val tempReg1 = TemporaryRegister((expression.leftExpression as Expression.Register).index)
        val tempReg2 = TemporaryRegister((expression.rightExpression as Expression.Register).index)

        emitter.emit(IRValue(IRType.CMP, tempReg1, tempReg2, expression.resultType))
        return expression.leftExpression
    }
}

private fun calculateSubCosts(ruleExpression: Expression, actualExpression: Expression, astGetSymbol: ASTGetSymbol, rules: List<Rule>): Int {
    // Expects rule and actual to have the same arity
    val ruleChildren = Utils.getChildren(ruleExpression)
    val actualChildren = Utils.getChildren(actualExpression)

    var cost = 0
    for (i in ruleChildren.indices) {
        cost += if (ruleChildren[i]::class != actualChildren[i]::class) {
            // Update new child
            rules[actualChildren[i].match[astGetSymbol.evaluate(ruleChildren[i])] ?: TODO()].getCost(actualChildren[i], astGetSymbol, rules)
        } else {
            calculateSubCosts(ruleChildren[i], actualChildren[i], astGetSymbol, rules)
        }
    }
    return cost
}


var currentRegister = 0


fun getRules(): List<Rule> {
    val result = mutableListOf<Rule>()

    result.add(PlusOperationRegReg())
    result.add(PlusOperationRegLit())
    result.add(PlusOperationLitReg())

    result.add(MulOperationLitReg())
    result.add(MulOperationRegLit())
    result.add(MulOperationRegReg())

    result.add(SubOperationRegLit())
    result.add(SubOperationRegReg())

    result.add(DivOperationRegReg())

    result.add(MovOffsetToOffset())

    result.add(ValueFromPointer())
    result.add(MovRegToReg())
    result.add(MovPointerRegToReg())
    result.add(ExtendTo64BitsRule())
    result.add(FramePointerRule())
    result.add(MovOffsetToReg())
    result.add(MovFromFPOffset())
    result.add(ComparisonRegReg())
    result.add(MovFromRegOffset())
    result.add(MovFromRegOffsetWithAdder())

    result.add(NegateRule())
    result.add(MovOffsetToValueFromPointerOffset())

    result.add(PointerGet())

    result.add(CallRule())

    result.add(LiteralToReg())
    return result
}

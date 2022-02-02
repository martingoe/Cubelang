package com.cubearrow.cubelang

import com.cubearrow.cubelang.common.Expression
import com.cubearrow.cubelang.common.NormalType
import com.cubearrow.cubelang.common.NormalTypes
import com.cubearrow.cubelang.common.Type
import com.cubearrow.cubelang.common.nasm_rules.ASMEmitter
import com.cubearrow.cubelang.common.tokens.Token
import com.cubearrow.cubelang.common.tokens.TokenType

abstract class Rule {
    abstract val expression: Expression
    abstract val resultSymbol: Char

    abstract fun getCost(expression: Expression, astGetSymbol: ASTGetSymbol, rules: List<Rule>): Int
    abstract fun constructString(expression: Expression, emitter: ASMEmitter, trie: Trie): Expression

    companion object {
        const val RULE_COUNT = 19
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


    override fun constructString(expression: Expression, emitter: ASMEmitter, trie: Trie): Expression {
        val castExpression = expression as Expression.Operation
        emitter.emit("add ${castExpression.leftExpression}, ${castExpression.rightExpression}")
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

    override fun constructString(expression: Expression, emitter: ASMEmitter, trie: Trie): Expression {
        val castExpression = expression as Expression.Operation
        if ((castExpression.rightExpression as Expression.Literal).value == 1)
            emitter.emit("inc ${castExpression.leftExpression}")
        else {
            emitter.emit("add ${castExpression.leftExpression}, ${castExpression.rightExpression}")
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

    override fun constructString(expression: Expression, emitter: ASMEmitter, trie: Trie): Expression {
        val castExpression = expression as Expression.Operation
        if ((castExpression.leftExpression as Expression.Literal).value == 1)
            emitter.emit("inc ${castExpression.rightExpression}")
        else {
            emitter.emit("add ${castExpression.rightExpression}, ${castExpression.leftExpression}")
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

    override fun constructString(expression: Expression, emitter: ASMEmitter, trie: Trie): Expression {
        val castExpression = expression as Expression.Operation
        if ((castExpression.rightExpression as Expression.Literal).value == 1)
            emitter.emit("dec ${castExpression.leftExpression}")
        else {
            emitter.emit("sub ${castExpression.leftExpression}, ${castExpression.rightExpression}")
        }
        return castExpression.leftExpression
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

    override fun constructString(expression: Expression, emitter: ASMEmitter, trie: Trie): Expression {
        val castExpression = expression as Expression.Operation
        emitter.emit("sub ${castExpression.rightExpression}, ${castExpression.leftExpression}")
        return castExpression.leftExpression
    }
}


class DivOperationRegLit : Rule() {
    override val expression: Expression
        get() = Expression.Operation(Expression.Register(), Token("/", TokenType.SLASH), Expression.Literal(null))
    override val resultSymbol: Char
        get() = 'r'

    override fun getCost(expression: Expression, astGetSymbol: ASTGetSymbol, rules: List<Rule>): Int {
        return 3 + calculateSubCosts(this.expression, expression, astGetSymbol, rules)
    }

    override fun constructString(expression: Expression, emitter: ASMEmitter, trie: Trie): Expression {
        val castExpression = expression as Expression.Operation
        if ((castExpression.rightExpression as Expression.Literal).value == 1)
            return castExpression.leftExpression
        else {
            emitter.emit("idiv ${castExpression.leftExpression}, ${castExpression.rightExpression}")
        }
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

    override fun constructString(expression: Expression, emitter: ASMEmitter, trie: Trie): Expression {
        val castExpression = expression as Expression.Operation
        emitter.emit("idiv ${castExpression.rightExpression}, ${castExpression.leftExpression}")
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

    override fun constructString(expression: Expression, emitter: ASMEmitter, trie: Trie): Expression {
        val castExpression = expression as Expression.Operation
        emitter.emit("imul ${castExpression.rightExpression}, ${castExpression.leftExpression}")
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

    override fun constructString(expression: Expression, emitter: ASMEmitter, trie: Trie): Expression {
        val castExpression = expression as Expression.Operation
        val left = castExpression.leftExpression
        val right = castExpression.rightExpression

        return emitMulRegLiteral(right, left, emitter)
    }
}

private fun emitMulRegLiteral(
    right: Expression,
    left: Expression,
    emitter: ASMEmitter
): Expression {
    when (val literalValue = getLiteralValue((right as Expression.Literal).value)) {
        1 -> return left
        2 -> {
            emitter.emit("add ${left}, $left")
        }
        4 -> {
            emitter.emit("sal ${left}, 2")
        }
        8 -> {
            emitter.emit("sal ${left}, 3")
        }
        else -> emitter.emit("imul ${left}, $literalValue")
    }
    return left
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

    override fun constructString(expression: Expression, emitter: ASMEmitter, trie: Trie): Expression {
        val castExpression = expression as Expression.Operation
        val right = castExpression.leftExpression
        val left = castExpression.rightExpression
        return emitMulRegLiteral(right, left, emitter)
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

    override fun constructString(expression: Expression, emitter: ASMEmitter, trie: Trie): Expression {
        val reg = getReg(expression.resultType, trie)
        emitter.emit("mov ${reg}, ${(expression as Expression.Literal).value}")
        return reg
    }


}

class MovRegToReg : Rule() {
    override val expression: Expression
        get() = Expression.Assignment(Expression.Register(), Expression.Register())
    override val resultSymbol: Char
        get() = 'r'

    override fun getCost(expression: Expression, astGetSymbol: ASTGetSymbol, rules: List<Rule>): Int {
        return 1
    }

    override fun constructString(expression: Expression, emitter: ASMEmitter, trie: Trie): Expression {
        val castExpression = expression as Expression.Assignment
        val valueRegister = (castExpression.valueExpression) as Expression.Register
        emitter.emit("mov ${castExpression.leftSide}, $valueRegister")

        return castExpression.leftSide
    }


}

class MovPointerRegToReg : Rule() {
    override val expression: Expression
        get() = Expression.Assignment(Expression.ValueFromPointer(Expression.Register()), Expression.Register())
    override val resultSymbol: Char
        get() = 'r'

    override fun getCost(expression: Expression, astGetSymbol: ASTGetSymbol, rules: List<Rule>): Int {
        return 2 + calculateSubCosts(this.expression, expression, astGetSymbol, rules)
    }

    override fun constructString(expression: Expression, emitter: ASMEmitter, trie: Trie): Expression {
        val castExpression = expression as Expression.Assignment
        val valueRegister = (castExpression.valueExpression) as Expression.Register
        val valueFromPointer = castExpression.leftSide as Expression.ValueFromPointer
        emitter.emit("mov ${getASMPointer(castExpression.resultType.getLength())} [${valueFromPointer.expression}], $valueRegister")

        return valueFromPointer.expression
    }
}

class MovOffsetToReg : Rule() {
    override val expression: Expression
        get() = Expression.Assignment(Expression.ValueFromPointer(Expression.Operation(Expression.FramePointer(), Token("-", TokenType.PLUSMINUS), Expression.Literal(null))), Expression.Register())
    override val resultSymbol: Char
        get() = 'r'

    override fun getCost(expression: Expression, astGetSymbol: ASTGetSymbol, rules: List<Rule>): Int {
        return 2
    }

    override fun constructString(expression: Expression, emitter: ASMEmitter, trie: Trie): Expression {
        val castExpression = expression as Expression.Assignment
        val valueRegister = (castExpression.valueExpression) as Expression.Register
        val valueFromPointer = castExpression.leftSide as Expression.ValueFromPointer
        val operation = valueFromPointer.expression as Expression.Operation
        emitter.emit("mov ${getASMPointer(castExpression.resultType.getLength())} [${operation.leftExpression} - ${operation.rightExpression}], $valueRegister")

        return valueFromPointer.expression
    }
}

class MovFromOffset : Rule() {
    override val expression: Expression
        get() = Expression.ValueFromPointer(Expression.Operation(Expression.FramePointer(), Token("-", TokenType.PLUSMINUS), Expression.Literal(null)))
    override val resultSymbol: Char
        get() = 'r'

    override fun getCost(expression: Expression, astGetSymbol: ASTGetSymbol, rules: List<Rule>): Int {
        return 2
    }

    override fun constructString(expression: Expression, emitter: ASMEmitter, trie: Trie): Expression {
        val reg = getReg(expression.resultType, trie)
        val castExpression = expression as Expression.ValueFromPointer
        val operation = castExpression.expression as Expression.Operation
        emitter.emit("mov $reg, ${getASMPointer(castExpression.resultType.getLength())} [${operation.leftExpression} - ${operation.rightExpression}]")

        return reg
    }
}

fun getReg(type: Type, trie: Trie): Expression {
    return Expression.Register(currentRegister++, type, trie.isArgument)
}

class ValueFromPointer : Rule() {
    override val expression: Expression
        get() = Expression.ValueFromPointer(Expression.Register())
    override val resultSymbol: Char
        get() = 'r'

    override fun getCost(expression: Expression, astGetSymbol: ASTGetSymbol, rules: List<Rule>): Int {
        return 2 + calculateSubCosts(this.expression, expression, astGetSymbol, rules)
    }

    override fun constructString(expression: Expression, emitter: ASMEmitter, trie: Trie): Expression {
        val castExpression = expression as Expression.ValueFromPointer
        val valueRegister = castExpression.expression as Expression.Register
        val reg = getReg(valueRegister.type, trie)
        emitter.emit("mov $reg, ${getASMPointer(valueRegister.type.getLength())} [${valueRegister}]")

        return reg
    }
}
class FramePointer : Rule(){
    override val expression: Expression
        get() = Expression.FramePointer()
    override val resultSymbol: Char
        get() = 'r'

    override fun getCost(expression: Expression, astGetSymbol: ASTGetSymbol, rules: List<Rule>): Int {
        return 1
    }

    override fun constructString(expression: Expression, emitter: ASMEmitter, trie: Trie): Expression {
        return getReg(NormalType(NormalTypes.I64), trie)
    }
}

class CallRule : Rule(){
    override val expression: Expression
        get() = Expression.Call(Expression.VarCall(Token("", TokenType.IDENTIFIER)), listOf())
    override val resultSymbol: Char
        get() = 'r'

    override fun getCost(expression: Expression, astGetSymbol: ASTGetSymbol, rules: List<Rule>): Int {
        return 1
    }

    override fun constructString(expression: Expression, emitter: ASMEmitter, trie: Trie): Expression {
        expression as Expression.Call
        for(arg in expression.arguments){
            trie.emitCodeForCallExpression(arg)
        }
        emitter.emit("call ${expression.callee.varName.substring}")
        return Expression.Register(0, expression.resultType, trie.isArgument)
    }
}

class ComparisonRegReg : Rule(){
    override val expression: Expression
        get() = Expression.Comparison(Expression.Register(), Token("", TokenType.COMPARATOR), Expression.Register())
    override val resultSymbol: Char
        get() = 'r'

    override fun getCost(expression: Expression, astGetSymbol: ASTGetSymbol, rules: List<Rule>): Int {
        return 1
    }

    override fun constructString(expression: Expression, emitter: ASMEmitter, trie: Trie): Expression {
        expression as Expression.Comparison
        emitter.emit("cmp ${expression.leftExpression}, ${expression.rightExpression}")
        return expression.leftExpression
    }
}

private fun getASMPointer(length: Int): String {
    return when (length) {
        1 -> "BYTE"
        2 -> "WORD"
        4 -> "DWORD"
        8 -> "QWORD"
        else -> TODO()
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

    result.add(DivOperationRegLit())
    result.add(DivOperationRegReg())

    result.add(ValueFromPointer())
    result.add(MovRegToReg())
    result.add(MovPointerRegToReg())
    result.add(FramePointer())
    result.add(MovOffsetToReg())
    result.add(MovFromOffset())
    result.add(ComparisonRegReg())

    result.add(CallRule())

    result.add(LiteralToReg())
    return result
}

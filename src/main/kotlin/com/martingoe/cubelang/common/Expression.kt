package com.martingoe.cubelang.common

import com.martingoe.cubelang.backend.instructionselection.Rule
import com.martingoe.cubelang.common.tokens.Token


/**
 * The expressions of a given program.
 *
 * @param state The trie state reached from the expression. This parameter is used in the instruction selection stage.
 * @param matchedResults The results of rules that have been matched. This parameter is used in the instruction selection stage.
 * @param cost The lowest rule cost that has been matched yet. This parameter is used in the instruction selection stage.
 * @param ruleMatchingBytes An array of integers used to match the given rules. Used in the instruction selection stage.
 * @param resultType The type resulting from the expression.
 */
abstract class Expression(
    var state: Int = 0,
    var matchedResults: MutableMap<Char, Int> = HashMap(),
    var cost: MutableMap<Char, Int> = HashMap(),
    var ruleMatchingBytes: Array<Int> = Array(Rule.RULE_COUNT) { 0 },
    var resultType: Type = NoneType()
) {
    class Operation(var leftExpression: Expression, val operator: Token, var rightExpression: Expression) : Expression() {
        override fun <R> accept(visitor: ExpressionVisitor<R>): R {
            return visitor.visitOperation(this)
        }
    }

    class Call(val callee: VarCall, var arguments: List<Expression>, val bracket: Token) : Expression() {
        override fun <R> accept(visitor: ExpressionVisitor<R>): R {
            return visitor.visitCall(this)
        }
    }

    class Literal(var value: Any?, val token: Token? = null) : Expression() {
        override fun <R> accept(visitor: ExpressionVisitor<R>): R {
            return visitor.visitLiteral(this)
        }

        override fun toString(): String {
            return "$value"
        }

    }

    class VarCall(val varName: Token) : Expression() {
        override fun <R> accept(visitor: ExpressionVisitor<R>): R {
            return visitor.visitVarCall(this)
        }
    }

    class InstanceGet(val expression: Expression, val identifier: Token) : Expression() {
        override fun <R> accept(visitor: ExpressionVisitor<R>): R {
            return visitor.visitInstanceGet(this)
        }
    }

    class Unary(val identifier: Token, var expression: Expression) : Expression() {
        override fun <R> accept(visitor: ExpressionVisitor<R>): R {
            return visitor.visitUnary(this)
        }
    }

    class Grouping(var expression: Expression) : Expression() {
        override fun <R> accept(visitor: ExpressionVisitor<R>): R {
            return visitor.visitGrouping(this)
        }
    }

    class ArrayGet(val expression: Expression, val inBrackets: Expression, val bracket: Token) : Expression() {
        override fun <R> accept(visitor: ExpressionVisitor<R>): R {
            return visitor.visitArrayGet(this)
        }
    }

    class PointerGet(var expression: Expression) : Expression() {
        override fun <R> accept(visitor: ExpressionVisitor<R>): R {
            return visitor.visitPointerGet(this)
        }
    }

    class ValueFromPointer(var expression: Expression, val star: Token) : Expression() {
        override fun <R> accept(visitor: ExpressionVisitor<R>): R {
            return visitor.visitValueFromPointer(this)
        }
    }

    class Comparison(var leftExpression: Expression, val comparator: Token, var rightExpression: Expression) : Expression() {
        override fun <R> accept(visitor: ExpressionVisitor<R>): R {
            return visitor.visitComparison(this)
        }
    }


    class Assignment(var leftSide: Expression, var valueExpression: Expression, val equals: Token) : Expression() {
        override fun <R> accept(visitor: ExpressionVisitor<R>): R {
            return visitor.visitAssignment(this)
        }
    }

    interface ExpressionVisitor<T> {
        fun visitValueFromPointer(valueFromPointer: ValueFromPointer): T
        fun visitLogical(logical: Logical): T
        fun visitPointerGet(pointerGet: PointerGet): T
        fun visitArrayGet(arrayGet: ArrayGet): T
        fun visitGrouping(grouping: Grouping): T
        fun visitUnary(unary: Unary): T
        fun visitInstanceGet(instanceGet: InstanceGet): T
        fun visitVarCall(varCall: VarCall): T
        fun visitLiteral(literal: Literal): T
        fun visitCall(call: Call): T
        fun visitOperation(operation: Operation): T
        fun visitComparison(comparison: Comparison): T
        fun visitRegister(register: Register): T
        fun visitAssignment(assignment: Assignment): T
        fun acceptFramePointer(framePointer: FramePointer): T
        fun acceptExtendTo64Bits(extendTo64Bit: ExtendTo64Bit): T
    }

    class Logical(var leftExpression: Expression, val logical: Token, var rightExpression: Expression) : Expression() {
        override fun <R> accept(visitor: ExpressionVisitor<R>): R {
            return visitor.visitLogical(this)
        }
    }

    class FramePointer : Expression() {
        override fun <R> accept(visitor: ExpressionVisitor<R>): R {
            return visitor.acceptFramePointer(this)
        }

        override fun toString(): String {
            return "rbp"
        }

    }

    class Register(var index: Int = -1, var type: Type = NoneType()) : Expression() {
        override fun <R> accept(visitor: ExpressionVisitor<R>): R {
            return visitor.visitRegister(this)
        }

        override fun toString(): String {
            return "r${index}"
        }


    }

    class ExtendTo64Bit(var expression: Expression) : Expression() {
        override fun <R> accept(visitor: ExpressionVisitor<R>): R {
            return visitor.acceptExtendTo64Bits(this)
        }

    }

    abstract fun <R> accept(visitor: ExpressionVisitor<R>): R
}
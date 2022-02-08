package com.cubearrow.cubelang.common

import com.cubearrow.cubelang.common.tokens.Token


abstract class Expression(
    var state: Int = 0,
    var match: MutableMap<Char, Int> = HashMap(),
    var cost: MutableMap<Char, Int> = HashMap(),
    var b: Array<Int> = Array(20) { 0 },
    var resultType: Type = NoneType()
) {


    class Operation(var leftExpression: Expression, val operator: Token, var rightExpression: Expression) : Expression() {
        override fun <R> accept(visitor: ExpressionVisitor<R>): R {
            return visitor.visitOperation(this)
        }
    }

    class Call(val callee: VarCall, var arguments: List<Expression>) : Expression() {
        override fun <R> accept(visitor: ExpressionVisitor<R>): R {
            return visitor.visitCall(this)
        }
    }

    class Literal(val value: Any?) : Expression() {
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

    class ArrayGet(val expression: Expression, val inBrackets: Expression) : Expression() {
        override fun <R> accept(visitor: ExpressionVisitor<R>): R {
            return visitor.visitArrayGet(this)
        }
    }

    class PointerGet(val varCall: VarCall) : Expression() {
        override fun <R> accept(visitor: ExpressionVisitor<R>): R {
            return visitor.visitPointerGet(this)
        }
    }

    class ValueFromPointer(var expression: Expression) : Expression() {
        override fun <R> accept(visitor: ExpressionVisitor<R>): R {
            return visitor.visitValueFromPointer(this)
        }
    }

    class Comparison(var leftExpression: Expression, val comparator: Token, var rightExpression: Expression) : Expression() {
        override fun <R> accept(visitor: ExpressionVisitor<R>): R {
            return visitor.visitComparison(this)
        }
    }

    class ValueToPointer(var pointer: Expression, var value: Expression) : Expression() {
        override fun <R> accept(visitor: ExpressionVisitor<R>): R {
            return visitor.visitValueToPointer(this)
        }
    }

    class Assignment(var leftSide: Expression, var valueExpression: Expression) : Expression() {
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
        fun visitValueToPointer(valueToPointer: ValueToPointer): T
        fun visitRegister(register: Register): T
        fun visitAssignment(assignment: Assignment): T
        fun acceptFramePointer(framePointer: FramePointer): T
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

    class Register(var index: Int = -1, var type: Type = NoneType(), val argument: Boolean = false) : Expression() {
        override fun <R> accept(visitor: ExpressionVisitor<R>): R {
            return visitor.visitRegister(this)
        }

        override fun toString(): String {
            return "${if (!argument) "r" else "R"}${index}"
        }

    }

    abstract fun <R> accept(visitor: ExpressionVisitor<R>): R
}
package com.cubearrow.cubelang.parsing.syntax

import com.cubearrow.cubelang.parsing.tokenization.Token

/**
 * This class is generated automatically by the [ASTGenerator]
 **/
abstract class Expression {
   class Assignment (var identifier1: Token, var equals1: Token, var expression1: Expression) : Expression() {
        override fun <R> accept(visitor: ExpressionVisitor<R>): R {
            return visitor.visitAssignment(this)
        }
    }
   class Operation (var expression1: Expression, var expression2: Expression) : Expression() {
        override fun <R> accept(visitor: ExpressionVisitor<R>): R {
            return visitor.visitOperation(this)
        }
    }
   class Call (var identifier1: Token, var brcktl1: Token, var expression1: Expression, var brcktr1: Token) : Expression() {
        override fun <R> accept(visitor: ExpressionVisitor<R>): R {
            return visitor.visitCall(this)
        }
    }
   class Literal (var number1: Token) : Expression() {
        override fun <R> accept(visitor: ExpressionVisitor<R>): R {
            return visitor.visitLiteral(this)
        }
    }
    interface ExpressionVisitor<R> {
        fun visitAssignment(assignment: Assignment): R
        fun visitOperation(operation: Operation): R
        fun visitCall(call: Call): R
        fun visitLiteral(literal: Literal): R
    }
    abstract fun <R> accept(visitor: ExpressionVisitor<R>): R
}

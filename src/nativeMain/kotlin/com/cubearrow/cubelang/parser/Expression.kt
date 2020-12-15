package com.cubearrow.cubelang.parser

import com.cubearrow.cubelang.lexer.Token

/**
 * This class is generated automatically by the [ASTGenerator]
 **/
abstract class Expression {

    class Assignment (var identifier1: Token, var expression1: Expression) : Expression() {
        override fun <R> accept(visitor: ExpressionVisitor<R>): R {
            return visitor.visitAssignment(this)
        }
    }

    class VarInitialization (var identifier1: Token, var identifierNull1: Token?, var expressionNull1: Expression?) : Expression() {
        override fun <R> accept(visitor: ExpressionVisitor<R>): R {
            return visitor.visitVarInitialization(this)
        }
    }

    class Operation (var expression1: Expression, var operator1: Token, var expression2: Expression) : Expression() {
        override fun <R> accept(visitor: ExpressionVisitor<R>): R {
            return visitor.visitOperation(this)
        }
    }

    class Call (var expression1: Expression, var expressionLst1: List<Expression>) : Expression() {
        override fun <R> accept(visitor: ExpressionVisitor<R>): R {
            return visitor.visitCall(this)
        }
    }

    class Literal (var any1: Any?) : Expression() {
        override fun <R> accept(visitor: ExpressionVisitor<R>): R {
            return visitor.visitLiteral(this)
        }
    }

    class VarCall (var identifier1: Token) : Expression() {
        override fun <R> accept(visitor: ExpressionVisitor<R>): R {
            return visitor.visitVarCall(this)
        }
    }

    class FunctionDefinition (var identifier1: Token, var expressionLst1: List<Expression>, var identifierNull1: Token?, var expression1: Expression) : Expression() {
        override fun <R> accept(visitor: ExpressionVisitor<R>): R {
            return visitor.visitFunctionDefinition(this)
        }
    }

    class Comparison (var expression1: Expression, var comparator1: Token, var expression2: Expression) : Expression() {
        override fun <R> accept(visitor: ExpressionVisitor<R>): R {
            return visitor.visitComparison(this)
        }
    }

    class IfStmnt (var expression1: Expression, var expression2: Expression, var expressionNull1: Expression?) : Expression() {
        override fun <R> accept(visitor: ExpressionVisitor<R>): R {
            return visitor.visitIfStmnt(this)
        }
    }

    class ReturnStmnt (var expressionNull1: Expression?) : Expression() {
        override fun <R> accept(visitor: ExpressionVisitor<R>): R {
            return visitor.visitReturnStmnt(this)
        }
    }

    class WhileStmnt (var expression1: Expression, var expression2: Expression) : Expression() {
        override fun <R> accept(visitor: ExpressionVisitor<R>): R {
            return visitor.visitWhileStmnt(this)
        }
    }

    class ForStmnt (var expressionLst1: List<Expression>, var expression1: Expression) : Expression() {
        override fun <R> accept(visitor: ExpressionVisitor<R>): R {
            return visitor.visitForStmnt(this)
        }
    }

    class ClassDefinition (var identifier1: Token, var identifierNull1: Token?, var expressionLst1: List<Expression>) : Expression() {
        override fun <R> accept(visitor: ExpressionVisitor<R>): R {
            return visitor.visitClassDefinition(this)
        }
    }

    class InstanceGet (var expression1: Expression, var identifier1: Token) : Expression() {
        override fun <R> accept(visitor: ExpressionVisitor<R>): R {
            return visitor.visitInstanceGet(this)
        }
    }

    class InstanceSet (var expression1: Expression, var identifier1: Token, var expression2: Expression) : Expression() {
        override fun <R> accept(visitor: ExpressionVisitor<R>): R {
            return visitor.visitInstanceSet(this)
        }
    }

    class ArgumentDefinition (var identifier1: Token, var identifier2: Token) : Expression() {
        override fun <R> accept(visitor: ExpressionVisitor<R>): R {
            return visitor.visitArgumentDefinition(this)
        }
    }

    class BlockStatement (var expressionLst1: List<Expression>) : Expression() {
        override fun <R> accept(visitor: ExpressionVisitor<R>): R {
            return visitor.visitBlockStatement(this)
        }
    }

    class Logical (var expression1: Expression, var identifier1: Token, var expression2: Expression) : Expression() {
        override fun <R> accept(visitor: ExpressionVisitor<R>): R {
            return visitor.visitLogical(this)
        }
    }

    class Unary (var identifier1: Token, var expression1: Expression) : Expression() {
        override fun <R> accept(visitor: ExpressionVisitor<R>): R {
            return visitor.visitUnary(this)
        }
    }

    class Grouping (var expression1: Expression) : Expression() {
        override fun <R> accept(visitor: ExpressionVisitor<R>): R {
            return visitor.visitGrouping(this)
        }
    }

    class Empty (var identifierNull1: Token?) : Expression() {
        override fun <R> accept(visitor: ExpressionVisitor<R>): R {
            return visitor.visitEmpty(this)
        }
    }
    interface ExpressionVisitor<R> {
        fun visitAssignment(assignment: Assignment): R
        fun visitVarInitialization(varInitialization: VarInitialization): R
        fun visitOperation(operation: Operation): R
        fun visitCall(call: Call): R
        fun visitLiteral(literal: Literal): R
        fun visitVarCall(varCall: VarCall): R
        fun visitFunctionDefinition(functionDefinition: FunctionDefinition): R
        fun visitComparison(comparison: Comparison): R
        fun visitIfStmnt(ifStmnt: IfStmnt): R
        fun visitReturnStmnt(returnStmnt: ReturnStmnt): R
        fun visitWhileStmnt(whileStmnt: WhileStmnt): R
        fun visitForStmnt(forStmnt: ForStmnt): R
        fun visitClassDefinition(classDefinition: ClassDefinition): R
        fun visitInstanceGet(instanceGet: InstanceGet): R
        fun visitInstanceSet(instanceSet: InstanceSet): R
        fun visitArgumentDefinition(argumentDefinition: ArgumentDefinition): R
        fun visitBlockStatement(blockStatement: BlockStatement): R
        fun visitLogical(logical: Logical): R
        fun visitUnary(unary: Unary): R
        fun visitGrouping(grouping: Grouping): R
        fun visitEmpty(empty: Empty): R
    }
    abstract fun <R> accept(visitor: ExpressionVisitor<R>): R
}

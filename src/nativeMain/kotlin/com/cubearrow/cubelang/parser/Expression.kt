package com.cubearrow.cubelang.parser


import com.cubearrow.cubelang.utils.Type
import com.cubearrow.cubelang.lexer.Token

/**
 * This class is generated automatically by the [ASTGenerator]
 **/
abstract class Expression {

    class Assignment (val name: Token, val valueExpression: Expression) : Expression() {
        override fun <R> accept(visitor: ExpressionVisitor<R>): R {
            return visitor.visitAssignment(this)
        }
    }

    class VarInitialization (val name: Token, val type: Type?, val valueExpression: Expression?) : Expression() {
        override fun <R> accept(visitor: ExpressionVisitor<R>): R {
            return visitor.visitVarInitialization(this)
        }
    }

    class Operation (val leftExpression: Expression, val operator: Token, val rightExpression: Expression) : Expression() {
        override fun <R> accept(visitor: ExpressionVisitor<R>): R {
            return visitor.visitOperation(this)
        }
    }

    class Call (val callee: Expression, val arguments: List<Expression>) : Expression() {
        override fun <R> accept(visitor: ExpressionVisitor<R>): R {
            return visitor.visitCall(this)
        }
    }

    class Literal (val value: Any?) : Expression() {
        override fun <R> accept(visitor: ExpressionVisitor<R>): R {
            return visitor.visitLiteral(this)
        }
    }

    class VarCall (val varName: Token) : Expression() {
        override fun <R> accept(visitor: ExpressionVisitor<R>): R {
            return visitor.visitVarCall(this)
        }
    }

    class FunctionDefinition (val name: Token, val args: List<Expression>, val type: Type?, val body: Expression) : Expression() {
        override fun <R> accept(visitor: ExpressionVisitor<R>): R {
            return visitor.visitFunctionDefinition(this)
        }
    }

    class Comparison (val leftExpression: Expression, val comparator: Token, val rightExpression: Expression) : Expression() {
        override fun <R> accept(visitor: ExpressionVisitor<R>): R {
            return visitor.visitComparison(this)
        }
    }

    class IfStmnt (val condition: Expression, val ifBody: Expression, val elseBody: Expression?) : Expression() {
        override fun <R> accept(visitor: ExpressionVisitor<R>): R {
            return visitor.visitIfStmnt(this)
        }
    }

    class ReturnStmnt (val returnValue: Expression?) : Expression() {
        override fun <R> accept(visitor: ExpressionVisitor<R>): R {
            return visitor.visitReturnStmnt(this)
        }
    }

    class WhileStmnt (val condition: Expression, val body: Expression) : Expression() {
        override fun <R> accept(visitor: ExpressionVisitor<R>): R {
            return visitor.visitWhileStmnt(this)
        }
    }

    class ForStmnt (val inBrackets: List<Expression>, val body: Expression) : Expression() {
        override fun <R> accept(visitor: ExpressionVisitor<R>): R {
            return visitor.visitForStmnt(this)
        }
    }

    class ClassDefinition (val name: Token, val type: Type?, val body: List<Expression>) : Expression() {
        override fun <R> accept(visitor: ExpressionVisitor<R>): R {
            return visitor.visitClassDefinition(this)
        }
    }

    class InstanceGet (val expression: Expression, val identifier: Token) : Expression() {
        override fun <R> accept(visitor: ExpressionVisitor<R>): R {
            return visitor.visitInstanceGet(this)
        }
    }

    class InstanceSet (val expression: Expression, val identifier: Token, val value: Expression) : Expression() {
        override fun <R> accept(visitor: ExpressionVisitor<R>): R {
            return visitor.visitInstanceSet(this)
        }
    }

    class ArgumentDefinition (val name: Token, val type: Type) : Expression() {
        override fun <R> accept(visitor: ExpressionVisitor<R>): R {
            return visitor.visitArgumentDefinition(this)
        }
    }

    class BlockStatement (val statements: List<Expression>) : Expression() {
        override fun <R> accept(visitor: ExpressionVisitor<R>): R {
            return visitor.visitBlockStatement(this)
        }
    }

    class Logical (val leftExpression: Expression, val logical: Token, val rightExpression: Expression) : Expression() {
        override fun <R> accept(visitor: ExpressionVisitor<R>): R {
            return visitor.visitLogical(this)
        }
    }

    class Unary (val identifier: Token, val expression: Expression) : Expression() {
        override fun <R> accept(visitor: ExpressionVisitor<R>): R {
            return visitor.visitUnary(this)
        }
    }

    class Grouping (val expression: Expression) : Expression() {
        override fun <R> accept(visitor: ExpressionVisitor<R>): R {
            return visitor.visitGrouping(this)
        }
    }

    class ArrayGet (val expression: Expression, val inBrackets: Expression) : Expression() {
        override fun <R> accept(visitor: ExpressionVisitor<R>): R {
            return visitor.visitArrayGet(this)
        }
    }

    class ArraySet (val arrayGet: ArrayGet, val value: Expression) : Expression() {
        override fun <R> accept(visitor: ExpressionVisitor<R>): R {
            return visitor.visitArraySet(this)
        }
    }

    class Empty (val any: Any?) : Expression() {
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
        fun visitArrayGet(arrayGet: ArrayGet): R
        fun visitArraySet(arraySet: ArraySet): R
        fun visitEmpty(empty: Empty): R
    }
    abstract fun <R> accept(visitor: ExpressionVisitor<R>): R
}


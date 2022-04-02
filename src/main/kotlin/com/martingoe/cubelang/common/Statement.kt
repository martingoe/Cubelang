package com.martingoe.cubelang.common


import com.martingoe.cubelang.common.tokens.Token

abstract class Statement {


    class VarInitialization (val name: Token, var type: Type, var valueExpression: Expression?) : Statement() {
        override fun <R> accept(visitor: StatementVisitor<R>): R {
            return visitor.visitVarInitialization(this)
        }
    }

    class FunctionDefinition (val name: Token, val args: List<Statement>, val type: Type, var body: Statement) : Statement() {
        override fun <R> accept(visitor: StatementVisitor<R>): R {
            return visitor.visitFunctionDefinition(this)
        }
    }


    class IfStmnt (var condition: Expression, var ifBody: Statement, var elseBody: Statement?) : Statement() {
        override fun <R> accept(visitor: StatementVisitor<R>): R {
            return visitor.visitIfStmnt(this)
        }
    }

    class ReturnStmnt (var returnValue: Expression?) : Statement() {
        override fun <R> accept(visitor: StatementVisitor<R>): R {
            return visitor.visitReturnStmnt(this)
        }
    }

    class WhileStmnt (var condition: Expression, var body: Statement) : Statement() {
        override fun <R> accept(visitor: StatementVisitor<R>): R {
            return visitor.visitWhileStmnt(this)
        }
    }

    class ForStmnt (var inBrackets: List<Statement>, var body: Statement) : Statement() {
        override fun <R> accept(visitor: StatementVisitor<R>): R {
            return visitor.visitForStmnt(this)
        }
    }

    class StructDefinition (val name: Token, val body: List<VarInitialization>) : Statement() {
        override fun <R> accept(visitor: StatementVisitor<R>): R {
            return visitor.visitStructDefinition(this)
        }
    }


    class ArgumentDefinition (val name: Token, val type: Type) : Statement() {
        override fun <R> accept(visitor: StatementVisitor<R>): R {
            return visitor.visitArgumentDefinition(this)
        }
    }

    class BlockStatement (var statements: List<Statement>) : Statement() {
        override fun <R> accept(visitor: StatementVisitor<R>): R {
            return visitor.visitBlockStatement(this)
        }
    }


    class ImportStmnt (val identifier: Token) : Statement() {
        override fun <R> accept(visitor: StatementVisitor<R>): R {
            return visitor.visitImportStmnt(this)
        }
    }


    class Empty : Statement() {
        override fun <R> accept(visitor: StatementVisitor<R>): R {
            return visitor.visitEmpty(this)
        }
    }
    class ExpressionStatement(var expression: Expression): Statement(){
        override fun <R> accept(visitor: StatementVisitor<R>): R {
            return visitor.visitExpressionStatement(this)
        }

    }


    interface StatementVisitor<R> {
        fun visitVarInitialization(varInitialization: VarInitialization): R
        fun visitFunctionDefinition(functionDefinition: FunctionDefinition): R
        fun visitIfStmnt(ifStmnt: IfStmnt): R
        fun visitReturnStmnt(returnStmnt: ReturnStmnt): R
        fun visitWhileStmnt(whileStmnt: WhileStmnt): R
        fun visitForStmnt(forStmnt: ForStmnt): R
        fun visitStructDefinition(structDefinition: StructDefinition): R
        fun visitArgumentDefinition(argumentDefinition: ArgumentDefinition): R
        fun visitBlockStatement(blockStatement: BlockStatement): R
        fun visitImportStmnt(importStmnt: ImportStmnt): R
        fun visitEmpty(empty: Empty): R
        fun visitExpressionStatement(expressionStatement: ExpressionStatement): R
    }
    abstract fun <R> accept(visitor: StatementVisitor<R>): R
}

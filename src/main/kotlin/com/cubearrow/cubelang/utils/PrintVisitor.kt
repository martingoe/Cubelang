package com.cubearrow.cubelang.utils

import com.cubearrow.cubelang.parser.Expression

/**
 * Returns the Expression in a [String] form in non-pretty json format
 */
class PrintVisitor : Expression.ExpressionVisitor<String> {
    override fun visitAssignment(assignment: Expression.Assignment): String {
        return """Assignment: {
    "VariableName": "${assignment.identifier1.substring}",
    "ValueExpression": {
        ${assignment.expression1.accept(this)}
    }
}        """
    }

    override fun visitOperation(operation: Expression.Operation): String {
        return """Operation: {
    "LeftSide": {
        ${operation.expression1.accept(this)}
    },
    "Operator": "${operation.operator1.substring}",
    "RightSide": {
        ${operation.expression2.accept(this)}
    }
}        """
    }

    override fun visitCall(call: Expression.Call): String {
        return """Call: {
    "FunctionName": "${call.identifier1.substring}",
    "Args": [${printExpressionList(call.expressionLst1)}]
}        """
    }


    override fun visitLiteral(literal: Expression.Literal): String {
        return """Literal: {
    "Value": "${literal.any1}",
}        """
    }

    override fun visitVarCall(varCall: Expression.VarCall): String {
        return """VarCall: {
    "VariableName": "${varCall.identifier1.substring}",
}        """
    }

    override fun visitFunctionDefinition(functionDefinition: Expression.FunctionDefinition): String {
        return """FunctionDefinition: {
            |   "FunctionName": "${functionDefinition.identifier1.substring}",
            |   "Arguments": [${printExpressionList(functionDefinition.expressionLst1)}],
            |   "Body": [${printExpressionList(functionDefinition.expressionLst2)}]
            |}
        """.trimMargin()
    }


    private fun printExpressionList(list: List<Expression>) =
            list.joinToString(", ", transform = { "{" + it.accept(this) + "}"})

    override fun visitComparison(comparison: Expression.Comparison): String {
        return """Comparison: {
    "LeftSide": {
        ${comparison.expression1.accept(this)}
    },
    "Comparator": "${comparison.comparator1.substring}",
    "RightSide": {
        ${comparison.expression2.accept(this)}
    }
}        """
    }

    override fun visitIfStmnt(ifStmnt: Expression.IfStmnt): String {
        return """If-Statement: {
            |   "Condition": [${ifStmnt.expression1.accept(this)}],
            |   "Body": [${printExpressionList(ifStmnt.expressionLst1)}],
            |   "Else-body": [${printExpressionList(ifStmnt.expressionLst2)}]
            |}
        """.trimMargin()
    }

    override fun visitReturnStmnt(returnStmnt: Expression.ReturnStmnt): String {
        return """Return: {
            |   "Value": ${returnStmnt.expression1.accept(this)}
            |}
        """.trimMargin()
    }

    override fun visitWhileStmnt(whileStmnt: Expression.WhileStmnt): String {
        return """While-loop: {
            |   "Condition": [${whileStmnt.expression1.accept(this)}],
            |   "Body": [${printExpressionList(whileStmnt.expressionLst1)}],
            |}
        """.trimMargin()
    }

    override fun visitForStmnt(forStmnt: Expression.ForStmnt): String {
        return """For-loop: {
            |   "Upper args": [${printExpressionList(forStmnt.expressionLst1)}],
            |   "Body": [${printExpressionList(forStmnt.expressionLst2)}],
            |}
        """.trimMargin()
    }
}
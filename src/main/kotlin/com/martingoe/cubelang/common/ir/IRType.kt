package com.martingoe.cubelang.common.ir

enum class IRType {
    COPY,
    COPY_FROM_REF,
    COPY_FROM_DEREF,
    COPY_TO_DEREF,
    COPY_FROM_REG_OFFSET,
    PLUS_OP,
    MINUS_OP,
    MUL_OP,
    DIV_OP,
    PUSH_ARG,
    CALL,
    INC,

    POP_ARG, NEG_UNARY, PUSH_REG, POP_REG, CMP, DEC, SAL, COPY_TO_FP_OFFSET, COPY_FROM_FP_OFFSET, EXTEND_TO_64BITS,
    COPY_STRING_REF
}
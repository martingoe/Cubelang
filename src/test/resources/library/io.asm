extern putchar
extern printf
intPrintFormat db "%d", 10, 0
charPrintFormat db "%c", 10, 0
pointerPrintFormat db "%p", 10, 0

printI8:
    sub rsp, 8
    mov esi, edi
    mov edi, intPrintFormat
    xor al, al
    call printf
    add rsp, 8
    ret

printI16:
    sub rsp, 8
    mov esi, edi
    mov edi, intPrintFormat
    xor al, al
    call printf
    add rsp, 8
    ret
printI64:
    sub rsp, 8
    mov rsi, rdi
    mov edi, intPrintFormat
    xor al, al
    call printf
    add rsp, 8
    ret

printChar:
    sub rsp, 8
    call putchar
    mov rdi, 10
    call putchar
    add rsp, 8
    ret
printI32:
    sub rsp, 8
    mov esi, edi
    mov edi, intPrintFormat
    xor al, al
    call printf
    add rsp, 8
    ret
printPointer:
    sub rsp, 8
    mov esi, edi
    mov edi, pointerPrintFormat
    xor al, al
    call printf
    add rsp, 8
    ret
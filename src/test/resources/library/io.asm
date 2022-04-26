extern putchar
extern printf
intPrintFormat db "%d", 10, 0
charPrintFormat db "%c", 10, 0
pointerPrintFormat db "%p", 10, 0

printI8:
    mov esi, edi
    mov edi, intPrintFormat
    xor al, al
    call printf
    ret

printI16:
    mov esi, edi
    mov edi, intPrintFormat
    xor al, al
    call printf
    ret
printI64:
    mov rsi, rdi
    mov edi, intPrintFormat
    xor al, al
    call printf
    ret

printChar:
    call putchar
    mov rdi, 10
    call putchar
    ret
printI32:
    mov esi, edi
    mov edi, intPrintFormat
    xor al, al
    call printf
    ret
printPointer:
    mov esi, edi
    mov edi, pointerPrintFormat
    xor al, al
    call printf
    ret
extern putchar
extern printf
intPrintFormat db "%d", 10, 0
charPrintFormat db "%c", 10, 0

printShort:
    mov esi, edi
    mov edi, intPrintFormat
    xor al, al
    call printf
    ret

printChar:
    call putchar
    mov rdi, 10
    call putchar
    ret
printInt:
    mov esi, edi
    mov edi, intPrintFormat
    xor al, al
    call printf
    ret
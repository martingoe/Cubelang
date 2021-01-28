%include "library/stdio.asm"

section .text
    global main
main:
push rbp
mov rbp, rsp
sub rsp, 4
mov DWORD [rbp - 4], 1

mov eax, DWORD [rbp-4]

neg eax
mov DWORD [rbp - 4], eax

mov edi, DWORD [rbp-4]
call printInt


leave
ret

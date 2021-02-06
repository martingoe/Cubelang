%include "library/stdio.asm"
%include "library/IntMath.asm"


section .text
    global main
test:
push rbp
mov rbp, rsp
sub rsp, 16
mov QWORD[rbp - 16], rdi
mov QWORD[rbp - 8], rsi
mov eax, DWORD [rbp-4]



leave
ret
main:
push rbp
mov rbp, rsp
sub rsp, 16

mov DWORD [rbp-16], 4

mov DWORD [rbp-4], 10

mov rdi, QWORD [rbp - 16]
mov rsi, QWORD [rbp - 8]
call test
mov edi, eax
call printInt


leave
ret

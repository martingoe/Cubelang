%include "library/io.asm"
%include "library/IntMath.asm"
%include "test2.asm"


section .text
    global main
main:
push rbp
mov rbp, rsp
sub rsp, 36


lea rax, [rbp-24]
mov QWORD [rbp-8], rax

mov DWORD [rbp-24], 2

push rcx
push rsi
mov r8, QWORD [rax + 0] 
mov QWORD [rbp-36], r8 
mov ecx, DWORD [rax + 8] 
mov DWORD [rbp-28], ecx 
pop rsi
pop rcx

mov edi, DWORD [rbp-36]
call printInt


leave
ret

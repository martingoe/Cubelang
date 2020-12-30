%include "library/stdio.asm"

section .text
    global main
main:
push rbp
mov rbp, rsp
sub rsp, 15

mov BYTE [rbp - 11], 72
mov BYTE [rbp - 10], 101
mov BYTE [rbp - 9], 108
mov BYTE [rbp - 8], 108
mov BYTE [rbp - 7], 111
mov BYTE [rbp - 6], 32
mov BYTE [rbp - 5], 87
mov BYTE [rbp - 4], 111
mov BYTE [rbp - 3], 114
mov BYTE [rbp - 2], 108
mov BYTE [rbp - 1], 100
mov DWORD [rbp - 15], 0
.L1:
push rbx

mov eax, DWORD [rbp-15]

mov ebx, 11
cmp eax, ebx
pop rbx
jge .L2
movsx rbx, DWORD [rbp-15]

movsx edi, BYTE [rbp-11+rbx*1]
call printChar


mov ebx, 1

mov eax, DWORD [rbp-15]
add eax, ebx 
mov DWORD [rbp - 15], eax
jmp .L1
.L2:

mov edi, 72
call printChar

mov edi, 3
call printInt


leave
ret


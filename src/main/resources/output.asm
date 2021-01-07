%include "library/stdio.asm"

section .text
    global main
main:
push rbp
mov rbp, rsp
sub rsp, 15

mov al[rbp - 11]

mov al[rbp - 10]

mov al[rbp - 9]

mov al[rbp - 8]

mov al[rbp - 7]

mov al[rbp - 6]

mov al[rbp - 5]

mov al[rbp - 4]

mov al[rbp - 3]

mov al[rbp - 2]

mov al[rbp - 1]

mov DWORD [rbp - 15], 0
.L1:
push rbx

mov eax, DWORD [rbp-15]

mov ebx, 11
cmp eax, ebx
pop rbx
jge .L2
movsx rbx, DWORD [rbp-15]
mov al, BYTE [rbp-11+rbx*1]
movsx edi, BYTE al
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

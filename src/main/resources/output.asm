%include "library/stdio.asm"

section .text
    global main
main:
push rbp
mov rbp, rsp
sub rsp, 4
mov DWORD [rbp - 4], 0

push rbx
mov eax, DWORD [rbp-4]

mov ebx, 0

cmp eax, ebx
pop rbx
jne .L2
push rbx
mov eax, 1

mov ebx, 3

cmp eax, ebx
pop rbx
jne .L2


.L1:
mov edi, 5
call printInt

.L2:



leave
ret

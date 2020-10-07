section .text
    global _start
_start:
mov rbp, rsp
sub rsp, 4

mov edi, 4 
call x 
mov DWORD [rbp - 4], eax


mov rax, 60
mov rdi, 0
syscall

x:
push rbp
mov rbp, rsp
sub rsp, 8
mov DWORD[rbp - 4], edi
mov DWORD [rbp - 8], DWORD [rbp-4]
cmp DWORD [rbp-8], 10
jge .L2
mov DWORD [rbp - 8], 10
.L2:
mov DWORD [rbp - 8], 3
mov DWORD [rbp - 8], 2
mov eax, DWORD [rbp-8]


leave
ret

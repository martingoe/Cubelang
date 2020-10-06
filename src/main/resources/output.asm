section .text
    global _start
_start:
mov rbp, rsp
sub rsp, 4

call x 
mov DWORD [rbp - 4], eax


mov rax, 60
mov rdi, 0
syscall

x:
push rbp
mov rbp, rsp
sub rsp, 4
mov DWORD [rbp - 4], 20
cmp DWORD [rbp-4], 10
jge .L2
mov DWORD [rbp - 4], 10
.L2:
mov DWORD [rbp - 4], 3
mov DWORD [rbp - 4], 2
mov eax, DWORD [rbp-4]


leave
ret

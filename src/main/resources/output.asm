section .text
    global _start
_start:
mov rbp, rsp
sub rsp, 5

call x 
mov BYTE [rbp - 1], al
mov DWORD [rbp - 5], 4


mov rax, 60
mov rdi, 0
syscall

x:
push rbp
mov rbp, rsp
sub rsp, 1
mov BYTE [rbp - 1], 20
mov al, BYTE [rbp-1]

leave
ret

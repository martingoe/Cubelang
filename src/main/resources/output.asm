section .text
    global _start
_start:
mov rbp, rsp
sub rsp, 12

call x 
mov QWORD [rbp - 8], rax
mov DWORD [rbp - 12], 4


mov rax, 60
mov rdi, 0
syscall

x:
push rbp
mov rbp, rsp
sub rsp, 4
mov DWORD [rbp - 4], 20
mov rax, DWORD PTR [rdb-4]

leave
ret

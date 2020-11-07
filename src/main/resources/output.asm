extern printf
extern putchar
section .data
    intPrintFormat db "%d", 10, 0
section .text
    global main
printInt:
    mov esi, edi
    mov edi, intPrintFormat
    xor al, al
    call printf
    ret
    
printChar:
    call putchar
    mov rdi, 10
    call putchar 
    ret
main:
mov rbp, rsp
sub rsp, 4

mov edi, 10 
call fib 
mov DWORD [rbp - 4], eax
mov edi, DWORD [rbp-4] 
call printInt


mov rax, 60
mov rdi, 0
syscall

fib:
push rbp
mov rbp, rsp
sub rsp, 4
mov DWORD[rbp - 4], edi
cmp DWORD [rbp-4], 2
jge .L2
mov eax, DWORD [rbp-4]
jmp .L3
.L2:

push r12
push rbx
mov eax, 2
mov r12, rax
mov eax, DWORD [rbp-4]
sub eax, r12d 
mov edi, eax 
call fib
mov rbx, rax
mov eax, 1
mov r12, rax
mov eax, DWORD [rbp-4]
sub eax, r12d 
mov edi, eax 
call fib
add eax, ebx
pop rbx
pop r12

.L3:
leave
ret

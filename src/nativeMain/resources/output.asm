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
getCurrentTime:
    mov rax, 201
    xor rdi, rdi        
    syscall
    ret
    
printChar:
    call putchar
    mov rdi, 10
    call putchar 
    ret
main:
mov rbp, rsp
sub rsp, 4

call getCurrentTime 
mov DWORD [rbp - 4], eax
mov edi, 40 
call fib 
mov edi, eax 
call printInt
mov eax, DWORD [rbp-4]
mov rbx, rax
call getCurrentTime
sub eax, ebx 
mov edi, eax 
call printInt



fib:
push rbp
mov rbp, rsp
sub rsp, 4
mov DWORD[rbp - 4], edi
cmp DWORD [rbp-4], 2
jge .L1
mov eax, DWORD [rbp-4]
jmp .L2

.L1:

push rbx
mov eax, 1
mov r12, rax
mov eax, DWORD [rbp-4]
sub eax, r12d 
mov edi, eax 
call fib
mov rbx, rax
mov eax, 2
mov r12, rax
mov eax, DWORD [rbp-4]
sub eax, r12d 
mov edi, eax 
call fib
add eax, ebx
pop rbx

.L2:
leave
ret


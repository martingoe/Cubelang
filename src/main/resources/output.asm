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
sub rsp, 5

mov BYTE [rbp - 1], 120
mov eax, 2
mov rbx, rax
mov al, BYTE [rbp-1] 
movsx rdi, al
call x
add eax, ebx 
mov DWORD [rbp - 5], eax
mov edi, DWORD [rbp-5] 
call printInt


mov rax, 60
mov rdi, 0
syscall

x:
push rbp
mov rbp, rsp
sub rsp, 2
mov eax, edi
mov BYTE[rbp - 1], al
mov al, BYTE [rbp - 1]
mov BYTE [rbp - 2], al
cmp BYTE [rbp-2], 10
jg .L2
mov edi, 49 
call printChar
mov eax, 2
jmp .L3
.L2:
mov edi, 48 
call printChar
mov eax, 5

.L3:
leave
ret

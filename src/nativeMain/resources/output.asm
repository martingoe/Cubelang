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
sub rsp, 8



mov DWORD [rbp - 4], 72

mov DWORD [rbp - 0], 101

mov DWORD [rbp - -4], 108

mov DWORD [rbp - -8], 108

mov DWORD [rbp - -12], 111

mov DWORD [rbp - -16], 32

mov DWORD [rbp - -20], 87

mov DWORD [rbp - -24], 111

mov DWORD [rbp - -28], 114

mov DWORD [rbp - -32], 108

mov DWORD [rbp - -36], 100

mov DWORD [rbp - -40], 10
mov DWORD [rbp - 8], 0
.L1:
cmp DWORD [rbp-8], 13
jge .L2
mov eax, DWORD [rbp-8]
mov rbx, rax

mov edi, DWORD [rbp-4+rbx*4]
call putchar

mov eax, 1
mov rbx, rax
mov eax, DWORD [rbp-8]
add eax, ebx 
mov DWORD [rbp - 8], eax
jmp .L1
.L2:





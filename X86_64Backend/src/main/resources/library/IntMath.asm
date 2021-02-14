min:
    push rbp
    mov rbp, rsp
    sub rsp, 8
    mov DWORD[rbp - 4], edi
    mov DWORD[rbp - 8], esi
    push rbx
    mov eax, DWORD [rbp-4]
    mov ebx, DWORD [rbp-8]
    cmp edi, esi
    pop rbx
    jl .L1
    mov eax, esi
    jmp .L2
    .L1:
    mov eax, edi
    .L2:
    leave
    ret

max:
    push rbp
    mov rbp, rsp
    sub rsp, 8
    mov DWORD[rbp - 4], edi
    mov DWORD[rbp - 8], esi
    push rbx
    mov eax, DWORD [rbp-4]
    mov ebx, DWORD [rbp-8]
    cmp edi, esi
    pop rbx
    jg .L1
    mov eax, esi
    jmp .L2
    .L1:
    mov eax, edi
    .L2:
    leave
    ret

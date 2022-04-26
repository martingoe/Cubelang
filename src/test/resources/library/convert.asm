i32toi64:
    xor rax, rax
    mov eax, edi
    ret

i32toi8:
    mov eax, edi
    ret

i32toi16:
    xor rax, rax
    mov eax, edi
    cwde
    ret

i32tochar:
    mov eax, edi
    ret

i8tochar:
    mov eax, edi
    ret

i8toi16:
    mov eax, edi
    ret

i8toi32:
    xor rax, rax
    mov eax, edi
    ret

i8toi64:
    xor rax, rax
    mov eax, edi
    ret

i16tochar:
    mov eax, edi
    ret

i16toi8:
    mov eax, edi
    ret

i16toi32:
    xor rax, rax
    mov eax, edi
    ret

i16toi64:
    xor rax, rax
    mov eax, edi
    ret


i64tochar:
    mov eax, edi
    ret

i64toi8:
    mov eax, edi
    ret

i64toi32:
    mov eax, edi
    ret

i64toi16:
    mov eax, edi
    ret
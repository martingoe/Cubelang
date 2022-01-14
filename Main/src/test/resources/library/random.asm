; Uses the https://en.wikipedia.org/wiki/Linear_congruential_generator with m=2^31, a=75, c=74
randomI32:
    mov eax, 75
    imul eax, edi
    add eax, 74
    ; Modulo of 2^31
    movsx   rdx, eax
    imul    rdx, rdx, 838860819
    shr     rdx, 32
    sar     edx, 22
    mov     ecx, eax
    sar     ecx, 31
    sub     edx, ecx
    imul    edx, edx, 21474836
    sub     eax, edx
    ret

section .text
    global main
test10Value:
push rbp
mov rbp, rsp
sub rsp, 0
mov eax, 10



leave
ret

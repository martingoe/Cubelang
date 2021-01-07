nasm -f elf64 output.asm
gcc output.o -no-pie
./a.out
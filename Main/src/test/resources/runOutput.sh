nasm -f elf64 test.asm
gcc test.o -no-pie
./a.out
# Cubelang
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg?style=flat)](https://opensource.org/licenses/MIT)

This is a very simple statically typed and compiled (x86_64 NASM) programming language.


## Reasoning behind this project
I started this project as a "Besondere Lernleistung" for school with the intent of learning about how both compiled and interpreted programming languages are laid out and how they work, 
how a CPU executes programs and how one uses low level programming languages to write said programs. 

Up until now, this project has helped immensely in learning about all of those things and has also introduced me to parsing properly.
So while I wouldn't use this language for any projects myself, it has been great at helping me learn. 

## Getting Started

These instructions will get you a copy of the project up and running on your local machine for development and testing purposes.

### Prerequisites

You will need to have the following programs installed on your system:

1. Java 15
2. NASM
3. GCC

### Installing

Clone the repository to your local machine
```bash
git clone https://gitlab.com/CubeArrow/Cubelang.git
```

Change directory to `Cubelang`
```bash
cd Cubelang 
```

Package the program in an executable JAR

```bash
./gradlew jar
```

Set the standard library path

```bash
export CUBELANG_LIB="src/test/resources/library"
```
Run the program
```bash
java -jar build/libs/Cubelang-1.0-SNAPSHOT.jar sourcefile
```

Assemble the program with
```bash
nasm -f elf64 output.asm
```

Include the c standard library
```bash
gcc -no-pie output.o
```


## Built With

* [Gradle](https://gradle.org/) - Dependency Management
* [JUnit](https://junit.org/junit5/) - Testing framework

## Examples
### Output
The following snippet writes the integer 10 in the console.
```
printI32(10);
```
### Fibonacci
The following function recursively calculates the n-th fibonacci number.
```
fun fib(n: i32): i32{
    if(n < 2) {
        return n;
    }
    return fib(n - 1) + fib(n - 2);
}
```

### Euclidean
This algorithm calculates the greatest common divisor of two integers.
```
fun euclidean(a: i32, b: i32): i32{
    while(b != 0){
        if (a > b)
            a = a - b;
        else
            b = b - a;
    }
    return a;
}
```
## Resources

These are very useful resources that I have used to get started with this project


* Chris Hathhorn. 2012. Engineering a compiler, second edition by Keith D. Cooper and Linda Torczon. SIGSOFT Softw. Eng. Notes 37, 1 (January 2012), 36–37. 
* Robert Nystrom. 2021. Crafting Interpreters.
* Alfred V. Aho, Mahadevan Ganapathi, and Steven W. K. Tjiang. 1989. Code generation using tree matching and dynamic programming. ACM Trans. Program. Lang. Syst. 11, 4 (Oct. 1989), 491–516.
* Massimiliano Poletto and Vivek Sarkar. 1999. Linear scan register allocation. ACM Trans. Program. Lang. Syst. 21, 5 (Sept. 1999), 895–913.


## License

This project is licensed under the MIT License - see the [LICENSE.md](LICENSE.md) file for details

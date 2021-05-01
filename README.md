# Cubelang
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg?style=flat)](https://opensource.org/licenses/MIT)
[![Maintainability](https://api.codeclimate.com/v1/badges/b56516951e9c3a93a8b0/maintainability)](https://codeclimate.com/github/CubeArrow/Cubelang/maintainability)

This is a very simple statically typed (WIP) compiled (x86_64 NASM) programming language.


## Reasoning behind this project
I started this project with the intent of learning about how both compiled and interpreted programming languages are laid out and how they work, 
how a CPU executes programs and how one uses low level programming languages to write those programs. 

Up until now, this project has helped immensely in learning about all of those things and has also introduced me to parsing properly.
So while I wouldn't use this language for any projects myself, it has been great at helping me learn and I hope that this will continue to be the case as it is still a WIP. 

## Getting Started

These instructions will get you a copy of the project up and running on your local machine for development and testing purposes.

### Prerequisites

You will need to have the following programs installed on your system:

1. Java 15

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
./gradlew fatJar
```

Run the program
```bash
java -jar build/libs/Cubelang-fat-1.0-SNAPSHOT.jar sourcefile
```


## Built With

* [Gradle](https://gradle.org/) - Dependency Management
* [JUnit](https://junit.org/junit5/) - Testing framework

## Examples
### Output
The following program writes the integer 10 in the console.
```
printInt(10);
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

* [Crafting interpreters](https://craftinginterpreters.com/) - A book about creating programming languages
* [Compiler Explorer](https://godbolt.org) - A website that simulates compilers like gcc and displays the x86 output



## License

This project is licensed under the MIT License - see the [LICENSE.md](LICENSE.md) file for details

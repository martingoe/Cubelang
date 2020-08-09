# Cubelang
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg?style=flat)](https://opensource.org/licenses/MIT)
[![Maintainability](https://api.codeclimate.com/v1/badges/c4c7b588ae7369fc8485/maintainability)](https://codeclimate.com/github/CubeArrow/Cubelang/maintainability)

This is a very simple interpreted programming language. It is not type-safe since type checking has not been implemented yet.



## Getting Started

These instructions will get you a copy of the project up and running on your local machine for development and testing purposes.

### Prerequisites

The applications you need to install the software and how to install them

1. Java 11

### Installing

Clone the repository to your local machine
```
git clone https://gitlab.com/CubeArrow/Cubelang.git
```

Change directory to `Cubelang`
```
cd Cubelang 
```

Package the program in an executable JAR

```
./gradlew jar
```

Run the program making sure you use `--enable-preview`
```
java -jar build/libs/Cubelang-1.0-SNAPSHOT.jar
```


## Built With

* [Gradle](https://gradle.org/) - Dependency Management
* [JUnit](https://junit.org/junit5/) - Testing framework

## Resources

These are very useful resources that I have used to get started with this project

* [Crafting interpreters](https://craftinginterpreters.com/) - A book about creating programming languages
* [Cell](https://gitlab.com/cell_lang/cell) - A programming language created for the purpose of teaching about interpreters. A tutorial can be found on YouTube.


## Authors

* **CubeArrow** - *Main development* - [CubeArrow](https://gitlab.com/CubeArrow)


## License

This project is licensed under the MIT License - see the [LICENSE.md](LICENSE.md) file for details

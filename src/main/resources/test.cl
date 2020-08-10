fun fib(n) {
    if (n < 2){
        return n;
    };

    return fib(n - 1) + fib(n - 2);
};

var i = 10;
i = 12;
println(i);

println(fib(10));
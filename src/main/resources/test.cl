class Y {
    var test = 10;
}

class X {
    var y = Y();

    fun init(){
        y.test = 5;
    };
    fun test(){
        return 10;
    };

    var x = 10;
}

var x = X();
println(x.test() / 2);
println(x.y.test + 2);
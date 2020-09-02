class Y {
    var test = 10.0;
    fun test(){
        return 5;
    };
}

class X : Y{
    var test = 5;
    fun test(){
        return 10;
    };

    var x = 10;
}

var x = X();
var y = Y();
var test = null;
println(x.test());
println(x.test);
println(y.test);
println(test);

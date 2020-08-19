class Y {
    var test = 10;
    fun init(){

    }
}

class X {
    var test = 10;
    var y = Y();

    fun init(){
    }
}

var x = X();
x.y.test = 5;
println(x.y.test);

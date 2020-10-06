fun x():int{
    var x = 20;
    if(x < 10){
        x = 10;
    } else {
        x = 3;
    };
    x = 2;
    return x;
};
var res = x();

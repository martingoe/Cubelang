fun x(z: int):int{
    var x = z;
    if(x < 10){
        x = 10;
    } else {
        x = 3;
    }
    return x;
}
var res = x(4);

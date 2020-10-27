fun x(z: char):int{
    var x = z;
    if(x <= 10){
        printChar('1');
        return 2;
    } else {
        printChar('0');
    }
    return 5;
}
var x = 'x';
var res = x(x) + 2;
printInt(res);

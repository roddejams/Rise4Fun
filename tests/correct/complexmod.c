int foo(int a)
    requires a == 0,
    ensures \result == 4
{
    int x;
    x = 5;

    if(x % a != 5) {
        x = 99999;
    } else {
        x = 4;
    }
    return x;
}
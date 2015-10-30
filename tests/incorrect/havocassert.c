int foo()
{
    int x;
    havoc x;
    if(x) {
        havoc x;
        assume x == 10;
    }
    assert x == 10;
    return 0;
}
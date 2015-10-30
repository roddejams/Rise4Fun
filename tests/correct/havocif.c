int foo()
{
    int x;
    havoc x;
    if(x) {
        x = 0;
    }
    assert x == 0;
    return 0;
}
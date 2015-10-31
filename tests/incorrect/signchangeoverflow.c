int foo()
{
    int x;

    assume x == -2147483648;

    x = -x;

    assert x > 0;
    return 0;
}
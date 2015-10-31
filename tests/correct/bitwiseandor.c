int foo()
{
    int x;
    assume x == 2;

    x = 1 & x;

    assert x == 0;

    x = 1 | x;

    assert x != 0;
    return 0;
}
int foo()
{
    int x;

    assume x == -1;

    x = -x;

    assert x == 1;
    return 0;
}
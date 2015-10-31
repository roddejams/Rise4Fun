int foo()
{
    int x;

    assume x < 0;

    x = -x;

    assert x > 0;
    return 0;
}
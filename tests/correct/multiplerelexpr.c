int foo()
    ensures \result == 1
{
    int x;

    x = 1 < 4 < 6 < 3;

    return x;
}
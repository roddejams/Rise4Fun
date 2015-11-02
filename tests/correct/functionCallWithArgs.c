int foo()
    ensures \result == 1
{
    int x;
    x = 0;

    x = bar(x, x - 1);

    return x;
}

int bar(int i, int x)
    requires x == i-1,
    ensures \result == i + 1
{
    return i + 1;
}
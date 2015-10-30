int f()
    ensures \result == 1
{
    int i;
    i = 1;
    int j;
    j = 0;
    int r;

    r = j ? i : i ? i : j;

    return r;
}
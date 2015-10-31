int f()
    ensures \result == 0
{
    int x;
    x = 1;
    return !x << 3;
}
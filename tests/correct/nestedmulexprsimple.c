int f()
    ensures \result == 2
{
    int x;
    x = 16 / 2 % 3;
    return x;
}
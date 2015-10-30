int f()
    ensures \result == 0
{
    int x;
    x = 1;
    int k;
    k = !x << 3;
    return k;
}
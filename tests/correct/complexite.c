int f(int x)
    requires x == 1,
    ensures \result != 0
{
    int r;
    if (!x ? 1 == 1 : x * 25) {
        assume r == 5;
    }
    return r;
}
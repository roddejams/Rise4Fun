int x;
int y;

int f()
    requires x != 0 && y != 0,
    ensures \result == \old(x)
{
    return y % 4 << 12 * 51
               ? (y / x / (y * (x << 2)))
               : x / !y;
}
int x;
int y;

int f()
    requires x != 0 && y != 0
    ensures \result == !\old(x)
{
    x = y % 4 << 12 * 51
            ? !x / !y
            : (y / x / (y * (x << 2)));

    return x;
}
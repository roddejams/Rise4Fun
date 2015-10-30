int x;
int y;

int swap()
    ensures x == \old(y) && y == \old(x)
{
    x = x ^ y;
    y = y ^ x;
    x = x ^ y;
    return 0;
}
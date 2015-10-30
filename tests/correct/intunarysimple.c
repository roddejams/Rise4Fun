int x;
int y;

int foo()
    requires x == 1 && y == 2,
    ensures \result == -\old(y)
{
    y = -x;
    y = y - +x;
    return y;
}
int x;
int y;

int f()
    requires x && y,
    ensures \result != 0
{
    x = ~(~(-(~ +y) || 5));
    return x;
}
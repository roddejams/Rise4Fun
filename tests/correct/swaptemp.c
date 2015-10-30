int x;
int y;

int swap()
    ensures x == \old(y) && y == \old(x)
{
    int temp;
    temp = x;
    x = y;
    y = temp;
    return 0;
}
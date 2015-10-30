int x;
int y;

int f(int x)
    requires x == 5 && y,
    ensures \result == 5
{
    int z;
    if (!-x) {
        havoc z;
    } else {
        z = - -x; // = x
        // 0 - (0 -x) = -2x
    }
    return z;
}
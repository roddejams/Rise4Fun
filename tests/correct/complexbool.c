int x()
    ensures \result == 0
{
    int x;
    x = 1;
    int z1;
    z1 = !x;
    int z2;
    z2 = (3 && 5 && ~(-1));
    return z1 || z2;
}
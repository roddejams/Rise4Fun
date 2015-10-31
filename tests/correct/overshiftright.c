int foo(int i, int j)
    requires j > 32,
    ensures \result == 0
{
    return i >> j;
}
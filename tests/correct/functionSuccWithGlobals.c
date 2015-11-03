int i;

int foo()
    ensures \result == \old(i) + 1
{
    int x;
    assume i > 0;
    x = succ(i);
    i = i + 1;
    return x;
}

int succ(int i)
    ensures \result == i + 1,
    requires i > 0
{
    return i + 1;
}
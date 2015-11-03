int g;

int foo()
    ensures g == 5
{
    g = 5;
    int x;
    x = iDontChangeGOk();
    return x;
}

int iDontChangeGOk()
{
    g = 6;
    return 0;
}
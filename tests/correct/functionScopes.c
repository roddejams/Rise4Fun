int i;

int main()
    ensures i == \old(i)
{

    int x;
    x = bar();

    return 0;
}

int bar()
{
    int k;
    havoc k;

    int i;
    i = k;

    return 0;
}
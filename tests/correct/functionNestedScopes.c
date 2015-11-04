int i;

int main()
    ensures i == \old(i)
{

    int x;
    if (1) {
        {
            x = bar();
        }
    } else {
        int i;
        i = 3;
    }

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
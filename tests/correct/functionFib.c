int a;
int b;

int main()
    ensures \result == 3
{
    int i;
    i = 4;
    int ret;

    ret = fib(i);
    return ret;
}

int fib(int n)
    ensures \result == ( n == 0 ? 0 : n == 1 ? 1 : a + b )
{
    int ret;
    if (n == 0) {
        ret = 0;
    } else {
        if (n == 1) {
            ret = 1;
        } else {
            a = fib(n - 1);
            b = fib(n - 2);
            ret = a + b;
        }
    }
    return ret;
}
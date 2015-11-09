int foo ()
    requires 0,
    requires 1
    {
        return 0;
    }
int main () {
    int x;
    x = foo();
    return 0;
}
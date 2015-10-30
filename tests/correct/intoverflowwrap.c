int f() {
    int x;
    x = 2147483647;
    x = x + 1;
    assert x < 0;
    return 0;
}
int foo() {
    int x;
    x = 0;
    if (!x) {
        x = 5;
    }
    assert x == 5;
    return 0;
}
int f() {
    int x;
    int y;
    x = 8 / (5 % 3);
    y = (8 / 5) % 3;
    assert x != y;
    return 0;
}
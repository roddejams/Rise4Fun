int foo() {
    int x;
    havoc x;
    assume x == 0;
    assume x != 0;
    assert 0;
    return 0;
}
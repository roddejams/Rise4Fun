int main() {
    int x;
    x = 0;
    while (x < 2)
        invariant x >= 0,
        invariant x <= 2
    {
        x = x + 1;
    }
    assert x == 2;
    return 0;
}
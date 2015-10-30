int x;

int f(int x) requires x {
    assert x;
    {
        int x;
        x = 0;
        if (x == 0) {
          int x;
          havoc x;
          assume x == 5;
          assert x == 5;
        }

        // this one fails
        assert x == 0;
    }
    assert x;
    return 0;
}
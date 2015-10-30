int x_;

int foo()
    requires x_ == 0
{
    int x;
    havoc x;
    assume x == 0;
    assert x == \old(x_);
    return 0;
}
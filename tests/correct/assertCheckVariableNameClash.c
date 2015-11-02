int assertCheck;

int foo()
    ensures \result == 1
{
    assertCheck = 1;

    return assertCheck;
}
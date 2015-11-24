// RUN: %tool "%s" > "%t"
// RUN: %diff %CORRECT "%t"

int foo(int x)
    candidate_requires x == 1,
    ensures \result == 1
{
    return x;
}
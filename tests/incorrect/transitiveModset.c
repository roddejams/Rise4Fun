// RUN: %tool "%s" > "%t"
// RUN: %diff %INCORRECT "%t"

int g;

int foo()
  ensures \result == \old(g)
{
  int t;
  t = bar();
  return g;
}

int bar()
  ensures \result == 0 {
  int t;
  t = baz();
  return 0;
}

int baz()
  ensures \result == 0 {
  g = 4;
  return 0;
}
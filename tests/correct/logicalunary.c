int foo() {
  int x;
  x = 0;
  if (!x) {
    x = 1;
  }
  assert x == 1;
  return 0;
}
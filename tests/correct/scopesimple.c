int foo() {
  int i;
  i = 1;
  if (1) {
    i = 2;
    int i;
    i = 4;
    assert i == 4;
  }
  assert i == 2;
  return 0;
}
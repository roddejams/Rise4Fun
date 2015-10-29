int foo() {
  int i;
  i = 4;
  i = (i == 4) + 5;
  assert i == 6;
  return 0;
}
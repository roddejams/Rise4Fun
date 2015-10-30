int foo() {
  int i;
  i = 1;
  havoc i;
  assert i == 1;
  return 0;
}
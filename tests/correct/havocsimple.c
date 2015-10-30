int foo() {
  int i;
  i = 0;
  havoc i;
  assume i == 5;
  assert i == 5;
  return 0;
}

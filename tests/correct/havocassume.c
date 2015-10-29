int foo() {
  int i; int j;

  i = 1;
  j = 2;

  if (j > i) {
    havoc i;
    assume i == 5;
    j = 10;
  }
  assert(i == 5 && j == 10);

  return 0;
}
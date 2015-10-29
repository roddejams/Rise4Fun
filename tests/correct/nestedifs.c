int foo() {
  int i; int j; int k; int f;
  i = 1;
  j = 1;
  k = 0;
  f = 0;

  if (i != (k + 1) << 5) {
    if (j < 0) {
      f = 0;
    } else {
      if (j + k > 0) {
        f = 1;
      }
    }
  }

  assert f == 1;
  return 0;
}
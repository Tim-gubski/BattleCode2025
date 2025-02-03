for x in range(-4, 5):
  for y in range(-4, 5):
    if x**2 + y**2 <= 20 and x**2 + y**2 >= 8 and abs(y) < abs(x) and x < 0:
      print(f"new MapLocation({x}, {y}).translate(x, y),")
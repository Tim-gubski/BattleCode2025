package v57_Optimaler.Helpers;

public class KDTree {
    public static class N {
        int x, y;
        N l, r;

        N(int xx, int yy) {
            x = xx;
            y = yy;
        }
    }

    N r; // root node

    // Insert, skipping duplicates
    public void insert(int X, int Y) {
        if (r == null) {
            r = new N(X, Y);
            return;
        }
        N n = r;
        int d = 0;
        while (true) {
            if (X == n.x && Y == n.y) return;
            // Axis: if d&1 == 0 then compare X, else compare Y
            if (((d & 1) == 0 && X < n.x) || ((d & 1) == 1 && Y < n.y)) {
                if (n.l == null) {
                    n.l = new N(X, Y);
                    return;
                }
                n = n.l;
            } else {
                if (n.r == null) {
                    n.r = new N(X, Y);
                    return;
                }
                n = n.r;
            }
            d++;
        }
    }

    // Nearest neighbor search (distance squared, fully iterative)
    public int[] nearestNeighbor(int X, int Y) {
        if (r == null) return null;
        N best = r, n = r;
        int bd = (r.x - X)*(r.x - X) + (r.y - Y)*(r.y - Y); // best dist^2
        int d = 0;

        while (n != null) {
            // Compute distance squared
            int dx = n.x - X, dy = n.y - Y;
            int dist = dx*dx + dy*dy;
            if (dist < bd) {
                bd = dist;
                best = n;
            }

            // Axis-based split
            int axis = d & 1;
            // c is how far the query point is from n along the current axis
            int c = (axis == 0 ? X - n.x : Y - n.y);
            int ad = c*c; // axis-dist^2

            // Decide which branch is primary vs. secondary
            N p = (c < 0) ? n.l : n.r;
            N s = (c < 0) ? n.r : n.l;

            if (p != null) {
                n = p;
                d++;
            } else if (s != null && ad < bd) {
                n = s;
                d++;
            } else {
                break;
            }
        }
        return new int[]{best.x, best.y};
    }
}
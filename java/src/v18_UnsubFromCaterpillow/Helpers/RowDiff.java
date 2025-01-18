package v18_UnsubFromCaterpillow.Helpers;

public class RowDiff {
    private final int w, h;
    private final int[][] diff;    // [h][w+1]
    private final int[][] prefix;  // [h][w]
    private final boolean[] dirty; // which rows need prefix rebuild

    public RowDiff(int w, int h) {
        this.w = w;
        this.h = h;
        diff = new int[h][w + 1];
        prefix = new int[h][w];
        dirty = new boolean[h];
    }

    // val = +1 or -1
    public void addRect(int x1, int y1, int x2, int y2, int val) {
        for (int r = y1; r <= y2; r++) {
            diff[r][x1] += val;
            if (x2 + 1 < w) {
                diff[r][x2 + 1] -= val;
            }
            dirty[r] = true;
        }
    }

    public boolean covered(int x, int y) {
        // If row is dirty, rebuild prefix for that row
        if (dirty[y]) {
            dirty[y] = false;
            int sum = 0;
            for (int c = 0; c < w; c++) {
                sum += diff[y][c];
                prefix[y][c] = sum;
            }
        }
        return prefix[y][x] > 0;
    }
}
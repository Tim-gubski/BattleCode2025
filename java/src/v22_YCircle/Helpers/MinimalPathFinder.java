package v22_YCircle.Helpers;

public class MinimalPathFinder {
    // 8 directions (including diagonals)
    static final int[] DX = {1, 1, 0, -1, -1, -1, 0, 1};
    static final int[] DY = {0, 1, 1, 1, 0, -1, -1, -1};

    // Cost function (treat negative as very large cost)
    static int costOf(int tile) {
        return (tile < 0) ? 9999 : (tile + 1);
    }

    public static int[][] findPath(int[][] grid, int sx, int sy, int tx, int ty) {
        int rows = grid.length, cols = grid[0].length, size = rows * cols;

        int[] dist = new int[size];
        int[] parent = new int[size];
        boolean[] visited = new boolean[size];
        for (int i = 0; i < size; i++) {
            dist[i] = 999999;
            parent[i] = -1;
        }
        dist[sy * cols + sx] = 0;

        // Simple Dijkstra without extra data structures
        for (int i = 0; i < size; i++) {
            int idx = -1, best = 999999;
            for (int j = 0; j < size; j++) {
                if (!visited[j] && dist[j] < best) {
                    best = dist[j];
                    idx = j;
                }
            }
            if (idx < 0) break;
            visited[idx] = true;
            int cx = idx % cols, cy = idx / cols;
            if (cx == tx && cy == ty) break;

            for (int d = 0; d < 8; d++) {
                int nx = cx + DX[d], ny = cy + DY[d];
                if (nx < 0 || ny < 0 || nx >= cols || ny >= rows) continue;
                if (visited[ny * cols + nx]) continue;
                int nd = dist[idx] + costOf(grid[ny][nx]);
                if (nd < dist[ny * cols + nx]) {
                    dist[ny * cols + nx] = nd;
                    parent[ny * cols + nx] = idx;
                }
            }
        }

        if (dist[ty * cols + tx] >= 999999) return new int[0][0];

        // Reconstruct path
        int[][] tempPath = new int[size][2];
        int length = 0, cur = ty * cols + tx;
        while (cur >= 0) {
            tempPath[length][0] = cur % cols;
            tempPath[length][1] = cur / cols;
            length++;
            cur = parent[cur];
        }

        // Reverse path in-place
        for (int a = 0, b = length - 1; a < b; a++, b--) {
            int tmpX = tempPath[a][0], tmpY = tempPath[a][1];
            tempPath[a][0] = tempPath[b][0];
            tempPath[a][1] = tempPath[b][1];
            tempPath[b][0] = tmpX;
            tempPath[b][1] = tmpY;
        }

        // Trim to exact size
        int[][] result = new int[length][2];
        for (int k = 0; k < length; k++) {
            result[k][0] = tempPath[k][0];
            result[k][1] = tempPath[k][1];
        }
        return result;
    }
}

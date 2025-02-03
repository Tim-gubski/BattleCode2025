package v42_TheAnswer.Helpers;

import battlecode.common.*;

public class DPPathFinder {

    // We'll assume the user has the following enum defined:
    // enum Direction { CENTER, EAST, NORTH, NORTHEAST, NORTHWEST, SOUTH,
    //                  SOUTHEAST, SOUTHWEST, WEST }

    // And a class MapLocation:
    // public final class MapLocation implements Comparable<MapLocation> {
    //     public final int x, y;
    //     public MapLocation(int x, int y) { this.x=x; this.y=y; }
    //     public MapLocation add(Direction d) { ... } // returns a new MapLocation
    //     public MapLocation subtract(Direction d) { ... }
    //     public MapLocation translate(int dx, int dy) { ... }
    //     public Direction directionTo(MapLocation loc) { ... }
    //     public int distanceSquaredTo(MapLocation loc) { ... }
    //     public boolean isWithinDistanceSquared(MapLocation loc, int distSq) { ... }
    //     public boolean isAdjacentTo(MapLocation loc) { ... }
    //     public int compareTo(MapLocation other) { ... }
    //     public boolean equals(Object o) { ... }
    //     public int hashCode() { ... }
    // }

    // 8 possible move directions (excluding CENTER):
    static final Direction[] MOVES = {
            Direction.NORTH,
            Direction.NORTHEAST,
            Direction.EAST,
            Direction.SOUTHEAST,
            Direction.SOUTH,
            Direction.SOUTHWEST,
            Direction.WEST,
            Direction.NORTHWEST
    };

    // Convert tile penalties to nonnegative costs:
    // ally(0)->0, empty(-1)->1, enemy(-2)->2, anything <= -3 => impassable => large cost
    private static int costOf(MapInfo tileVal) {
        if (tileVal == null || !tileVal.isPassable()) return 999999;
        if (tileVal.getPaint().isAlly())  return 0;
        if (tileVal.getPaint() == PaintType.EMPTY) return 1;
        if (tileVal.getPaint().isEnemy()) return 2;
        return 999999;
    }

    // We'll compute a DP value dp[y][x] over all tiles within "vision" steps
    // of origin O, by BFS layering (distance from O).
    // Then we pick a move direction from O that best leads to T if T is in range,
    // otherwise a boundary direction by a simple heuristic.
    //
    // map is a 2D array of tile penalties: map[row][col].
    // origin, target are MapLocations.
    // vision is how far we look in BFS steps (Manhattan or Chebyshev).
    //
    // Returns a Direction (N, NE, E, etc.) or CENTER if none found.
    public static Direction computeMove(MapInfo[][] map, MapLocation origin, MapLocation target, int vision) {
        int h = map.length, w = map[0].length;

        // dist[y][x] => BFS-layer distance from origin
        // dp[y][x]   => DP cost
        int[][] dist = new int[h][w];
        int[][] dp   = new int[h][w];
        for (int y=0; y<h; y++) {
            for (int x=0; x<w; x++) {
                dist[y][x] = 999999;
                dp[y][x]   = 999999;
            }
        }

        // BFS queue as an array of MapLocations (avoid heavy structures).
        MapLocation[] queue = new MapLocation[vision * vision * 8];
        int head=0, tail=0;

        // Initialize BFS
        int ox = origin.x, oy = origin.y;
        dist[oy][ox] = 0;
        dp[oy][ox]   = costOf(map[oy][ox]);
        queue[tail++] = origin; // enqueue origin

        // BFS in ascending distance
        while (head < tail) {
            MapLocation cur = queue[head++];
            int cx = cur.x, cy = cur.y;
            int cd = dist[cy][cx];
            if (cd >= vision) continue; // only compute in vision range

            // For each neighbor
            for (Direction d : MOVES) {
                MapLocation neighbor = cur.add(d);
                int nx = neighbor.x, ny = neighbor.y;
                // bounds
                if (nx < 0 || ny < 0 || nx >= w || ny >= h) continue;

                // if we found a shorter BFS distance, update
                int nd = cd+1;
                if (nd < dist[ny][nx]) {
                    dist[ny][nx] = nd;
                    queue[tail++] = neighbor;
                }

                // DP recurrence: t(L) = cost(L) + min_{neighbor strictly closer to origin} t(neighbor)
                // "strictly closer" => dist[cur] < dist[neighbor]
                // we just set dist[neighbor] = nd, so compare dist[cur] and nd - 1
                if (dist[cy][cx] < dist[ny][nx]) {
                    int candidate = dp[cy][cx] + costOf(map[ny][nx]);
                    if (candidate < dp[ny][nx]) {
                        dp[ny][nx] = candidate;
                    }
                }
            }
        }

        // Now decide where to move from origin:
        int tx = target.x, ty = target.y;
        if (dist[ty][tx] < 999999) {
            // target is in vision => pick neighbor with minimal dp
            int bestCost = 999999;
            Direction bestDir = Direction.CENTER;
            for (Direction d : MOVES) {
                MapLocation n = origin.add(d);
                int nx = n.x, ny = n.y;
                if (nx<0||ny<0||nx>=w||ny>=h) continue;
                int cost = dp[ny][nx];
                if (cost < bestCost) {
                    bestCost = cost;
                    bestDir  = d;
                }
            }
            return bestDir;
        } else {
            // target out of range => pick a boundary direction with some heuristic
            int bestScore = -999999999;
            Direction bestDir = Direction.CENTER;
            int distOT = origin.distanceSquaredTo(target);
            for (Direction d : MOVES) {
                MapLocation n = origin.add(d);
                int nx = n.x, ny = n.y;
                if (nx<0||ny<0||nx>=w||ny>=h) continue;
                if (dp[ny][nx]>=999999) continue;
                int distLT = n.distanceSquaredTo(target);
                int gain = distOT - distLT;
                int score = gain*1000 - dp[ny][nx];
                if (score>bestScore){
                    bestScore=score;
                    bestDir=d;
                }
            }
            return bestDir;
        }
    }
}

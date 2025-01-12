package v05_ALilSmth.Helpers;

import battlecode.common.MapLocation;

public class MapData {
    public boolean[][] mapData;

    public MapLocation[] paintTowers;
    public short towerIndex = 0;

    public MapData(int width, int height) {
        mapData = new boolean[width][height];
        paintTowers = new MapLocation[3]; // arbitrary
    }

    public void setPaintTower(MapLocation towerLoc, MapLocation currLoc) {
        if (towerIndex >= paintTowers.length) {
            int furthestDist = currLoc.distanceSquaredTo(towerLoc);
            int furthestIndex = -1;
            for (int i = 0; i < paintTowers.length; i++) {
                int dist =  currLoc.distanceSquaredTo(paintTowers[i]);
                if (dist > furthestDist) {
                    furthestDist = dist;
                    furthestIndex = i;
                }
            }

            if (furthestIndex != -1) {
                paintTowers[furthestIndex] = towerLoc;
            }
        } else {
            paintTowers[towerIndex] = towerLoc;
            towerIndex++;
        }
    }

    public int containsPaintTowerLocation(MapLocation loc) {
        for (int i = 0; i < towerIndex; i++) {
            if (paintTowers[i].equals(loc)) {
                return i;
            }
        }
        return -1;
    }

    public void removePaintTowerLocation(int index) {
        paintTowers[index] = paintTowers[--towerIndex];
    }

    public MapLocation getNearestPaintTower(MapLocation currLoc) {
        if (towerIndex == 0) {
            return null;
        }
        if (towerIndex == 1) {
            return paintTowers[0];
        }

        int closestDist = currLoc.distanceSquaredTo(paintTowers[0]);
        int closestIndex = 0;
        for (int i = 1; i < towerIndex; i++) {
            int dist = currLoc.distanceSquaredTo(paintTowers[i]);
            if (dist < closestDist) {
                closestDist = dist;
                closestIndex = i;
            }
        }

        return paintTowers[closestIndex];
    }

    public boolean getVisited(int x, int y) {
        return mapData[x][y];
    }

    public void setVisited(int x, int y) {
        mapData[x][y] = true;
    }

    public void setVisited(MapLocation loc) {
        mapData[loc.x][loc.y] = true;
    }

}

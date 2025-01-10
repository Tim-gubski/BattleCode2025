package MoneyMan_v9.Helpers;

import battlecode.common.*;

import java.util.HashSet;
import java.util.Set;

public class MapData {
    public boolean[][] visited;
    public Set<MapLocation> ruins = new HashSet<MapLocation>();
    public Set<RobotInfo> friendlyTowers = new HashSet<RobotInfo>();


    public MapLocation[] paintTowers;
    public short towerIndex = 0;

    public MapData(int width, int height) {
        visited = new boolean[width][height];
        paintTowers = new MapLocation[3]; // arbitrary
    }


    int[] nearbyFriendlyTowersID = new int[10];
    int numNearbyFriendlyTowers = 0;
    public void updateLandmarks(MapLocation[] ruins, RobotInfo[] allies, MapLocation currentLocation){
        // add new stuff
        markRuins(ruins);

        numNearbyFriendlyTowers = 0;
        for(RobotInfo ally : allies){
            if(ally.type.isTowerType()){
                markFriendlyTower(ally);
                nearbyFriendlyTowersID[numNearbyFriendlyTowers++] = ally.ID;
            }
        }

        // remove stuff thats gone
        for(RobotInfo previousTower : friendlyTowers){
            if(currentLocation.distanceSquaredTo(previousTower.location) < GameConstants.VISION_RADIUS_SQUARED){
                boolean stillThere = false;
                for(int i = 0; i < numNearbyFriendlyTowers; i++){
                    if(previousTower.ID == nearbyFriendlyTowersID[i]){
                        stillThere = true;
                        break;
                    }
                }
                if(!stillThere){
                    removeFriendlyTower(previousTower);
                }
            }
        }
    }

    // RUIN STUFF
    public void markRuins(MapLocation[] loc) {
        for (MapLocation l : loc) {
            ruins.add(l);
        }
    }

    public void markRuin(MapLocation loc) {
        ruins.add(loc);
    }

    public boolean isRuins(MapLocation loc) {
        return ruins.contains(loc);
    }


    // FRIENDLY TOWER STUFF
    public void markFriendlyTower(RobotInfo tower) {
        friendlyTowers.add(tower);
    }

    public void markFriendlyTowers(RobotInfo[] towers) {
        for (RobotInfo tower : towers) {
            friendlyTowers.add(tower);
        }
    }

    public void removeFriendlyTower(RobotInfo tower) {
        friendlyTowers.remove(tower);
    }

    public RobotInfo[] getFriendlyTowers(){
        return friendlyTowers.toArray(new RobotInfo[friendlyTowers.size()]);
    }

    public MapLocation[] getPaintTowers(){
        MapLocation[] paintTowers = new MapLocation[friendlyTowers.size()];
        int towerIndex = 0;
        for(RobotInfo tower : friendlyTowers){
            if(tower.type.getBaseType() == UnitType.LEVEL_ONE_PAINT_TOWER){
                paintTowers[towerIndex++] = tower.location;
            }
        }
        return paintTowers;
    }

    // OTHER STUFF
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
        return visited[x][y];
    }

    public void setVisited(int x, int y) {
        visited[x][y] = true;
    }

    public void setVisited(MapLocation loc) {
        visited[loc.x][loc.y] = true;
    }

}

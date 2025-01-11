package MoneyMan_v9.Helpers;

import battlecode.common.*;
import MoneyMan_v9.Helpers.Fast.FastIterableLocSet;
import MoneyMan_v9.Helpers.Fast.FastLocIntMap;

import java.util.HashSet;
import java.util.Set;

public class MapData {
    public boolean[][] visited;
    public MapInfo[][] mapInfos;
    public FastIterableLocSet ruins = new FastIterableLocSet(100);
    public boolean[][] SRPExclusionZone;
    public FastIterableLocSet SRPs = new FastIterableLocSet(100);
    public FastLocIntMap tileColors = new FastLocIntMap();
    public FastSetRobotInfo friendlyTowers = new FastSetRobotInfo(30);

    public short towerIndex = 0;

    public MapData(int width, int height) {
        visited = new boolean[width][height];
        mapInfos = new MapInfo[width][height];
        SRPExclusionZone = new boolean[width][height];

        // exclude the edges of the map for SRPs
        for(int i = width; --i >= 0;){
            SRPExclusionZone[i][0] = true;
            SRPExclusionZone[i][1] = true;
            SRPExclusionZone[i][height - 1] = true;
            SRPExclusionZone[i][height - 2] = true;
        }
        for(int i = height; --i >= 0;){
            SRPExclusionZone[0][i] = true;
            SRPExclusionZone[1][i] = true;
            SRPExclusionZone[width - 1][i] = true;
            SRPExclusionZone[width - 2][i] = true;
        }
    }


    int[] nearbyFriendlyTowersID = new int[10];
    int numNearbyFriendlyTowers = 0;
    public void updateLandmarks(MapLocation[] ruins, UnitType[] ruinTypes, RobotInfo[] allies, MapLocation currentLocation) throws GameActionException{
        // add new stuff
        markRuins(ruins, ruinTypes);

        numNearbyFriendlyTowers = 0;
        for(RobotInfo ally : allies){
            if(ally.type.isTowerType()){
                markFriendlyTower(ally);
                nearbyFriendlyTowersID[numNearbyFriendlyTowers++] = ally.ID;
            }
        }

        // remove stuff thats gone
        for(RobotInfo previousTower : friendlyTowers.getArray()){
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
    public void markRuins(MapLocation[] locs, UnitType[] ruinTypes) throws GameActionException{
        for (int i = locs.length; --i >= 0;) {
            if(ruins.add(locs[i])){
                // mark pattern
                for(int x = -2; x <= 2; x++){
                    for(int y = -2; y <= 2; y++){
                        tileColors.add(locs[i].translate(x, y), determinePaintType(ruinTypes[i], x+2, y+2) == PaintType.ALLY_PRIMARY ? 0 : 1); // 2 offset since -2, -2 -> 0,0;
                    }
                }

                // mark that no srps should be built here
                for (int x = -4; x <= 4; x++) {
                    for (int y = -4; y <= 4; y++) {
                        if(onTheMap(locs[i].x + x, locs[i].y + y))
                        SRPExclusionZone[locs[i].x + x][locs[i].y + y] = true;
                    }
                }
            }
        }
    }

    // x and y are local to 5x5 pattern -> 0,0 is topleft
    public PaintType determinePaintType(UnitType towerType, int x, int y) throws GameActionException{
        // find paint type using bit extraction
        // method: pattern >> (24 - (x + y * 5)) & 1
        if (towerType.getBaseType() == UnitType.LEVEL_ONE_PAINT_TOWER) {
            return (GameConstants.PAINT_TOWER_PATTERN >> (24 - (x + y * 5)) & 1) == 1 ? PaintType.ALLY_SECONDARY : PaintType.ALLY_PRIMARY;
        } else if (towerType.getBaseType() == UnitType.LEVEL_ONE_MONEY_TOWER) {
            return (GameConstants.MONEY_TOWER_PATTERN >> (24 - (x + y * 5)) & 1) == 1 ? PaintType.ALLY_SECONDARY : PaintType.ALLY_PRIMARY;
        }
        return (GameConstants.DEFENSE_TOWER_PATTERN >> (24 - (x + y * 5)) & 1) == 1 ? PaintType.ALLY_SECONDARY : PaintType.ALLY_PRIMARY;
    }

    private boolean onTheMap(int x, int y){
        return x >= 0 && y >= 0 && x < visited.length && y < visited[0].length;
    }

    private boolean onTheMap(MapLocation loc){
        return onTheMap(loc.x, loc.y);
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
        return friendlyTowers.getArray();
    }

    public MapLocation[] getPaintTowers(){
        MapLocation[] paintTowers = new MapLocation[friendlyTowers.size()];
        int towerIndex = 0;
        for(RobotInfo tower : friendlyTowers.getArray()){
            if(tower.type.getBaseType() == UnitType.LEVEL_ONE_PAINT_TOWER){
                paintTowers[towerIndex++] = tower.location;
            }
        }
        return paintTowers;
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

    public void setMapInfo(MapInfo info) {
        mapInfos[info.getMapLocation().x][info.getMapLocation().y] = info;
    }

    public void setMapInfos(MapInfo[] infos, PaintType SRP_COLOR) {
        for (MapInfo info : infos) {
            mapInfos[info.getMapLocation().x][info.getMapLocation().y] = info;
            visited[info.getMapLocation().x][info.getMapLocation().y] = true;
            if(info.getMark() == SRP_COLOR){
                if(!SRPs.contains(info.getMapLocation())) {
                    markSRP(info.getMapLocation());
                }
            }
        }
    }

    int[][] exlusionOffsets = new int[][]{
            {-2, 3}, {0, 3}, {2, 3},
            {-2, 4}, {-1, 4}, {1, 4}, {2, 4},
            {-2, -3}, {0, -3}, {2, -3},
            {-2, -4}, {-1, -4}, {1, -4}, {2, -4},
            {3, -2}, {3, 0}, {3, 2},
            {4, -2}, {4, -1}, {4, 1}, {4, 2},
            {-3, -2}, {-3, 0}, {-3, 2},
            {-4, -2}, {-4, -1}, {-4, 1}, {-4, 2},
    };
    private void markSRP(MapLocation loc){
        SRPs.add(loc);
        for (int x = -2; x <= 2; x++) {
            if(loc.x + x >= SRPExclusionZone.length || loc.x + x < 0) continue;
            for (int y = -2; y <= 2; y++) {
                if (loc.y + y >= SRPExclusionZone[0].length || loc.y + y < 0) continue;
                // resource pattern hard coded
                MapLocation newLoc = loc.translate(x, y);
                SRPExclusionZone[loc.x + x][loc.y + y] = true;
                if (x == 0 && y == 0) {
                    tileColors.add(newLoc, 0);
                    SRPExclusionZone[newLoc.x][newLoc.y] = false;
                }else if((x + y) % 2 == 0){
                    tileColors.add(newLoc, 1);
                }else{
                    tileColors.add(newLoc, 0);
                }

            }
        }
        for(int x = exlusionOffsets.length; --x >= 0;){
            if(loc.x + exlusionOffsets[x][0] >= SRPExclusionZone.length || loc.x + exlusionOffsets[x][0] < 0) continue;
            if(loc.y + exlusionOffsets[x][1] >= SRPExclusionZone[0].length || loc.y + exlusionOffsets[x][1] < 0) continue;
            SRPExclusionZone[loc.x + exlusionOffsets[x][0]][loc.y + exlusionOffsets[x][1]] = true;
        }
    }

    public MapInfo getMapInfo(MapLocation loc) {
        return mapInfos[loc.x][loc.y];
    }

}

package SuperiorCowPowers_v11.Helpers;

import SuperiorCowPowers_v11.Helpers.Fast.FastLocIntMap;
import battlecode.common.*;
import SuperiorCowPowers_v11.Helpers.Fast.FastIterableLocSet;
import SuperiorCowPowers_v11.Helpers.KDTree;

import java.sql.SQLOutput;
import java.util.Arrays;

public class MapData {
    public RobotController rc;
    public int width;
    public int height;
    public MapInfo[][] mapInfos;
    public FastIterableLocSet ruins = new FastIterableLocSet(100);
    public boolean[][] SRPExclusionZone;
    public int[][] SRPExclusionZoneInt;
    public FastIterableLocSet SRPs = new FastIterableLocSet(100);
    public int[][] tileColors; // 0 is undefined, 1 is primary, 2 is secondary
    public FastSetRobotInfo friendlyTowers = new FastSetRobotInfo(30);

//    StringBuilder SRPExclusionZone2;

    public short towerIndex = 0;

    public MapData(RobotController rc, int width, int height) {
        this.rc = rc;
        this.width = width;
        this.height = height;
        mapInfos = new MapInfo[width][height];
        SRPExclusionZone = new boolean[width][height];
        SRPExclusionZoneInt = new int[width][height];
        tileColors = new int[width][height];

//        SRPExclusionZone2 = new StringBuilder("0".repeat(width*height));

        // exclude the edges of the map for SRPs
//        for(int i = width;  --i >= 0;)  SRPExclusionZone[i][0] = SRPExclusionZone[i][1] = SRPExclusionZone[i][height - 1] = SRPExclusionZone[i][height - 2] = true;
//        for(int i = height; --i >= 0;)  SRPExclusionZone[0][i] = SRPExclusionZone[1][i] = SRPExclusionZone[width - 1][i] = SRPExclusionZone[width - 2][i] = true;
        for(int i = width;  --i >= 0;)  SRPExclusionZoneInt[i][0] = SRPExclusionZoneInt[i][1] = SRPExclusionZoneInt[i][height - 1] = SRPExclusionZoneInt[i][height - 2] = 1;
        for(int i = height; --i >= 0;)  SRPExclusionZoneInt[0][i] = SRPExclusionZoneInt[1][i] = SRPExclusionZoneInt[width - 1][i] = SRPExclusionZoneInt[width - 2][i] = 1;
    }


    int[] nearbyFriendlyTowersID = new int[10];
    int numNearbyFriendlyTowers = 0;
    public void updateLandmarks(MapLocation[] ruins, UnitType[] ruinTypes, RobotInfo[] allies, MapLocation currentLocation) throws GameActionException{
        // add new stuff
        markRuins(ruins, ruinTypes); // this shit uses crazy bytecode

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
    boolean[] bigFillRow = new boolean[]{true, true, true, true, true, true, true, true, true};
    public void markRuins(MapLocation[] locs, UnitType[] ruinTypes) throws GameActionException{
        for (int i = locs.length; --i >= 0;) {
            MapLocation loc = locs[i];
            if(ruins.add(loc)){ // only mark and exclude if tower not finished
                if(!rc.canSenseRobotAtLocation(loc)) {
                    // mark pattern
                    int[][] pattern = determinePaintType(ruinTypes[i]);
                    for (int x = -2; x <= 2; x++) {
                        System.arraycopy(pattern[x + 2], 0, tileColors[loc.x + x], loc.y - 2, 5);
                    }
                }

                // mark that no srps should be built here
//                int newX = 0;
//                int startY = 0;
//                int endY = 0;
//                for (int x = -4; x <= 4; x++) {
//                    newX = loc.x + x;
//                    if(newX >= width || newX < 0) continue;
//                    //array coppy with bounds checking
//                    startY = Math.max(0, loc.y - 4);
//                    endY = Math.min(height, loc.y + 5);
//                    System.arraycopy(bigFillRow, 0, SRPExclusionZone[newX], startY, endY - startY);
//                }
                int newX, newY = 0;
                for(int x = -4; x <= 4; x++){
                    newX = loc.x + x;
                    if(newX >= width || newX < 0) continue;
                    for(int y = -4; y <= 4; y++){
                        newY = loc.y + y;
                        if(newY >= height || newY < 0) continue;
                        SRPExclusionZoneInt[newX][newY]++;
                    }
                }
            }
        }
    }

    // manual patterns, replace with get tower patterns later
    // binary = 1000101010001000101010001
    int[][] paintTowerPattern = new int[][]{
            {2, 1, 1, 1, 2},
            {1, 2, 1, 2, 1},
            {1, 1, 2, 1, 1},
            {1, 2, 1, 2, 1},
            {2, 1, 1, 1, 2},
    };

    // binary = 0111011011100011101101110
    int[][] moneyTowerPattern = new int[][]{
            {1, 2, 2, 2, 1},
            {2, 2, 1, 2, 2},
            {2, 1, 1, 1, 2},
            {2, 2, 1, 2, 2},
            {1, 2, 2, 2, 1},
    };

    // binary = 10001110111110111000100;
    int[][] defenseTowerPattern = new int[][]{
            {2, 1, 1, 1, 2},
            {1, 2, 2, 2, 1},
            {1, 2, 2, 2, 1},
            {1, 2, 2, 2, 1},
            {2, 1, 1, 1, 2},
    };

    // x and y are local to 5x5 pattern -> 0,0 is topleft
    public int[][] determinePaintType(UnitType towerType) throws GameActionException{
        // find paint type using bit extraction
        // method: pattern >> (24 - (x + y * 5)) & 1
        if (towerType.getBaseType() == UnitType.LEVEL_ONE_PAINT_TOWER) {
            return paintTowerPattern;
        } else if (towerType.getBaseType() == UnitType.LEVEL_ONE_MONEY_TOWER) {
            return moneyTowerPattern;
        }
        return defenseTowerPattern;
    }


    public void markRuin(MapLocation loc) {
        ruins.add(loc);
    }

    public boolean isRuins(MapLocation loc) {
        return ruins.contains(loc);
    }


    // FRIENDLY TOWER STUFF
    public void markFriendlyTower(RobotInfo tower) {
        if(friendlyTowers.add(tower)) {
            // remove exclusion zone around it
            int newX, newY = 0;
            for (int x = -4; x <= 4; x++) {
                newX = tower.location.x + x;
                if (newX >= width || newX < 0) continue;
                for (int y = -4; y <= 4; y++) {
                    newY = tower.location.y + y;
                    if (newY >= height || newY < 0) continue;
                    SRPExclusionZoneInt[newX][newY]--;
                }
            }
        }
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

    // can probably optimize this hella seems like storing the mapinfo objects takes a bunch of bytecode
    public void setMapInfos(MapInfo[] infos, PaintType SRP_COLOR) {
        if(infos.length == 0) return;
//        int x = infos[0].getMapLocation().x;
//        int lineStart = 0;
//        int minY = infos[0].getMapLocation().y;
        MapInfo info;


        for (int i = infos.length; --i >= 0;) {
            info = infos[i];
//            if(info.getMapLocation().x != x || i == infos.length - 1){
//                System.arraycopy(infos, lineStart, mapInfos[x], minY, (i == infos.length - 1) ? (i - lineStart + 1) : (i - lineStart));
//                lineStart = i;
//                x = info.getMapLocation().x;
//                minY = info.getMapLocation().y;
//            }

            // do we actually need this or can we just sense each turn.
            mapInfos[info.getMapLocation().x][info.getMapLocation().y] = info; // 12 bytecode just to set a map info

            if(info.getMark() == SRP_COLOR){
                if(!SRPs.contains(info.getMapLocation())) {
                    markSRP(info.getMapLocation());
                }
            }
        }

    }

    int[][] exclusionOffsets = new int[][]{
            {-2, 3}, {0, 3}, {2, 3},
            {-2, 4}, {-1, 4}, {1, 4}, {2, 4},
            {-2, -3}, {0, -3}, {2, -3},
            {-2, -4}, {-1, -4}, {1, -4}, {2, -4},
            {3, -2}, {3, 0}, {3, 2},
            {4, -2}, {4, -1}, {4, 1}, {4, 2},
            {-3, -2}, {-3, 0}, {-3, 2},
            {-4, -2}, {-4, -1}, {-4, 1}, {-4, 2},
    };
    // binary =1010101010100010101010101
    int[][] resourcePattern = new int[][]{
            {2, 1, 2, 1, 2},
            {1, 2, 1, 2, 1},
            {2, 1, 1, 1, 2},
            {1, 2, 1, 2, 1},
            {2, 1, 2, 1, 2},
    };
    boolean[] fillRow = new boolean[]{true, true, true, true, true};
    private void markSRP(MapLocation loc){ // uses 3k bytecode
        SRPs.add(loc);
        int newX = 0;
        int newY = 0;
        // this is weird to think about because map is [x][y] but x selects the row, freaky
        for(int x = -2; x <= 2; x++){
            newX = loc.x + x;
            newY = loc.y - 2;
//            System.arraycopy(fillRow, 0, SRPExclusionZone[newX], newY, 5);
            System.arraycopy(resourcePattern[x + 2], 0, tileColors[newX], newY, 5);
        }
        // no need to check bounds, srp already valid location
        for(int x = -2; x <= 2; x++) {
            for (int y = -2; y <= 2; y++) {
                newX = loc.x + x;
                newY = loc.y + y;
                SRPExclusionZoneInt[newX][newY]++;
            }
        }
        SRPExclusionZoneInt[loc.x][loc.y]--;

        for(int x = exclusionOffsets.length; --x >= 0;){
            newX = loc.x + exclusionOffsets[x][0];
            newY = loc.y + exclusionOffsets[x][1];
            if(newX >= SRPExclusionZoneInt.length || newX < 0) continue;
            if(newY >= SRPExclusionZoneInt[0].length || newY < 0) continue;
            SRPExclusionZoneInt[newX][newY]++;
        }


//        SRPExclusionZone[loc.x][loc.y] = false;
//
//
//        // still need bounds checking here
//        for(int x = exclusionOffsets.length; --x >= 0;){
//            newX = loc.x + exclusionOffsets[x][0];
//            newY = loc.y + exclusionOffsets[x][1];
//            if(newX >= SRPExclusionZone.length || newX < 0) continue;
//            if(newY >= SRPExclusionZone[0].length || newY < 0) continue;
//            SRPExclusionZone[newX][newY] = true;
//        }
    }

    public MapInfo getMapInfo(MapLocation loc) {
        return mapInfos[loc.x][loc.y];
    }

}

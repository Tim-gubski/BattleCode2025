package MoneyMan_v9.Helpers;

import battlecode.common.*;
import MoneyMan_v9.Helpers.Fast.FastIterableLocSet;
import MoneyMan_v9.Helpers.Fast.FastLocIntMap;

import java.util.HashSet;
import java.util.Set;

public class MapData {
    public RobotController rc;
    public int width;
    public int height;
    public MapInfo[][] mapInfos;
    public FastIterableLocSet ruins = new FastIterableLocSet(100);
    public boolean[][] SRPExclusionZone;
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
        tileColors = new int[width][height];

//        SRPExclusionZone2 = new StringBuilder("0".repeat(width*height));

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
    public void markRuins(MapLocation[] locs, UnitType[] ruinTypes) throws GameActionException{
        for (int i = locs.length; --i >= 0;) {
            MapLocation loc = locs[i];
            if(ruins.add(loc)){
                // mark pattern
                boolean[][] pattern = determinePaintType(ruinTypes[i]);
                for(int x = -2; x <= 2; x++){
                    for(int y = -2; y <= 2; y++){
                        // this costs 83 bytecode, and we're doing it 25 times, 48 bytecode now, 25 now
                        tileColors[loc.x + x][loc.y + y] = pattern[x + 2][y + 2] ? 2 : 1; // 2 offset since -2, -2 -> 0,0;
                    }
                }

                // mark that no srps should be built here
                for (int x = -4; x <= 4; x++) {
                    for (int y = -4; y <= 4; y++) {
                        // costs 51 bytecode for some reason, 43 now, 39 now, 23 now
                        MapLocation newLoc = loc.translate(x, y);
                        if(rc.onTheMap(newLoc)) {
                            SRPExclusionZone[newLoc.x][newLoc.y] = true; // 18 bytecode
//                            SRPExclusionZone2.setCharAt((loc.y + y) * width + loc.x + x, '1'); // 16bytecode
                        }
                    }
                }
            }

        }
    }

    // manual patterns
    // binary = 1000101010001000101010001
    boolean[][] paintTowerPattern = new boolean[][]{
            {true, false, false, false, true},
            {false, true, false, true, false},
            {false, false, true, false, false},
            {false, true, false, true, false},
            {true, false, false, false, true},
    };

    // binary = 0111011011100011101101110
    boolean[][] moneyTowerPattern = new boolean[][]{
            {false, true, true, true, false},
            {true, true, false, true, true},
            {true, false, false, false, true},
            {true, true, false, true, true},
            {false, true, true, true, false},
    };

    // binary = 10001110111110111000100;
    boolean[][] defenseTowerPattern = new boolean[][]{
            {true, false, false, false, true},
            {false, true, true, true, false},
            {false, true, true, true, false},
            {false, true, true, true, false},
            {true, false, false, false, true},
    };

    // x and y are local to 5x5 pattern -> 0,0 is topleft
    public boolean[][] determinePaintType(UnitType towerType) throws GameActionException{
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

    // can probably optimize this hella seems like storing the mapinfo objects takes a bunch of bytecode
    public void setMapInfos(MapInfo[] infos, PaintType SRP_COLOR) {
        for (MapInfo info : infos) {
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
    boolean[][] resourcePattern = new boolean[][]{
            {true, false, true, false, true},
            {false, true, false, true, false},
            {true, false, false, false, true},
            {false, true, false, true, false},
            {true, false, true, false, true},
    };
    private void markSRP(MapLocation loc){ // uses 3k bytecode
        int bytecode = Clock.getBytecodeNum();
        SRPs.add(loc);
        int newX = 0;
        int newY = 0;
        for (int x = -2; x <= 2; x++) {
            newX = loc.x + x;
            if(newX >= width || newX < 0) continue;
            for (int y = -2; y <= 2; y++) {
                newY = loc.y + y;
                if (newY >= height || newY < 0) continue;
                // resource pattern hard coded
                SRPExclusionZone[newX][newY] = !(x == 0 && y == 0);
                tileColors[newX][newY] = resourcePattern[x + 2][y + 2] ? 2 : 1;
            }
        }
        for(int x = exclusionOffsets.length; --x >= 0;){
            newX = loc.x + exclusionOffsets[x][0];
            newY = loc.y + exclusionOffsets[x][1];
            if(newX >= SRPExclusionZone.length || newX < 0) continue;
            if(newY >= SRPExclusionZone[0].length || newY < 0) continue;
            SRPExclusionZone[newX][newY] = true;
        }
        System.out.println("markSRP bytecode: " + (Clock.getBytecodeNum() - bytecode));

    }

    public MapInfo getMapInfo(MapLocation loc) {
        return mapInfos[loc.x][loc.y];
    }

}

package v57_Optimaler.Helpers;

import battlecode.common.*;
import v57_Optimaler.Helpers.Fast.FastIterableLocSet;
import v57_Optimaler.Robot;

import java.util.HashMap;
import java.util.Map;

public class MapData {
    public RobotController rc;
    public Robot robot;
    public int width;
    public int height;
    public MapInfo[][] mapInfos;
    public FastIterableLocSet ruins = new FastIterableLocSet(100);
    public Map<MapLocation, Integer> visitedRuins = new HashMap<>();
    public boolean[][] SRPExclusionZone;
    public int[][] SRPExclusionZoneInt;
    public FastIterableLocSet SRPs = new FastIterableLocSet(100);
    public int[][] tileColors; // 0 is undefined, 1 is primary, 2 is secondary
    public int[][] adjacentAllies;
    public FastSetRobotInfo friendlyTowers = new FastSetRobotInfo(30);
    public FastSetRobotInfo enemyDefenseTowers = new FastSetRobotInfo(30);
    public enum symType {NONE, VERTICAL, HORIZONTAL, DIAGONAL};
    public symType[] symPossibilities = new symType[]{symType.VERTICAL, symType.HORIZONTAL, symType.DIAGONAL};
    public symType confirmedSym = symType.NONE;
    public short towerIndex = 0;

    public MapData(RobotController rc, int width, int height, Robot robot) {
        this.rc = rc;
        this.robot = robot;
        this.width = width;
        this.height = height;
        mapInfos = new MapInfo[width][height];
        adjacentAllies = new int[11][11];
        if(rc.getType() == UnitType.SOLDIER) {
            SRPExclusionZoneInt = new int[width][height];
            tileColors = new int[width][height];
            // exclude the edges of the map for SRPs
            int h1 = height - 1;
            int w1 = width - 1;
            int h2 = height - 2;
            int w2 = width - 2;
            for(int i = width;  --i >= 0;)  SRPExclusionZoneInt[i][0] = SRPExclusionZoneInt[i][1] = SRPExclusionZoneInt[i][h1] = SRPExclusionZoneInt[i][h2] = 1;
            for(int i = height; --i >= 0;)  SRPExclusionZoneInt[0][i] = SRPExclusionZoneInt[1][i] = SRPExclusionZoneInt[w1][i] = SRPExclusionZoneInt[w2][i] = 1;
        }
    }

    MapLocation adjStartLoc = new MapLocation(0, 0);
    public void updateAdjacentAllies(RobotInfo[] allies){
        adjacentAllies = new int[11][11];
        int relX = 0;
        int relY = 0;
        adjStartLoc = rc.getLocation();
        for(RobotInfo ally : allies){
            relX = ally.location.x - adjStartLoc.x + 5;
            relY = ally.location.y - adjStartLoc.y + 5;
            if(relX > 0) adjacentAllies[relX - 1][relY]++;
            if(relX < width - 1) adjacentAllies[relX + 1][relY]++;
            if(relY > 0) adjacentAllies[relX][relY - 1]++;
            if(relY < height - 1) adjacentAllies[relX][relY + 1]++;
            if(relX > 0 && relY > 0) adjacentAllies[relX - 1][relY - 1]++;
            if(relX > 0 && relY < height - 1) adjacentAllies[relX - 1][relY + 1]++;
            if(relX < width - 1 && relY > 0) adjacentAllies[relX + 1][relY - 1]++;
            if(relX < width - 1 && relY < height - 1) adjacentAllies[relX + 1][relY + 1]++;
        }
    }

    public int getAdjacentAllies(MapLocation loc) throws GameActionException{
        return adjacentAllies[loc.x - adjStartLoc.x + 5][loc.y - adjStartLoc.y + 5];
//        return adjacentAllies[loc.x][loc.y];
    }

    RobotInfo[] nearbyFriendlyTowers = new RobotInfo[10];
    int numNearbyFriendlyTowers = 0;
    public void updateLandmarks(MapLocation[] ruins, RobotInfo[] allies, RobotInfo[] enemies, MapLocation currentLocation) throws GameActionException{
        // add new stuff
        markRuins(ruins); // this shit uses crazy bytecode

        for(RobotInfo enemy : enemies){
            if(enemy.type.getBaseType() == UnitType.LEVEL_ONE_DEFENSE_TOWER){
                enemyDefenseTowers.add(enemy);
            }
        }

        numNearbyFriendlyTowers = 0;
        for(RobotInfo ally : allies){
            if(ally.type.isTowerType()){
                markFriendlyTower(ally);
                nearbyFriendlyTowers[numNearbyFriendlyTowers++] = ally;
            }
        }

        // remove stuff thats gone
        for(RobotInfo previousTower : friendlyTowers.getArray()){
            if(currentLocation.distanceSquaredTo(previousTower.location) < GameConstants.VISION_RADIUS_SQUARED){
                boolean stillThere = false;
                for(int i = 0; i < numNearbyFriendlyTowers; i++){
//                    rc.setIndicatorLine(rc.getLocation(), nearbyFriendlyTowers[i].location, 0, 0, 255);
                    if(previousTower.getLocation().equals(nearbyFriendlyTowers[i].location) && previousTower.getType().getBaseType() == nearbyFriendlyTowers[i].type.getBaseType()){
                        stillThere = true;
                        break;
                    }
                }
                if(!stillThere){
                    rc.setIndicatorLine(rc.getLocation(), previousTower.location, 255, 0, 0);
                    removeFriendlyTower(previousTower);
                }
            }
        }
    }

    public void markTowerPattern(MapLocation loc, UnitType type) throws GameActionException{
        int[][] pattern = determinePaintType(type);
        for (int x = -2; x <= 2; x++) {
            System.arraycopy(pattern[x + 2], 0, tileColors[loc.x + x], loc.y - 2, 5);
        }
    }

    public void markResourcePattern(MapLocation loc) throws GameActionException{
        int newX, newY;
        for(int x = -2; x <= 2; x++){
            newX = loc.x + x;
            newY = loc.y - 2;
            System.arraycopy(resourcePattern[x + 2], 0, tileColors[newX], newY, 5);
        }
    }

    // RUIN STUFF
    public void markRuins(MapLocation[] locs) throws GameActionException{
        for (int i = locs.length; --i >= 0;) {
            markRuin(locs[i], true, true);
        }
    }

    public void markRuin(MapLocation loc, boolean exclusionZone, boolean checkSyms) throws GameActionException{
        if(ruins.add(loc)){
            // check sym stuff
//            if(confirmedSym == symType.NONE){
//                for(symType sym : symPossibilities){
//                    if(ruins.contains(symLoc(loc, sym))){
//                        confirmedSym = sym;
//                        break;
//                    }
//                }
//            }
//            if(confirmedSym != symType.NONE && checkSyms){
//                markRuin(symLoc(loc, confirmedSym), exclusionZone, false);
//            }

            if(exclusionZone && rc.getType() == UnitType.SOLDIER) {
                int newX, newY = 0;
                for (int x = -4; x <= 4; x++) {
                    newX = loc.x + x;
                    if (newX >= width || newX < 0) continue;
                    for (int y = -4; y <= 4; y++) {
                        newY = loc.y + y;
                        if (newY >= height || newY < 0) continue;
                        SRPExclusionZoneInt[newX][newY]++;
                    }
                }
            }
        }
    }

    public MapLocation symLoc(MapLocation loc, symType type){
        if(type == symType.VERTICAL){
            return new MapLocation(loc.x, height - loc.y - 1);
        }else if(type == symType.HORIZONTAL){
            return new MapLocation(width - loc.x - 1, loc.y);
        }else if(type == symType.DIAGONAL){
            return new MapLocation(height - loc.y - 1, width - loc.x - 1);
        }
        return null;
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

    // binary = 00100 01110 11111 01110 00100;
    int[][] defenseTowerPattern = new int[][]{
            {1, 1, 2, 1, 1},
            {1, 2, 2, 2, 1},
            {2, 2, 2, 2, 2},
            {1, 2, 2, 2, 1},
            {1, 1, 2, 1, 1},
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


    // FRIENDLY TOWER STUFF
    public void markFriendlyTower(RobotInfo tower) throws GameActionException{
        if(friendlyTowers.add(tower)) {
            if(!ruins.contains(tower.location)){
                rc.setIndicatorLine(rc.getLocation(), tower.location, 255, 0, 255);
                markRuin(tower.location, false, true);
                return;
            }
            rc.setIndicatorLine(rc.getLocation(), tower.location, 0, 255, 0);
            // remove exclusion zone around it, ignore if we didnt have the ruin for this registered
            if(rc.getType() == UnitType.SOLDIER) {
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
    }

    public void markFriendlyTowers(RobotInfo[] towers) {
        for (RobotInfo tower : towers) {
            friendlyTowers.add(tower);
        }
    }

    public void removeFriendlyTower(RobotInfo tower) {
        friendlyTowers.remove(tower);
        if(rc.getType() == UnitType.SOLDIER) {
            MapLocation loc = tower.location;
            int newX, newY = 0;
            for (int x = -4; x <= 4; x++) {
                newX = loc.x + x;
                if (newX >= width || newX < 0) continue;
                for (int y = -4; y <= 4; y++) {
                    newY = loc.y + y;
                    if (newY >= height || newY < 0) continue;
                    SRPExclusionZoneInt[newX][newY]++;
                }
            }
        }

    }

    public RobotInfo[] getFriendlyTowers(){
        return friendlyTowers.getArray();
    }

    public MapLocation[] getPaintTowers(){
        MapLocation[] paintTowers = new MapLocation[friendlyTowers.paintTowers];
        int towerIndex = 0;
        for(RobotInfo tower : friendlyTowers.getArray()){
            if(tower.type.getBaseType() == UnitType.LEVEL_ONE_PAINT_TOWER){
                paintTowers[towerIndex++] = tower.location;
            }
        }
        return paintTowers;
    }

    // can probably optimize this hella seems like storing the mapinfo objects takes a bunch of bytecode
    public void setMapInfos(MapInfo[] infos, PaintType SRP_COLOR, PaintType TOWER_MARKER_COLOR) throws GameActionException{
        if(infos.length == 0) return;

        MapInfo info;
        MapLocation infoLoc;
        if(rc.getType() != UnitType.SOLDIER) {
            for (int i = infos.length; --i >= 0;) {
                infoLoc = infos[i].getMapLocation();
                mapInfos[infoLoc.x][infoLoc.y] = infos[i];
            }
        }else {
            for (int i = infos.length; --i >= 0; ) {
                info = infos[i];
                infoLoc = info.getMapLocation();

                // do we actually need this or can we just sense each turn.
                mapInfos[infoLoc.x][infoLoc.y] = info; // 12 bytecode just to set a map info

                if (info.getMark() == SRP_COLOR) {
                    if (!SRPs.contains(info.getMapLocation())) {
                        markSRP(info.getMapLocation());
                    }
                } else if (info.getMark() == TOWER_MARKER_COLOR) {
                    Direction ruinDir = null;

                    if (rc.canSenseLocation(infoLoc.add(Direction.SOUTH)) && rc.senseMapInfo(infoLoc.add(Direction.SOUTH)).hasRuin()) {
                        ruinDir = Direction.SOUTH;
                    } else if (rc.canSenseLocation(infoLoc.add(Direction.SOUTHWEST)) && rc.senseMapInfo(infoLoc.add(Direction.SOUTHWEST)).hasRuin()) {
                        ruinDir = Direction.SOUTHWEST;
                    } else if (rc.canSenseLocation(infoLoc.add(Direction.SOUTHEAST)) && rc.senseMapInfo(infoLoc.add(Direction.SOUTHEAST)).hasRuin()) {
                        ruinDir = Direction.SOUTHEAST;
                    }
                    if (ruinDir != null && !rc.canSenseRobotAtLocation(infoLoc.add(ruinDir))) {
                        markTowerPattern(infoLoc.add(ruinDir), robot.markDirectionToTowerType(ruinDir.opposite()));
                    }
                }
            }
        }
    }

    int[][] exclusionOffsets = new int[][]{
            {-4, -2}, {-4, -1}, {-4, 1}, {-4, 2},
            {-3, -3}, {-3, -2}, {-3, -1}, {-3, 0}, {-3, 1}, {-3, 2}, {-3, 3},
            {-2, -4}, {-2, -3}, {-2, 3}, {-2, 4},
            {-1, -4}, {-1, -3}, {-1, 3}, {-1, 4},
            {0, -3}, {0, 3},
            {1, -4}, {1, -3}, {1, 3}, {1, 4},
            {2, -4}, {2, -3}, {2, 3}, {2, 4},
            {3, -3}, {3, -2}, {3, -1}, {3, 0}, {3, 1}, {3, 2}, {3, 3},
            {4, -2}, {4, -1}, {4, 1}, {4, 2},
    };

    public int[][] resourcePattern = new int[][]{
            {2, 2, 1, 2, 2},
            {2, 1, 1, 1, 2},
            {1, 1, 2, 1, 1},
            {2, 1, 1, 1, 2},
            {2, 2, 1, 2, 2},
    };
    boolean[] fillRow = new boolean[]{true, true, true, true, true};
    private void markSRP(MapLocation loc) throws GameActionException{ // uses 3k bytecode
        SRPs.add(loc);
        if(rc.getType() != UnitType.SOLDIER) return; // only soldiers
        markResourcePattern(loc);
        int newX = 0;
        int newY = 0;
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

package v57_Optimaler;

import v57_Optimaler.Helpers.Explore;
import v57_Optimaler.Helpers.MapData;
import v57_Optimaler.Util.Symmetry;
import v57_Optimaler.Util.Comms;
import battlecode.common.*;

import java.util.*;

abstract public class Robot {

    static public final Direction[] directions = {
            Direction.NORTH,
            Direction.NORTHEAST,
            Direction.EAST,
            Direction.SOUTHEAST,
            Direction.SOUTH,
            Direction.SOUTHWEST,
            Direction.WEST,
            Direction.NORTHWEST,
    };

    static public final UnitType[] towerTypes = {
            UnitType.LEVEL_ONE_PAINT_TOWER,
            UnitType.LEVEL_ONE_MONEY_TOWER,
            UnitType.LEVEL_ONE_DEFENSE_TOWER,
    };

    static public final UnitType[] robotTypes = {
            UnitType.SOLDIER,
            UnitType.MOPPER,
            UnitType.SPLASHER
    };

    public RobotController rc;
    public int width;
    public int height;
    public MapLocation spawnLoc;
    public int spawnTurn;
    public StringBuilder debugString = new StringBuilder();

    public Comms communication;
    public MapLocation returnLoc = null;
    public Symmetry symmetry;

    public MapData mapData;
    public Explore explorer;
    public MapLocation needMoppers;

    public boolean RIGHT;

    /**
     * A random number generator.
     * We will use this RNG to make some random moves. The Random class is provided by the java.util.Random
     * import at the top of this file. Here, we *seed* the RNG with a constant number (6147); this makes sure
     * we get the same sequence of numbers every time this code is run. This is very useful for debugging!
     */
    static public Random rng;

    public Robot(RobotController robot) throws GameActionException {
        rc = robot;
        if(rc.getType() == UnitType.SOLDIER
                && rc.senseMapInfo(rc.getLocation()).getPaint() == PaintType.EMPTY
                && rc.canAttack(rc.getLocation())){
            rc.attack(rc.getLocation());
        }
        width = rc.getMapWidth();
        height = rc.getMapHeight();
        spawnLoc = rc.getLocation();
        spawnTurn = rc.getRoundNum();
        rng = new Random(rc.getID());
        communication = new Comms(rc, this);
        mapData = new MapData(robot, width, height, this);
        if(!rc.getType().isTowerType()) {
            explorer = new Explore(rc, width, height, mapData);
        }
        prevChip = 0;
        RIGHT = rc.getID() % 2 == 0;
    }

    abstract public void turn() throws Exception;

    public void run() {
        while (true) {
            try {
//                debugString = new StringBuilder();
                turn();
//                rc.setIndicatorString(debugString.toString());
            } catch (GameActionException e) {
                System.out.println("GameActionException");
                e.printStackTrace();
            }catch (Exception e) {
                System.out.println("Exception");
                e.printStackTrace();
            }finally {
                Clock.yield();
            }
        }
    }

    public static boolean isPaintTower(UnitType type) throws GameActionException {
        return type == UnitType.LEVEL_ONE_PAINT_TOWER || type == UnitType.LEVEL_TWO_PAINT_TOWER || type == UnitType.LEVEL_THREE_PAINT_TOWER;
    }

    public static boolean isMoneyTower(UnitType type) throws GameActionException {
        return type == UnitType.LEVEL_ONE_MONEY_TOWER || type == UnitType.LEVEL_TWO_MONEY_TOWER || type == UnitType.LEVEL_THREE_MONEY_TOWER;
    }

    public static boolean isDefenseTower (UnitType type) throws GameActionException {
        return type == UnitType.LEVEL_ONE_DEFENSE_TOWER || type == UnitType.LEVEL_TWO_DEFENSE_TOWER || type == UnitType.LEVEL_THREE_DEFENSE_TOWER;
    }

    public static boolean isEnemyPaint (PaintType paint) throws  GameActionException {
//        return paint == PaintType.ENEMY_PRIMARY || paint == PaintType.ENEMY_SECONDARY;
        return paint.isEnemy();
    }

    public boolean reachableFrom(MapLocation loc, MapLocation target) throws GameActionException {
        MapInfo targetInfo = mapData.mapInfos[target.x][target.y];
        if (targetInfo == null || targetInfo.isWall()) {
            return false;
        }
        MapLocation checkLoc = loc;
        while(!checkLoc.equals(target)){
//            if(!rc.onTheMap(checkLoc) || rc.senseMapInfo(checkLoc).isWall()){
//                return false;
//            }
            MapInfo info = mapData.mapInfos[checkLoc.x][checkLoc.y];
            if(!rc.onTheMap(checkLoc) || (info == null || mapData.mapInfos[checkLoc.x][checkLoc.y].isWall())){
                return false;
            }
            checkLoc = checkLoc.add(checkLoc.directionTo(target));
        }
        return true;
    }

    public static void updateEnemyRobots(RobotController rc) throws GameActionException{
        // Sensing methods can be passed in a radius of -1 to automatically
        // use the largest possible value.
        RobotInfo[] enemyRobots = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
        if (enemyRobots.length != 0){
            rc.setIndicatorString("There are nearby enemy robots! Scary!");
            // Save an array of locations with enemy robots in them for possible future use.
            MapLocation[] enemyLocations = new MapLocation[enemyRobots.length];
            for (int i = 0; i < enemyRobots.length; i++){
                enemyLocations[i] = enemyRobots[i].getLocation();
            }
            RobotInfo[] allyRobots = rc.senseNearbyRobots(-1, rc.getTeam());
            // Occasionally try to tell nearby allies how many enemy robots we see.
            if (rc.getRoundNum() % 20 == 0){
                for (RobotInfo ally : allyRobots){
                    if (rc.canSendMessage(ally.location, enemyRobots.length)){
                        rc.sendMessage(ally.location, enemyRobots.length);
                    }
                }
            }
        }
    }

    public static <T> void shuffleArray(T[] array) {
        List<T> list = Arrays.asList(array);
        Collections.shuffle(list);
        list.toArray(array); // This step is optional as the original array will already be modified.
    }

    // TOWER PATTERN STUFF //
    public UnitType randomTowerType() throws GameActionException{
        return towerTypes[(int)(Math.random()*towerTypes.length)];
    }

    public boolean tryMarkTower(UnitType type, MapLocation loc) throws GameActionException{
        if(rc.canMarkTowerPattern(type, loc)){
            rc.markTowerPattern(type, loc);
            return true;
        }
        return false;
    }

    public boolean tryAttack(MapLocation loc) throws GameActionException{
        if(rc.canAttack(loc)){
            rc.attack(loc);
            return true;
        }
        return false;
    }

    // RESOURCE PATTERN STUFF //
    public boolean isResourcePatternCenter(MapLocation loc) throws GameActionException{
        return (loc.x - 2) % 4 == 0 && (loc.y - 2) % 4 == 0;
    }

    public boolean getTileTargetColor(MapLocation loc) throws GameActionException{
        if(isResourcePatternCenter(loc)){
            return false;
        }
        return (loc.x + loc.y) % 2 == 0;
    }

    public boolean tryConfirmResourcePattern(MapLocation loc) throws GameActionException{
        if(rc.getLocation().distanceSquaredTo(loc) <= 2 && rc.canCompleteResourcePattern(loc)){
            rc.completeResourcePattern(loc);
            return true;
        }
        return false;
    }
    //=================================//

    public void trySetIndicatorDot(MapLocation loc, int r, int g, int b) throws GameActionException{
        if (rc.onTheMap(loc)) {
            rc.setIndicatorDot(loc, r, g, b);
        }
    }

    public Direction towerTypeToMarkDirection(UnitType towerType){
        if(towerType == UnitType.LEVEL_ONE_PAINT_TOWER){
            return Direction.NORTH;
        }else if (towerType == UnitType.LEVEL_ONE_MONEY_TOWER){
            return Direction.NORTHWEST;
        }else{
            return Direction.NORTHEAST;
        }
    }

    public UnitType markDirectionToTowerType(Direction markDirection){
        if(markDirection == Direction.NORTH){
            return UnitType.LEVEL_ONE_PAINT_TOWER;
        }else if (markDirection == Direction.NORTHWEST){
            return UnitType.LEVEL_ONE_MONEY_TOWER;
        }else{
            return UnitType.LEVEL_ONE_DEFENSE_TOWER;
        }
    }

    public MapLocation[] defenseTowerNorth(MapLocation loc) throws GameActionException{
        int x = loc.x;
        int y = loc.y;
        return new MapLocation[]{
                new MapLocation(-2, 3).translate(x, y),
                new MapLocation(-2, 4).translate(x, y),
                new MapLocation(-1, 3).translate(x, y),
                new MapLocation(-1, 4).translate(x, y),
                new MapLocation(0, 3).translate(x, y),
                new MapLocation(0, 4).translate(x, y),
                new MapLocation(1, 3).translate(x, y),
                new MapLocation(1, 4).translate(x, y),
                new MapLocation(2, 3).translate(x, y),
                new MapLocation(2, 4).translate(x, y),
        };
    }

    public MapLocation[] defenseTowerSouth(MapLocation loc) throws GameActionException{
        int x = loc.x;
        int y = loc.y;
        return new MapLocation[]{
                new MapLocation(-2, -4).translate(x, y),
                new MapLocation(-2, -3).translate(x, y),
                new MapLocation(-1, -4).translate(x, y),
                new MapLocation(-1, -3).translate(x, y),
                new MapLocation(0, -4).translate(x, y),
                new MapLocation(0, -3).translate(x, y),
                new MapLocation(1, -4).translate(x, y),
                new MapLocation(1, -3).translate(x, y),
                new MapLocation(2, -4).translate(x, y),
                new MapLocation(2, -3).translate(x, y),
        };
    }

    public MapLocation[] defenseTowerEast(MapLocation loc) throws GameActionException{
        int x = loc.x;
        int y = loc.y;
        return new MapLocation[]{
                new MapLocation(3, -2).translate(x, y),
                new MapLocation(3, -1).translate(x, y),
                new MapLocation(3, 0).translate(x, y),
                new MapLocation(3, 1).translate(x, y),
                new MapLocation(3, 2).translate(x, y),
                new MapLocation(4, -2).translate(x, y),
                new MapLocation(4, -1).translate(x, y),
                new MapLocation(4, 0).translate(x, y),
                new MapLocation(4, 1).translate(x, y),
                new MapLocation(4, 2).translate(x, y),
        };
    }

    public MapLocation[] defenseTowerWest(MapLocation loc) throws GameActionException{
        int x = loc.x;
        int y = loc.y;
        return new MapLocation[]{
                new MapLocation(-4, -2).translate(x, y),
                new MapLocation(-4, -1).translate(x, y),
                new MapLocation(-4, 0).translate(x, y),
                new MapLocation(-4, 1).translate(x, y),
                new MapLocation(-4, 2).translate(x, y),
                new MapLocation(-3, -2).translate(x, y),
                new MapLocation(-3, -1).translate(x, y),
                new MapLocation(-3, 0).translate(x, y),
                new MapLocation(-3, 1).translate(x, y),
                new MapLocation(-3, 2).translate(x, y),
        };
    }

    public UnitType determineTowerPattern(MapLocation ruinLoc) throws GameActionException{
//        if(ruinLoc.distanceSquaredTo(new MapLocation(width/2, height/2)) < 50 && mapData.friendlyTowers.defenseTowers == 0 && rc.getNumberTowers()>2){
//            return UnitType.LEVEL_ONE_DEFENSE_TOWER;
//        }
        if(mapData.friendlyTowers.defenseTowers < 2 && ruinLoc.distanceSquaredTo(new MapLocation(width/2, height/2)) < 50){
            boolean northWall = false;
            boolean southWall = false;
            boolean eastWall = false;
            boolean westWall = false;
            MapInfo info;
            for(MapLocation loc : defenseTowerNorth(ruinLoc)){
                if(!rc.onTheMap(loc)){
                    continue;
                }
                info = mapData.mapInfos[loc.x][loc.y];
                if (info != null && info.isWall()) {
                    northWall = true;
                    break;
                }
            }
            for(MapLocation loc : defenseTowerSouth(ruinLoc)){
                if(!rc.onTheMap(loc)){
                    continue;
                }
                info = mapData.mapInfos[loc.x][loc.y];
                if (info != null && info.isWall()) {
                    southWall = true;
                    break;
                }
            }
            if(northWall && southWall){
                return UnitType.LEVEL_ONE_DEFENSE_TOWER;
            }
            for(MapLocation loc : defenseTowerEast(ruinLoc)){
                if(!rc.onTheMap(loc)){
                    continue;
                }
                info = mapData.mapInfos[loc.x][loc.y];
                if (info != null && info.isWall()) {
                    eastWall = true;
                    break;
                }
            }
            for(MapLocation loc : defenseTowerWest(ruinLoc)){
                if(!rc.onTheMap(loc)){
                    continue;
                }
                info = mapData.mapInfos[loc.x][loc.y];
                if (info != null && info.isWall()) {
                    westWall = true;
                    break;
                }
            }
            if(eastWall && westWall){
                return UnitType.LEVEL_ONE_DEFENSE_TOWER;
            }
        }

        double ratio = mapData.friendlyTowers.moneyTowers < 6 ? 2.5 : 1.5;
        if(mapData.friendlyTowers.moneyTowers < Math.max(1, mapData.friendlyTowers.paintTowers)*ratio && rc.getChips() < 4000){// || (mapData.friendlyTowers.paintTowers==0 && spawnTurn == 4)){
            return UnitType.LEVEL_ONE_MONEY_TOWER;
        }else{
            return UnitType.LEVEL_ONE_PAINT_TOWER;
        }
    }

    public MapLocation extendLocToEdge(MapLocation loc, Direction dir) throws GameActionException {
        MapLocation newLoc = loc;
        while(rc.onTheMap(newLoc.add(dir))){
            if(rc.getID()== 13547){
                System.out.println("extending: " + loc);
            }
            newLoc = newLoc.add(dir);
        }
        return newLoc;
    }

    public UnitType determineTowerPattern2(MapLocation ruinLoc) {
        if(getChipRate() < 20) {
            return UnitType.LEVEL_ONE_MONEY_TOWER;
        }
        if ((ruinLoc.x * 17 + ruinLoc.y * 31) % 2 == 0) {
            return UnitType.LEVEL_ONE_PAINT_TOWER;
        } else {
            return UnitType.LEVEL_ONE_MONEY_TOWER;
        }
    }

    public int prevChip;
    public int getChipRate() {
        int delta = rc.getChips() - prevChip;
        prevChip = rc.getChips();

        return delta;
    }

    public MapLocation getClosest(MapLocation[] locs) throws GameActionException{
        int closestDist = Integer.MAX_VALUE;
        MapLocation closestLoc = null;
        for(MapLocation loc : locs){
            int dist = distTo(loc);
            if(dist<closestDist){
                closestDist = dist;
                closestLoc = loc;
            }
        }
        return closestLoc;
    }

    public RobotInfo getClosest(RobotInfo[] robots) throws GameActionException{
        int closestDist = Integer.MAX_VALUE;
        RobotInfo closestRobot = null;
        for(RobotInfo robot : robots){
            int dist = distTo(robot.location);
            if(dist<closestDist){
                closestDist = dist;
                closestRobot = robot;
            }
        }
        return closestRobot;
    }

    public int extract_bits(int input, int start, int end){
        return (((1 << (end-start+1))-1) & (input >> start));
    }

    public MapLocation decodeLocation(int message){
        int x = extract_bits(message, 4, 9);
        int y = extract_bits(message, 10,15);
        return new MapLocation(x,y);
    }

    public Direction[] fuzzyDirs(Direction dir) throws GameActionException{
        if (!RIGHT) {
            return new Direction[]{
                    dir,
                    dir.rotateLeft(),
                    dir.rotateRight(),
                    dir.rotateLeft().rotateLeft(),
                    dir.rotateRight().rotateRight(),
                    dir.rotateLeft().rotateLeft().rotateLeft(),
                    dir.rotateRight().rotateRight().rotateRight()
            };
        }else{
            return new Direction[] {
                    dir,
                    dir.rotateRight(),
                    dir.rotateLeft(),
                    dir.rotateRight().rotateRight(),
                    dir.rotateLeft().rotateLeft(),
                    dir.rotateRight().rotateRight().rotateRight(),
                    dir.rotateLeft().rotateLeft().rotateLeft(),
            };
        }
    }

    public Direction dirTo(MapLocation loc) throws GameActionException{
        return rc.getLocation().directionTo(loc);
    }

    public int distTo(MapLocation loc) throws GameActionException{
        return rc.getLocation().distanceSquaredTo(loc);
    }

    public boolean touching(MapLocation loc) throws GameActionException{
        return rc.getLocation().isAdjacentTo(loc) || rc.getLocation().equals(loc);
    }

    public int locToInt(MapLocation loc) throws GameActionException{
        return loc.x + loc.y*rc.getMapWidth();
    }

    public MapLocation intToLoc(int i) throws GameActionException{
        return new MapLocation((i%rc.getMapWidth()), (i/rc.getMapWidth()));
    }

    public MapLocation randomLocation(MapLocation[] locs){
        return locs[(int)(Math.random()*locs.length)];
    }

    public MapLocation randomLocation(ArrayList<MapLocation> locs){
        return locs.get((int)(Math.random()*locs.size()));
    }

    public Direction randomDirection() throws GameActionException{
        return directions[(int)(Math.random()*directions.length)];
    }
}
package v28_TomHolland;

import v28_TomHolland.Helpers.Explore;
import v28_TomHolland.Helpers.MapData;
import v28_TomHolland.Util.Symmetry;
import v28_TomHolland.Util.Comms;
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
        width = rc.getMapWidth();
        height = rc.getMapHeight();
        spawnLoc = rc.getLocation();
        spawnTurn = rc.getRoundNum();
        rng = new Random(rc.getID());
        communication = new Comms(rc, this);
        mapData = new MapData(robot, width, height, this);
        explorer = new Explore(rc, width, height, mapData);
        prevChip = 0;
        RIGHT = rc.getID() % 2 == 0;
    }

    abstract public void turn() throws Exception;

    public void run() {
        while (true) {
            try {
                debugString = new StringBuilder();
                turn();
                rc.setIndicatorString(debugString.toString());
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

    /**
     * ############# FROM THE EXAMPLEFUNCSPLAYER #############
     */
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


    // deterministic random function to determine what tower type goes here
//    public UnitType determineTowerPattern(MapLocation ruinLoc) {
//        return (ruinLoc.x + ruinLoc.y) % 2 == 0 ? UnitType.LEVEL_ONE_PAINT_TOWER : UnitType.LEVEL_ONE_MONEY_TOWER;
//    }

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

    public UnitType determineTowerPattern(MapLocation ruinLoc) {
//       int selection = (ruinLoc.x*17 + ruinLoc.y*31) % 3;
//       if (selection == 0) {
//           return UnitType.LEVEL_ONE_PAINT_TOWER;
//       } else if (selection == 1) {
//           return UnitType.LEVEL_ONE_MONEY_TOWER;
//       }
//       return UnitType.LEVEL_ONE_MONEY_TOWER;
//        if(rc.getMoney()>2000){
//            return UnitType.LEVEL_ONE_DEFENSE_TOWER;
//        }

        if(mapData.friendlyTowers.moneyTowers < Math.max(1, mapData.friendlyTowers.paintTowers)*2){// || (mapData.friendlyTowers.paintTowers==0 && spawnTurn == 4)){
            return UnitType.LEVEL_ONE_MONEY_TOWER;
        }else{
            return UnitType.LEVEL_ONE_PAINT_TOWER;
        }
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
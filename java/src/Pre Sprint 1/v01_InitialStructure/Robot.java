package v01_InitialStructure;

import battlecode.common.*;

import java.util.ArrayList;
import java.util.Random;

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

    public RobotController rc;
    public int width;
    public int height;

    public boolean RIGHT;

    /**
     * A random number generator.
     * We will use this RNG to make some random moves. The Random class is provided by the java.util.Random
     * import at the top of this file. Here, we *seed* the RNG with a constant number (6147); this makes sure
     * we get the same sequence of numbers every time this code is run. This is very useful for debugging!
     */
    static public final Random rng = new Random(6147);

    public Robot(RobotController robot) throws GameActionException {
        rc = robot;
        width = rc.getMapWidth();
        height = rc.getMapHeight();
    }

    abstract public void turn() throws Exception;

    public void run() {
        while (true) {
            try {
                turn();
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
package v02_FirstBot;

import v02_FirstBot.Helpers.Explore;
import battlecode.common.*;
import v02_FirstBot.Helpers.MapData;

public abstract class Unit extends Robot {
    public MapData mapData;
    public Explore explorer;
    public Unit(RobotController robot) throws GameActionException {
        super(robot);
        mapData = new MapData(width, height);
        explorer = new Explore(rc, width, height, mapData);
    }

    public void markNearbyMapData() throws GameActionException{
        for(MapLocation loc : rc.getAllLocationsWithinRadiusSquared(rc.getLocation(),-1)){
            mapData.setVisited(loc);
        }
    }

    protected MapLocation nearestPaintTower = null;
    protected void refillSelf() throws GameActionException {
        // cant do anything if no nearest tower
        if (nearestPaintTower == null) {
            return;
        }

        // check if tower in range
        if (distTo(nearestPaintTower) > 16) {
            return;
        }

        int needed = rc.getType().paintCapacity - rc.getPaint();
        int transferAmount = 0;
        if (rc.canSenseRobotAtLocation(nearestPaintTower)) {
            RobotInfo tower = rc.senseRobotAtLocation(nearestPaintTower);
            if (tower.getTeam() != rc.getTeam()) {
                nearestPaintTower = null;
                return;
            }

            int availablePaint = tower.getPaintAmount();

            transferAmount = Math.min(needed, availablePaint);
        }

        if (transferAmount > 0 && rc.canTransferPaint(nearestPaintTower, -transferAmount)) {
            rc.transferPaint(nearestPaintTower, -transferAmount);
        }
    }

    protected void findNearestPaintTower(RobotInfo[] robots) throws GameActionException {
        // update prior belief if currently have a tower but it no longer
        if (nearestPaintTower != null) {
            if (rc.canSenseLocation(nearestPaintTower) && !rc.canSenseRobotAtLocation(nearestPaintTower)) {
                nearestPaintTower = null;
            } else if (rc.canSenseLocation(nearestPaintTower) && rc.canSenseRobotAtLocation(nearestPaintTower)) {
                RobotInfo tower = rc.senseRobotAtLocation(nearestPaintTower);
                if (tower.getTeam() != rc.getTeam()) {
                    nearestPaintTower = null;
                }
            }
        }

        if (nearestPaintTower == null) {
            int closestDist = 999999;
            for (RobotInfo robot : robots) {
                int dist = rc.getLocation().distanceSquaredTo(robot.getLocation());
                if (robot.getTeam() == rc.getTeam() && isPaintTower(robot.getType()) && dist < closestDist) {
                    nearestPaintTower = robot.getLocation();
                    closestDist = dist;
                }
            }
        }
    }

    protected boolean moveToNearestPaintTower() throws GameActionException {
        // make some way to find tower if no available ones
        if (nearestPaintTower == null) {
            return false;
        }

        // move to tower
        if (!rc.getLocation().isWithinDistanceSquared(nearestPaintTower, 2)) {
            bugNav(nearestPaintTower);
        } else {
            refillSelf();
        }
        return true;
    }

    public Direction paintExploreDirection = null;
    public boolean tryExploreOnPaint() throws GameActionException {
        if(!rc.isMovementReady()){
            return false;
        }
        if (paintExploreDirection == null) {
            paintExploreDirection = randomDirection();
        }
        for(Direction dir : fuzzyDirs(paintExploreDirection)){
            MapLocation loc = rc.getLocation().add(dir);
            if(rc.canSenseLocation(loc)){
                MapInfo info = rc.senseMapInfo(loc);
                rc.setIndicatorDot(loc, 0, 0, 255);
                if(info.getPaint().isAlly()){
                    if(tryMove(dir)){
                        paintExploreDirection = dir;
                        return true;
                    }
                }
            }
        }
        paintExploreDirection = paintExploreDirection.opposite();
        return false;
    }

    public void navAdjacent(MapLocation loc) throws GameActionException {
        if (!rc.getLocation().isAdjacentTo(loc)) {
            bugNav(loc);
        }
    }

    // tries moving in the given direction dir
    public boolean tryMove(Direction dir) throws GameActionException {
        if(rc.canMove(dir)){
            rc.move(dir);
            return true;
        }
        return false;
    }

    public void fuzzyMove(Direction dir) throws GameActionException {
        for(Direction d: fuzzyDirs(dir)){
            if(rc.canMove(d)){
                rc.move(d);
                return;
            }
        }
    }

    public void fuzzyMove(MapLocation loc) throws GameActionException{
        fuzzyMove(dirTo(loc));
    }

    int MAX_STACK_SIZE = 100;
    Direction[] bugStack = new Direction[MAX_STACK_SIZE];
    int bugStackIndex = 0;
    MapLocation lastTargetLocation = null;
    int stuckTurns = 0;
    public void bugNav(MapLocation loc) throws GameActionException{
        rc.setIndicatorLine(rc.getLocation(),loc,0,255,0);
        if(!rc.isMovementReady()){
            return;
        }
        //reset
        if(lastTargetLocation != loc || bugStackIndex >= MAX_STACK_SIZE-10){
            bugStack = new Direction[MAX_STACK_SIZE];
            bugStackIndex = 0;
            lastTargetLocation = loc;
        }

        // pop directions off of the stack
        while(bugStackIndex != 0 && rc.canMove(bugStack[bugStackIndex-1])){
            bugStackIndex--;
        }

        // going directly to the target
        if(bugStackIndex == 0){
            Direction dirToTarget = dirTo(loc);

            boolean isWater = false;
            if(rc.canMove(dirToTarget)){
                rc.move(dirToTarget);
                return;
            }else if(rc.canMove(dirToTarget.rotateLeft())){
                rc.move(dirToTarget.rotateLeft());
                return;
            }else if(rc.canMove(dirToTarget.rotateRight())) {
                rc.move(dirToTarget.rotateRight());
                return;
            }
            MapLocation locCheck = rc.getLocation().add(dirToTarget);

            // check if all units are robots
            locCheck = rc.getLocation().add(dirToTarget);
            boolean checkFrontRobot = rc.onTheMap(locCheck) && rc.canSenseRobotAtLocation(locCheck);
            locCheck = rc.getLocation().add(dirToTarget.rotateLeft());
            boolean checkLeftRobot = rc.onTheMap(locCheck) && rc.canSenseRobotAtLocation(locCheck);
            locCheck = rc.getLocation().add(dirToTarget.rotateRight());
            boolean checkRightRobot = rc.onTheMap(locCheck) && rc.canSenseRobotAtLocation(locCheck);
            if(checkFrontRobot && checkLeftRobot && checkRightRobot){
//                stuckTurns++;
                fuzzyMove(dirToTarget.opposite());
                return;
            }
//            stuckTurns = 0;


            bugStack[bugStackIndex] = dirToTarget.rotateRight();
            bugStackIndex++;
        }
        // add subsequent directions if cant move towards the target
        Direction dir = bugStack[bugStackIndex-1].rotateLeft();
        for(int i = 0; i < 8; i++){
            if(!rc.canMove(dir)){
                bugStack[bugStackIndex] = dir;
                bugStackIndex++;
            }else{
                rc.move(dir);
                return;
            }
            dir = dir.rotateLeft();
        }
    }
}


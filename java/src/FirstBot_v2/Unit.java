package FirstBot_v2;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public abstract class Unit extends Robot {
    public Unit(RobotController robot) throws GameActionException {
        super(robot);
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


package FirstBot_v2;

import battlecode.common.*;

public abstract class Tower extends Robot {
    public Tower(RobotController robot) throws GameActionException {
        super(robot);
    }

    public void turn() throws GameActionException {
        trySummonAnything();
        if(rc.canAttack(null)){
            rc.attack(null);
        }
    }

    public boolean trySummon(UnitType type, MapLocation loc) throws GameActionException{
        if(rc.canBuildRobot(type, loc)){
            rc.buildRobot(type,loc);
            return true;
        }
        return false;
    }

    public boolean trySummon(UnitType type) throws GameActionException{
        for(MapLocation loc : rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), GameConstants.BUILD_ROBOT_RADIUS_SQUARED)){
            if(trySummon(type, loc)){
                return true;
            }
        }
        return false;
    }

    public boolean trySummonAnything() throws GameActionException{
        if(rng.nextInt(2) == 0){
            return trySummon(UnitType.SOLDIER);
        } else {
            return trySummon(UnitType.MOPPER);
        }
    }
}


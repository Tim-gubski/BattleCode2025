package PoolParty_v4;

import FirstBot_v2.Units.Mopper;
import battlecode.common.*;

public abstract class Tower extends Robot {
    boolean firstTower = false;

    public Tower(RobotController robot) throws GameActionException {
        super(robot);
        if(rc.getRoundNum()<8 && isPaintTower(rc.getType())){
            firstTower = true;
        }
    }

    public void turn() throws GameActionException {
        if(rc.senseNearbyRobots(-1, rc.getTeam()).length < 5) {
            int paint = rc.getPaint();
            if (paint <= 249) {
                trySummon(UnitType.MOPPER);
            } else if (paint <= 299) {
                trySummon(UnitType.SOLDIER);
            } else {
                trySummonAnything();
            }
        }
        tryAttack(null);
        RobotInfo[] enemies = rc.senseNearbyRobots(-1,rc.getTeam().opponent());
        if(enemies.length > 0){
            tryAttack(getClosest(enemies).getLocation());
        }

        boolean instaRespawn = true;
        for(int x = -2; x <= 2; x++){
            for(int y = -2; y <= 2; y++) {
                MapInfo info = rc.senseMapInfo(rc.getLocation().translate(x,y));
                if(info.getMark() != info.getPaint()){
                    instaRespawn = false;
                }
            }
        }

        if(rc.getPaint()<50 && rc.senseNearbyRobots(-1, rc.getTeam()).length > 0 && enemies.length == 0 && rc.senseMapInfo(rc.getLocation()).hasRuin() && !firstTower && instaRespawn){
            rc.disintegrate();
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

    public boolean trySummonAnything() throws GameActionException {
        double rand = rng.nextDouble();
        if(rand < 0.4 || rc.getRoundNum() <= 5) {
            return trySummon(UnitType.SOLDIER);
        } else if (rand < 0.8 || rc.getRoundNum() <= 50) {
            return trySummon(UnitType.MOPPER);
        } else {
            return trySummon(UnitType.SPLASHER);
        }
    }
}


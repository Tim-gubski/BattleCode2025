package Refactor_v6.Units;

import Refactor_v6.Unit;
import battlecode.common.*;

public class Mopper extends Unit {
    Direction exploreDir = randomDirection();


    public Mopper(RobotController robot) throws GameActionException {
        super(robot);
        state = UnitState.EXPLORE;
    }
    /**
     * Run a single turn for a Mopper.
     * This code is wrapped inside the infinite loop in run(), so it is called once per turn.
     */

    public void turnv2() throws GameActionException {
        senseNearby();

        // state switch case
        UnitState next = state;
        String stateName = "";
        markNearbyMapData();
        switch (state) {
            case UnitState.EXPLORE -> {
                next = exploreState();
                stateName = "EXPLORE";
            }
            case UnitState.REFILL -> {
                next = refillState();
                stateName = "REFILL";
            }
            case UnitState.REFILLING -> {
                next = refillingState();
                stateName = "REFILLING";
            }
            case UnitState.MOPPING -> {
                next = moppingState();
                stateName = "MOPPING";
            }
        }

        rc.setIndicatorDot(currentTargetLoc, 255, 125, 0);
        rc.setIndicatorLine(rc.getLocation(), currentTargetLoc, 125, 0, 125);
        rc.setIndicatorString("Currently in state: " + stateName);
        state = next;
    }

    // movement methods --------------------------------------------------------------------------------------------- //


    private UnitState exploreState() throws GameActionException {
        // explore movement (just head towards explore direction)
        currentTargetLoc = explorer.getExploreTarget();
        safeFuzzyMove(currentTargetLoc, enemies);

        // perform actions in explore
        refillAllies(allies);

        // transition to next state
        MapLocation nearbyEnemyPaint = findNearbyEnemyPaint();
        if (shouldRefill()) {
            previousState = state;
            return UnitState.REFILL;
        } else if (nearbyEnemyPaint != null) {
            currentTargetLoc = nearbyEnemyPaint;
            return UnitState.MOPPING;
        }
        return state;
    }

    private UnitState refillState() throws GameActionException {
        // refill movement (just head towrads nearest paint tower)
        MapLocation foundTower = findNearestPaintTower();
        if (foundTower == null) {
            return UnitState.EXPLORE;
        }

        currentTargetLoc = foundTower;
        nearestPaintTower = currentTargetLoc;
        safeFuzzyMove(currentTargetLoc, enemies);

        // transition to next state
        if (rc.getLocation().isWithinDistanceSquared(currentTargetLoc, 2)) {
            return UnitState.REFILLING;
        }
        return state;
    }

    private UnitState refillingState() throws GameActionException {
        // ensure tower is still good
        MapLocation foundTower = findNearestPaintTower();
        if (foundTower == null) {
            return UnitState.EXPLORE;
        }

        currentTargetLoc = foundTower;
        nearestPaintTower = currentTargetLoc;

        // if in range of tower, refill
        if (rc.getLocation().isWithinDistanceSquared(currentTargetLoc, 2)) {
            refillSelf();
            rc.setIndicatorDot(currentTargetLoc, 0, 255, 0);
            if (rc.getPaint() > rc.getType().paintCapacity * 0.75) {
                UnitState next = previousState;
                previousState = null;
                return next;
            }
        }

        // move closer to tower
        else {
            rc.setIndicatorDot(currentTargetLoc, 255, 255, 255);
            safeFuzzyMove(currentTargetLoc, enemies);
        }
        return state;
    }

    private UnitState moppingState() throws GameActionException {
        // do some simple mopping
        MapLocation nearbyEnemyPaint = findNearbyEnemyPaint();

        if (nearbyEnemyPaint == null) {
            return UnitState.EXPLORE;
        }

        currentTargetLoc = nearbyEnemyPaint;

        if (rc.getLocation().isWithinDistanceSquared(nearbyEnemyPaint, rc.getType().actionRadiusSquared)) {
            if (rc.isActionReady() && rc.canAttack(nearbyEnemyPaint)) {
                rc.attack(nearbyEnemyPaint);
            }
        } else {
            safeFuzzyMove(nearbyEnemyPaint, enemies);
        }

        if (shouldRefill()) {
            previousState = state;
            return UnitState.REFILL;
        }

        return state;
    }

    private MapLocation findNearbyEnemyPaint() throws GameActionException {
        int closestDist = 999999;
        MapLocation closestLoc = null;
        for (MapInfo info : mapInfo) {
            if (isEnemyPaint(info.getPaint()) && info.getPaint() != PaintType.EMPTY) {
                int dist = rc.getLocation().distanceSquaredTo(info.getMapLocation());
                if (dist < closestDist) {
                    closestDist = dist;
                    closestLoc = info.getMapLocation();
                }
            }
        }
        return closestLoc;
    }

    public void turn() throws GameActionException {
        turnv2();

//        if (shouldRefill()) {
//            findNearestPaintTower();
//            if(!moveToNearestPaintTower()){
//                fuzzyMove(randomDirection());
//            }
//        } else {
//            refillAllies(rc.senseNearbyRobots(-1, rc.getTeam()));
//
//            RobotInfo tower = inEnemyTowerRange(rc.senseNearbyRobots(-1, rc.getTeam().opponent()));
//            if(tower != null) {
//                // incase we're too close
//                if (rc.getLocation().distanceSquaredTo(tower.getLocation()) <= Math.max(tower.type.actionRadiusSquared, 17)) {
//                    fuzzyMove(dirTo(tower.getLocation()).opposite());
//                }
//            }
//
//            MapInfo[] nearbyTiles = rc.senseNearbyMapInfos(-1);
//            MapLocation closestEnemyPaint = null;
//            int closestDist = 999999;
//            for(MapInfo tile : nearbyTiles){
//                if(tile.getPaint() == PaintType.ENEMY_PRIMARY || tile.getPaint() == PaintType.ENEMY_SECONDARY){
//                    int dist = rc.getLocation().distanceSquaredTo(tile.getMapLocation());
//                    if(dist < closestDist){
//                        closestDist = dist;
//                        closestEnemyPaint = tile.getMapLocation();
//                    }
//                }
//            }
//            if (closestEnemyPaint != null) {
//                if(!attemptMopSweep(rc.senseNearbyRobots(2, rc.getTeam().opponent())) && rc.getLocation().isAdjacentTo(closestEnemyPaint)){
//                    if(rc.canAttack(closestEnemyPaint)){
//                        rc.attack(closestEnemyPaint);
//                    }
//                }else{
//                    fuzzyMove(rc.getLocation().directionTo(closestEnemyPaint));
//                }
//            } else {
//                bugNav(explorer.getExploreTarget());
//            }
//        }
//
//        // confirm nearby towers
//        MapLocation[] nearbyRuins = rc.senseNearbyRuins(-1);
//        if(nearbyRuins.length > 0) {
//            MapLocation closestRuin = getClosest(nearbyRuins);
//            // Complete the ruin if we can
//            for (UnitType type : towerTypes) {
//                if (rc.canCompleteTowerPattern(type, closestRuin)) {
//                    rc.completeTowerPattern(type, closestRuin);
//                    rc.setTimelineMarker("Tower built", 0, 255, 0);
//                    System.out.println("Built a tower at " + closestRuin + "!");
//                }
//            }
//        }
//        markNearbyMapData();
    }

    private void refillAllies(RobotInfo[] allies) throws GameActionException {
        for (RobotInfo robot : allies) {
            // skip if is a tower
            if (robot.getType().isTowerType()) {
                continue;
            }

            // skip if ally is a mopper
            if (robot.getType() == UnitType.MOPPER) {
                continue;
            }

            int amount = Math.max(0, rc.getPaint() - robot.paintAmount - 10); // leave mopper with a little bit
            if (rc.canTransferPaint(robot.getLocation(), amount)) {
                rc.transferPaint(robot.getLocation(), amount);
            }
        }
    }

    private boolean attemptMopSweep(RobotInfo[] enemies) throws GameActionException {
        if (!rc.isActionReady()) {
            return false;
        }
        int up = 0;
        int down = 0;
        int left = 0;
        int right = 0;

        for (RobotInfo enemy : enemies) {
            if (enemy.getLocation().isWithinDistanceSquared(rc.getLocation(), 1)) {
                Direction dir = rc.getLocation().directionTo(enemy.getLocation());
                if (dir == Direction.NORTH || dir == Direction.NORTHWEST || dir == Direction.NORTHEAST) {
                    up++;
                }
                if (dir == Direction.SOUTH || dir == Direction.SOUTHWEST || dir == Direction.SOUTHEAST) {
                    down++;
                }
                if (dir == Direction.WEST || dir == Direction.NORTHWEST || dir == Direction.SOUTHWEST) {
                    left++;
                }
                if (dir == Direction.EAST || dir == Direction.NORTHEAST || dir == Direction.SOUTHEAST) {
                    right++;
                }
            }
        }

        int mostEnemy = Math.max(Math.max(up, down), Math.max(left, right));
        if (mostEnemy < 2) return false;
        Direction swingDir = null;
        if (mostEnemy == up) {
            swingDir = Direction.NORTH;
        } else if (mostEnemy == down) {
            swingDir = Direction.SOUTH;
        } else if (mostEnemy == left) {
            swingDir = Direction.WEST;
        } else {
            swingDir = Direction.EAST;
        }

        if (rc.canMopSwing(swingDir)) {
            rc.mopSwing(swingDir);
            return true;
        }

        return false;
    }
}

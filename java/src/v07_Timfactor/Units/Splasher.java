package v07_Timfactor.Units;

import v07_Timfactor.Unit;
import battlecode.common.*;

public class Splasher extends Unit {
    public Splasher(RobotController robot) throws GameActionException {
        super(robot);
    }

    MapLocation returnLoc = null;

    public void turn() throws Exception {
        debugString = new StringBuilder();
        senseNearby(); // perform all scans

        previousState = state;
        state = determineState();

        switch (state) {
            case UnitState.EXPLORE -> {
                exploreState();
            }
            case UnitState.REFILLING -> {
                refillingState();
            }
        }

        stateInvariantActions();

        if (currentTargetLoc != null) {
            rc.setIndicatorDot(currentTargetLoc, 255, 125, 0);
            rc.setIndicatorLine(rc.getLocation(), currentTargetLoc, 125, 0, 125);
        }
        debugString.append("Currently in state: ").append(state.toString());
        rc.setIndicatorString(debugString.toString());
    }

    private UnitState determineState() throws GameActionException {
        if ((state != UnitState.REFILLING && shouldRefill()) || (state == UnitState.REFILLING && rc.getPaint() < rc.getType().paintCapacity * 0.75)) {
            if (returnLoc == null) {
                returnLoc = rc.getLocation();
            }
            return UnitState.REFILLING;
        }
        return UnitState.EXPLORE;
    }

    private void stateInvariantActions() throws GameActionException{
        markNearbyMapData();
    }

    private void exploreState() throws GameActionException {
        // check if enemy tower in range, attack
        RobotInfo tower = inEnemyTowerRange(rc.senseNearbyRobots(-1, rc.getTeam().opponent()));
        if (tower != null) {
//            for (Direction dir : directions) {
//                MapLocation newLocation = rc.getLocation().add(dir);
//                int dist = newLocation.distanceSquaredTo(tower.getLocation());
//                if (dist > tower.type.actionRadiusSquared && rc.canMove(dir) && !isEnemyPaint(rc.senseMapInfo(newLocation).getPaint())) {
//                    rc.move(dir);
//                    break;
//                }
//            }
//            for (MapLocation loc : rc.getAllLocationsWithinRadiusSquared(tower.getLocation(), 4)) {
//                if (rc.canAttack(loc)) {
//                    rc.attack(loc);
//                    break;
//                }
//            }
            // incase we're too close
            if (rc.getLocation().distanceSquaredTo(tower.getLocation()) <= tower.type.actionRadiusSquared) {
                safeFuzzyMove(dirTo(tower.getLocation()).opposite(), enemies);
                rc.setIndicatorString("Retreating");
            }
            // otherwise explore
        }
        else {
            if (rc.isActionReady()) {
                for (RobotInfo robot : rc.senseNearbyRobots(rc.getType().actionRadiusSquared, rc.getTeam().opponent())) {
                    MapLocation enemyLoc = robot.getLocation();
                    Direction enemyDir = rc.getLocation().directionTo(enemyLoc);
                    MapLocation targetLoc = rc.getLocation().add(enemyDir).add(enemyDir);
                    if ((!rc.senseMapInfo(targetLoc).getPaint().isAlly() && tryAttack(targetLoc)) || (!rc.senseMapInfo(enemyLoc).getPaint().isAlly() && tryAttack(enemyLoc))) {
                        break;
                    }
                }
            }

            if (returnLoc != null) {
                if (rc.canSenseLocation(returnLoc)) {
                    returnLoc = null;
                } else {
                    safeFuzzyMove(returnLoc, enemies);
                }
            }

            Direction enemyPaintDirection = getEnemyPaintDirection();
            if (enemyPaintDirection != null) {
                if (isEnemyPaint(rc.senseMapInfo(rc.getLocation()).getPaint())) {
                    safeFuzzyMove(enemyPaintDirection.opposite(), enemies);
                }
                Direction startingDirection;
                if (RIGHT) {
                    startingDirection = enemyPaintDirection.rotateRight().rotateRight();
                } else {
                    startingDirection = enemyPaintDirection.rotateLeft().rotateLeft();
                }

                for (Direction dir : fuzzyDirs(startingDirection)) {
                    MapLocation loc = rc.getLocation().add(dir);
                    if (rc.onTheMap(loc) && !isEnemyPaint(rc.senseMapInfo(loc).getPaint()) && rc.canMove(dir)) {
                        rc.move(dir);
                        break;
                    }
                }
                if (rc.isActionReady()) {
                    RIGHT = !RIGHT;
                }
            } else {
                safeFuzzyMove(explorer.getExploreTarget(), enemies);
            }
        }

        // spray and pray
        MapInfo[] attackableTilesInfo = rc.senseNearbyMapInfos(rc.getLocation(), rc.getType().actionRadiusSquared);
        for (MapInfo info : attackableTilesInfo) {
            if (isEnemyPaint(info.getPaint()) && rc.canAttack(info.getMapLocation())) {
                rc.attack(info.getMapLocation());
                break;
            }
        }

        // confirm nearby towers
        MapLocation[] nearbyRuins = rc.senseNearbyRuins(-1);
        if (nearbyRuins.length > 0) {
            MapLocation closestRuin = getClosest(nearbyRuins);
            // Complete the ruin if we can
            for (UnitType type : towerTypes) {
                if (rc.canCompleteTowerPattern(type, closestRuin)) {
                    rc.completeTowerPattern(type, closestRuin);
                    rc.setTimelineMarker("Tower built", 0, 255, 0);
                    System.out.println("Built a tower at " + closestRuin + "!");
                }
            }
        }
    }
}

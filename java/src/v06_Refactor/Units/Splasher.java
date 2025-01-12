package v06_Refactor.Units;

import v06_Refactor.Unit;
import battlecode.common.*;

public class Splasher extends Unit {
    public Splasher(RobotController robot) throws GameActionException {
        super(robot);
    }

    public void turn() throws Exception {
        if (shouldRefill()) {
            findNearestPaintTower();
            if (nearestPaintTower != null){
                rc.setIndicatorString("Exploring on paint!! Trying to refill at: " + nearestPaintTower);
            } else {
                rc.setIndicatorString("Can't find refill station");
            }
            if(!moveToNearestPaintTower()){
                tryExploreOnPaint();
            }
        } else {
            // check if enemy tower in range, attack
            RobotInfo tower = inEnemyTowerRange(rc.senseNearbyRobots(-1, rc.getTeam().opponent()));
            if (tower != null) {
                for (Direction dir : directions) {
                    MapLocation newLocation = rc.getLocation().add(dir);
                    int dist = newLocation.distanceSquaredTo(tower.getLocation());
                    if (dist > tower.type.actionRadiusSquared && rc.canMove(dir) && !isEnemyPaint(rc.senseMapInfo(newLocation).getPaint())) {
                        rc.move(dir);
                        break;
                    }
                }
                tryAttack(tower.getLocation());
                // incase we're too close
                if (rc.getLocation().distanceSquaredTo(tower.getLocation()) <= tower.type.actionRadiusSquared) {
                    fuzzyMove(dirTo(tower.getLocation()).opposite());
                }
                // otherwise explore
            } else {
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
                Direction enemyPaintDirection = getEnemyPaintDirection();
                if (enemyPaintDirection != null) {
                    if (isEnemyPaint(rc.senseMapInfo(rc.getLocation()).getPaint())) {
                        fuzzyMove(enemyPaintDirection.opposite());
                    }
                    if (RIGHT) {
                        fuzzyMove(enemyPaintDirection.rotateRight().rotateRight());
                    } else {
                        fuzzyMove(enemyPaintDirection.rotateLeft().rotateLeft());
                    }
                } else {
                    bugNav(explorer.getExploreTarget());
                }
            }
        }
        markNearbyMapData();
    }
}

package v02_FirstBot.Units;

import v02_FirstBot.Unit;
import battlecode.common.*;

public class Mopper extends Unit {
    Direction exploreDir = randomDirection();

    public Mopper(RobotController robot) throws GameActionException {
        super(robot);
    }
    /**
     * Run a single turn for a Mopper.
     * This code is wrapped inside the infinite loop in run(), so it is called once per turn.
     */
    public void turn() throws Exception {
        findNearestPaintTower(rc.senseNearbyRobots(-1));
        if (rc.getPaint() < 30) {
            if(!moveToNearestPaintTower()){
                fuzzyMove(randomDirection());
            }
        } else {
            refillAlies(rc.senseNearbyRobots(-1, rc.getTeam()));

            MapInfo[] nearbyTiles = rc.senseNearbyMapInfos(-1);
            MapLocation closestEnemyPaint = null;
            int closestDist = 999999;
            for(MapInfo tile : nearbyTiles){
                if(tile.getPaint() == PaintType.ENEMY_PRIMARY || tile.getPaint() == PaintType.ENEMY_SECONDARY){
                    int dist = rc.getLocation().distanceSquaredTo(tile.getMapLocation());
                    if(dist < closestDist){
                        closestDist = dist;
                        closestEnemyPaint = tile.getMapLocation();
                    }
                }
            }
            if (closestEnemyPaint != null) {
                if(!attemptMopSweep(rc.senseNearbyRobots(2, rc.getTeam().opponent())) && rc.getLocation().isAdjacentTo(closestEnemyPaint)){
                    if(rc.canAttack(closestEnemyPaint)){
                        rc.attack(closestEnemyPaint);
                    }
                }else{
                    fuzzyMove(rc.getLocation().directionTo(closestEnemyPaint));
                }
            } else {
                bugNav(explorer.getExploreTarget());
            }
        }

        markNearbyMapData();
    }

    private void refillAlies(RobotInfo[] allies) throws GameActionException {
        for (RobotInfo robot : allies) {
            // skip if is a tower
            if (robot.getType().isTowerType()) {
                continue;
            }

            int amount = rc.getPaint() - robot.paintAmount - 10; // leave mopper with a little bit
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

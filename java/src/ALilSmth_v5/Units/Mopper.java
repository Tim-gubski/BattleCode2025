package ALilSmth_v5.Units;

import ALilSmth_v5.Unit;
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
        if (shouldRefill()) {
            findNearestPaintTower();
            if(!moveToNearestPaintTower()){
                fuzzyMove(randomDirection());
            }
        } else {
            refillAllies(rc.senseNearbyRobots(-1, rc.getTeam()));

            RobotInfo tower = inEnemyTowerRange(rc.senseNearbyRobots(-1, rc.getTeam().opponent()));
            if(tower != null) {
                // incase we're too close
                if (rc.getLocation().distanceSquaredTo(tower.getLocation()) <= Math.max(tower.type.actionRadiusSquared, 17)) {
                    fuzzyMove(dirTo(tower.getLocation()).opposite());
                }
            }

            MapInfo[] nearbyTiles = rc.senseNearbyMapInfos(-1);
            MapLocation closestEnemyPaint = null;
            boolean bestOnOurMarker = false;
            int closestDist = 999999;
            for(MapInfo tile : nearbyTiles){
                if(tile.getPaint() == PaintType.ENEMY_PRIMARY || tile.getPaint() == PaintType.ENEMY_SECONDARY){
                    boolean onMarker = tile.getMark().isAlly();
                    int dist = rc.getLocation().distanceSquaredTo(tile.getMapLocation());

                    if(onMarker && !bestOnOurMarker) {
                        closestDist = dist;
                        closestEnemyPaint = tile.getMapLocation();
                        bestOnOurMarker = true;
                    } else if (onMarker && bestOnOurMarker) {
                        if(dist < closestDist){
                            closestDist = dist;
                            closestEnemyPaint = tile.getMapLocation();
                        }
                    } else if (!onMarker && !bestOnOurMarker) {
                        if(dist < closestDist){
                            closestDist = dist;
                            closestEnemyPaint = tile.getMapLocation();
                        }
                    }
                }
            }
            if (closestEnemyPaint != null) {
                rc.setIndicatorDot(closestEnemyPaint, 255, 255, 0);
                if(!attemptMopSweep(rc.senseNearbyRobots(2, rc.getTeam().opponent())) && rc.getLocation().isAdjacentTo(closestEnemyPaint)){
                    tryAttack(closestEnemyPaint);
                }else{
                    fuzzyMove(rc.getLocation().directionTo(closestEnemyPaint));
                    tryAttack(closestEnemyPaint);
                }
            } else {
                RobotInfo[] friends = rc.senseNearbyRobots(-1,rc.getTeam());
                RobotInfo bestFriend = null;
                for(RobotInfo friend : friends){
                    if(friend.getType() == UnitType.SOLDIER && (bestFriend == null || Math.abs(friend.getID()-rc.getID()) < Math.abs(bestFriend.getID()-rc.getID()))){
                        bestFriend = friend;
                    }
                }
                if(bestFriend!=null){
                    fuzzyMove(dirTo(bestFriend.getLocation()));
                }else{
                    bugNav(explorer.getExploreTarget());
                }
            }
        }

        // confirm nearby towers
        MapLocation[] nearbyRuins = rc.senseNearbyRuins(-1);
        if(nearbyRuins.length > 0) {
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

        markNearbyMapData();
    }

    public RobotInfo inEnemyTowerRange(RobotInfo[] enemies) throws GameActionException {
        for (RobotInfo enemy : enemies) {
            if (!enemy.getType().isTowerType()) {
                continue;
            }
            return enemy;
        }
        return null;
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

package YapYapYap_v10.Units;

import YapYapYap_v10.Unit;
import battlecode.common.*;

public class Mopper extends Unit {
    Direction exploreDir = randomDirection();
    MapLocation closestEnemyPaint = null;

    public Mopper(RobotController robot) throws GameActionException {
        super(robot);
        state = UnitState.EXPLORE;
    }
    /**
     * Run a single turn for a Mopper.
     * This code is wrapped inside the infinite loop in run(), so it is called once per turn.
     */

    public void turn() throws GameActionException {
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
            case UnitState.MOPPING -> {
                moppingState();
            }
        }

        stateInvariantActions();

//        trySetIndicatorDot(currentTargetLoc, 255, 125, 0);
//        trySetIndicatorLine(rc.getLocation(), currentTargetLoc, 125, 0, 125);
        rc.setIndicatorString("Currently in state: " + state.toString());
    }

    // movement methods --------------------------------------------------------------------------------------------- //
    private UnitState determineState() throws GameActionException {
        if ((state != UnitState.REFILLING && shouldRefill()) || (state == UnitState.REFILLING && rc.getPaint() < rc.getType().paintCapacity * 0.75)) {
            if (returnLoc == null) {
                returnLoc = rc.getLocation();
            }
            return UnitState.REFILLING;
        }

        if(closestAnyRuin != null){
            trySetIndicatorDot(closestAnyRuin, 0, 255, 0);
        }

        MapInfo[] nearbyTiles = rc.senseNearbyMapInfos(-1);
        closestEnemyPaint = null;
        boolean bestOnOurMarker = false;
        int closestDist = 999999;
        for(MapInfo tile : nearbyTiles) {
            if (tile.getPaint() == PaintType.ENEMY_PRIMARY || tile.getPaint() == PaintType.ENEMY_SECONDARY) {
                boolean onMarker = closestAnyRuin != null && tile.getMapLocation().distanceSquaredTo(closestAnyRuin) <= 8;
                int dist = rc.getLocation().distanceSquaredTo(tile.getMapLocation());

                if (onMarker && !bestOnOurMarker) {
                    closestDist = dist;
                    closestEnemyPaint = tile.getMapLocation();
                    bestOnOurMarker = true;
                } else if (onMarker && bestOnOurMarker) {
                    if (dist < closestDist) {
                        closestDist = dist;
                        closestEnemyPaint = tile.getMapLocation();
                    }
                } else if (!onMarker && !bestOnOurMarker) {
                    if (dist < closestDist) {
                        closestDist = dist;
                        closestEnemyPaint = tile.getMapLocation();
                    }
                }
            }
        }

        if (closestEnemyPaint != null) {
            return UnitState.MOPPING;
        }
        return UnitState.EXPLORE;
    }

    private void stateInvariantActions() throws GameActionException {
        refillAllies(rc.senseNearbyRobots(-1, rc.getTeam()));

        // confirm all tower patterns
        if(closestAnyRuin != null) {
            for (UnitType type : towerTypes) {
                if (rc.canCompleteTowerPattern(type, closestAnyRuin)) {
                    rc.completeTowerPattern(type, closestAnyRuin);
                    rc.setTimelineMarker("Tower built", 0, 255, 0);
                    System.out.println("Built a tower at " + closestAnyRuin + "!");
                }
            }
        }

        // confirm all resource patterns
        for (MapLocation loc : getResourcePatternCenterLocations()) {
            if (tryConfirmResourcePattern(loc)) {
                //rc.setTimelineMarker("Resource pattern confirmed", 0, 255, 0);
                trySetIndicatorDot(loc, 0, 255, 0);
                System.out.println("Resource pattern confirmed at " + loc + "!");
            }
        }
        markNearbyMapData();
    }

    private void exploreState() throws GameActionException {
        if (returnLoc != null) {
            if (distTo(returnLoc) <= 8) {
                returnLoc = null;
            } else {
                currentTargetLoc = returnLoc;
                safeFuzzyMove(returnLoc, enemies);
            }
        }

        RobotInfo[] friends = rc.senseNearbyRobots(-1,rc.getTeam());
        RobotInfo bestFriend = null;
        for(RobotInfo friend : friends){
            if(friend.getType() == UnitType.SOLDIER && (bestFriend == null || Math.abs(friend.getID()-rc.getID()) < Math.abs(bestFriend.getID()-rc.getID()))){
                bestFriend = friend;
            }
        }
        if(bestFriend!=null){
            safeFuzzyMove(bestFriend.getLocation(), enemies);
        }else{
            safeFuzzyMove(explorer.getExploreTarget(), enemies);
        }
    }

    private UnitState moppingState() throws GameActionException {
        trySetIndicatorDot(closestEnemyPaint, 255, 255, 0);
        if(!attemptMopSweep(rc.senseNearbyRobots(2, rc.getTeam().opponent())) && rc.getLocation().isAdjacentTo(closestEnemyPaint)){
            tryAttack(closestEnemyPaint);
        }else{
            safeFuzzyMove(rc.getLocation().directionTo(closestEnemyPaint), enemies);
            tryAttack(closestEnemyPaint);
        }
        return state;
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

            int amount = Math.max(0, Math.min(rc.getPaint() - 30, robot.getType().paintCapacity - robot.paintAmount)); // leave mopper with a little bit
            if (amount != 0 && rc.canTransferPaint(robot.getLocation(), amount)) {
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
        if (mostEnemy < 1) return false;
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

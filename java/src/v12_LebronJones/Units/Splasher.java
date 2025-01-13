package v12_LebronJones.Units;

import v12_LebronJones.Unit;
import battlecode.common.*;

public class Splasher extends Unit {
    Direction exploreDirection;

    public Splasher(RobotController robot) throws GameActionException {
        super(robot);

        MapLocation closestTower = rc.getLocation();
        int minDist = 999999;
        for (RobotInfo r : rc.senseNearbyRobots(4,rc.getTeam())){
            int dist = rc.getLocation().distanceSquaredTo(r.location);
            if (dist < minDist && r.type.isTowerType()){
                minDist = dist;
                closestTower = r.location;
            }
        }
        spawnTower = closestTower;
        exploreDirection = closestTower.directionTo(rc.getLocation());
    }

    MapLocation returnLoc = null;

    public void turn() throws Exception {
        senseNearby(); // perform all scans
        communication.parseMessages();

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
        return;
    }

    private void exploreState() throws GameActionException {
        // spray and pray
        MapLocation bestTile = null;
        int[][] maxAdjacent = new int[9][9];
        for (MapInfo info : mapInfo) {
            if (isEnemyPaint(info.getPaint())){// || info.getPaint() == PaintType.EMPTY){
                int x = info.getMapLocation().x - rc.getLocation().x + 4;
                int y = info.getMapLocation().y - rc.getLocation().y + 4;
                maxAdjacent[x][y]++;
                if(x+1 < 9) maxAdjacent[x+1][y]++;
                if(x-1 >= 0) maxAdjacent[x-1][y]++;
                if(y+1 < 9) maxAdjacent[x][y+1]++;
                if(y-1 >= 0) maxAdjacent[x][y-1]++;
                if(x+1 < 9 && y+1 < 9) maxAdjacent[x+1][y+1]++;
                if(x-1 >= 0 && y-1 >= 0) maxAdjacent[x-1][y-1]++;
                if(x+1 < 9 && y-1 >= 0) maxAdjacent[x+1][y-1]++;
                if(x-1 >= 0 && y+1 < 9) maxAdjacent[x-1][y+1]++;
            }
        }
        int max = 0;
        for(MapLocation loc : rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 4)){
            int x = loc.x - rc.getLocation().x + 4;
            int y = loc.y - rc.getLocation().y + 4;
            if(maxAdjacent[x][y] > max && rc.canAttack(loc)){
                max = maxAdjacent[x][y];
                bestTile = loc;
            }
        }
        if(bestTile != null && max >= 4){
            rc.attack(bestTile);
        }


        // check if enemy tower in range, evade
        RobotInfo tower = inEnemyTowerRange(rc.senseNearbyRobots(-1, rc.getTeam().opponent()));
        if (tower != null) {
            // incase we're too close
            if (rc.getLocation().distanceSquaredTo(tower.getLocation()) <= tower.type.actionRadiusSquared) {
                safeFuzzyMove(dirTo(tower.getLocation()).opposite(), enemies);
                debugString.append("Retreating");
            }
            // otherwise explore
        }

        if (returnLoc != null) {
            if (distTo(returnLoc) <= 8) {
                returnLoc = null;
            } else {
                currentTargetLoc = returnLoc;
                safeFuzzyMove(returnLoc, enemies);
            }
        }

        Direction enemyPaintDirection = getEnemyPaintDirection();
        if (enemyPaintDirection != null && enemyPaintDirection != Direction.CENTER) {
            rc.setIndicatorLine(rc.getLocation(), rc.getLocation().add(enemyPaintDirection), 255, 0, 255);
            if (isEnemyPaint(rc.senseMapInfo(rc.getLocation()).getPaint())) {
                safeFuzzyMove(enemyPaintDirection.opposite(), enemies);
                debugString.append(enemyPaintDirection);
            }else{
                safeFuzzyMove(enemyPaintDirection, enemies);
                debugString.append(enemyPaintDirection);
            }
        } else {
//            safeFuzzyMove(explorer.getExploreTarget(), enemies);
//            debugString.append(explorer.getExploreTarget().toString());
            if(!rc.onTheMap(rc.getLocation().add(exploreDirection).add(exploreDirection).add(exploreDirection))){
                exploreDirection = randomDirection();
            }
            safeFuzzyMove(exploreDirection, enemies);

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

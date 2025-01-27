package v40_HandyAndy.Units;

import v40_HandyAndy.Unit;
import battlecode.common.*;

public class Splasher extends Unit {
//    Direction exploreDirection;
    MapLocation exploreLocation;

    public Splasher(RobotController robot) throws GameActionException {
        super(robot);
        Direction exploreDirection = spawnTower.directionTo(rc.getLocation());
        exploreLocation = extendLocToEdge(rc.getLocation(), exploreDirection);
    }



    MapLocation returnLoc = null;

    public void turn() throws Exception {
        debugString.append("h1");
        senseNearby(); // perform all scans

        debugString.append("h2");
        previousState = state;
        state = determineState();
        debugString.append("Currently in state: ").append(state.toString());

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

    }

    private UnitState determineState() throws GameActionException {
        if ((state != UnitState.REFILLING && rc.getPaint()<75) || (state == UnitState.REFILLING && rc.getPaint() < rc.getType().paintCapacity * 0.75)) {
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
//        int bytecode = Clock.getBytecodesLeft();
        MapLocation bestTile = null;
        int[][] maxAdjacent = new int[11][11];
        for (MapInfo info : mapInfo) {
            if (info.getPaint().isEnemy()){// || info.getPaint() == PaintType.EMPTY){
                int x = info.getMapLocation().x - rc.getLocation().x + 5;
                int y = info.getMapLocation().y - rc.getLocation().y + 5;
                maxAdjacent[x][y]++;
                maxAdjacent[x+1][y]++;
                maxAdjacent[x-1][y]++;
                maxAdjacent[x][y+1]++;
                maxAdjacent[x][y-1]++;
                maxAdjacent[x+1][y+1]++;
                maxAdjacent[x-1][y-1]++;
                maxAdjacent[x+1][y-1]++;
                maxAdjacent[x-1][y+1]++;
            }
        }
        for(RobotInfo enemy : enemies){
            if(enemy.type.isTowerType()){
                int x = enemy.location.x - rc.getLocation().x + 5;
                int y = enemy.location.y - rc.getLocation().y + 5;
                maxAdjacent[x][y] += 5;
                maxAdjacent[x+1][y] += 5;
                maxAdjacent[x-1][y] += 5;
                maxAdjacent[x][y+1] += 5;
                maxAdjacent[x][y-1] += 5;
                maxAdjacent[x+1][y+1] += 5;
                maxAdjacent[x-1][y-1] += 5;
                maxAdjacent[x+1][y-1] += 5;
                maxAdjacent[x-1][y+1] += 5;
            }
        }
//        System.out.println("Bytecode used for splasher thing: " + (bytecode - Clock.getBytecodesLeft()));
        int max = 0;
        for(MapLocation loc : rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 4)){
            int x = loc.x - rc.getLocation().x + 5;
            int y = loc.y - rc.getLocation().y + 5;
            if(maxAdjacent[x][y] > max && rc.canAttack(loc)){// && reachableFrom(rc.getLocation(), loc)){
                max = maxAdjacent[x][y];
                bestTile = loc;
            }
        }
        debugString.append("Max: " + max);
        if(bestTile != null){
            rc.setIndicatorLine(rc.getLocation(), bestTile, 0, 150, 0);
        }
        if(bestTile != null && (max >= 3 || bestTile.distanceSquaredTo(closestAnyRuin) <= 8)){
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

        MapLocation enemyPaintLoc = getEnemyPaintLoc();

        if (enemyPaintLoc != null && reachableFrom(rc.getLocation(), enemyPaintLoc)) {
            Direction enemyPaintDirection = dirTo(enemyPaintLoc);
            rc.setIndicatorLine(rc.getLocation(), rc.getLocation().add(enemyPaintDirection), 255, 0, 255);
            // get off the enemy paint if youre on it
            if (rc.senseMapInfo(rc.getLocation()).getPaint().isEnemy()) {
                safeFuzzyMove(enemyPaintDirection.opposite(), enemies);
                debugString.append(enemyPaintDirection);
                // otherwise get closer to enemy paint
            }else{
                MapLocation newLoc = null;
                for(Direction dir : fuzzyDirs(enemyPaintDirection)){
                    newLoc = rc.getLocation().add(dir);
                    if(rc.onTheMap(newLoc) && !mapData.getMapInfo(newLoc).getPaint().isEnemy()
                            && (closestEnemyTower == null || newLoc.distanceSquaredTo(closestEnemyTower.location) > closestEnemyTower.type.actionRadiusSquared)
                            && rc.canMove(dir)){
                        debugString.append("moving towards enemy paint");
                        if(closestEnemyTower != null){
                            rc.setIndicatorLine(rc.getLocation(), closestEnemyTower.location, 255, 0, 0);
                        }
                        rc.move(dir);
                        break;
                    }
                }
                if(rc.isMovementReady() && bestTile != null){
                    if(tryAttack(bestTile)){
                        safeFuzzyMove(bestTile, enemies);
                        tryAttack(bestTile);
                    }
                }
//                safeFuzzyMove(enemyPaintDirection, enemies);
                debugString.append(enemyPaintDirection);
            }
        } else {
//            safeFuzzyMove(explorer.getExploreTarget(), enemies);
//            debugString.append(explorer.getExploreTarget().toString());
            if(distTo(exploreLocation) <= 8){
                exploreLocation = extendLocToEdge(rc.getLocation(), randomDirection());
            }
            bugNav(exploreLocation);
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

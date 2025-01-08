package Refactor_v6.Units;

import Refactor_v6.Unit;
import battlecode.common.*;

// TODO: Dont go into tower range, attack towers maybe?, moppers attack enemy units

public class Soldier extends Unit {
    Direction exploreDir = randomDirection();

    public Soldier(RobotController robot) throws GameActionException {
        super(robot);
        state = UnitState.EXPLORE;
    }

    public void turnv2() throws GameActionException {
        senseNearby();

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
            case UnitState.BUILD -> {
                next = buildState();
                stateName = "BUILD";
            }
        }

        rc.setIndicatorDot(currentTargetLoc, 255, 125, 0);
        rc.setIndicatorLine(rc.getLocation(), currentTargetLoc, 125, 0, 125);
        rc.setIndicatorString("Currently in state: " + stateName);
        state = next;
    }

    private UnitState exploreState() throws GameActionException {
        // explore movement (just head towards explore direction)
        currentTargetLoc = explorer.getExploreTarget();
        safeFuzzyMove(currentTargetLoc, enemies);

        // fill in nearby tiles if possible
//        if (checkAndPaintTile(rc.getLocation())) {
//
//            if (shouldRefill()) {
//                previousState = state;
//                return UnitState.REFILL;
//            }
//            return state;
//        }

        if (!checkAndPaintTile(rc.getLocation())) {
            MapInfo[] attackableTilesInfo = rc.senseNearbyMapInfos(rc.getLocation(), UnitType.SOLDIER.actionRadiusSquared);
            //shuffleArray(attackableTiles);
            for (MapInfo info : attackableTilesInfo) {
                if (checkAndPaintTile(info)) {
                    if (shouldRefill()) {
                        previousState = state;
                        return UnitState.REFILL;
                    }
                    return state;
                }
            }
        }

        // find nearby ruins
        MapLocation[] nearbyRuins = senseNearbyCompletableTowerlessRuins();
        if (nearbyRuins.length > 0) {
            // add in logic to instantly start building if can -> avoid waiting extra turn to attack
            return UnitState.BUILD;
        }

        if (shouldRefill()) {
            previousState = state;
            return UnitState.REFILL;
        }

        return state;
    }

    private UnitState buildState() throws GameActionException {
        MapLocation[] nearbyRuins = senseNearbyCompletableTowerlessRuins();
        if (nearbyRuins.length == 0) {
            return UnitState.EXPLORE;
        }

        // get closer to/hover around ruin
        MapLocation closestNearby = getClosest(nearbyRuins);
        safeFuzzyMove(closestNearby, enemies);

        // try to complete pattern
        UnitType towerType = determineTowerPattern(closestNearby);
        // prioritize current tile
        MapInfo currTile = rc.senseMapInfo(rc.getLocation());
        int xOff = currTile.getMapLocation().x - closestNearby.x + 2;
        int yOff = currTile.getMapLocation().y - closestNearby.y + 2;
        PaintType neededPaint = determinePaintType(towerType, xOff, yOff);
        if (currTile.getPaint() == PaintType.EMPTY || currTile.getPaint() != neededPaint) {
            if (rc.canAttack(currTile.getMapLocation())) {
                rc.attack(currTile.getMapLocation(), neededPaint == PaintType.ALLY_SECONDARY);

                // complete tower if possible
                if (rc.canCompleteTowerPattern(towerType, closestNearby)) {
                    rc.completeTowerPattern(towerType, closestNearby);
                    return UnitState.EXPLORE;
                }
                // move on if tower can't be built
                else if (rc.getMoney() < towerType.moneyCost) {
                    return UnitState.EXPLORE;
                }

                return state;
            }
        }
        // otherwise, fill in the rest
        for (MapInfo patternTile : rc.senseNearbyMapInfos(closestNearby, 8)) {
            if (patternTile.getMapLocation().equals(closestNearby)) {
                continue;
            }
            xOff = patternTile.getMapLocation().x - closestNearby.x + 2;
            yOff = patternTile.getMapLocation().y - closestNearby.y + 2;
            neededPaint = determinePaintType(towerType, xOff, yOff);
            if (patternTile.getPaint() == PaintType.EMPTY || patternTile.getPaint() != neededPaint) {
                rc.setIndicatorDot(patternTile.getMapLocation(), 125, 125, 0);
                if (rc.canAttack(patternTile.getMapLocation())) {
                    rc.attack(patternTile.getMapLocation(), neededPaint == PaintType.ALLY_SECONDARY);

                    // complete tower if possible
                    if (rc.canCompleteTowerPattern(towerType, closestNearby)) {
                        rc.completeTowerPattern(towerType, closestNearby);
                        return UnitState.EXPLORE;
                    }
                    // move on if tower can't be built
                    else if (rc.getMoney() < towerType.moneyCost) {
                        return UnitState.EXPLORE;
                    }

                    return state;
                }
            }
        }

        // Repaint finished ruins
        MapLocation[] allRuins = rc.senseNearbyRuins(-1);
        if(allRuins.length > 0) {
            MapLocation closestRuin = getClosest(allRuins);

            // repaint
            for (MapInfo patternTile : rc.senseNearbyMapInfos(closestRuin, 8)) {
                if (patternTile.getMark() != patternTile.getPaint() && patternTile.getMark() != PaintType.EMPTY) {
                    boolean useSecondaryColor = patternTile.getMark() == PaintType.ALLY_SECONDARY;
                    if (rc.canAttack(patternTile.getMapLocation())) {
                        rc.attack(patternTile.getMapLocation(), useSecondaryColor);
                        rc.setIndicatorDot(patternTile.getMapLocation(), 255, 0, 0);
                        return state;
                    }
                }
            }
        }

        if (shouldRefill()) {
            previousState = state;
            return UnitState.REFILL;
        }

        return state;
    }

    private UnitState refillState() throws GameActionException {
        // refill movement (just head towrads nearest paint tower)
        currentTargetLoc = findNearestPaintTower();
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
        currentTargetLoc = findNearestPaintTower();
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

    public void turn() throws GameActionException {
        turnv2();
//
//        if (shouldRefill()) {
//            findNearestPaintTower();
//            if (nearestPaintTower != null){
//                rc.setIndicatorString("Exploring on paint!! Trying to refill at: " + nearestPaintTower);
//            } else {
//                rc.setIndicatorString("Can't find refill station");
//            }
//            if(!moveToNearestPaintTower()){
//                tryExploreOnPaint();
//            }
//        } else {
//            MapLocation[] nearbyRuins = senseNearbyCompletableTowerlessRuins();
//            rc.setIndicatorString("Nearby ruins: " + nearbyRuins.length);
//            if (nearbyRuins.length > 0) {
//                MapLocation closestRuin = getClosest(nearbyRuins);
//                // Check if ruin has already been marked
//                if (rc.senseMapInfo(closestRuin.subtract(dirTo(closestRuin))).getMark() == PaintType.EMPTY) {
//                    if (!tryMarkTower(randomTowerType(), closestRuin)) {
//                        fuzzyMove(closestRuin);
//                    }
//                    // if ruin has been marked
//                } else {
//                    fuzzyMove(closestRuin);
//                    // prioritize tile we're standing on
//                    MapInfo ourTile = rc.senseMapInfo(rc.getLocation());
//                    if (ourTile.getMark() != ourTile.getPaint() && ourTile.getMark() != PaintType.EMPTY) {
//                        boolean useSecondaryColor = ourTile.getMark() == PaintType.ALLY_SECONDARY;
//                        if (rc.canAttack(ourTile.getMapLocation())) {
//                            rc.setIndicatorDot(ourTile.getMapLocation(), 255, 0, 0);
//                            rc.attack(ourTile.getMapLocation(), useSecondaryColor);
//                        }
//                    }
//                    // fill in correct paint
//                    for (MapInfo patternTile : rc.senseNearbyMapInfos(closestRuin, 8)) {
//                        if (patternTile.getMark() != patternTile.getPaint() && patternTile.getMark() != PaintType.EMPTY) {
//                            boolean useSecondaryColor = patternTile.getMark() == PaintType.ALLY_SECONDARY;
//                            if (rc.canAttack(patternTile.getMapLocation())) {
//                                rc.attack(patternTile.getMapLocation(), useSecondaryColor);
//                                rc.setIndicatorDot(patternTile.getMapLocation(), 255,0,0);
//                            }
//                        }
//                    }
//                    // Complete the ruin if we can
//                    for (UnitType type : towerTypes) {
//                        if (rc.canCompleteTowerPattern(type, closestRuin)) {
//                            rc.completeTowerPattern(type, closestRuin);
//                            rc.setTimelineMarker("Tower built", 0, 255, 0);
//                            System.out.println("Built a tower at " + closestRuin + "!");
//                        }
//                    }
//                }
//                // no ruins nearby
//            } else {
//                // check if enemy tower in range, attack
//                RobotInfo tower = inEnemyTowerRange(rc.senseNearbyRobots(-1, rc.getTeam().opponent()));
//                if(tower != null) {
//                    for (Direction dir : directions) {
//                        MapLocation newLocation = rc.getLocation().add(dir);
//                        int dist = newLocation.distanceSquaredTo(tower.getLocation());
//                        if (dist > tower.type.actionRadiusSquared && dist <= rc.getType().actionRadiusSquared && rc.canMove(dir) && !isEnemyPaint(rc.senseMapInfo(newLocation).getPaint())) {
//                            rc.move(dir);
//                            break;
//                        }
//                    }
//                    tryAttack(tower.getLocation());
//                    // incase we're too close
//                    if (rc.getLocation().distanceSquaredTo(tower.getLocation()) <= tower.type.actionRadiusSquared) {
//                        fuzzyMove(dirTo(tower.getLocation()).opposite());
//                    }
//                // otherwise explore
//                }else{
//                    Direction enemyPaintDirection = getEnemyPaintDirection();
//                    if(enemyPaintDirection != null){
//                        if(isEnemyPaint(rc.senseMapInfo(rc.getLocation()).getPaint())){
//                            fuzzyMove(enemyPaintDirection.opposite());
//                        }
//                        if(RIGHT){
//                            fuzzyMove(enemyPaintDirection.rotateRight().rotateRight());
//                        }else{
//                            fuzzyMove(enemyPaintDirection.rotateLeft().rotateLeft());
//                        }
//                    }else{
//                        bugNav(explorer.getExploreTarget());
//                    }
//                }
//            }
//
//            // Repaint finished ruins
//            MapLocation[] allRuins = rc.senseNearbyRuins(-1);
//            if(allRuins.length > 0) {
//                MapLocation closestRuin = getClosest(allRuins);
//
//                // repaint
//                for (MapInfo patternTile : rc.senseNearbyMapInfos(closestRuin, 8)) {
//                    if (patternTile.getMark() != patternTile.getPaint() && patternTile.getMark() != PaintType.EMPTY) {
//                        boolean useSecondaryColor = patternTile.getMark() == PaintType.ALLY_SECONDARY;
//                        if (rc.canAttack(patternTile.getMapLocation())) {
//                            rc.attack(patternTile.getMapLocation(), useSecondaryColor);
//                            rc.setIndicatorDot(patternTile.getMapLocation(), 255, 0, 0);
//                        }
//                    }
//                }
//            }
//
//            if(!checkAndPaintTile(rc.getLocation())) {
//                MapInfo[] attackableTilesInfo = rc.senseNearbyMapInfos(rc.getLocation(), UnitType.SOLDIER.actionRadiusSquared);
//                //shuffleArray(attackableTiles);
//                for (MapInfo info : attackableTilesInfo) {
//                    if (checkAndPaintTile(info)) {
//                        break;
//                    }
//                }
//            }
//            for (MapLocation loc : getResourcePatternCenterLocations()) {
//                if (tryConfirmResourcePattern(loc)) {
//                    //rc.setTimelineMarker("Resource pattern confirmed", 0, 255, 0);
//                    rc.setIndicatorDot(loc, 0, 255, 0);
//                    System.out.println("Resource pattern confirmed at " + loc + "!");
//                }
//            }
//        }
//        markNearbyMapData();
    }
}

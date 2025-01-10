package YapYapYap_v10.Units;

import YapYapYap_v10.Unit;
import battlecode.common.*;

// TODO: Dont go into tower range, attack towers maybe?, moppers attack enemy units

public class Soldier extends Unit {

    Direction lastEnemyPaintDirection = null;

    public Soldier(RobotController robot) throws GameActionException {
        super(robot);
        state = UnitState.EXPLORE;
    }

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
            case UnitState.BUILD -> {
                buildState();
            }
        }

        stateInvariantActions();

        if(currentTargetLoc != null) {
            trySetIndicatorDot(currentTargetLoc, 255, 125, 0);
            trySetIndicatorLine(rc.getLocation(), currentTargetLoc, 125, 0, 125);
        }
        debugString.append("Currently in state: ").append(state.toString());
        rc.setIndicatorString(debugString.toString());
    }

    private UnitState determineState() throws GameActionException{
        if ((state != UnitState.REFILLING && shouldRefill()) || (state == UnitState.REFILLING && rc.getPaint() < rc.getType().paintCapacity * 0.75)) {
            if (returnLoc == null) {
                returnLoc = rc.getLocation();
            }
            return UnitState.REFILLING;
        }
        if (closestCompletableRuin != null) {
            return UnitState.BUILD;
        }
        return UnitState.EXPLORE;
    }

    private void stateInvariantActions() throws GameActionException{
        // fill nearby incomplete ruins
        fillRuin(closestAnyRuin);

        // dont fill stuff if youre low
        if(state != UnitState.REFILLING) {
            if (!checkAndPaintTile(rc.getLocation())) {
                MapInfo[] attackableTilesInfo = rc.senseNearbyMapInfos(rc.getLocation(), UnitType.SOLDIER.actionRadiusSquared);
                for (MapInfo info : attackableTilesInfo) {
                    if (checkAndPaintTile(info)) {
                        break;
                    }
                }
            }
        }
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

    private UnitState exploreState() throws GameActionException {
        // otherwise explore
        if(rc.isMovementReady()){
            if (returnLoc != null) {
                if (distTo(returnLoc) <= 8) {
                    returnLoc = null;
                } else {
                    currentTargetLoc = returnLoc;
                    safeFuzzyMove(returnLoc, enemies);
                }
            }
            Direction enemyPaintDirection = getEnemyPaintDirection();
            RobotInfo tower = inEnemyTowerRange(enemies);
            if(enemyPaintDirection != null){
                if(lastEnemyPaintDirection == null){
                    RIGHT = !RIGHT;
                }
                lastEnemyPaintDirection = enemyPaintDirection;
                MapInfo paintInfo = rc.senseMapInfo(rc.getLocation().add(enemyPaintDirection));
                if(isEnemyPaint(rc.senseMapInfo(rc.getLocation()).getPaint())){
                    fuzzyMove(enemyPaintDirection.opposite());
                }else{
                    if(!isEnemyPaint(paintInfo.getPaint())) {
                        safeFuzzyMove(enemyPaintDirection, enemies);
                    }
                    else{
                        if(rc.getID() % 2 == 0 && rc.getRoundNum() % 20 < 10){
                            safeFuzzyMove(enemyPaintDirection.rotateRight().rotateRight(), enemies);
                        }else{
                            safeFuzzyMove(enemyPaintDirection.rotateLeft().rotateLeft(), enemies);
                        }
                    }
                }
//                Direction startingDirection;
//                if(RIGHT){
//                    startingDirection = enemyPaintDirection.rotateRight().rotateRight();
//                }else{
//                    startingDirection = enemyPaintDirection.rotateLeft().rotateLeft();
//                }
////                trySetIndicatorLine(rc.getLocation(), rc.getLocation().add(startingDirection), 0, 0, 255);
////                trySetIndicatorLine(rc.getLocation(), rc.getLocation().add(enemyPaintDirection), 255, 0, 0);
//                rc.setIndicatorString("trying to following enemy paint");
//                for (Direction dir : fuzzyDirs(startingDirection)) {
//                    if(rc.onTheMap(rc.getLocation().add(dir)) && !isEnemyPaint(rc.senseMapInfo(rc.getLocation().add(dir)).getPaint()) && (tower == null || rc.getLocation().distanceSquaredTo(tower.getLocation()) > tower.type.actionRadiusSquared) && rc.canMove(dir)){
//                        rc.move(dir);
//                        rc.setIndicatorString("Following enemy paint");
//                        break;
//                    }
//                }
//                if(rc.isMovementReady()){
//                    RIGHT = !RIGHT;
//                }
            }else{
                if(rc.isMovementReady()) {
                    currentTargetLoc = explorer.getExploreTarget();
                    safeFuzzyMove(currentTargetLoc, enemies);
                }
            }
        }

        return state;
    }

    private UnitState buildState() throws GameActionException {
        // get closer to/hover around ruin
        safeFuzzyMove(closestCompletableRuin, enemies);

        fillRuin(closestCompletableRuin);
        return state;
    }

    private boolean fillRuin(MapLocation ruinLoc) throws GameActionException {
        // try to complete pattern
        UnitType towerType = determineTowerPattern(ruinLoc);
        // prioritize current tile if in range
        if (rc.getLocation().isWithinDistanceSquared(ruinLoc, 8)) {
            MapInfo currTile = rc.senseMapInfo(rc.getLocation());
            int xOff = currTile.getMapLocation().x - ruinLoc.x + 2;
            int yOff = currTile.getMapLocation().y - ruinLoc.y + 2;
            PaintType neededPaint = determinePaintType(towerType, xOff, yOff);
            if (currTile.getPaint() == PaintType.EMPTY || currTile.getPaint() != neededPaint) {
                if (rc.canAttack(currTile.getMapLocation())) {
                    rc.attack(currTile.getMapLocation(), neededPaint == PaintType.ALLY_SECONDARY);
                    return true;
                }
            }
        }
        // otherwise, fill in the rest
        for (MapInfo patternTile : rc.senseNearbyMapInfos(ruinLoc, 8)) {
            if (patternTile.getMapLocation().equals(ruinLoc)) {
                continue;
            }
            int xOff = patternTile.getMapLocation().x - ruinLoc.x + 2;
            int yOff = patternTile.getMapLocation().y - ruinLoc.y + 2;
            PaintType neededPaint = determinePaintType(towerType, xOff, yOff);
            if (patternTile.getPaint() == PaintType.EMPTY || patternTile.getPaint() != neededPaint) {
                trySetIndicatorDot(patternTile.getMapLocation(), 125, 125, 0);
                if (rc.canAttack(patternTile.getMapLocation())) {
                    rc.attack(patternTile.getMapLocation(), neededPaint == PaintType.ALLY_SECONDARY);
                    return true;
                }
            }
        }
        return false;
    }


}

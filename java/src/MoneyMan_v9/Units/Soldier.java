package MoneyMan_v9.Units;

import MoneyMan_v9.Unit;
import battlecode.common.*;

import java.util.ArrayList;
import java.util.Arrays;

// TODO: Dont go into tower range, attack towers maybe?, moppers attack enemy units

public class Soldier extends Unit {

    Direction lastEnemyPaintDirection = null;

    public Soldier(RobotController robot) throws GameActionException {
        super(robot);
        state = UnitState.EXPLORE;
    }

    public void turn() throws GameActionException {
        debugString.setLength(0);
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
            case UnitState.BUILDSRP -> {
                buildSRPState();
            }
        }

        stateInvariantActions();

        if(currentTargetLoc != null) {
            trySetIndicatorDot(currentTargetLoc, 255, 125, 0);
            rc.setIndicatorLine(rc.getLocation(), currentTargetLoc, 125, 0, 125);
        }
        debugString.append("Currently in state: ").append(state.toString());
        rc.setIndicatorString(debugString.toString());
    }

    MapLocation completableSRP = null;
    private UnitState determineState() throws GameActionException{
        if ((state != UnitState.REFILLING && shouldRefill()) || (state == UnitState.REFILLING && rc.getPaint() < rc.getType().paintCapacity * 0.75)) {
            if (returnLoc == null) {
                returnLoc = rc.getLocation();
            }
            return UnitState.REFILLING;
        }
        if (closestCompletableRuin != null && rc.getChips() >= 900){//UnitType.LEVEL_ONE_PAINT_TOWER.paintCost - 100) {
            return UnitState.BUILD;
        }
        completableSRP = closestCompletableSRP();
        if (completableSRP != null){
            return UnitState.BUILDSRP;
        }

        return UnitState.EXPLORE;
    }

    private void stateInvariantActions() throws GameActionException{
        // dont fill stuff if youre low
        if(state != UnitState.REFILLING) {
//            for(MapLocation loc : rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), rc.getType().actionRadiusSquared)){
//                switch(mapData.tileColors.getVal(loc)){
//                    case -1:
//                        trySetIndicatorDot(loc, 0, 0, 150);
//                        break;
//                    case 0:
//                        trySetIndicatorDot(loc, 0, 150, 0);
//                        break;
//                    case 1:
//                        trySetIndicatorDot(loc, 150, 0, 0);
//                        break;
//                }
//            }

            // fill underneath yourself
            if (!checkAndPaintTile(rc.getLocation()) && rc.isActionReady()) {
                // then fill other stuff
                if(rc.isActionReady()) {
                    MapLocation[] targets = mapLocationSpiral(rc.getLocation(), 3);
                    for (MapLocation target : targets) {
                        if (checkAndPaintTile(target)) {
                            break;
                        }
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
        mapData.SRPs.updateIterable();
        for (int i = mapData.SRPs.size; --i >= 0; ) {
            if (tryConfirmResourcePattern(mapData.SRPs.locs[i])) {
                //rc.setTimelineMarker("Resource pattern confirmed", 0, 255, 0);
                trySetIndicatorDot(mapData.SRPs.locs[i], 0, 255, 0);
                System.out.println("Resource pattern confirmed at " + mapData.SRPs.locs[i] + "!");
            }
        }
    }


    private void buildSRPState() throws GameActionException{
        trySetIndicatorDot(completableSRP, 0, 255, 255);
        if(!rc.getLocation().equals(completableSRP)){
            fuzzyMove(completableSRP);
        }else{
            if(mapData.getMapInfo(completableSRP).getMark() == PaintType.EMPTY && rc.canMark(completableSRP)){
                debugString.append("Marking SRP");
                rc.mark(completableSRP, SRP_MARKER_BOOL);
            }else{
                debugString.append("Cant mark SRP");
            }
        }
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
////                rc.setIndicatorLine(rc.getLocation(), rc.getLocation().add(startingDirection), 0, 0, 255);
////                rc.setIndicatorLine(rc.getLocation(), rc.getLocation().add(enemyPaintDirection), 255, 0, 0);
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
        return state;
    }

//    private boolean fillRuin(MapLocation ruinLoc) throws GameActionException {
//        // try to complete pattern
//        UnitType towerType = determineTowerPattern(ruinLoc);
//        // prioritize current tile if in range
//        if (rc.getLocation().isWithinDistanceSquared(ruinLoc, 8)) {
//            MapInfo currTile = rc.senseMapInfo(rc.getLocation());
//            int xOff = currTile.getMapLocation().x - ruinLoc.x + 2;
//            int yOff = currTile.getMapLocation().y - ruinLoc.y + 2;
//            PaintType neededPaint = determinePaintType(towerType, xOff, yOff);
//            if (currTile.getPaint() == PaintType.EMPTY || currTile.getPaint() != neededPaint) {
//                if (rc.canAttack(currTile.getMapLocation())) {
//                    rc.attack(currTile.getMapLocation(), neededPaint == PaintType.ALLY_SECONDARY);
//                    return true;
//                }
//            }
//        }
//        // otherwise, fill in the rest
//        for (MapInfo patternTile : rc.senseNearbyMapInfos(ruinLoc, 8)) {
//            if (patternTile.getMapLocation().equals(ruinLoc)) {
//                continue;
//            }
//            int xOff = patternTile.getMapLocation().x - ruinLoc.x + 2;
//            int yOff = patternTile.getMapLocation().y - ruinLoc.y + 2;
//            PaintType neededPaint = determinePaintType(towerType, xOff, yOff);
//            if (patternTile.getPaint() == PaintType.EMPTY || patternTile.getPaint() != neededPaint) {
//                trySetIndicatorDot(patternTile.getMapLocation(), 125, 125, 0);
//                if (rc.canAttack(patternTile.getMapLocation())) {
//                    rc.attack(patternTile.getMapLocation(), neededPaint == PaintType.ALLY_SECONDARY);
//                    return true;
//                }
//            }
//        }
//        return false;
//    }

    private MapLocation closestCompletableSRP() throws GameActionException{
        mapData.ruins.updateIterable();
        mapData.SRPs.updateIterable();
        for(MapLocation loc : mapLocationSpiral(rc.getLocation(), 3)){
            if(rc.getLocation().distanceSquaredTo(loc) > GameConstants.RESOURCE_PATTERN_RADIUS_SQUARED || !rc.onTheMap(loc)){
                continue;
            }
            if(!mapData.SRPExclusionZone[loc.x][loc.y] && rc.canMarkResourcePattern(loc) ){
                boolean bad = false;
                boolean finished = true;
                for(int x = -2; x <= 2; x++){
                    if(!finished || bad) {
                        break;
                    }
                    for(int y = -2; y <= 2; y++){
                        MapLocation checkLoc = loc.translate(x, y);
                        MapInfo checkLocInfo = mapData.getMapInfo(checkLoc);
                        // check to make sure its not finished and that theres no enemy paint on it
                        if(checkLocInfo == null){
                            continue;
                        }
                        if(checkLocInfo.getPaint() == PaintType.EMPTY){
                            finished = false;
                            break;
                        }
                        if(isEnemyPaint(checkLocInfo.getPaint())){
                            bad = true;
                            break;
                        }
                    }
                }
                if(!bad && !finished){
                    return loc;
                }
            }else{
                trySetIndicatorDot(loc, 0, 0, 0);
            }
//            boolean incompletable = false;
//            for(int x = -2; x <= 2; x++){
//                if(incompletable){
//                    break;
//                }
//                for(int y = -2; y <= 2; y++){
//                    MapLocation checkLoc = loc.translate(x, y);
//                    MapInfo checkLocInfo = mapData.getMapInfo(checkLoc);
//                    if(!rc.canSenseLocation(loc) || checkLocInfo == null || isEnemyPaint(checkLocInfo.getPaint()) || checkLocInfo.hasRuin()){
//                        incompletable = true;
//                        break;
//                    }
//                }
//            }
//            if(!incompletable){
//                return loc;
//            }
        }
        return null;
    }

}

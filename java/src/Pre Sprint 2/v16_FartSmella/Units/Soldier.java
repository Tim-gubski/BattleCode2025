package v16_FartSmella.Units;

import v16_FartSmella.Unit;
import battlecode.common.*;

// TODO: Dont go into tower range, attack towers maybe?, moppers attack enemy units

public class Soldier extends Unit {

    Direction lastEnemyPaintDirection = null;

    public Soldier(RobotController robot) throws GameActionException {
        super(robot);
        state = UnitState.EXPLORE;
    }

    public void turn() throws GameActionException {
        debugString.setLength(0);
//        int bytecode = Clock.getBytecodesLeft();
        senseNearby(); // perform all scans
        communication.parseMessages();
//        System.out.println("Bytecodes used for senseNearby: " + (bytecode - Clock.getBytecodesLeft()));

//        int bytecode = Clock.getBytecodesLeft();
//        int[] closestRuin = mapData.ruinTree.nearestNeighbor(rc.getLocation().x, rc.getLocation().y);
//        System.out.println("Closest ruin by tree: " + closestRuin[0] + ", " + closestRuin[1]);
//        System.out.println("Bytecodes used for nearestNeighbor: " + (bytecode - Clock.getBytecodesLeft()));

        previousState = state;
        state = determineState();

        switch (state) {
            case UnitState.EXPLORE -> {
                exploreState();
            }
            case UnitState.COMBAT -> {
                combatState();
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
        debugString.append("Currently in state: ").append(state.toString());
    }

    MapLocation completableSRP = null;
    private UnitState determineState() throws GameActionException{
        // make the build check smarter, could see if you have enough paint to finish a tower.
        if ((state != UnitState.REFILLING && shouldRefill() && (state != UnitState.BUILD || rc.getPaint() < 25)) || (state == UnitState.REFILLING && rc.getPaint() < rc.getType().paintCapacity * 0.75)) {
            if (returnLoc == null) {
                returnLoc = rc.getLocation();
            }
            return UnitState.REFILLING;
        }
        if(closestEnemyTower != null){
            return UnitState.COMBAT;
        }

        if (closestCompletableRuin != null && (rc.getChips() >= 700 || rc.getNumberTowers() < 5)){//UnitType.LEVEL_ONE_PAINT_TOWER.paintCost - 100) {
            return UnitState.BUILD;
        }
//        int startByte = Clock.getBytecodesLeft();
        completableSRP = closestCompletableSRP();
//        System.out.println("Bytecodes used for finding srp: " + (startByte - Clock.getBytecodesLeft()));
        if (completableSRP != null && rc.getNumberTowers() >= 5){
            return UnitState.BUILDSRP;
        }

        return UnitState.EXPLORE;
    }

    private void stateInvariantActions() throws GameActionException{
        // dont fill stuff if youre low
        if(state != UnitState.REFILLING) {
            // fill underneath yourself
            if (!checkAndPaintTile(rc.getLocation()) && rc.isActionReady()) {
                // then fill other stuff
                if(rc.isActionReady()) {
                    MapLocation[] targets = mapLocationSpiral(rc.getLocation(), 3);
                    // fill ruins first
                    if(closestCompletableRuin != null) {
                        int xOffset = closestCompletableRuin.x - rc.getLocation().x;
                        int yOffset = closestCompletableRuin.y - rc.getLocation().y;
                        for (MapLocation target : targets) {
                            if (Clock.getBytecodesLeft() < 1000 || checkAndPaintTile(target.translate(xOffset, yOffset))) {
                                break;
                            }
                        }
                    }else{
                        // fill other stuff
                        for (MapLocation target : targets) {
                            if (Clock.getBytecodesLeft() < 1000 || checkAndPaintTile(target)) {
                                break;
                            }
                        }
                    }
                }
            }
        }
        // confirm all tower patterns
        // optimize later if you can
        if(closestAnyRuin != null) {
            for(UnitType towerType : towerTypes) {
                if (rc.canCompleteTowerPattern(towerType, closestAnyRuin)) {
                    rc.completeTowerPattern(towerType, closestAnyRuin);
                    rc.setTimelineMarker("Tower built", 0, 255, 0);
                    System.out.println("Built a tower at " + closestAnyRuin + "!");
                }
            }
        }

        // confirm all resource patterns
        if(rc.getNumberTowers() > 5) {
            mapData.SRPs.updateIterable();
            for (int i = mapData.SRPs.size; --i >= 0; ) {
                if (tryConfirmResourcePattern(mapData.SRPs.locs[i])) {
                    //rc.setTimelineMarker("Resource pattern confirmed", 0, 255, 0);
                    trySetIndicatorDot(mapData.SRPs.locs[i], 0, 255, 0);
                }
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

    private void combatState() throws GameActionException {
        if(distTo(closestEnemyTower.getLocation()) <= rc.getType().actionRadiusSquared){
            if(!mapData.getMapInfo(rc.getLocation()).getPaint().isAlly()){
                tryAttack(rc.getLocation());
            }else{
                tryAttack(closestEnemyTower.getLocation());
            }
            if(!tryMoveOutOfRange(closestEnemyTower.getLocation(), closestEnemyTower.type.actionRadiusSquared)){
                fuzzyMove(dirTo(closestEnemyTower.getLocation()).opposite());
            }
        }else{
            if(!tryMoveIntoRange(closestEnemyTower.getLocation(), rc.getType().actionRadiusSquared)){
                fuzzyMove(closestEnemyTower.getLocation());
            }
            if(!mapData.getMapInfo(rc.getLocation()).getPaint().isAlly()){
                tryAttack(rc.getLocation());
            }
            tryAttack(closestEnemyTower.getLocation());
        }
    }

    private UnitState exploreState() throws GameActionException {
        // otherwise explore
        if(rc.isMovementReady()){
            if (returnLoc != null) {
                debugString.append(String.format("returning to %d, %d", returnLoc.x, returnLoc.y));
                if (distTo(returnLoc) <= 8) {
                    returnLoc = null;
                } else {
                    currentTargetLoc = returnLoc;
                    safeFuzzyMove(returnLoc, enemies);
                }
            }
            Direction enemyPaintDirection = getEnemyPaintDirection();
//            RobotInfo tower = inEnemyTowerRange(enemies);
            if(enemyPaintDirection != null && enemyPaintDirection != Direction.CENTER && enemies.length > 0){
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
                    } else {
                        if(rc.getID() % 2 == 0 && rc.getRoundNum() % 20 < 10){
                            safeFuzzyMove(enemyPaintDirection.rotateRight().rotateRight(), enemies);
                        } else {
                            safeFuzzyMove(enemyPaintDirection.rotateLeft().rotateLeft(), enemies);
                        }
                    }
                }
            }else{
                boolean surroundedByAllyPaint = true;
                for(Direction dir : directions){
                    if(!rc.onTheMap(rc.getLocation().add(dir))) continue;
                    MapInfo info = mapData.getMapInfo(rc.getLocation().add(dir));
                    if(info == null || !info.getPaint().isAlly()){
                        surroundedByAllyPaint = false;
                        break;
                    }
                }
                if(surroundedByAllyPaint){
                    MapLocation closestEmptyPaint = null;
                    int closestDist = Integer.MAX_VALUE;
                    for(MapInfo info : mapInfo){
                        if(info.getPaint() == PaintType.EMPTY && !info.hasRuin() && !info.isWall()){
                            int dist = distTo(info.getMapLocation());
                            if(dist < closestDist){
                                closestDist = dist;
                                closestEmptyPaint = info.getMapLocation();
                            }
                        }
                    }
                    if (closestEmptyPaint != null) {
                        rc.setIndicatorLine(rc.getLocation(), closestEmptyPaint, 0, 100, 100);
                        debugString.append("Moving to closest empty paint " + closestEmptyPaint);
                        safeFuzzyMove(closestEmptyPaint, enemies);
                    }
                }

                if(rc.isMovementReady()) {
                    currentTargetLoc = explorer.getExploreTarget();
                    rc.setIndicatorLine(rc.getLocation(), currentTargetLoc, 100, 0, 100);
                    safeFuzzyMove(currentTargetLoc, enemies);
                }
            }
        }

        return state;
    }

    private UnitState buildState() throws GameActionException {
        Direction markDir = null;
        MapInfo info = null;
        for(Direction dir : directions){
            info = mapData.getMapInfo(closestCompletableRuin.add(dir));
            if(info != null && info.getMark() == TOWER_MARKER_COLOR){
                markDir = dir;
                break;
            }
        }
        if(markDir == null){
            UnitType towerType = determineTowerPattern(closestCompletableRuin);
            Direction markDirection = towerTypeToMarkDirection(towerType);
            MapLocation markLocation = closestCompletableRuin.add(markDirection);
            if(rc.canMark(markLocation)){
                rc.mark(markLocation, true);
                markDir = markDirection;
            }
        }

        if(markDir != null) {
            mapData.markPattern(closestCompletableRuin, markDirectionToTowerType(markDir));
        }

        // get closer to/hover around ruin
        //TODO make this smarter
        for(MapLocation loc : rc.getAllLocationsWithinRadiusSquared(closestCompletableRuin, 8)){
            if(!mapData.getMapInfo(loc).getPaint().isAlly()){
                fuzzyMove(loc);
                break;
            }
        }
//        safeFuzzyMove(closestCompletableRuin, enemies);
        return state;
    }

    private MapLocation[] srpGridLocs(MapLocation loc) throws GameActionException{
        int x = rc.getLocation().x;
        int y = rc.getLocation().y;
        // Find the nearest valid y such that (y+1) % 3 == 0
        int newY = y - (y + 1) % 3;

        // Compute the base x that satisfies the second condition
        int baseX = ((3 - (newY + 1) / 3) % 4 + 4) % 4; // Ensure non-negative remainder
        int closestX = x - (x % 4) + baseX;
        int x1 = closestX, x2 = closestX + 4;
        int d1 = (x1 - x) * (x1 - x) + (newY - y) * (newY - y);
        int d2 = (x2 - x) * (x2 - x) + (newY - y) * (newY - y);

        if (d2 < d1) closestX = x2;

        return new MapLocation[]{
                new MapLocation(closestX, newY),
                new MapLocation(closestX-1, newY+3),
                new MapLocation(closestX+4, newY),
                new MapLocation(closestX+3, newY+3),
        };
    }

    private MapLocation closestCompletableSRP() throws GameActionException{
        mapData.ruins.updateIterable();
        mapData.SRPs.updateIterable();
//        MapLocation[] locsToCheck = new MapLocation[49];
//        System.arraycopy(srpGridLocs(rc.getLocation()),0,locsToCheck,0,4);
//        System.arraycopy(mapLocationSpiral(rc.getLocation(),3),0,locsToCheck,4, 49);
        for(MapLocation loc : mapLocationSpiral(rc.getLocation(),3)){
            if(Clock.getBytecodeNum()>10000){
                debugString.append("Terminating closest SRP early");
                return null;
            }
            if(rc.getLocation().distanceSquaredTo(loc) > GameConstants.RESOURCE_PATTERN_RADIUS_SQUARED || !rc.onTheMap(loc)){
                continue;
            }
            if(mapData.SRPExclusionZoneInt[loc.x][loc.y] == 0){
                if(!rc.canMarkResourcePattern(loc)){
                    mapData.SRPExclusionZoneInt[loc.x][loc.y]++;
                    continue;
                }
                boolean bad = false;
                boolean finished = true;
                MapLocation checkLoc;
                MapInfo checkLocInfo;
                for(int x = -2; x <= 2; x++){
                    for(int y = -2; y <= 2; y++){
                        checkLoc = loc.translate(x, y);
                        checkLocInfo = mapData.getMapInfo(checkLoc);
                        // check to make sure its not finished and that theres no enemy paint on it
                        if(checkLocInfo == null){
                            continue;
                        }
                        if((checkLocInfo.getPaint() != (mapData.resourcePattern[x+2][y+2] == 2 ? PaintType.ALLY_SECONDARY : PaintType.ALLY_PRIMARY))){
                            finished = false;
                        }
                        if(checkLocInfo.getPaint().isEnemy()){
                            bad = true;
                            break;
                        }
                    }
                    if(bad) {   // mista white
                        break; // jesse
                    }
                }
//                if(bad){
//                    mapData.SRPExclusionZone[loc.x][loc.y] = true;
//                }
                if(!bad && !finished){
                    return loc;
                }
            }else{
//                trySetIndicatorDot(loc, 0, 0, 0);
            }
        }
        return null;
    }
}

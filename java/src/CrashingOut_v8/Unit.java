package CrashingOut_v8;

import CrashingOut_v8.Helpers.Explore;
import CrashingOut_v8.Helpers.MapData;
import battlecode.common.*;

import java.util.Arrays;

public abstract class Unit extends Robot {
    public MapData mapData;
    public Explore explorer;
    public MapLocation nearestPaintTower = null;
    public MapLocation returnLoc = null;

    public MapInfo[] mapInfo = null;
    public RobotInfo[] allies = null;
    public RobotInfo[] enemies = null;
    public MapLocation[] completableRuins = null;
    public MapLocation closestCompletableRuin = null;
    public MapLocation[] allRuins = null;
    public MapLocation closestAnyRuin = null;

    protected MapLocation currentTargetLoc = null;

    public MapLocation lastSeenTower = null;
    // enumerate states
    protected enum UnitState {
        REFILLING, // for when bot is actively refilling
        EXPLORE, // for when bot is exploring

        // unit specific states, have to start here unfortunately
        MOPPING, // moppers mopping

        ATTACK, // soldiers shoot, moppers sweep, splashers splash

        BUILD, // finish ruin and build tower
    }

    protected UnitState state = null;
    protected UnitState previousState = null;

    public Unit(RobotController robot) throws GameActionException {
        super(robot);
        mapData = new MapData(width, height);
        explorer = new Explore(rc, width, height, mapData);
        markNearbyMapData();
        nearestPaintTower = findNearestPaintTower();
    }

    protected void senseNearby() throws GameActionException {
        mapInfo = rc.senseNearbyMapInfos();
        allies = rc.senseNearbyRobots(-1, rc.getTeam());
        enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
        completableRuins = senseNearbyCompletableTowerlessRuins();
        allRuins = rc.senseNearbyRuins(-1);
        closestCompletableRuin = getClosest(completableRuins);
        if(allRuins.length > 0){
            closestAnyRuin = getClosest(allRuins);
        }
        debugString.append("Closest ruin: ").append(closestCompletableRuin);
    }

    public void markNearbyMapData() throws GameActionException{
        for(MapLocation loc : rc.getAllLocationsWithinRadiusSquared(rc.getLocation(),-1)){
            mapData.setVisited(loc);

            // check if loc is a ruin
            if (rc.canSenseLocation(loc)) {
                MapInfo info = rc.senseMapInfo(loc);
                if (info.hasRuin()) {
                    // mark the tower for recall
                    if (rc.canSenseRobotAtLocation(loc) && rc.senseRobotAtLocation(loc).getTeam() == rc.getTeam() && lastSeenTower == null) {
                        lastSeenTower = loc;
                    }

                    // if we already have this tower saved as a paint tower
                    int containsPaintTower = mapData.containsPaintTowerLocation(loc);

                    // check if paint tower here and is not added and has paint -> add as refill station
                    if (containsPaintTower == -1 && rc.canSenseRobotAtLocation(loc)) {
                        RobotInfo robot = rc.senseRobotAtLocation(loc);
                        if (robot.getTeam() == rc.getTeam() && (isPaintTower(robot.getType()) || isMoneyTower(robot.getType())) && robot.getPaintAmount() > 30) {
                            mapData.setPaintTower(robot.getLocation(), rc.getLocation());
                        }
                    }

                    // check if paint tower is here and already added but empty -> remove as refill station
                    else if (containsPaintTower != -1 && rc.canSenseRobotAtLocation(loc)) {
                        RobotInfo robot = rc.senseRobotAtLocation(loc);
                        if (robot.getPaintAmount() == 0) {
                            mapData.removePaintTowerLocation(containsPaintTower);
                        }
                    }

                    // otherwise, check if location in stored paint locations, replace if paint tower no longer exists
                    else if (containsPaintTower != -1 && !rc.canSenseRobotAtLocation(loc)) {
                        mapData.removePaintTowerLocation(containsPaintTower);
                    }
                }
            }
        }
    }

    protected void confirmNearbyTowers() throws GameActionException {
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
    }

    protected boolean shouldRefill() throws GameActionException {
        return rc.getPaint() <= rc.getType().paintCapacity * 0.25;
    }

    protected void refillSelf() throws GameActionException {
        // cant do anything if no nearest tower
        if (nearestPaintTower == null) {
            rc.setIndicatorString("FAILED REFILL 1");
            return;
        }

        // check if tower in range
        if (!rc.getLocation().isWithinDistanceSquared(nearestPaintTower, rc.getType().actionRadiusSquared)) {
            return;
        }

        int needed = rc.getType().paintCapacity - rc.getPaint();
        int transferAmount = 0;
        if (rc.canSenseRobotAtLocation(nearestPaintTower)) {
            RobotInfo tower = rc.senseRobotAtLocation(nearestPaintTower);
            if (tower.getTeam() != rc.getTeam()) {
                nearestPaintTower = null;
                rc.setIndicatorString("FAILED REFILL 3");
                return;
            }

            int availablePaint = tower.getPaintAmount();

            transferAmount = Math.min(needed, availablePaint);
        }

        if (transferAmount <= 0) {
            rc.setIndicatorString("FAILED REFILL 4");
            return;
        }

        if (rc.canTransferPaint(nearestPaintTower, -transferAmount)) {
            rc.transferPaint(nearestPaintTower, -transferAmount);
        }
    }

    protected UnitState refillingState() throws GameActionException {

        // ensure tower is still good
        MapLocation foundTower = findNearestPaintTower();
        debugString.append(foundTower);
        if (foundTower == null) {
            // need to find a tower atp
            // path find on paint
//            safeFuzzyMove(lastSeenTower, enemies);
//            if (rc.getLocation().isWithinDistanceSquared(lastSeenTower, 8)) {
//                lastSeenTower = explorer.getExploreTarget();
//            }
            debugString.append("Exploring on Paint");
            if(!tryExploreOnPaint()){
                // find closest ally paint
                MapLocation nearestAllyPaint = locateAllyPaint();
                if(nearestAllyPaint != null){
                    safeFuzzyMove(nearestAllyPaint, enemies);
                }else {
                    safeFuzzyMove(explorer.getExploreTarget(), enemies);
                }
            }

            return state;
        }
        nearestPaintTower = foundTower;
        
        // if in range of tower, refill
        if (rc.getLocation().isWithinDistanceSquared(foundTower, 2)) {
            refillSelf();
            rc.setIndicatorDot(foundTower, 0, 255, 0);
            // done filling
            if (rc.getPaint() > rc.getType().paintCapacity * 0.75) {
                state = UnitState.EXPLORE;
                return UnitState.EXPLORE;
            }
        }

        // remove tower
        if (rc.canSenseRobotAtLocation(foundTower) && rc.senseRobotAtLocation(foundTower).getPaintAmount() < 30) {
            if (isMoneyTower(rc.senseRobotAtLocation(foundTower).getType())) {
                int index = mapData.containsPaintTowerLocation(foundTower);
                mapData.removePaintTowerLocation(index);
            }
            return state;
        }

        // move closer to tower
        if (rc.getLocation().distanceSquaredTo(foundTower) > 2) {
            rc.setIndicatorDot(foundTower, 255, 255, 255);
            safeFuzzyMove(foundTower, enemies);
        }
        return state;
    }

    protected MapLocation findNearestPaintTower() throws GameActionException {
        return mapData.getNearestPaintTower(rc.getLocation());
    }

    protected boolean moveToNearestPaintTower() throws GameActionException {
        // make some way to find tower if no available ones
        if (nearestPaintTower == null) {
            return false;
        }

        // move to tower
        if (!rc.getLocation().isWithinDistanceSquared(nearestPaintTower, 2)) {
            bugNav(nearestPaintTower);
        } else {
            refillSelf();
        }
        return true;
    }

    public Direction paintExploreDirection = null;
    public boolean tryExploreOnPaint() throws GameActionException {
        if(!rc.isMovementReady()){
            return false;
        }
        if (paintExploreDirection == null) {
            paintExploreDirection = randomDirection();
        }
        for(Direction dir : fuzzyDirs(paintExploreDirection)){
            MapLocation loc = rc.getLocation().add(dir);
            if(rc.canSenseLocation(loc)){
                MapInfo info = rc.senseMapInfo(loc);
                rc.setIndicatorDot(loc, 0, 0, 255);
                if(info.getPaint().isAlly()){
                    if(tryMove(dir)){
                        paintExploreDirection = dir;
                        return true;
                    }
                }
            }
        }
        paintExploreDirection = paintExploreDirection.opposite();
        return false;
    }

    public void navAdjacent(MapLocation loc) throws GameActionException {
        if (!rc.getLocation().isAdjacentTo(loc)) {
            bugNav(loc);
        }
    }

    // tries moving in the given direction dir
    public boolean tryMove(Direction dir) throws GameActionException {
        if(rc.canMove(dir)){
            rc.move(dir);
            return true;
        }
        return false;
    }

    public void fuzzyMove(Direction dir) throws GameActionException {
        for(Direction d: fuzzyDirs(dir)){
            if(rc.canMove(d)){
                rc.move(d);
                return;
            }
        }
    }

    public void fuzzyMove(MapLocation loc) throws GameActionException{
        fuzzyMove(dirTo(loc));
    }

    // safety filter method to check if location is safe to move to
    // TODO: can make this threshold based so that robots can do risky things when necessary/if low risk
    public boolean isSafe(MapLocation loc, RobotInfo[] enemies) throws GameActionException {
        // check if in tower range
        for (RobotInfo enemy : enemies) {
            if (!enemy.getType().isTowerType()) {
                continue;
            }

            if (loc.isWithinDistanceSquared(enemy.getLocation(), enemy.getType().actionRadiusSquared)) {
                rc.setIndicatorDot(enemy.getLocation(), 255, 0, 0);
                return false;
            }
        }

        // check if on ally paint
        MapInfo info = rc.senseMapInfo(loc);
        if ((isEnemyPaint(info.getPaint()) && rc.getPaint() < rc.getType().paintCapacity * 0.4) ||
                (info.getPaint() == PaintType.EMPTY && rc.getPaint() < rc.getType().paintCapacity * 0.2)) {
            rc.setIndicatorDot(info.getMapLocation(), 255, 0, 0);
            return false;
        }

        return true;
    }

    MapLocation[] prevPositions = new MapLocation[5];
    int prevPosIndex = 0;

    // returns true if able to fuzzy move safely in desired direction, returns false if unable to move
    public boolean safeFuzzyMove(Direction dir, RobotInfo[] enemies) throws GameActionException {
        for (Direction d : fuzzyDirs(dir)) {
            if (!rc.canMove(d)) {
                rc.setIndicatorDot(rc.getLocation().add(d), 255, 0, 0);
                continue;
            }

            MapLocation fuzzyLoc = rc.getLocation().add(d);
            rc.setIndicatorDot(fuzzyLoc, 0, 0, 125);
            if (isSafe(fuzzyLoc, enemies) && !isPreviousLocation(fuzzyLoc)) {
                rc.move(d);
                prevPositions[prevPosIndex] = fuzzyLoc;
                prevPosIndex = (prevPosIndex + 1) % prevPositions.length;
                return true;
            } else {
                rc.setIndicatorDot(rc.getLocation().add(d), 255, 0, 0);
            }
        }
        fuzzyMove(dir);
        return false;
    }

    public boolean isPreviousLocation(MapLocation loc) {
        for (MapLocation prevLoc : prevPositions) {
            if (prevLoc != null && prevLoc.equals(loc)) {
                return true;
            }
        }
        return false;
    }

    public boolean safeFuzzyMove(MapLocation loc, RobotInfo[] enemies) throws GameActionException {
        return safeFuzzyMove(rc.getLocation().directionTo(loc), enemies);
    }

    int MAX_STACK_SIZE = 100;
    Direction[] bugStack = new Direction[MAX_STACK_SIZE];
    int bugStackIndex = 0;
    MapLocation lastTargetLocation = null;
    int stuckTurns = 0;
    public void bugNav(MapLocation loc) throws GameActionException{
        rc.setIndicatorLine(rc.getLocation(),loc,0,255,0);
        if(!rc.isMovementReady()){
            return;
        }
        //reset
        if(lastTargetLocation != loc || bugStackIndex >= MAX_STACK_SIZE-10){
            bugStack = new Direction[MAX_STACK_SIZE];
            bugStackIndex = 0;
            lastTargetLocation = loc;
        }

        // pop directions off of the stack
        while(bugStackIndex != 0 && rc.canMove(bugStack[bugStackIndex-1])){
            bugStackIndex--;
        }

        // going directly to the target
        if(bugStackIndex == 0){
            Direction dirToTarget = dirTo(loc);

            boolean isWater = false;
            if(rc.canMove(dirToTarget)){
                rc.move(dirToTarget);
                return;
            }else if(rc.canMove(dirToTarget.rotateLeft())){
                rc.move(dirToTarget.rotateLeft());
                return;
            }else if(rc.canMove(dirToTarget.rotateRight())) {
                rc.move(dirToTarget.rotateRight());
                return;
            }
            MapLocation locCheck = rc.getLocation().add(dirToTarget);

            // check if all units are robots
            locCheck = rc.getLocation().add(dirToTarget);
            boolean checkFrontRobot = rc.onTheMap(locCheck) && rc.canSenseRobotAtLocation(locCheck);
            locCheck = rc.getLocation().add(dirToTarget.rotateLeft());
            boolean checkLeftRobot = rc.onTheMap(locCheck) && rc.canSenseRobotAtLocation(locCheck);
            locCheck = rc.getLocation().add(dirToTarget.rotateRight());
            boolean checkRightRobot = rc.onTheMap(locCheck) && rc.canSenseRobotAtLocation(locCheck);
            if(checkFrontRobot && checkLeftRobot && checkRightRobot){
//                stuckTurns++;
                fuzzyMove(dirToTarget.opposite());
                return;
            }
//            stuckTurns = 0;


            bugStack[bugStackIndex] = dirToTarget.rotateRight();
            bugStackIndex++;
        }
        // add subsequent directions if cant move towards the target
        Direction dir = bugStack[bugStackIndex-1].rotateLeft();
        for(int i = 0; i < 8; i++){
            if(!rc.canMove(dir)){
                bugStack[bugStackIndex] = dir;
                bugStackIndex++;
            }else{
                rc.move(dir);
                return;
            }
            dir = dir.rotateLeft();
        }
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

    // TODO: make sure not to paint over tower pattern before tower is built -> check if ruin nearby and if paint is part of determined pattern
    public boolean checkAndPaintTile(MapLocation loc) throws GameActionException{
        if(!rc.canSenseLocation(loc)){
            return false;
        }
        MapInfo info = rc.senseMapInfo(loc);
        return checkAndPaintTile(info);
    }

    public boolean checkAndPaintTile(MapInfo info) throws GameActionException{
        boolean targetColor = getTileTargetColor(info.getMapLocation());
        if ((info.getPaint() == PaintType.EMPTY
                || (info.getPaint() != boolToColor(targetColor) && info.getPaint().isAlly()))
                && rc.canAttack(info.getMapLocation())
                && !info.hasRuin() && info.getMark() == PaintType.EMPTY
                && (closestAnyRuin == null || info.getMapLocation().distanceSquaredTo(closestAnyRuin) > 8)
        ){
            rc.attack(info.getMapLocation(), targetColor);
            if(closestAnyRuin != null){
                rc.setIndicatorDot(closestAnyRuin, 0, 255, 255);
            }
            rc.setIndicatorDot(info.getMapLocation(), 255, 0, 255);
            return true;
        }
        return false;
    }

    public PaintType boolToColor(boolean bool){
        return bool ? PaintType.ALLY_SECONDARY : PaintType.ALLY_PRIMARY;
    }

    public Direction getEnemyPaintDirection() throws GameActionException{
        MapInfo[] infos = rc.senseNearbyMapInfos(-1);
        int x = 0;
        int y = 0;
        int enemyPaint = 0;
        for(MapInfo info : infos){
            if(isEnemyPaint(info.getPaint())){
                x += info.getMapLocation().x;
                y += info.getMapLocation().y;
                enemyPaint++;
            }

        }
        if(enemyPaint < 3) return null;
        return dirTo(new MapLocation(x/enemyPaint,y/enemyPaint));
    }

    public MapLocation locateAllyPaint() throws GameActionException {
        int closestDist = 999999;
        MapLocation closestPaint = null;
        for (MapInfo info : mapInfo) {
            if (info.getPaint().isAlly()) {
                int dist = rc.getLocation().distanceSquaredTo(info.getMapLocation());
                if (dist < closestDist) {
                    closestDist = dist;
                    closestPaint = info.getMapLocation();
                }
            }
        }
        return closestPaint;
    }

}


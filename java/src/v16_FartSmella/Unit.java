package v16_FartSmella;

import v16_FartSmella.Util.Comms;
import battlecode.common.*;

import java.util.Arrays;

public abstract class Unit extends Robot {
    // CONSTANTS
    public PaintType SRP_MARKER_COLOR = PaintType.ALLY_PRIMARY;
    public boolean SRP_MARKER_BOOL = false; // change these together ^

    public PaintType TOWER_MARKER_COLOR = PaintType.ALLY_SECONDARY;
    public boolean TOWER_MARKER_BOOL = true; // change these together ^

    public MapLocation spawnTower = null;

    public MapLocation nearestPaintTower = null;

    public MapInfo[] mapInfo = null;

    public RobotInfo[] allies = null;
    public RobotInfo[] enemies = null;
    public RobotInfo closestEnemyTower = null;

    public MapLocation[] completableRuins = null;
    public MapLocation closestCompletableRuin = null;

    public MapLocation[] allRuins = null;
    public MapLocation closestAnyRuin = null;

    public boolean returningFromFight = false;

    protected MapLocation currentTargetLoc = null;
    // enumerate states
    protected enum UnitState {
        REFILLING, // for when bot is actively refilling
        EXPLORE, // for when bot is exploring

        // unit specific states, have to start here unfortunately
        MOPPING, // moppers mopping

        COMBAT, // soldiers shoot, moppers sweep, splashers splash

        BUILD, // finish ruin and build tower

        BUILDSRP,
    }

    protected UnitState state = null;
    protected UnitState previousState = null;

    public Unit(RobotController robot) throws GameActionException {
        super(robot);
    }


    protected void senseNearby() throws GameActionException {
        mapInfo = rc.senseNearbyMapInfos();
        mapData.setMapInfos(mapInfo, SRP_MARKER_COLOR, TOWER_MARKER_COLOR);

        communication.parseMessages();

        allRuins = rc.senseNearbyRuins(-1);
//        int bytecode = Clock.getBytecodeNum();
        completableRuins = senseNearbyCompletableTowerlessRuins(); // 1k-3k bytecode usage
//        bytecode = Clock.getBytecodeNum() - bytecode;

        allies = rc.senseNearbyRobots(-1, rc.getTeam());
        mapData.updateAdjacentAllies(allies);
        sendMapInfo();

        enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
        closestEnemyTower = inEnemyTowerRange(enemies);

        if (enemies != null && state != UnitState.REFILLING) returningFromFight = true;

        closestCompletableRuin = getClosest(completableRuins);
        if(allRuins.length > 0){
            closestAnyRuin = getClosest(allRuins);
        }

        mapData.updateLandmarks(allRuins, allies, rc.getLocation()); // usually low but i saw it spike to 7k???

        for(RobotInfo tower : mapData.getFriendlyTowers()){
            trySetIndicatorDot(tower.location, 255, 0, 255);
        }
    }

    int towerIndex = 0;
    public void sendMapInfo() throws GameActionException{
        for(RobotInfo robot : allies){
            if(robot.getType().isTowerType()){
                if(rc.canSendMessage(robot.getLocation())){
                    RobotInfo[] friendlyTowers = mapData.friendlyTowers.getArray();
                    if(friendlyTowers.length==0){
                        return;
                    }
                    rc.sendMessage(robot.location, communication.constructMessage(Comms.Codes.TOWER_LOC, friendlyTowers[towerIndex%friendlyTowers.length].getLocation(), friendlyTowers[towerIndex%friendlyTowers.length].type.getBaseType()));
                    towerIndex++;
                    break;
                }
            }
        }
    }

//    protected void confirmNearbyTowers() throws GameActionException {
//        MapLocation[] nearbyRuins = rc.senseNearbyRuins(-1);
//        if(nearbyRuins.length > 0) {
//            MapLocation closestRuin = getClosest(nearbyRuins);
//            // Complete the ruin if we can
//            for (UnitType type : towerTypes) {
//                if (rc.canCompleteTowerPattern(type, closestRuin)) {
//                    rc.completeTowerPattern(type, closestRuin);
//                    rc.setTimelineMarker("Tower built", 0, 255, 0);
//                    System.out.println("Built a tower at " + closestRuin + "!");
//                }
//            }
//        }
//    }

    // MAP HELPER FUNCTIONS //
    public MapLocation[] senseNearbyTowerlessRuins() throws GameActionException{
        MapLocation[] nearbyRuins = rc.senseNearbyRuins(-1);
        int toRemove = 0;
        for (int i = 0; i < nearbyRuins.length; i++){
            if(rc.senseRobotAtLocation(nearbyRuins[i]) != null){
                nearbyRuins[i] = null;
                toRemove++;
            }
        }
        MapLocation[] newNearbyRuins = new MapLocation[nearbyRuins.length - toRemove];
        int j = 0;
        for (MapLocation nearbyRuin : nearbyRuins) {
            if (nearbyRuin != null) {
                newNearbyRuins[j] = nearbyRuin;
                j++;
            }
        }
        return newNearbyRuins;
    }

    public MapLocation[] senseNearbyCompletableTowerlessRuins() throws GameActionException{
        MapLocation[] nearbyRuins = rc.senseNearbyRuins(-1);
        int toRemove = 0;
        for (int i = 0; i < nearbyRuins.length; i++){
            if(rc.senseRobotAtLocation(nearbyRuins[i]) != null){
                nearbyRuins[i] = null;
                toRemove++;
            }
            int correctPaint = 0;
            int sensable = -1;
            // Enemy Paint around it, Ignore it
            MapLocation senseLoc;
            MapInfo tileInfo;
            for(int x = -2; x <= 2; x++){
                if (nearbyRuins[i] == null) break;
                for(int y = -2; y <= 2; y++){
                    senseLoc = nearbyRuins[i].translate(x,y);
                    if(rc.onTheMap(senseLoc) && rc.canSenseLocation(senseLoc)){
                        sensable++;
                        tileInfo = mapData.mapInfos[senseLoc.x][senseLoc.y];
                        if(tileInfo.getPaint().isEnemy()){
                            nearbyRuins[i] = null;
                            toRemove++;
                            break;
                        }
                        if(tileInfo.getPaint().isAlly()){
                            correctPaint++;
                        }
                    }
                }
            }
            if (nearbyRuins[i] == null) continue;
            RobotInfo[] workingFriends = rc.senseNearbyRobots(nearbyRuins[i], 2, rc.getTeam());
            int soldiers = 0;
            for(int j = workingFriends.length; --j >= 0;){
                if(workingFriends[j].type == UnitType.SOLDIER){
                    soldiers++;
                }
            }
//            debugString.append("Working Friends: " + soldiers + " Correct Paint: " + correctPaint + " Sensable: " + sensable + "\n");
            if(soldiers >= 2 || (correctPaint == sensable && soldiers == 1)){
                nearbyRuins[i] = null;
                toRemove++;
            }
        }
        MapLocation[] newNearbyRuins = new MapLocation[nearbyRuins.length - toRemove];
        int j = 0;
        for (MapLocation nearbyRuin : nearbyRuins) {
            if (nearbyRuin != null) {
                newNearbyRuins[j] = nearbyRuin;
                j++;
            }
        }
        return newNearbyRuins;
    }

    protected boolean shouldRefill() throws GameActionException {
        return rc.getPaint() <= rc.getType().paintCapacity * 0.25;
    }

    protected boolean refillSelf(RobotInfo refillTower) throws GameActionException {
        // cant do anything if no nearest tower
        if (refillTower == null) {
            debugString.append("FAILED REFILL 1");
            return false;
        }

        int transferAmount = Math.min(rc.getType().paintCapacity - rc.getPaint(), refillTower.getPaintAmount());

        if (transferAmount <= 0) {
            debugString.append("FAILED REFILL 4");
            return false;
        }

        if (rc.canTransferPaint(refillTower.location, -transferAmount)) {
            rc.transferPaint(refillTower.location, -transferAmount);
            return true;
        }

        return false;
    }

    int targetTowerIndex = 0;
    MapLocation[] paintTowers = null;
    MapLocation closestPaintTower = null;
    final int REFILL_THRESHOLD = 50;
    protected UnitState refillingState() throws GameActionException {
        MapLocation[] newPaintTowers = mapData.getPaintTowers();
        if(paintTowers == null || paintTowers.length != newPaintTowers.length){
            paintTowers = newPaintTowers;
            targetTowerIndex = 0;
            int misDist = 999999;
            for(MapLocation tower : paintTowers){
                if(tower == null){
                    continue;
                }
                int dist = rc.getLocation().distanceSquaredTo(tower);
                if(dist < misDist) {
                    misDist = dist;
                    closestPaintTower = tower;
                }
            }
        }

        MapLocation targetTower = null;
        if (paintTowers.length > 0) {
            if(closestPaintTower != null) {
                targetTower = closestPaintTower;
            }else if(targetTowerIndex < paintTowers.length && paintTowers[targetTowerIndex] != null) {
                targetTower = paintTowers[targetTowerIndex];
            }
        }

        debugString.append(targetTower);
        if (targetTower == null) {
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

        // move to next tower if empty
        if (rc.canSenseRobotAtLocation(targetTower) && (rc.senseRobotAtLocation(targetTower).getPaintAmount() < 30 && paintTowers.length > 1)) {
            // heuristic to tell if its actually worth to go to new tower or just wait
            if(paintTowers[targetTowerIndex+1] != null && distTo(paintTowers[targetTowerIndex+1]) < Math.pow((rc.getType().paintCapacity*0.75 - rc.getPaint())/5.0, 2)) {
                debugString.append(Arrays.toString(paintTowers));
                if (closestPaintTower != null) {
                    closestPaintTower = null;
                } else {
                    targetTowerIndex++;
                }
                return state;
            }
        }

//        if (returningFromFight && rc.canSendMessage(targetTower)){
//            returningFromFight = false;
//            rc.sendMessage(targetTower, communication.constructMessage(Comms.Codes.FRONTLINE, returnLoc));
//        }

        // if in range of tower, refill
        if (rc.getLocation().isWithinDistanceSquared(targetTower, GameConstants.VISION_RADIUS_SQUARED)) {
            RobotInfo tower = rc.senseRobotAtLocation(targetTower);
            if(tower.getPaintAmount() > REFILL_THRESHOLD){
                if(distTo(targetTower) > 2){
                    fuzzyMove(targetTower);
                }
                if(distTo(targetTower) <= 2){
                    refillSelf(tower);
                }
            }else{
                int minAdj = mapData.getAdjacentAllies(rc.getLocation());
                Direction bestDir = Direction.CENTER;

                MapLocation loc;
                int adj = 0;
                for(Direction dir : directions){
                    loc = rc.getLocation().add(dir);
                    adj = mapData.getAdjacentAllies(loc);
                    if(loc.distanceSquaredTo(targetTower) < GameConstants.VISION_RADIUS_SQUARED && rc.canMove(dir) && adj < minAdj){
                        minAdj = adj;
                        bestDir = dir;
                    }
                }
                if(bestDir != Direction.CENTER){
                    rc.move(bestDir);
                }
            }

            rc.setIndicatorDot(targetTower, 0, 255, 0);
            // done filling
            if (rc.getPaint() > rc.getType().paintCapacity * 0.75) {
                state = UnitState.EXPLORE;
                paintTowers = null;
                closestPaintTower = null;
                return UnitState.EXPLORE;
            }
        }

        // if out of range move closer to tower
        if (rc.getLocation().distanceSquaredTo(targetTower) > 8) {
            rc.setIndicatorLine(rc.getLocation(), targetTower, 100, 100, 0);
            rc.setIndicatorDot(targetTower, 255, 255, 255);
            safeFuzzyMove(targetTower, enemies);
        }
        return state;
    }


    public Direction paintExploreDirection = null;
    public MapLocation[] previousPositions = new MapLocation[15];
    public int previousPositionIndex = 0;
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
                MapInfo info = mapData.getMapInfo(loc);
                rc.setIndicatorDot(loc, 0, 0, 255);
                if(info.getPaint().isAlly() && !isPreviousLocation(loc, previousPositions)){
                    if(tryMove(dir)){
                        paintExploreDirection = dir;
                        previousPositions[previousPositionIndex] = loc;
                        previousPositionIndex = (previousPositionIndex + 1) % previousPositions.length;
                        return true;
                    }
                }
            }
        }
        safeFuzzyMove(paintExploreDirection, enemies);
        if(mapData.getMapInfo(rc.getLocation()).getPaint().isAlly()){
            paintExploreDirection = paintExploreDirection.opposite();
        }
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

    public int paintToScore(PaintType paint){
        if(paint.isAlly()){
            return 1;
        }else if(paint.isEnemy()){
            return -1;
        }else{
            return 0;
        }
    }

    public boolean tryMoveOutOfRange(MapLocation avoidLoc, int dist) throws GameActionException{
        Direction bestDir = null;
        int bestDirScore = -9999;
        for(Direction dir : fuzzyDirs(dirTo(avoidLoc).opposite())){
            if(rc.getLocation().add(dir).distanceSquaredTo(avoidLoc) > dist && rc.canMove(dir)){
                int score = paintToScore(mapData.getMapInfo(rc.getLocation().add(dir)).getPaint());
                if(score > bestDirScore){
                    bestDir = dir;
                    bestDirScore = score;
                }
            }
        }
        if(bestDir != null){
            rc.move(bestDir);
            return true;
        }
        return false;
    }

    public boolean tryMoveIntoRange(MapLocation targetLoc, int dist) throws GameActionException{
        Direction bestDir = null;
        int bestDirScore = -9999;
        for(Direction dir : fuzzyDirs(dirTo(targetLoc))){
//            rc.setIndicatorDot(rc.getLocation().add(dir), 0, 100, 100);
            if(rc.getLocation().add(dir).distanceSquaredTo(targetLoc) <= dist && rc.canMove(dir)){
                int score = paintToScore(mapData.getMapInfo(rc.getLocation().add(dir)).getPaint());
                if(score > bestDirScore){
                    bestDir = dir;
                    bestDirScore = score;
                }
            }
        }
        if(bestDir != null){
            rc.move(bestDir);
            return true;
        }
        return false;
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

    // penalty for going onto a tile
    public int tileScore(MapLocation loc) throws GameActionException{
        int score = 0;
        MapInfo info = mapData.getMapInfo(loc);
        for (RobotInfo enemy : enemies) {
            if (!enemy.getType().isTowerType()) {
                continue;
            }

            if (loc.isWithinDistanceSquared(enemy.getLocation(), enemy.getType().actionRadiusSquared)) {
                score -= 20;
            }
        }
        if(info.getPaint().isAlly()){
            score -= mapData.adjacentAllies[loc.x][loc.y];
        }else if(info.getPaint() == PaintType.EMPTY){
            score -= mapData.adjacentAllies[loc.x][loc.y] + 1;
        }else{
            score -= 2 * mapData.adjacentAllies[loc.x][loc.y] + 2;
        }
        return score;
    }

    MapLocation lastSafeFuzzyLoc = null;
    public boolean safeFuzzyMove(MapLocation loc, RobotInfo[] enemies) throws GameActionException {
        if(lastSafeFuzzyLoc == null || lastSafeFuzzyLoc.distanceSquaredTo(loc) > 4){
            prevPositions = new MapLocation[8];
            prevPosIndex = 0;
            debugString.append("New path");
        }
        lastSafeFuzzyLoc = loc;
        return safeFuzzyMove(rc.getLocation().directionTo(loc), enemies);
    }

    MapLocation[] prevPositions = new MapLocation[8];
    int prevPosIndex = 0;

    // returns true if able to fuzzy move safely in desired direction, returns false if unable to move
    public boolean safeFuzzyMove(Direction dir, RobotInfo[] enemies) throws GameActionException {
//        if(rc.getRoundNum() - lastSafeFuzzyTurn > 2){
//            prevPositions = new MapLocation[5];
//            prevPosIndex = 0;
//        }
        Direction bestDirection = null;
        int bestDirScore = -1000;
        MapLocation loc = null;
        int score;
        for (Direction d : fuzzyDirs(dir)) {
            loc = rc.getLocation().add(d);
            if (!rc.canMove(d) || isPreviousLocation(loc, prevPositions)) {
                trySetIndicatorDot(loc, 255, 0, 0);
                continue;
            }
            score = tileScore(loc);
            trySetIndicatorDot(loc, 0, 0, 125);
            if (score > bestDirScore){
                bestDirection = d;
                bestDirScore = score;
            }
        }

        if(bestDirection != null){
            rc.move(bestDirection);
            prevPositions[prevPosIndex] = rc.getLocation();
            prevPosIndex = (prevPosIndex + 1) % prevPositions.length;
            return true;
        }
        prevPositions[prevPosIndex] = rc.getLocation();
        prevPosIndex = (prevPosIndex + 1) % prevPositions.length;
        return false;
    }



    public boolean isPreviousLocation(MapLocation loc, MapLocation[] previous) {
        for (MapLocation prevLoc : previous) {
            if (prevLoc != null && prevLoc.equals(loc)) {
                return true;
            }
        }
        return false;
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

    int[][] layerOne = new int[][]{
            {0, 0},
            {-1,1},
            {0, 1},
            {1, 1},
            {1, 0},
            {1, -1},
            {0, -1},
            {-1, -1},
            {-1, 0}
    };
    int[][] layerTwo = new int[][]{
            {-2, 2}, {-1, 2}, {0, 2}, {1, 2}, {2, 2},
            {2, 1}, {2, 0}, {2, -1}, {2, -2}, {1, -2},
            {0, -2}, {-1, -2}, {-2, -2}, {-2, -1}, {-2, 0},
            {-2, 1}
    };

    int[][] layerThree = new int[][]{
            {-3, 3}, {-2, 3}, {-1, 3}, {0, 3}, {1, 3}, {2, 3}, {3, 3},
            {3, 2}, {3, 1}, {3, 0}, {3, -1}, {3, -2}, {3, -3},
            {2, -3}, {1, -3}, {0, -3}, {-1, -3}, {-2, -3}, {-3, -3},
            {-3, -2}, {-3, -1}, {-3, 0}, {-3, 1}, {-3, 2}
    };

    public MapLocation[] spiral(int x, int y){
        return new MapLocation[]{
                new MapLocation(0, 0).translate(x, y),
                new MapLocation(-1, 1).translate(x, y),
                new MapLocation(0, 1).translate(x, y),
                new MapLocation(1, 1).translate(x, y),
                new MapLocation(1, 0).translate(x, y),
                new MapLocation(1, -1).translate(x, y),
                new MapLocation(0, -1).translate(x, y),
                new MapLocation(-1, -1).translate(x, y),
                new MapLocation(-1, 0).translate(x, y),
                new MapLocation(-2, 2).translate(x, y),
                new MapLocation(-1, 2).translate(x, y),
                new MapLocation(0, 2).translate(x, y),
                new MapLocation(1, 2).translate(x, y),
                new MapLocation(2, 2).translate(x, y),
                new MapLocation(2, 1).translate(x, y),
                new MapLocation(2, 0).translate(x, y),
                new MapLocation(2, -1).translate(x, y),
                new MapLocation(2, -2).translate(x, y),
                new MapLocation(1, -2).translate(x, y),
                new MapLocation(0, -2).translate(x, y),
                new MapLocation(-1, -2).translate(x, y),
                new MapLocation(-2, -2).translate(x, y),
                new MapLocation(-2, -1).translate(x, y),
                new MapLocation(-2, 0).translate(x, y),
                new MapLocation(-2, 1).translate(x, y),
                new MapLocation(-3, 3).translate(x, y),
                new MapLocation(-2, 3).translate(x, y),
                new MapLocation(-1, 3).translate(x, y),
                new MapLocation(0, 3).translate(x, y),
                new MapLocation(1, 3).translate(x, y),
                new MapLocation(2, 3).translate(x, y),
                new MapLocation(3, 3).translate(x, y),
                new MapLocation(3, 2).translate(x, y),
                new MapLocation(3, 1).translate(x, y),
                new MapLocation(3, 0).translate(x, y),
                new MapLocation(3, -1).translate(x, y),
                new MapLocation(3, -2).translate(x, y),
                new MapLocation(3, -3).translate(x, y),
                new MapLocation(2, -3).translate(x, y),
                new MapLocation(1, -3).translate(x, y),
                new MapLocation(0, -3).translate(x, y),
                new MapLocation(-1, -3).translate(x, y),
                new MapLocation(-2, -3).translate(x, y),
                new MapLocation(-3, -3).translate(x, y),
                new MapLocation(-3, -2).translate(x, y),
                new MapLocation(-3, -1).translate(x, y),
                new MapLocation(-3, 0).translate(x, y),
                new MapLocation(-3, 1).translate(x, y),
                new MapLocation(-3, 2).translate(x, y),
        };
    }

    public MapLocation[] mapLocationSpiral(MapLocation loc, int radius){
        return spiral(loc.x, loc.y);
    }

    public RobotInfo inEnemyTowerRange(RobotInfo[] enemies) throws GameActionException {
        for (RobotInfo enemy : enemies) {
            if (enemy.getType().isTowerType()) return enemy;
        }
        return null;
    }

    // TODO: make sure not to paint over tower pattern before tower is built -> check if ruin nearby and if paint is part of determined pattern
    public boolean checkAndPaintTile(MapLocation loc) throws GameActionException{
        if(rc.getLocation().distanceSquaredTo(loc) >= rc.getType().actionRadiusSquared || !rc.canSenseLocation(loc)){
            return false;
        }
        MapInfo info = mapData.getMapInfo(loc);
        return checkAndPaintTile(info);
    }

    public boolean checkAndPaintTile(MapInfo info) throws GameActionException{
        if(info == null){
            return false;
        }
//        boolean targetColor = getTileTargetColor(info.getMapLocation());
        int targetColorInt = mapData.tileColors[info.getMapLocation().x][info.getMapLocation().y] - 1;
        boolean targetColor = targetColorInt == 1;
        if ((info.getPaint() == PaintType.EMPTY
                || (targetColorInt != -1 && info.getPaint() != boolToColor(targetColor)))
                && rc.canAttack(info.getMapLocation())
                && !info.hasRuin() //&& info.getMark() == PaintType.EMPTY
                //&& (closestAnyRuin == null || info.getMapLocation().distanceSquaredTo(closestAnyRuin) > 8)
        ){
            rc.attack(info.getMapLocation(), targetColor);
            rc.setIndicatorDot(info.getMapLocation(), 255, 0, 255);
            return true;
        }
        return false;
    }

    public PaintType boolToColor(boolean bool){
        return bool ? PaintType.ALLY_SECONDARY : PaintType.ALLY_PRIMARY;
    }

    public MapLocation getEnemyPaintLoc() throws GameActionException{
        int x = 0;
        int y = 0;
        int enemyPaint = 0;
        for(MapInfo info : mapInfo){
            if(isEnemyPaint(info.getPaint())){
                x += info.getMapLocation().x;
                y += info.getMapLocation().y;
                enemyPaint++;
            }

        }
        if(enemyPaint < 3) return null;
        return new MapLocation(x/enemyPaint, y/enemyPaint);
    }

    public Direction getEnemyPaintDirection() throws GameActionException{
        return dirTo(getEnemyPaintLoc());
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


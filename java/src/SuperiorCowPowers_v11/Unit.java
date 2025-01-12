package SuperiorCowPowers_v11;

import SuperiorCowPowers_v11.Helpers.Explore;
import SuperiorCowPowers_v11.Helpers.MapData;
import SuperiorCowPowers_v11.Util.Comms;
import battlecode.common.*;

public abstract class Unit extends Robot {
    // CONSTANTS
    public PaintType SRP_MARKER_COLOR = PaintType.ALLY_PRIMARY;
    public boolean SRP_MARKER_BOOL = false; // change these together ^

    public MapData mapData;
    public Explore explorer;
    public MapLocation nearestPaintTower = null;

    public MapInfo[] mapInfo = null;
    public RobotInfo[] allies = null;
    public RobotInfo[] enemies = null;
    public MapLocation[] completableRuins = null;
    public MapLocation closestCompletableRuin = null;
    public MapLocation[] allRuins = null;
    public MapLocation closestAnyRuin = null;

    public boolean returningFromFight = false;

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

        BUILDSRP,
    }

    protected UnitState state = null;
    protected UnitState previousState = null;

    public Unit(RobotController robot) throws GameActionException {
        super(robot);
        mapData = new MapData(robot, width, height);
        explorer = new Explore(rc, width, height, mapData);
    }


    protected void senseNearby() throws GameActionException {
        mapInfo = rc.senseNearbyMapInfos();
        mapData.setMapInfos(mapInfo, SRP_MARKER_COLOR);

        communication.parseMessages();

        allRuins = rc.senseNearbyRuins(-1);
//        int bytecode = Clock.getBytecodeNum();
        completableRuins = senseNearbyCompletableTowerlessRuins(); // 1k-3k bytecode usage
//        bytecode = Clock.getBytecodeNum() - bytecode;

        allies = rc.senseNearbyRobots(-1, rc.getTeam());
        enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());

        if (enemies != null && state != UnitState.REFILLING) returningFromFight = true;

        closestCompletableRuin = getClosest(completableRuins);
        if(allRuins.length > 0){
            closestAnyRuin = getClosest(allRuins);
        }
        UnitType[] ruinTypes = new UnitType[allRuins.length];
        for(int i = ruinTypes.length; --i >= 0;){
            ruinTypes[i] = determineTowerPattern(allRuins[i]);
        }

        mapData.updateLandmarks(allRuins, ruinTypes, allies, rc.getLocation()); // usually low but i saw it spike to 7k???
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
            //debugString.append("Working Friends: " + workingFriends + " Correct Paint: " + correctPaint + " Sensable: " + sensable + "\n");
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

    protected boolean refillSelf(MapLocation refillLoc) throws GameActionException {
        // cant do anything if no nearest tower
        if (refillLoc == null) {
            debugString.append("FAILED REFILL 1");
            return false;
        }

        // check if tower in range
        if (!rc.getLocation().isWithinDistanceSquared(refillLoc, rc.getType().actionRadiusSquared)) {
            return false;
        }

        int needed = rc.getType().paintCapacity - rc.getPaint();
        int transferAmount;
        if (rc.canSenseRobotAtLocation(refillLoc)) {
            RobotInfo tower = rc.senseRobotAtLocation(refillLoc);
            int availablePaint = tower.getPaintAmount();
            transferAmount = Math.min(needed, availablePaint);
        }else{
            debugString.append("FAILED REFILL 2");
            return false;
        }

        if (transferAmount <= 0) {
            debugString.append("FAILED REFILL 4");
            return false;
        }

        if (rc.canTransferPaint(refillLoc, -transferAmount)) {
            rc.transferPaint(refillLoc, -transferAmount);
            return true;
        }

        return false;
    }

    int targetTowerIndex = 0;
    MapLocation[] paintTowers = null;
    MapLocation closestPaintTower = null;
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

        if (returningFromFight && rc.canSendMessage(targetTower)){
            returningFromFight = false;
            rc.sendMessage(targetTower, communication.constructMessage(Comms.Codes.FRONTLINE));
        }

        // if in range of tower, refill
        if (rc.getLocation().isWithinDistanceSquared(targetTower, 2)) {
            refillSelf(targetTower);
            rc.setIndicatorDot(targetTower, 0, 255, 0);
            // done filling
            if (rc.getPaint() > rc.getType().paintCapacity * 0.75) {
                state = UnitState.EXPLORE;
                paintTowers = null;
                closestPaintTower = null;
                return UnitState.EXPLORE;
            }
        }

        // move to next tower if empty
        if (rc.canSenseRobotAtLocation(targetTower) && (rc.senseRobotAtLocation(targetTower).getPaintAmount() < 30 && paintTowers.length > 1)) {
            if(closestPaintTower != null){
                closestPaintTower = null;
            }else{
                targetTowerIndex++;
            }
        }

        // move closer to tower
        if (rc.getLocation().distanceSquaredTo(targetTower) > 2) {
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
                trySetIndicatorDot(rc.getLocation().add(d), 255, 0, 0);
                continue;
            }

            MapLocation fuzzyLoc = rc.getLocation().add(d);
            trySetIndicatorDot(fuzzyLoc, 0, 0, 125);
            if (isSafe(fuzzyLoc, enemies) && !isPreviousLocation(fuzzyLoc, prevPositions)) {
                rc.move(d);
                prevPositions[prevPosIndex] = fuzzyLoc;
                prevPosIndex = (prevPosIndex + 1) % prevPositions.length;
                return true;
            } else {
                trySetIndicatorDot(rc.getLocation().add(d), 255, 0, 0);
            }
        }
        fuzzyMove(dir);
        return false;
    }

    public void trySetIndicatorDot(MapLocation loc, int r, int g, int b) throws GameActionException{
        if (rc.onTheMap(loc)) {
            rc.setIndicatorDot(loc, r, g, b);
        }
    }

    public boolean isPreviousLocation(MapLocation loc, MapLocation[] previous) {
        for (MapLocation prevLoc : previous) {
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

    public MapLocation[] mapLocationSpiral(MapLocation loc, int radius) {
        int bytecode = Clock.getBytecodeNum();
        int maxSize = (radius == 1) ? 9 : (radius == 2) ? 25 : 49;
        MapLocation[] coordinates = new MapLocation[maxSize];
        int index = 0;

        // Unrolled layerOne loop (Closest points first, using translate())
        coordinates[index++] = loc.translate(0, 0);
        coordinates[index++] = loc.translate(-1, 1);
        coordinates[index++] = loc.translate(0, 1);
        coordinates[index++] = loc.translate(1, 1);
        coordinates[index++] = loc.translate(1, 0);
        coordinates[index++] = loc.translate(1, -1);
        coordinates[index++] = loc.translate(0, -1);
        coordinates[index++] = loc.translate(-1, -1);
        coordinates[index++] = loc.translate(-1, 0);

        if (radius >= 2) {
            int[] pos;
            for (int i = 0; i < 16; i++) {
                pos = layerTwo[i]; // Reduce array lookup overhead
                coordinates[index++] = loc.translate(pos[0], pos[1]);
            }
        }
        if (radius >= 3) {
            int[] pos;
            for (int i = 0; i < 24; i++) {
                pos = layerThree[i];
                coordinates[index++] = loc.translate(pos[0], pos[1]);
            }
        }
        System.out.println("Bytecode: " + (Clock.getBytecodeNum() - bytecode));
//        System.out.println(Arrays.toString(coordinates));
        return coordinates;
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


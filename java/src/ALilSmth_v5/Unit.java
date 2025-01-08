package ALilSmth_v5;

import ALilSmth_v5.Helpers.Explore;
import ALilSmth_v5.Helpers.MapData;
import battlecode.common.*;

public abstract class Unit extends Robot {
    public MapData mapData;
    public Explore explorer;
    public Unit(RobotController robot) throws GameActionException {
        super(robot);
        mapData = new MapData(width, height);
        explorer = new Explore(rc, width, height, mapData);
        markNearbyMapData();
        findNearestPaintTower();
    }

    public void markNearbyMapData() throws GameActionException{
        for(MapLocation loc : rc.getAllLocationsWithinRadiusSquared(rc.getLocation(),-1)){
            mapData.setVisited(loc);

            // check if loc is a ruin
            if (rc.canSenseLocation(loc)) {
                MapInfo info = rc.senseMapInfo(loc);
                if (info.hasRuin()) {
                    int containsPaintTower = mapData.containsPaintTowerLocation(loc);

                    // check if paint tower here, add if so
                    if (containsPaintTower == -1 && rc.canSenseRobotAtLocation(loc)) {
                        RobotInfo robot = rc.senseRobotAtLocation(loc);
                        if (robot.getTeam() == rc.getTeam() && (isPaintTower(robot.getType()) || isMoneyTower(robot.getType())) && robot.getPaintAmount() > 0) {
                            mapData.setPaintTower(robot.getLocation(), rc.getLocation());
                        }
                    }
                    else if (containsPaintTower != -1 && rc.canSenseRobotAtLocation(loc)) {
                        RobotInfo robot = rc.senseRobotAtLocation(loc);
                        if (robot.getPaintAmount() == 0) {
                            mapData.removePaintTowerLocation(containsPaintTower);
                        }
                    }
                    // otherwise, check if location in stored paint locations, replace if no more paint tower
                    else if (containsPaintTower != -1 && !rc.canSenseRobotAtLocation(loc)) {
                        mapData.removePaintTowerLocation(containsPaintTower);
                    }
                }
            }
        }
    }
    protected MapLocation nearestPaintTower = null;

    protected boolean shouldRefill() throws GameActionException {
        if (nearestPaintTower == null) {
            return false;
        }

        return rc.getPaint() <= 25;
    }

    protected void refillSelf() throws GameActionException {
        // cant do anything if no nearest tower
        if (nearestPaintTower == null) {
            return;
        }

        // check if tower in range
        if (distTo(nearestPaintTower) > 16) {
            return;
        }

        int needed = rc.getType().paintCapacity - rc.getPaint();
        int transferAmount = 0;
        if (rc.canSenseRobotAtLocation(nearestPaintTower)) {
            RobotInfo tower = rc.senseRobotAtLocation(nearestPaintTower);
            if (tower.getTeam() != rc.getTeam()) {
                nearestPaintTower = null;
                return;
            }

            int availablePaint = tower.getPaintAmount();

            transferAmount = Math.min(needed, availablePaint);
        }

        if (transferAmount <= 0) {
            return;
        }

        if (rc.canTransferPaint(nearestPaintTower, -transferAmount)) {
            rc.transferPaint(nearestPaintTower, -transferAmount);
        }
    }

    protected void findNearestPaintTower() throws GameActionException {
        nearestPaintTower = mapData.getNearestPaintTower(rc.getLocation());
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
    public boolean isSafe(MapLocation loc, RobotInfo[] towers) throws GameActionException {
        // check if in tower range
        for (RobotInfo tower : towers) {
            if (loc.isWithinDistanceSquared(tower.getLocation(), tower.getType().actionRadiusSquared)) {
                return false;
            }
        }

        // check if on ally paint
        MapInfo info = rc.senseMapInfo(loc);
        if (isEnemyPaint(info.getPaint())) {
            return false;
        }

        return true;
    }

    // returns true if able to fuzzy move safely in desired direction
    public boolean safeFuzzyMove(Direction dir, RobotInfo[] enemyTowers) throws GameActionException {
        for (Direction d : fuzzyDirs(dir)) {
            if (!rc.canMove(d)) {
                continue;
            }

            MapLocation fuzzyLoc = rc.getLocation().add(d);
            if (isSafe(fuzzyLoc, enemyTowers)) {
                rc.move(d);
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

    public RobotInfo inEnemyTowerRange(RobotInfo[] enemies) throws GameActionException {
        for (RobotInfo enemy : enemies) {
            if (!enemy.getType().isTowerType()) {
                continue;
            }
            return enemy;
        }
        return null;
    }

    public boolean checkAndPaintTile(MapLocation loc) throws GameActionException{
        if(!rc.canSenseLocation(loc)){
            return false;
        }
        MapInfo info = rc.senseMapInfo(loc);
        return checkAndPaintTile(info);
    }

    public boolean checkAndPaintTile(MapInfo info) throws GameActionException{
        boolean targetColor = getTileTargetColor(info.getMapLocation());
        if ((info.getPaint() == PaintType.EMPTY || (info.getPaint() != boolToColor(targetColor) && info.getPaint().isAlly())) && rc.canAttack(info.getMapLocation()) && !info.hasRuin() && info.getMark() == PaintType.EMPTY){
            rc.attack(info.getMapLocation(), targetColor);
            rc.setIndicatorDot(info.getMapLocation(), 255, 0, 255);
            return true;
        }
        return false;
    }

    public PaintType boolToColor(boolean bool){
        return bool ? PaintType.ALLY_SECONDARY : PaintType.ALLY_PRIMARY;
    }

    public Direction    getEnemyPaintDirection() throws GameActionException{
        MapInfo[] infos = rc.senseNearbyMapInfos(8);
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
        return dirTo(new MapLocation(x,y));
    }

}


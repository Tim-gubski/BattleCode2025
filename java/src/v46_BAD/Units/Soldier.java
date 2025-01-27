package v46_BAD.Units;

import v46_BAD.Unit;
import battlecode.common.*;

// TODO: Dont go into tower range, attack towers maybe?, moppers attack enemy units

public class Soldier extends Unit {

    Direction lastEnemyPaintDirection = null;
    final int ATTACK_HEALTH_THRESHOLD = 40;
    final int SRP_TOWER_THRESHOLD = Math.max(2,(int)Math.round(6 - Math.min(rc.getMapWidth(),rc.getMapHeight())/10.0));
    MapLocation exploreLocation;
    Direction exploreDirection;
    MapLocation explorationWaypoint;
    final int MIN_EXPLORE_DIST = 10;


    public Soldier(RobotController robot) throws GameActionException {
        super(robot);
        state = UnitState.EXPLORE;
        exploreDirection = spawnTower.directionTo(rc.getLocation());
//        exploreLocation = extendInDir(rc.getLocation(), exploreDirection, Math.max(MIN_EXPLORE_DIST,width/3));
        exploreLocation = extendLocToEdge(rc.getLocation(), exploreDirection);
    }

    boolean currentlyConnectedToSpawnTower = false;
    boolean shouldFillAround = true;
    public void turn() throws GameActionException {
        debugString.setLength(0);
        if(!connected && spawnTower != null && rc.canSenseRobotAtLocation(spawnTower)){
            currentlyConnectedToSpawnTower = rc.canSendMessage(spawnTower);
        }
        senseNearby(); // perform all scans
//        tryMove(DPPathFinder.computeMove(mapData.mapInfos, rc.getLocation(), new MapLocation(0,0), 2));
        previousState = state;
        state = determineState();

        shouldFillAround = state != UnitState.REFILLING && state != UnitState.COMBAT &&
                (mapData.friendlyTowers.paintTowers >= 2 || state == UnitState.BUILD || state == UnitState.BUILDSRP);// && rc.getPaint()>10;

        switch (state) {
            case UnitState.CONNECTING_TO_TOWER -> {
                connectingToTower();
            }
            case UnitState.EXPLORE -> {
                exploreState();
            }
            case UnitState.COMBAT -> {
                combatState();
            }
            case UnitState.REFILLING -> {
                refillingState();
            }
            case UnitState.QUICK_REFILL ->{
                quickRefillState();
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
    boolean connected = false;
    int numSoldiers = 0;
    private UnitState determineState() throws GameActionException{
        debugString.append("\n Need Moppers: ").append(needMoppers);
        // thug it out!
        if(state == UnitState.BUILD && closestCompletableRuin != null && paintLeftForCompletableRuin*5+1<rc.getPaint()){
            debugString.append("Thugging it out, tiles left to complete: " + paintLeftForCompletableRuin);
            return UnitState.BUILD;
        }
        if(closestCompletableRuin != null && paintLeftForCompletableRuin == 0){
            debugString.append("Completing ruin");
            return UnitState.BUILD;
        }
        if(state == UnitState.QUICK_REFILL){
            return UnitState.QUICK_REFILL;
        }
        // make the build check smarter, could see if you have enough paint to finish a tower.
        if ((state != UnitState.REFILLING && shouldRefill() ||
                (state == UnitState.REFILLING && rc.getPaint() < rc.getType().paintCapacity * 0.75))) {
            if (returnLoc == null && (state != UnitState.EXPLORE)) {
                returnLoc = rc.getLocation();
            }
            return UnitState.REFILLING;
        }

        if(spawnTower != null) {
            if (!connected) {
                return UnitState.CONNECTING_TO_TOWER;
            }
        }

        numSoldiers = 0;
        if(closestEnemyTower != null && reachableFrom(rc.getLocation(), closestEnemyTower.location) && closestEnemyTower.type.getBaseType() != UnitType.LEVEL_ONE_DEFENSE_TOWER){
            for (RobotInfo ally : allies) {
                if (ally.type == UnitType.SOLDIER && ally.location.distanceSquaredTo(closestEnemyTower.location) <= GameConstants.VISION_RADIUS_SQUARED) {
                    numSoldiers++;
                }
            }
            if (rc.getHealth() > ATTACK_HEALTH_THRESHOLD){// && (numSoldiers > 0 || closestEnemyTower.health < 400)) {
                return UnitState.COMBAT;
            }
        }

        if (closestCompletableRuin != null && (rc.getChips() >= 700 || rc.getNumberTowers() < 5)){//UnitType.LEVEL_ONE_PAINT_TOWER.paintCost - 100) {
            return UnitState.BUILD;
        }

        completableSRP = closestCompletableSRP();
        if (completableSRP != null
                && Math.min(rc.getNumberTowers(), mapData.friendlyTowers.size()) >= SRP_TOWER_THRESHOLD
                && mapData.friendlyTowers.paintTowers > 0
                && enemies.length == 0){
            return UnitState.BUILDSRP;
        }
        return UnitState.EXPLORE;
    }

    private void stateInvariantActions() throws GameActionException{
//        if(completableSRP != null && rc.getLocation().equals(completableSRP)){
//            if(rc.canMark(completableSRP)){
//                rc.mark(completableSRP, false);
//            }
//        }
        // fill moppers on ruins
        if(closestCompletableRuin != null){
            for(RobotInfo ally : allies){
                if(ally.type == UnitType.MOPPER && ally.location.distanceSquaredTo(closestCompletableRuin) <= 8){
                    checkAndPaintTile(ally.location);
                }
            }
        }

        //cuck
        if(cuckLocation != null){
            debugString.append("cucking\n");
            for(MapLocation loc : rc.getAllLocationsWithinRadiusSquared(cuckLocation, 8)){
                if(checkAndPaintTile(loc)){
                    cuckLocation = null;
                    break;
                }
            }
        }

        // dont fill stuff if youre low
        if(shouldFillAround) {
            debugString.append("fillin");

            // fill underneath yourself
            if (!checkAndPaintTile(rc.getLocation()) && rc.isActionReady()) {
                // then fill other stuff
                if(rc.isActionReady()) {
                    MapLocation[] targets = mapLocationSpiral(rc.getLocation(), 3);
                    // fill ruins first
                    if(closestCompletableRuin != null){
                        debugString.append("Trying to fill ruins");
                        rc.setIndicatorLine(closestCompletableRuin, rc.getLocation(), 255, 0, 0);
                        for (MapLocation loc : mapLocationSpiral(closestCompletableRuin, 2)) {
                            if (Clock.getBytecodesLeft() < 1000 || checkAndPaintTile(loc)) {
                                debugString.append("Filled Ruins \n");
                                break;
                            }
                        }
                    }
                    if(closestCompletableRuin == null || distTo(closestCompletableRuin) > 8){
                        debugString.append("Trying to fill around");
                        // fill other stuff
                        for (MapLocation target : targets) {
                            if (Clock.getBytecodesLeft() < 1000 || checkAndPaintTile(target)) {
                                debugString.append("Filled Around \n");
                                break;
                            }
                        }
                    }
                }
            }else{
                debugString.append("Filled Under Self \n");
            }
        }
        // confirm all tower patterns
        // optimize later if you can
        if(closestAnyRuin != null) {
            for(UnitType towerType : towerTypes) {
                if (rc.canCompleteTowerPattern(towerType, closestAnyRuin) && (rc.getChips() >= towerType.moneyCost || mapData.friendlyTowers.paintTowers == 0)) {
                    rc.completeTowerPattern(towerType, closestAnyRuin);
                    state = UnitState.QUICK_REFILL;
                    rc.setTimelineMarker("Tower built", 0, 255, 0);
                    System.out.println("Built a tower at " + closestAnyRuin + "!");
                }
            }
        }

        // confirm all resource patterns
        if(mapData.friendlyTowers.size() >= SRP_TOWER_THRESHOLD) {
            mapData.SRPs.updateIterable();
            for (int i = mapData.SRPs.size; --i >= 0; ) {
                if (tryConfirmResourcePattern(mapData.SRPs.locs[i])) {
                    //rc.setTimelineMarker("Resource pattern confirmed", 0, 255, 0);
                    trySetIndicatorDot(mapData.SRPs.locs[i], 0, 255, 0);
                }
            }
        }
    }

    private void quickRefillState() throws GameActionException{
        RobotInfo closestTower = null;
        int minDist = 99999;
        for(RobotInfo ally : allies){
            if(ally.type.isTowerType() && distTo(ally.location) < minDist){
                minDist = distTo(ally.location);
                closestTower = ally;
            }
        }
        if(closestTower != null){
            if(closestTower.paintAmount == 0){
                state = UnitState.EXPLORE;
                return;
            }
            if(rc.getLocation().distanceSquaredTo(closestTower.location) > 2){
                fuzzyMove(closestTower.location);
            }
            if(refillSelf(closestTower)){
                state = UnitState.EXPLORE;
            }
        }else{
            state = UnitState.EXPLORE;
        }
    }

    private void connectingToTower() throws GameActionException{
        if(rc.getLocation().isAdjacentTo(spawnTower)){
            tryMoveOutOfRange(spawnTower, 2);
        }
        MapLocation loc = rc.getLocation();
        MapInfo info;
        for(int i = 3; i-- >= 0;){
            info = mapData.getMapInfo(loc);
            if(info.getPaint() == PaintType.EMPTY){
                tryAttack(loc);
            }else if(info.getPaint().isEnemy()){
                connected = true;
                return;
            }
            if(loc.x < spawnTower.x){
                loc = loc.translate(1,0);
                continue;
            }
            if(loc.x > spawnTower.x){
                loc = loc.translate(-1,0);
                continue;
            }
            if(loc.y < spawnTower.y){
                loc = loc.translate(0,1);
                continue;
            }
            if(loc.y > spawnTower.y){
                loc = loc.translate(0,-1);
            }
        }
        connected = currentlyConnectedToSpawnTower;
    }

    private void buildSRPState() throws GameActionException{
        trySetIndicatorDot(completableSRP, 0, 255, 255);
        if(!rc.getLocation().equals(completableSRP)){
            fuzzyMove(completableSRP);
        }else{
            mapData.markResourcePattern(completableSRP);
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
//            if(mapData.getMapInfo(rc.getLocation()).getPaint() == PaintType.EMPTY){
//                tryAttack(rc.getLocation());
//            }else{
            tryAttack(closestEnemyTower.getLocation());
//            }
            if(!tryMoveOutOfRange(closestEnemyTower.getLocation(), closestEnemyTower.type.actionRadiusSquared)){
                fuzzyMove(dirTo(closestEnemyTower.getLocation()).opposite());
            }
        }else{
            if(distTo(closestEnemyTower.location)>18){
                fuzzyMove(closestEnemyTower.location);
            }
            // get in attack range
//            if((rc.getRoundNum()%2==0 || numSoldiers == 0) && rc.isActionReady()) {
            if(rc.isActionReady()) {
                if (!tryMoveIntoRange(closestEnemyTower.getLocation(), rc.getType().actionRadiusSquared)) {
                    fuzzyMove(closestEnemyTower.getLocation());
                }
            }
//            if(mapData.getMapInfo(rc.getLocation()).getPaint() == PaintType.EMPTY){
//                tryAttack(rc.getLocation());
//            }
            tryAttack(closestEnemyTower.getLocation());
        }
    }

    private UnitState exploreState() throws GameActionException {
        // otherwise explore
        if(rc.isMovementReady()){
            if (returnLoc != null) {
                debugString.append(String.format("returning to %d, %d", returnLoc.x, returnLoc.y));
                if (distTo(returnLoc) <= 4) {
                    returnLoc = null;
                } else {
                    currentTargetLoc = returnLoc;
                    bugNav(returnLoc);
                }
            }
//            Direction enemyPaintDirection = getEnemyPaintDirection();
//            RobotInfo tower = inEnemyTowerRange(enemies);
//            if(enemyPaintDirection != null && enemyPaintDirection != Direction.CENTER && enemies.length > 0 && rc.getHealth() >= ATTACK_HEALTH_THRESHOLD){
//                if(lastEnemyPaintDirection == null){
//                    RIGHT = !RIGHT;
//                }
//                lastEnemyPaintDirection = enemyPaintDirection;
//                MapInfo paintInfo = rc.senseMapInfo(rc.getLocation().add(enemyPaintDirection));
//                if(isEnemyPaint(rc.senseMapInfo(rc.getLocation()).getPaint())){
//                    fuzzyMove(enemyPaintDirection.opposite());
//                }else{
//                    if(!isEnemyPaint(paintInfo.getPaint())) {
//                        safeFuzzyMove(enemyPaintDirection, enemies, false);
//                    } else {
//                        if(rc.getID() % 2 == 0 && rc.getRoundNum() % 20 < 10){
//                            safeFuzzyMove(enemyPaintDirection.rotateRight().rotateRight(), enemies, false);
//                        } else {
//                            safeFuzzyMove(enemyPaintDirection.rotateLeft().rotateLeft(), enemies, false);
//                        }
//                    }
//                }
//            }else{
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
                // go to empty paint if its not right next to you, and if you dont have money for towers
                if (closestEmptyPaint != null && distTo(closestEmptyPaint) > 4 && shouldFillAround && rc.getNumberTowers() > 5 && rc.isActionReady()) {
                    rc.setIndicatorLine(rc.getLocation(), closestEmptyPaint, 0, 100, 100);
                    debugString.append("Moving to closest empty paint " + closestEmptyPaint);
                    safeFuzzyMove(closestEmptyPaint, enemies, true);
                }

//                if(explorationWaypoint == null){
//                    nextWayPoint();
//                }
                if(distTo(exploreLocation) <= 8 || tooCloseToEnemyTower(exploreLocation)){// || previousState != UnitState.EXPLORE){
                    for(int tries = 15; --tries >= 0;){
                        exploreDirection = randomDirection();
                        exploreLocation = extendInDir(rc.getLocation(), exploreDirection, Math.max(MIN_EXPLORE_DIST,width/3));
//                        exploreLocation = extendLocToEdge(rc.getLocation(), exploreDirection);
                        if(mapData.getMapInfo(exploreLocation)==null){
                            break;
                        }
                    }

                    explorationWaypoint = null;
                }

                if(rc.isMovementReady()) {
                    rc.setIndicatorLine(rc.getLocation(), exploreLocation, 100, 0, 100);
//                    bugNav(explorationWaypoint);
                    bugNav(exploreLocation);
                }
//                if(distTo(explorationWaypoint) <=2 || (rc.canSenseLocation(explorationWaypoint) && !reachableFrom(rc.getLocation(), explorationWaypoint))){
//                    nextWayPoint();
//                }

//            }
        }

        return state;
    }

    boolean zig = false;
    int startStep = 2;
    int stepIncrement = 2;
    int currentStep = 0;
    private void nextWayPoint() throws GameActionException{
        if(explorationWaypoint == null){
            zig = false;
            currentStep = startStep;
            explorationWaypoint = extendInDir(rc.getLocation(), exploreDirection.rotateRight(), currentStep);
        }
        if(zig){
            explorationWaypoint = extendInDir(rc.getLocation(), exploreDirection.rotateRight(), currentStep);
            currentStep += stepIncrement;
            zig = false;
        }else{
            explorationWaypoint = extendInDir(rc.getLocation(), exploreDirection.rotateLeft(), currentStep);
            currentStep += stepIncrement;
            zig = true;
        }
    }

    private MapLocation extendInDir(MapLocation start, Direction dir, int steps) throws GameActionException{
        int i = 0;
        while(rc.onTheMap(start.add(dir)) && i <= steps){
            start = start.add(dir);
            i++;
        }
        return start;
    }

    private UnitState buildState() throws GameActionException {
        rc.setIndicatorLine(rc.getLocation(), closestCompletableRuin, 255, 255, 255);
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
            if(rc.canSenseLocation(closestCompletableRuin)) {
                mapData.markTowerPattern(closestCompletableRuin, towerType);
            }
            Direction markDirection = towerTypeToMarkDirection(towerType);
            MapLocation markLocation = closestCompletableRuin.add(markDirection);
            if(rc.canMark(markLocation)){
                rc.mark(markLocation, true);
                markDir = markDirection;
            }
            if(rc.isMovementReady()) {
                if (distTo(closestCompletableRuin) <= 8) {
                    debugString.append("here 1");
                    safeFuzzyMove(closestCompletableRuin, enemies, true);
                } else {
                    debugString.append("here 2");
                    bugNav(closestCompletableRuin);
                }
            }

        }

        if(markDir != null) {
            UnitType towerType = determineTowerPattern(closestCompletableRuin);
            UnitType markTowerType = markDirectionToTowerType(markDir);
            if(markTowerType == UnitType.LEVEL_ONE_MONEY_TOWER && towerType == UnitType.LEVEL_ONE_PAINT_TOWER){
                Direction markDirection = towerTypeToMarkDirection(towerType);
                MapLocation markLocation = closestCompletableRuin.add(markDirection);
                debugString.append("Converting to paint tower");
                fuzzyMove(markLocation);
                if(rc.getLocation().equals(markLocation)){
                    if(rc.canRemoveMark(closestCompletableRuin.add(markDir))){
                        rc.removeMark(closestCompletableRuin.add(markDir));
                    }
                    if(rc.canMark(markLocation)){
                        rc.mark(markLocation, true);
                        markDir = markDirection;
                    }
                }
            }
            mapData.markTowerPattern(closestCompletableRuin, markDirectionToTowerType(markDir));
        }

        // go to empty paint on the ruin
        //TODO make this smarter
        if(rc.isMovementReady()) {
            for (MapLocation loc : rc.getAllLocationsWithinRadiusSquared(closestCompletableRuin, 8)) {
                info = mapData.getMapInfo(loc);
                if (!info.getPaint().isAlly() && !info.hasRuin()) {
                    rc.setIndicatorLine(rc.getLocation(), loc, 255, 255, 255);
                    if(distTo(loc) > 8){
                        debugString.append("here 3");
                        bugNav(loc);
                    }else if(distTo(loc) > 2){
                        debugString.append("here 4");
                        fuzzyMove(loc);
                    }else{
                        tryMoveIntoRange(loc, 2);
                    }
                    break;
                }
            }
        }
        if(rc.isMovementReady()){
            if(distTo(closestCompletableRuin) > 8) {
                debugString.append("here 5");
                bugNav(closestCompletableRuin);
            }else{
                debugString.append("here 6");
                Direction bestDir = Direction.CENTER;
                int bestDirScore = distTo(closestCompletableRuin) <= 2 ? tileScore(rc.getLocation(), false) : -9999;
                for(Direction dir : fuzzyDirs(dirTo(closestCompletableRuin))){
                    if(rc.getLocation().add(dir).distanceSquaredTo(closestCompletableRuin) <= 2 && rc.canMove(dir)){
                        int score = tileScore(rc.getLocation().add(dir), false);
                        if(score >= bestDirScore){
                            bestDir = dir;
                            bestDirScore = score;
                        }
                    }
                }
                if(bestDir != Direction.CENTER){
                    rc.move(bestDir);
                }
                if(rc.isMovementReady()){
                    fuzzyMove(closestCompletableRuin);
                }
            }

        }
        return state;
    }

//    private MapLocation[] srpGridLocs(MapLocation loc) throws GameActionException{
//        int x = loc.x;
//        int y = loc.y;
//        // Find the nearest valid y such that (y+1) % 3 == 0
//        int newY = y - (y + 2) % 4;
//
//        // Compute the base x that satisfies the second condition
//        int baseX = x - (x + 2) % 4; // Ensure non-negative remainder
//        int closestX = baseX;
//        int x1 = closestX, x2 = closestX + 4;
//        int d1 = (x1 - x) * (x1 - x) + (newY - y) * (newY - y);
//        int d2 = (x2 - x) * (x2 - x) + (newY - y) * (newY - y);
//
//        if (d2 < d1) closestX = x2;
//
//        return new MapLocation[]{
//                new MapLocation(closestX, newY),
//                new MapLocation(closestX, newY+4),
//                new MapLocation(closestX+4, newY),
//                new MapLocation(closestX+4, newY+4),
//        };
//    }
    private MapLocation[] srpGridLocs(MapLocation loc) throws GameActionException {
        int mapWidth = rc.getMapWidth();
        int mapHeight = rc.getMapHeight();
        int x = loc.x, y = loc.y;

        // Decide which horizontal edge to anchor
        boolean anchorLeft = (x < mapWidth / 2);
        int baseX;
        if (anchorLeft) {
            baseX = x - (x + 2) % 4;
        } else {
            int offsetX = (mapWidth - 1 - (x + 2)) % 4;
            baseX = x + offsetX;
        }
        int x1 = baseX, x2 = baseX + 4;
        if ((x2 - x) * (x2 - x) < (x1 - x) * (x1 - x)) baseX = x2;

        // Decide which vertical edge to anchor
        boolean anchorBottom = (y < mapHeight / 2);
        int baseY;
        if (anchorBottom) {
            baseY = y - (y + 2) % 4;
        } else {
            int offsetY = (mapHeight - 1 - (y + 2)) % 4;
            baseY = y + offsetY;
        }
        int y1 = baseY, y2 = baseY + 4;
        if ((y2 - y) * (y2 - y) < (y1 - y) * (y1 - y)) baseY = y2;

        return new MapLocation[]{
                new MapLocation(baseX,     baseY),
                new MapLocation(baseX,     baseY + 4),
                new MapLocation(baseX + 4, baseY),
                new MapLocation(baseX + 4, baseY + 4)
        };
    }

    private MapLocation closestCompletableSRP() throws GameActionException{
        mapData.ruins.updateIterable();
        mapData.SRPs.updateIterable();
        MapLocation[] locsToCheck = new MapLocation[49+4];
        for(MapLocation loc : srpGridLocs(rc.getLocation())){
            trySetIndicatorDot(loc, 0, 255, 255);
        }
        System.arraycopy(srpGridLocs(rc.getLocation()),0,locsToCheck,0,4);
        System.arraycopy(mapLocationSpiral(rc.getLocation(),3),0,locsToCheck,4, 49);
        for(MapLocation loc : locsToCheck){
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

package v54_Ben.Units;

import v54_Ben.Unit;
import battlecode.common.*;

public class Mopper extends Unit {
    Direction exploreDir = randomDirection();
    MapLocation closestEnemyPaint = null;
    RobotInfo targetEnemy = null;

    public Mopper(RobotController robot) throws GameActionException {
        super(robot);
        state = UnitState.EXPLORE;
    }
    /**
     * Run a single turn for a Mopper.
     * This code is wrapped inside the infinite loop in run(), so it is called once per turn.
     */

    boolean currentlyConnectedToSpawnTower = false;
    public void turn() throws GameActionException {
        if(!connected && spawnTower != null && rc.canSenseRobotAtLocation(spawnTower)){
            currentlyConnectedToSpawnTower = rc.canSendMessage(spawnTower);
            if(currentlyConnectedToSpawnTower){
                connected = true;
            }
        }
        senseNearby(); // perform all scans

        previousState = state;
        state = determineState();

        switch (state) {
            case UnitState.CONNECTING_TO_TOWER -> {
                connectingToTower();
            }
            case UnitState.REZING -> {
                rezState();
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
            case UnitState.MOPPING -> {
                moppingState();
            }
        }

        stateInvariantActions();

        debugString.append("Currently in state: ").append(state.toString());

//        rc.setIndicatorDot(currentTargetLoc, 255, 125, 0);
//        rc.setIndicatorLine(rc.getLocation(), currentTargetLoc, 125, 0, 125);
    }

    // movement methods --------------------------------------------------------------------------------------------- //
    boolean connected = false;
    MapLocation closestDeadHomieLoc;
    final int REZ_PAINT_AMOUNT = 20;
    private UnitState determineState() throws GameActionException {
        debugString.append("Needed here: " + needMoppers);

//        int closestDeadHomie = 9999;
//        closestDeadHomieLoc = null;
//        for(RobotInfo friend : allies){
//            if(friend.type.isTowerType()) continue;
//            if(friend.paintAmount <= 10){
//                int dist = rc.getLocation().distanceSquaredTo(friend.location);
//                if(dist < closestDeadHomie){
//                    closestDeadHomie = dist;
//                    closestDeadHomieLoc = friend.location;
//                }
//            }
//        }

        if(closestDeadHomieLoc != null && rc.getPaint() > REZ_PAINT_AMOUNT + 5){
            return UnitState.REZING;
        }

        boolean closeToHomeTower = false;
        RobotInfo closestFriendlyTower = getClosest(mapData.getFriendlyTowers());
        if(closestFriendlyTower != null){
            if(distTo(closestFriendlyTower.location) <= 30){
                closeToHomeTower = true;
                targetEnemy = getTargetEnemyByPaint();
            }else{
                targetEnemy = getTargetEnemyByDist();
            }
        }else{
            targetEnemy = getTargetEnemyByDist();
        }

        if(state == UnitState.COMBAT && targetEnemy != null && mapData.friendlyTowers.paintTowers == 0){ // moppers get paint from attacking so this could be better than dying
            return UnitState.COMBAT;
        }

        if ((state != UnitState.REFILLING && shouldRefill()) || (state == UnitState.REFILLING && rc.getPaint() < rc.getType().paintCapacity * 0.75)) {
            if (returnLoc == null) {
                returnLoc = rc.getLocation();
            }
            return UnitState.REFILLING;
        }

        if(closestAnyRuin != null){
            rc.setIndicatorDot(closestAnyRuin, 0, 255, 0);
        }

        MapInfo[] nearbyTiles = rc.senseNearbyMapInfos(-1);
        closestEnemyPaint = null;
        boolean bestNearRuin = false;
        int closestDist = 999999;
        boolean nearRuin;
        for(MapInfo tile : nearbyTiles) {
            if (tile.getPaint() == PaintType.ENEMY_PRIMARY || tile.getPaint() == PaintType.ENEMY_SECONDARY) {
                nearRuin = false;
                for(MapLocation loc : allRuins){
                    rc.setIndicatorLine(rc.getLocation(), loc, 255, 0, 0);
                    nearRuin = tile.getMapLocation().distanceSquaredTo(loc) <= 8;// && !rc.canSenseRobotAtLocation(loc);
                    if(nearRuin){
                        rc.setIndicatorDot(tile.getMapLocation(), 255, 0, 0);
                        break;
                    }
                }
                int dist = rc.getLocation().distanceSquaredTo(tile.getMapLocation());

                if (nearRuin && !bestNearRuin) {
                    closestDist = dist;
                    closestEnemyPaint = tile.getMapLocation();
                    bestNearRuin = true;
                } else if (nearRuin && bestNearRuin) {
                    if (dist < closestDist) {
                        closestDist = dist;
                        closestEnemyPaint = tile.getMapLocation();
                    }
                } else if (!nearRuin && !bestNearRuin) {
                    if (dist < closestDist) {
                        closestDist = dist;
                        closestEnemyPaint = tile.getMapLocation();
                    }
                }
            }
        }


        if(targetEnemy != null && (!bestNearRuin || (closeToHomeTower && targetEnemy.type == UnitType.SOLDIER))){// || (spawnTower != null && distTo(spawnTower) < 100))){
            return UnitState.COMBAT;
        }

        if(!connected && rc.getRoundNum() - spawnTurn < 5){
            return UnitState.CONNECTING_TO_TOWER;
        }

        if (closestEnemyPaint != null && needMoppers == null &&
                (closestEnemyTower == null ||
                        closestEnemyPaint.distanceSquaredTo(closestEnemyTower.location) > closestEnemyTower.type.actionRadiusSquared)) {
            return UnitState.MOPPING;
        }
        return UnitState.EXPLORE;
    }

    private void stateInvariantActions() throws GameActionException {
        if(state != UnitState.REFILLING && needMoppers == null) {
            refillAllies(rc.senseNearbyRobots(-1, rc.getTeam()));
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
        for (int i = 0; i < mapData.SRPs.size; i++) {
            if (tryConfirmResourcePattern(mapData.SRPs.locs[i])) {
                //rc.setTimelineMarker("Resource pattern confirmed", 0, 255, 0);
                rc.setIndicatorDot(mapData.SRPs.locs[i], 0, 255, 0);
                System.out.println("Resource pattern confirmed at " + mapData.SRPs.locs[i] + "!");
            }
        }

        MapLocation attackTarget = null;
        RobotInfo[] attackableEnemies = rc.senseNearbyRobots(rc.getType().actionRadiusSquared, rc.getTeam().opponent());
        if (attackableEnemies.length > 0) {
            int lowestHealth = 9999;
            for (RobotInfo enemy : attackableEnemies) {
                if (enemy.health < lowestHealth) {
                    attackTarget = enemy.getLocation();
                    lowestHealth = enemy.health;
                }
            }
        }

        if (attackTarget == null) {
            for (MapInfo info : rc.senseNearbyMapInfos(rc.getType().actionRadiusSquared)) {
                if (info.getPaint().isEnemy()) {
                    tryAttack(info.getMapLocation());
                    break;
                }
            }
        } else {
            tryAttack(attackTarget);
        }
    }

    private void rezState() throws GameActionException{
        if(distTo(closestDeadHomieLoc) > 2){
            fuzzyMove(closestDeadHomieLoc);
        }
        if(distTo(closestDeadHomieLoc) <= 2 && rc.canTransferPaint(closestDeadHomieLoc, REZ_PAINT_AMOUNT)){
            rc.transferPaint(closestDeadHomieLoc, REZ_PAINT_AMOUNT);
        }
    }

    private void connectingToTower() throws GameActionException{
        if(spawnTower == null || !rc.canSenseRobotAtLocation(spawnTower)){
            connected = true;
        }else{
            connected = currentlyConnectedToSpawnTower;
        }
        if(connected){
            return;
        }

        MapLocation closestAllyPaint = null;
        int minDist = 9999;
        for(MapInfo tile : mapInfo){
            if(tile.getPaint().isAlly() && tile.getMapLocation().distanceSquaredTo(spawnTower) <= 8){
                int dist = rc.getLocation().distanceSquaredTo(tile.getMapLocation());
                if(dist < minDist){
                    minDist = dist;
                    closestAllyPaint = tile.getMapLocation();
                }
            }
        }
        if(closestAllyPaint == null){
            connected = true;
            return;
        }else{
            fuzzyMove(closestAllyPaint);
        }
    }

    private void exploreState() throws GameActionException {
        if(needMoppers != null){
            bugNav(needMoppers);
            if(rc.getLocation().distanceSquaredTo(needMoppers) <= 2){
                needMoppers = null;
            }
            return;
        }
        if (returnLoc != null) {
            if (distTo(returnLoc) <= 8) {
                returnLoc = null;
            } else {
                currentTargetLoc = returnLoc;
                debugString.append("Returning");
                bugNav(returnLoc);
                rc.setIndicatorLine(rc.getLocation(), returnLoc, 0, 255, 0);
            }
        }

        RobotInfo[] friends = rc.senseNearbyRobots(-1,rc.getTeam());
        RobotInfo bestFriend = null;
//        for(RobotInfo friend : friends){
//            if(friend.getType() == UnitType.SOLDIER && (bestFriend == null || Math.abs(friend.getID()-rc.getID()) < Math.abs(bestFriend.getID()-rc.getID()))){
//                bestFriend = friend;
//            }
//        }
        if(rc.isMovementReady()) {
            if (bestFriend != null) {
                safeFuzzyMove(bestFriend.getLocation(), enemies);
            } else {
                bugNav(explorer.getExploreTarget());
                rc.setIndicatorLine(rc.getLocation(), explorer.getExploreTarget(), 0, 255, 0);
            }
        }
    }

    private RobotInfo getTargetEnemyByDist() throws GameActionException{
        RobotInfo targetEnemy = null;
        boolean isSoldier = false;
        int minDist = 999999;
        int minPaint = 999999;
        for(RobotInfo enemy : enemies){
            if(enemy.getPaintAmount() == 0 || enemy.type.isTowerType() || !reachableFrom(rc.getLocation(), enemy.getLocation())){
                continue;
            }
            int dist = rc.getLocation().distanceSquaredTo(enemy.getLocation());
            if(!isSoldier && enemy.getType() == UnitType.SOLDIER){
                isSoldier = true;
                minDist = dist;
                targetEnemy = enemy;
            }else if(isSoldier && enemy.getType() == UnitType.SOLDIER && dist < minDist){
                minDist = dist;
                targetEnemy = enemy;
            }else if(!isSoldier && dist < minDist){
                minDist = dist;
                targetEnemy = enemy;
            }
        }
        return targetEnemy;
    }

    private RobotInfo getTargetEnemyByPaint() throws GameActionException{
        RobotInfo targetEnemy = null;
        boolean foundSoldier = false;
        int minDist = Integer.MAX_VALUE;
        int minPaint = Integer.MAX_VALUE;

        for (RobotInfo enemy : enemies) {
            if (enemy.getPaintAmount() == 0
                    || enemy.getType().isTowerType()
                    || !reachableFrom(rc.getLocation(), enemy.getLocation())) {
                continue;
            }
            int dist = rc.getLocation().distanceSquaredTo(enemy.getLocation());
            int paint = enemy.getPaintAmount();
            boolean isSoldier = (enemy.getType() == UnitType.SOLDIER);

            if (isSoldier && !foundSoldier) {
                foundSoldier = true;
                targetEnemy = enemy;
                minDist = dist;
                minPaint = paint;
            } else if (isSoldier && foundSoldier) {
                if (paint < minPaint || (paint == minPaint && dist < minDist)) {
                    targetEnemy = enemy;
                    minDist = dist;
                    minPaint = paint;
                }
            } else if (!foundSoldier) {
                if (paint < minPaint || (paint == minPaint && dist < minDist)) {
                    targetEnemy = enemy;
                    minDist = dist;
                    minPaint = paint;
                }
            }
        }
        return targetEnemy;
    }

    private void combatState() throws GameActionException{
        rc.setIndicatorLine(rc.getLocation(), targetEnemy.getLocation(), 200, 10, 0);
        RobotInfo[][] cachedSenses = new RobotInfo[9][9];
        MapLocation placeToMove = null;
        Direction dirToSwing = null;
        int maxEnemiesHit = -5;
        // check if mop swing is worth it
        for(RobotInfo enemy : enemies){
            if(!enemy.type.isTowerType() && enemy.getPaintAmount() > 0){
                cachedSenses[enemy.getLocation().x - rc.getLocation().x + 4][enemy.getLocation().y - rc.getLocation().y + 4] = enemy;
            }
        }

        for(Direction dir : directions) {
            if (!rc.canMove(dir) && (closestEnemyTower == null || rc.getLocation().add(dir).distanceSquaredTo(closestEnemyTower.location) > closestEnemyTower.type.actionRadiusSquared)) {
                continue;
            }
            MapLocation swingLoc = rc.getLocation().add(dir);
            int north = 0;
            int east = 0;
            int south = 0;
            int west = 0;
            for (int i = -1; i <= 1; i++) {
                // north
                int relX = swingLoc.x - rc.getLocation().x + 4;
                int relY = swingLoc.y - rc.getLocation().y + 4;
                int relXI = relX + i;
                int relYI = relY + i;

                if (cachedSenses[relXI][relY + 1] != null) {
                    north++;
                }
                if(cachedSenses[relXI][relY + 2] != null){
                    north++;
                }
                // south
                if (cachedSenses[relXI][relY - 1] != null) {
                    south++;
                }
                if(cachedSenses[relXI][relY - 2] != null){
                    south++;
                }
                // east
                if (cachedSenses[relX + 1][relYI] != null) {
                    east++;
                }
                if(cachedSenses[relX + 2][relYI] != null){
                    east++;
                }
                // west
                if (cachedSenses[relX - 1][relYI] != null) {
                    west++;
                }
                if(cachedSenses[relX - 2][relYI] != null){
                    west++;
                }
            }
            int max = Math.max(Math.max(north, south), Math.max(east, west));
            if (max > maxEnemiesHit) {
                maxEnemiesHit = max;
                if(north == max){
                    dirToSwing = Direction.NORTH;
                }else if(south == max){
                    dirToSwing = Direction.SOUTH;
                }else if(east == max){
                    dirToSwing = Direction.EAST;
                }else if(west == max){
                    dirToSwing = Direction.WEST;
                }
                placeToMove = swingLoc;
            }
        }

        if(maxEnemiesHit >= 2
                && rc.isActionReady()
                && (rc.getPaint() > 30 || mapData.getMapInfo(rc.getLocation()).getPaint().isAlly())
                && !tooCloseToEnemyTower(placeToMove)){ // TODO optimize by paint remaining maybe
            rc.move(dirTo(placeToMove));
            if(rc.canMopSwing(dirToSwing)){
                rc.mopSwing(dirToSwing);
                rc.setIndicatorLine(rc.getLocation(), rc.getLocation().add(dirToSwing), 0, 255, 0);
            }
        }
        if(rc.isActionReady()) {
            if (!tryAttack(targetEnemy.getLocation()) && !rc.getLocation().isAdjacentTo(targetEnemy.getLocation())) {
                if (!tryMoveIntoRange(targetEnemy.getLocation(), 2, true)) {
                    safeFuzzyMove(rc.getLocation().directionTo(targetEnemy.getLocation()), enemies);
                    debugString.append("Moving towards enemy");
                }
                tryAttack(targetEnemy.getLocation());
            }

            if (rc.isActionReady()) {
                int xOff = targetEnemy.getLocation().x - rc.getLocation().x;
                int yOff = targetEnemy.getLocation().y - rc.getLocation().y;
                if (xOff < 0 && xOff >= -2 && yOff <= 1 && yOff >= -1) {
                    if (rc.canMopSwing(Direction.WEST)) {
                        rc.mopSwing(Direction.WEST);
                    }
                }
                if (yOff > 0 && yOff <= 2 && xOff <= 1 && xOff >= -1) {
                    if (rc.canMopSwing(Direction.NORTH)) {
                        rc.mopSwing(Direction.NORTH);
                    }
                }
                if (xOff > 0 && xOff <= 2 && yOff <= 1 && yOff >= -1) {
                    if (rc.canMopSwing(Direction.EAST)) {
                        rc.mopSwing(Direction.EAST);
                    }
                }
                if (yOff < 0 && yOff >= -2 && xOff <= 1 && xOff >= -1) {
                    if (rc.canMopSwing(Direction.SOUTH)) {
                        rc.mopSwing(Direction.SOUTH);
                    }
                }
            }
        }else{
            tryMoveIntoRange(targetEnemy.getLocation(), 8, true);
            if(rc.isMovementReady()) {
                if (!mapData.getMapInfo(rc.getLocation()).getPaint().isAlly()) {
                    MapLocation closestFriendly = closestFriendlyTileInRange(targetEnemy.location, 8);
                    if (closestFriendly != null) {
                        rc.setIndicatorLine(rc.getLocation(), closestFriendly, 0, 0, 150);
                        fuzzyMove(rc.getLocation().directionTo(closestFriendly));
                    } else if(distTo(targetEnemy.location) > 8){
                        safeFuzzyMove(rc.getLocation().directionTo(targetEnemy.location), enemies);
                    }
                }
            }
        }
    }

    private UnitState moppingState() throws GameActionException {
        rc.setIndicatorDot(closestEnemyPaint, 255, 255, 0);
//        if(!(rc.getLocation().isAdjacentTo(closestEnemyPaint) && rc.getLocation() != closestEnemyPaint) && !tryMoveIntoRange(closestEnemyPaint, 2)){
        if(rc.isActionReady()){
            if(!tryMoveIntoRange(closestEnemyPaint, 2)){
                bugNav(closestEnemyPaint);
                needMoppers = closestEnemyPaint;
            }
            tryAttack(closestEnemyPaint);
        }else{
            tryMoveIntoRange(closestEnemyPaint, 8);
            if(rc.isMovementReady()) {
                if (!mapData.getMapInfo(rc.getLocation()).getPaint().isAlly()) {
                    MapLocation closestFriendly = closestFriendlyTileInRange(rc.getLocation(), 8);
                    if (closestFriendly != null) {
                        rc.setIndicatorLine(rc.getLocation(), closestFriendly, 0, 0, 150);
                        fuzzyMove(rc.getLocation().directionTo(closestFriendly));
                    } else if(distTo(closestEnemyPaint) > 8){
                        safeFuzzyMove(rc.getLocation().directionTo(closestEnemyPaint), enemies);
                    }
                }
            }
        }
        return state;
    }

    public MapLocation closestFriendlyTileInRange(MapLocation target, int range) throws GameActionException{
        int closestFriendlyInRange = 9999;
        MapLocation closestFriendly = null;
        for (MapLocation loc : rc.getAllLocationsWithinRadiusSquared(target, 8)) {
            MapInfo info = mapData.getMapInfo(loc);
            if (info != null && info.getPaint().isAlly()) {
                int dist = rc.getLocation().distanceSquaredTo(loc);
                if (dist < closestFriendlyInRange) {
                    closestFriendlyInRange = dist;
                    closestFriendly = loc;
                }
            }
        }
        return closestFriendly;
    }

    private void refillAllies(RobotInfo[] allies) throws GameActionException {
        for (RobotInfo robot : allies) {
            // skip if is a tower
            if (robot.getType().isTowerType()) {
                continue;
            }

            // skip if ally is a mopper
            if (robot.getType() == UnitType.MOPPER) {
                continue;
            }

            int amount = Math.max(0, Math.min(rc.getPaint() - 30, robot.getType().paintCapacity - robot.paintAmount)); // leave mopper with a little bit
            if (amount != 0 && rc.canTransferPaint(robot.getLocation(), amount)) {
                rc.transferPaint(robot.getLocation(), amount);
            }
        }
    }

    private boolean attemptMopSweep(RobotInfo[] enemies) throws GameActionException {
        if (!rc.isActionReady()) {
            return false;
        }
        int up = 0;
        int down = 0;
        int left = 0;
        int right = 0;

        for (RobotInfo enemy : enemies) {
            if (enemy.getLocation().isWithinDistanceSquared(rc.getLocation(), 1)) {
                Direction dir = rc.getLocation().directionTo(enemy.getLocation());
                if (dir == Direction.NORTH || dir == Direction.NORTHWEST || dir == Direction.NORTHEAST) {
                    up++;
                }
                if (dir == Direction.SOUTH || dir == Direction.SOUTHWEST || dir == Direction.SOUTHEAST) {
                    down++;
                }
                if (dir == Direction.WEST || dir == Direction.NORTHWEST || dir == Direction.SOUTHWEST) {
                    left++;
                }
                if (dir == Direction.EAST || dir == Direction.NORTHEAST || dir == Direction.SOUTHEAST) {
                    right++;
                }
            }
        }

        int mostEnemy = Math.max(Math.max(up, down), Math.max(left, right));
        if (mostEnemy < 1) return false;
        Direction swingDir = null;
        if (mostEnemy == up) {
            swingDir = Direction.NORTH;
        } else if (mostEnemy == down) {
            swingDir = Direction.SOUTH;
        } else if (mostEnemy == left) {
            swingDir = Direction.WEST;
        } else {
            swingDir = Direction.EAST;
        }

        if (rc.canMopSwing(swingDir)) {
            rc.mopSwing(swingDir);
            return true;
        }

        return false;
    }
}

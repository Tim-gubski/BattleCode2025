package v21_Blub.Units;

import v21_Blub.Unit;
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
        if(!connected && spawnTower != null){
            currentlyConnectedToSpawnTower = rc.canSendMessage(spawnTower);
        }
        senseNearby(); // perform all scans

        previousState = state;
        state = determineState();

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
    private UnitState determineState() throws GameActionException {
        if ((state != UnitState.REFILLING && shouldRefill()) || (state == UnitState.REFILLING && rc.getPaint() < rc.getType().paintCapacity * 0.75)) {
            if (returnLoc == null) {
                returnLoc = rc.getLocation();
            }
            return UnitState.REFILLING;
        }

        if(!connected && rc.getRoundNum() - spawnTurn < 5){
            return UnitState.CONNECTING_TO_TOWER;
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
                    nearRuin = tile.getMapLocation().distanceSquaredTo(loc) <= 8;
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

        targetEnemy = getTargetEnemy();
        if(targetEnemy != null && !bestNearRuin){
            return UnitState.COMBAT;
        }

        if (closestEnemyPaint != null) {
            return UnitState.MOPPING;
        }
        return UnitState.EXPLORE;
    }

    private void stateInvariantActions() throws GameActionException {
//        if(state != UnitState.COMBAT && state != UnitState.REFILLING) {
            refillAllies(rc.senseNearbyRobots(-1, rc.getTeam()));
//        }

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
    }

    private void connectingToTower() throws GameActionException{
        MapLocation closestAllyPaint = null;
        int minDist = 9999;
        for(MapInfo tile : mapInfo){
            if(tile.getPaint().isAlly()){
                int dist = rc.getLocation().distanceSquaredTo(tile.getMapLocation());
                if(dist < minDist){
                    minDist = dist;
                    closestAllyPaint = tile.getMapLocation();
                }
            }
        }
        if(closestAllyPaint == null && allies.length < 2){
            connected = true;
            return;
        }else{
            fuzzyMove(closestAllyPaint);
        }
        if(spawnTower == null){
            connected = true;
        }else{
            connected = currentlyConnectedToSpawnTower;
        }
    }

    private void exploreState() throws GameActionException {
        if(needMoppers != null){
            safeFuzzyMove(needMoppers, enemies);
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
                safeFuzzyMove(returnLoc, enemies);
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
                safeFuzzyMove(explorer.getExploreTarget(), enemies);
                rc.setIndicatorLine(rc.getLocation(), explorer.getExploreTarget(), 0, 255, 0);
            }
        }
    }

    private RobotInfo getTargetEnemy() {
        RobotInfo targetEnemy = null;
        boolean isSoldier = false;
        int minDist = 999999;
        for(RobotInfo enemy : enemies){
            if(enemy.getPaintAmount() == 0 || enemy.type.isTowerType()){
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

    private void combatState() throws GameActionException{
        // check if mop swing is worth it
        int[][][] enemyInSwingRange = new int[3][3][4]; // 0 left, 1 up, 2 right, 3 down
        int max = 0;
        int[] maxIndex = new int[3];
        int myX = rc.getLocation().x;
        int myY = rc.getLocation().y;
        for(RobotInfo enemy : enemies){
            for(int xOff = -2; xOff <= 2; xOff++){
                int x = enemy.getLocation().x - myX + 1 + xOff;
                if(x < 0 || x >= 3){
                    continue;
                }
                for(int yOff = -2; yOff <= 2; yOff++){
                    int y = enemy.getLocation().y - myY + 1 + yOff;
                    if(y < 0 || y >= 3 || (Math.abs(xOff) == 2 && Math.abs(yOff) == 2)){ // trim corners
                        continue;
                    }
                    if(!rc.canMove(dirTo(rc.getLocation().translate(xOff, yOff)))){
                        continue;
                    }
                    if(x < myX && yOff <=1 && yOff >= -1){
                        enemyInSwingRange[x][y][0]++;
                    }
                    if(y > myY && xOff <=1 && xOff >= -1){
                        enemyInSwingRange[x][y][1]++;
                    }
                    if(x > myX && yOff <=1 && yOff >= -1){
                        enemyInSwingRange[x][y][2]++;
                    }
                    if(y < myY && xOff <=1 && xOff >= -1){
                        enemyInSwingRange[x][y][3]++;
                    }
                    for(int i = 0; i < 4; i++){
                        if(enemyInSwingRange[x][y][i] > max){
                            max = enemyInSwingRange[x][y][i];
                            maxIndex[0] = xOff;
                            maxIndex[1] = yOff;
                            maxIndex[2] = i;
                        }
                    }
                }
            }
        }

        if(max > 2){
            if(maxIndex[0] != 1 || maxIndex[1] != 1){
                tryMove(dirTo(rc.getLocation().translate(maxIndex[0]-1, maxIndex[1]-1)));
            }
            Direction dir;
            if(maxIndex[2] == 0){
                dir = Direction.WEST;
            }else if(maxIndex[2] == 1){
                dir = Direction.NORTH;
            }else if(maxIndex[2] == 2){
                dir = Direction.EAST;
            }else{
                dir = Direction.SOUTH;
            }
            if(rc.canMopSwing(dir)){
                rc.mopSwing(dir);
            }
        }



        if(!tryAttack(targetEnemy.getLocation()) && !rc.getLocation().isAdjacentTo(targetEnemy.getLocation())){
            if(!tryMoveIntoRange(targetEnemy.getLocation(), 2)){
                safeFuzzyMove(rc.getLocation().directionTo(targetEnemy.getLocation()), enemies);
            }
            tryAttack(targetEnemy.getLocation());
        }
    }

    private UnitState moppingState() throws GameActionException {
        rc.setIndicatorDot(closestEnemyPaint, 255, 255, 0);
//        if(!(rc.getLocation().isAdjacentTo(closestEnemyPaint) && rc.getLocation() != closestEnemyPaint) && !tryMoveIntoRange(closestEnemyPaint, 2)){
        if(rc.isActionReady()){
            if(!tryMoveIntoRange(closestEnemyPaint, 2)){
                safeFuzzyMove(rc.getLocation().directionTo(closestEnemyPaint), enemies);
            }
            tryAttack(closestEnemyPaint);
        }else{
            if(!tryMoveIntoRange(closestEnemyPaint, 8)){
                safeFuzzyMove(rc.getLocation().directionTo(closestEnemyPaint), enemies);
            }
        }
        return state;
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

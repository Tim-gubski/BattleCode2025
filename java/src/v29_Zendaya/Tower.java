package v29_Zendaya;

import v29_Zendaya.Util.Comms;
import battlecode.common.*;

import java.util.Map;

public abstract class Tower extends Robot {
    boolean firstTower = false;
    int TOWER_BUILD_COST = 100;
    Direction enemyDirection;
    Map<Integer, Integer> messagesSent = new java.util.HashMap<>();
    MapLocation[] spawnLocs;

//    int randomIndex = 0;
//    int[] randomShiz = { 0, 1, 3, 8, 11, 0, 1, 8, 2, 7, 6, 3, 5, 7, 12, 4, 0, 1, 10, 12, 2, 5, 7, 12, 8, 8, 10, 6, 6, 5, 8, 2, 4, 4, 11, 5, 9, 3, 0, 6, 12, 10, 1, 10, 10, 1, 8, 4, 3, 6, 12, 3, 9, 6, 3, 6, 11, 11, 9, 3, 11, 1, 6, 5, 11, 11, 5, 12, 2, 9, 1, 5, 8, 2, 10, 11, 0, 9, 3, 2, 3, 4, 0, 9, 1, 11, 0, 3, 2, 6, 9, 1, 0, 10, 3, 9, 0, 5, 7, 0, 3, 9, 0, 11, 9, 3, 8, 2, 5, 12, 9, 10, 2, 11, 4, 10, 7, 7, 2, 0, 10, 11, 2, 9, 1, 4, 10, 6, 6, 7, 2, 6, 4, 6, 12, 10, 2, 8, 4, 11, 8, 4, 3, 7, 11, 3, 4, 1, 1, 7, 0, 9, 1, 7, 11, 12, 11, 11, 1, 1, 9, 4, 0, 11, 0, 1, 9, 4, 0, 5, 1, 8, 1, 10, 1, 10, 9, 2, 9, 7, 3, 3, 2, 7, 6, 8, 8, 6, 9, 10, 9, 2, 9, 3, 9, 6, 11, 2, 2, 1, 6, 0, 2, 9, 8, 2, 5, 0, 0, 3, 9, 3, 5, 7, 6, 2, 2, 8, 8, 3, 5, 0, 9, 6, 8, 12, 1, 4, 2, 4, 6, 5, 10, 3, 11, 5, 10, 9, 1, 9, 2, 7, 0, 6, 7, 10, 1, 2, 8, 9, 12, 11, 2, 9, 9, 12, 4, 7, 11, 3, 1, 10, 0, 7, 3, 1, 2, 7, 2, 10, 10, 10, 10, 2, 6, 5, 12, 12, 10, 1, 2, 7, 6, 6, 5, 7, 12, 9, 0, 6, 3, 7, 10, 4, 4, 5, 2, 2, 3, 0, 8, 10, 1, 4, 3, 5, 10, 5, 11, 5, 10, 6, 10, 9, 1, 8, 12, 7, 1, 1, 7, 2, 2, 2, 6, 10, 3, 1, 11, 3, 1, 2, 4, 9, 8, 12, 9, 6, 4, 8, 0, 9, 9, 6, 8, 2, 3, 11, 4, 11, 8, 9, 3, 8, 0, 7, 5, 7, 12, 6, 9, 0, 11, 10, 8, 6, 12, 12, 12, 7, 6, 12, 11, 0, 8, 6, 4, 9, 6, 9, 3, 12, 4, 6, 5, 11, 12, 5, 1, 2, 12, 11, 1, 2, 7, 4, 6, 12, 3, 8, 10, 3, 9, 10, 6, 0, 3, 4, 7, 5, 3, 6, 1, 12, 5, 7, 0, 5, 7, 9, 8, 1, 4, 3, 3, 7, 11, 1, 7, 4, 9, 3, 11, 7, 8, 6, 2, 5, 3, 3, 8, 8, 11, 2, 3, 8, 11, 6, 1, 0, 7, 10, 5, 6, 7, 8, 5, 9, 12, 3, 0, 12, 1, 4, 5, 4, 3, 3, 4, 4, 0, 7, 12, 4, 10, 0, 7, 0, 11, 7, 10, 9, 10, 12, 6, 0, 10, 9, 5, 2, 12, 10, 8, 0, 0, 5, 2, 10, 7, 1, 6, 1, 0, 4, 12, 11, 7, 0, 2, 11, 9, 5, 4, 9, 8, 3, 11, 7, 1, 4, 4, 1, 6, 1, 2, 8, 11, 11, 10, 6, 10, 2, 11, 8, 4, 4, 11, 12, 0, 2, 12, 1, 12, 5, 1, 0, 12, 10, 6, 12, 7, 3, 10, 2, 11, 12, 1, 0, 10, 2, 2, 9, 1, 4, 12, 9, 8, 5, 2, 0, 9, 6, 0, 11, 3, 3, 11, 0, 4, 7, 1, 12, 1, 4, 4, 3, 7, 8, 12, 11, 4, 1, 11, 3, 6, 0, 9, 2, 3, 6, 3, 0, 11, 8, 3, 8, 4, 8, 8, 2, 1, 0, 3, 2, 0, 7, 9, 9, 11, 3, 0, 5, 6, 2, 12, 11, 11, 2, 11, 8, 1, 12, 0, 5, 10, 7, 0, 0, 2, 1, 9, 9, 0, 9, 10, 3, 2, 2, 10, 6, 12, 9, 5, 11, 11, 9, 11, 7, 3, 8, 2, 0, 1, 11, 11, 2, 9, 7, 3, 7, 6, 7, 9, 8, 9, 2, 2, 6, 9, 7, 7, 7, 12, 7, 2, 2, 4, 5, 6, 3, 0, 7, 4, 2, 12, 5, 4, 12, 3, 6, 2, 1, 2, 10, 2, 1, 3, 4, 1, 8, 9, 7, 8, 0, 9, 3, 8, 3, 2, 5, 11, 12, 4, 2, 8, 12, 6, 2, 4, 4, 7, 5, 11, 11, 5, 5, 4, 7, 3, 9, 4, 10, 6, 12, 4, 10, 12, 0, 2, 11, 7, 2, 2, 0, 7, 5, 4, 8, 9, 11, 6, 9, 1, 2, 1, 2, 10, 6, 5, 3, 10, 0, 9, 8, 10, 2, 0, 0, 9, 1, 0, 7, 2, 1, 2, 6, 12, 5, 10, 3, 4, 2, 4, 4, 6, 11, 3, 6, 10, 2, 2, 10, 0, 7, 6, 12, 6, 6, 12, 7, 4, 4, 9, 1, 12, 4, 11, 5, 5, 10, 10, 0, 9, 5, 9, 0, 2, 1, 12, 4, 5, 9, 7, 9, 7, 8, 6, 11, 4, 1, 1, 5, 11, 7, 9, 0, 10, 3, 3, 4, 8, 9, 1, 0, 5, 1, 3, 11, 4, 1, 4, 10, 3, 0, 0, 5, 11, 12, 6, 1, 2, 8, 5, 1, 1, 10, 9, 1, 0, 0, 5, 2, 0, 6, 0, 0, 12, 5, 0, 12, 2, 7, 2, 11, 8, 3, 2, 10, 7, 7, 6, 3, 7, 8, 9, 5, 3, 10, 11, 12, 1, 0, 1, 11, 4, 11, 1, 11, 9, 10, 9, 3, 2, 7, 7, 11, 9, 8, 3, 9, 11, 7, 7, 0, 11, 1, 7, 8, 5, 12, 12, 9, 12, 11, 7, 10, 5, 11, 10, 6, 11, 12, 4, 6, 3, 10, 12, 9, 9, 12, 4, 12, 6, 5, 11, 9, 7, 7, 12, 2, 0, 1, 9, 8, 3, 6, 8, 1, 2, 9, 10, 6, 1, 3, 11, 10, 5, 8, 10, 7, 2, 1, 9, 8, 8, 12, 10, 9, 1, 1 };

    public Tower(RobotController robot) throws GameActionException {
        super(robot);
        if(rc.getRoundNum()<8){
            firstTower = true;
        }

        enemyDirection = rc.getLocation().directionTo(new MapLocation(width/2,height/2));
        spawnLocs = rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), GameConstants.BUILD_ROBOT_RADIUS_SQUARED);
        if(firstTower){
            spawnTurn = 0;
        }
    }

    int spawnTurn = rc.getRoundNum()-1;
    int startSpawnTurn = spawnTurn;
    int emergencyMopperTurn = -1000;
    final int MOPPER_COOLDOWN = 25;
    public void turn() throws GameActionException {
        if(mapData.friendlyTowers.size() < rc.getNumberTowers()) {
            communication.parseMessages();
        }

        RobotInfo[] allies = rc.senseNearbyRobots(-1, rc.getTeam());
        mapData.updateAdjacentAllies(allies);
//        MapLocation temp;
//        int j;
//        for (int i = 12; i > 0; i--) {
//            j = randomShiz[randomIndex];
//            randomIndex = (randomIndex + 1) % randomShiz.length;
//            temp = spawnLocs[i];
//            spawnLocs[i] = spawnLocs[j];
//            spawnLocs[j] = temp;
//        }
        shuffleArray(spawnLocs);
        shuffleArray(allies);

        // tower upgrade
        if(rc.getChips()>3000 && rc.canUpgradeTower(rc.getLocation()) && (allies.length > 3 || rc.getChips()>4000)){
            debugString.append("Broadcasting");
            rc.upgradeTower(rc.getLocation());
        }

        // broadcast spawn location
        if(rc.getRoundNum() < 5 && rc.canBroadcastMessage()){
            rc.broadcastMessage(communication.constructMessage(Comms.Codes.TOWER_LOC, rc.getLocation(), rc.getType()));
        }

        // send messages to robots
        RobotInfo[] friendlyTowers = mapData.friendlyTowers.getArray();
        for(RobotInfo ally : allies){
            if(ally.type == UnitType.MOPPER && needMoppers != null){
                if(rc.canSendMessage(ally.location)){
                    rc.sendMessage(ally.location, communication.constructMessage(Comms.Codes.MOPPER_LOC, needMoppers));
                    needMoppers = null;
                }
            }
            if(!messagesSent.containsKey(ally.ID)){
                messagesSent.put(ally.ID, 0);
            }
            for(int i = messagesSent.get(ally.ID); i < friendlyTowers.length; i++){
//                rc.setIndicatorLine(rc.getLocation(), ally.location, 150, 0 ,150);
//                rc.setIndicatorLine(ally.location, friendlyTowers[i].location, 0, 150, 150);
                if(rc.canSenseRobotAtLocation(ally.location) && rc.canSendMessage(ally.location)){
                    rc.sendMessage(ally.location, communication.constructMessage(Comms.Codes.TOWER_LOC, friendlyTowers[i].location, friendlyTowers[i].type.getBaseType()));
                    rc.setIndicatorLine(rc.getLocation(), ally.location, 150, 0 ,150);
                    rc.setIndicatorLine(ally.location, friendlyTowers[i].location, 0, 150, 150);
                    messagesSent.put(ally.ID, i+1);
                }else{
                    break;
                }
            }
        }
        // indicate friendly towers
        for(RobotInfo tower : mapData.getFriendlyTowers()){
            trySetIndicatorDot(tower.location, 255, 0, 255);
        }

        // look for nearby enemy paint
        MapInfo[] nearbyTiles = rc.senseNearbyMapInfos(-1);
//        boolean enemyPaint = false;
        for(MapInfo tile : nearbyTiles) {
            if (tile.getPaint().isEnemy() && reachableFrom(rc.getLocation(), tile.getMapLocation())) {
//                enemyPaint = true;
                needMoppers = tile.getMapLocation();
                break;
            }
        }

        // emergency moppers
//        if (emergencyMopperTurn)
        RobotInfo[] enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
        int soldiers = 0;
        for(RobotInfo enemy : enemies){
            if(enemy.type == UnitType.SOLDIER && reachableFrom(rc.getLocation(), enemy.location)){
                soldiers++;
            }
        }
        if(soldiers >= 2 && rc.getRoundNum() - emergencyMopperTurn > MOPPER_COOLDOWN){// && rc.getRoundNum() < 100){
            if(trySummonInDirection(UnitType.MOPPER, dirTo(enemies[0].location))){
                emergencyMopperTurn = rc.getRoundNum();
            }
        }

        // spawner logic
        if(rc.getType().getBaseType() == UnitType.LEVEL_ONE_PAINT_TOWER || firstTower) {
            if(rc.getRoundNum()<400) {
                if (allies.length < 3 && (((spawnTurn - startSpawnTurn < 2 || (spawnTurn - startSpawnTurn < 3 && needMoppers != null)) && (firstTower || rc.getNumberTowers() > 5)) || rc.getChips() > 2100)) {
                    if (spawnTurn % 5 < 2) {
                        debugString.append("trying to make soldier");
                        if (trySummon(UnitType.SOLDIER)) {
                            spawnTurn++;
                        }
                    } else if (spawnTurn % 5 < 4 && needMoppers != null) {
                        debugString.append("trying to make mopper");
                        if (trySummon(UnitType.MOPPER)) {
                            spawnTurn++;
                        }
                    } else {
                        debugString.append("trying to make splasher");
                        if (trySummonInDirection(UnitType.SPLASHER, enemyDirection)) {
                            spawnTurn++;
                        }
                    }
                }
            }else{
                if (spawnTurn % 5 < 1) {
                    debugString.append("trying to make soldier");
                    if (trySummon(UnitType.SOLDIER)) {
                        spawnTurn++;
                    }
                } else if (spawnTurn % 5 < 3) {
                debugString.append("trying to make mopper");
                if (trySummon(UnitType.MOPPER)) {
                    spawnTurn++;
                }
                } else {
                    debugString.append("trying to make splasher");
                    if (trySummonInDirection(UnitType.SPLASHER, enemyDirection)) {
                        spawnTurn++;
                    }
                }
            }
        }else if(rc.getType().getBaseType() == UnitType.LEVEL_ONE_MONEY_TOWER && (rc.getNumberTowers()>=4 || rc.getChips() > 2000)){
            if(rc.getPaint() > UnitType.SPLASHER.paintCost){
                trySummonInDirection(UnitType.SPLASHER, enemyDirection);
            }else{
                if(needMoppers != null){
                    trySummon(UnitType.MOPPER);
                }else{
                    trySummon(UnitType.SOLDIER);
                }
            }
        }

        tryAttack(null);
        enemies = rc.senseNearbyRobots(-1,rc.getTeam().opponent());
        RobotInfo bestEnemy = null;
        int minHealth = 9999;
        boolean isSoldier = false;
        for(RobotInfo enemy : enemies){
            if(!rc.canAttack(enemy.location)){
                continue;
            }
            if(!isSoldier && enemy.getType() == UnitType.SOLDIER){
                bestEnemy = enemy;
                minHealth = enemy.getHealth();
                isSoldier = true;
            }else if(isSoldier && enemy.getType() == UnitType.SOLDIER && enemy.getHealth() < minHealth){
                bestEnemy = enemy;
                minHealth = enemy.getHealth();
            }else if(!isSoldier && enemy.getType() != UnitType.SOLDIER && enemy.getHealth() < minHealth){
                bestEnemy = enemy;
                minHealth = enemy.getHealth();
            }
        }
        if(bestEnemy != null){
            tryAttack(bestEnemy.location);
        }

        if (returnLoc != null) {
            int frontlineMessage = communication.constructMessage(Comms.Codes.FRONTLINE, returnLoc);
            for (RobotInfo friend : allies) {
                if (rc.canSendMessage(friend.getLocation())) rc.sendMessage(friend.getLocation(), frontlineMessage);
            }
        }
    }



    public boolean trySummon(UnitType type, MapLocation loc) throws GameActionException{
        if(rc.canBuildRobot(type, loc)){
            rc.buildRobot(type,loc);
            return true;
        }
        return false;
    }

    public boolean isCardinal(Direction dir){
        return dir.dx == 0 || dir.dy == 0;
    }

    public boolean trySummon(UnitType type) throws GameActionException{
        MapLocation bestLoc = null;
        int minAdj = 999;
        for(MapLocation loc : spawnLocs){
            int adj = mapData.getAdjacentAllies(loc);
            MapInfo info = rc.senseMapInfo(loc);
            if(info.getPaint().isAlly()){
                adj -= 2;
            }
            if(loc.isAdjacentTo(rc.getLocation())){
                adj += 1;
            }
            if(rc.canBuildRobot(type, loc) && (adj < minAdj || adj == minAdj && isCardinal(rc.getLocation().directionTo(loc)))){
                minAdj = adj;
                bestLoc = loc;
            }
        }
        if(bestLoc != null){
            rc.buildRobot(type, bestLoc);
            return true;
        }
        return false;
    }


    public boolean trySummonInDirection(UnitType type, Direction targetDirection) throws GameActionException{
        for(Direction dir : fuzzyDirs(targetDirection)){
            if(trySummon(type, rc.getLocation().add(dir).add(dir)) || trySummon(type, rc.getLocation().add(dir))){
                return true;
            }
        }
        return false;
    }

    public boolean trySummonAnything() throws GameActionException {
//        double soldierChance    = 0.4 * Math.min(1, 50 / (rc.getRoundNum() * 1.2));
//        double mopperChance     = soldierChance + (0.4 * Math.min(1, 50 / (rc.getRoundNum() * 1.2)));
//        double rand = rng.nextDouble();
//        if(rand < soldierChance || rc.getRoundNum() <= 5) {
//            return trySummon(UnitType.SOLDIER);
//        } else if (rand < mopperChance || rc.getRoundNum() <= 50) {
//            return trySummon(UnitType.MOPPER);
//        } else {
//            return trySummon(UnitType.SPLASHER);
//        }
        if(rc.getRoundNum()<2){
            return trySummon(UnitType.SOLDIER);
        }
        double rand = rng.nextDouble();
        if(rc.getRoundNum()<40){
            if(rand < 0.5){
                return trySummon(UnitType.SOLDIER);
            }else{
                return trySummon(UnitType.MOPPER);
            }
        }else{
            double soldierChance    = 0.4 * Math.min(1, 50 / (rc.getRoundNum() * 1.2));
            double mopperChance     = soldierChance + (0.4 * Math.min(1, 50 / (rc.getRoundNum() * 1.2)));
            if(rand < soldierChance || rc.getRoundNum() <= 5) {
                return trySummon(UnitType.SOLDIER);
            } else if (rand < mopperChance || rc.getRoundNum() <= 50) {
                return trySummon(UnitType.MOPPER);
            } else {
                return trySummon(UnitType.SPLASHER);
            }
        }
    }
}


package v47_GodSlayer;

import v47_GodSlayer.Util.Comms;
import battlecode.common.*;

import java.util.Map;

public abstract class Tower extends Robot {
    boolean firstTower = false;
    int TOWER_BUILD_COST = 100;
    Direction enemyDirection;
    Map<Integer, Integer> messagesSent = new java.util.HashMap<>();
    MapLocation[] spawnLocs;

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
        mapData.setMapInfos(rc.senseNearbyMapInfos(-1), PaintType.ALLY_PRIMARY, PaintType.ALLY_SECONDARY);
    }

    int spawnTurn = rc.getRoundNum()-1;
    int startSpawnTurn = spawnTurn;
    int emergencyMopperTurn = -1000;
    int needMopperTurn = -1000;
    final int MOPPER_COOLDOWN = 25;
    int mopperCount = 0;
    boolean nearbyPaintFound = false;
    int lastTurnTowerCount = 2;
    int currentTurnTowerCount = 2;
    int lastTowerDeathTurn = -100;
    public void turn() throws GameActionException {
        lastTurnTowerCount = currentTurnTowerCount;
        currentTurnTowerCount = rc.getNumberTowers();
        if(lastTurnTowerCount > currentTurnTowerCount){
            lastTowerDeathTurn = rc.getRoundNum();
        }

        if(mapData.friendlyTowers.size() < rc.getNumberTowers()) {
            communication.parseMessages();
        }

        RobotInfo[] allies = rc.senseNearbyRobots(-1, rc.getTeam());
        mapData.updateAdjacentAllies(allies);
        shuffleArray(spawnLocs);
        shuffleArray(allies);

        // tower upgrade
        if(rc.getType().getBaseType() == UnitType.LEVEL_ONE_PAINT_TOWER) {
            if (rc.getChips() > 2500 && rc.canUpgradeTower(rc.getLocation()) && (allies.length >= 3 || rc.getChips() > 3500)) {
                debugString.append("Upgrading");
                rc.upgradeTower(rc.getLocation());
            }
        }else{
            if (rc.getChips() > 3600 && rc.canUpgradeTower(rc.getLocation()) && (allies.length > 3 || rc.getChips() > 4000)) {
                debugString.append("Upgrading");
                rc.upgradeTower(rc.getLocation());
            }
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
        boolean enemyPaint = false;
        if(!nearbyPaintFound) {
            for (MapInfo tile : nearbyTiles) {
                if (tile.getPaint().isEnemy() && reachableFrom(rc.getLocation(), tile.getMapLocation())) {
                    enemyPaint = true;
                    needMoppers = tile.getMapLocation();
                    debugString.append("Enemy Paint Found\n");
                    nearbyPaintFound = true;
                    break;
                }
            }
        }

        // kill self if rich
        if(rc.getType().getBaseType() == UnitType.LEVEL_ONE_MONEY_TOWER
                && rc.getChips()>10000
                && rc.getRoundNum() - lastTowerDeathTurn > 50
                && !enemyPaint
                && rc.senseNearbyRobots(8, rc.getTeam()).length > 0
                && rc.senseNearbyRobots(-1, rc.getTeam().opponent()).length == 0){
            rc.disintegrate();
        }

        // emergency moppers
        RobotInfo[] enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
        int soldiers = 0;
        int moppers = 0;
        for(RobotInfo enemy : enemies){
            if(enemy.type == UnitType.SOLDIER && reachableFrom(rc.getLocation(), enemy.location)){
                soldiers++;
            }
        }
        for(RobotInfo ally : allies){
            if(ally.type == UnitType.MOPPER){
                moppers++;
            }
        }
        if(soldiers >= 1 && moppers < soldiers && (rc.getRoundNum() - emergencyMopperTurn > MOPPER_COOLDOWN || mopperCount < soldiers)){// && rc.getRoundNum() < 100){
            if(trySummonInDirection(UnitType.MOPPER, dirTo(enemies[0].location))){
                if(rc.getRoundNum() - emergencyMopperTurn > MOPPER_COOLDOWN){
                    emergencyMopperTurn = rc.getRoundNum();
                    mopperCount = 1;
                }else {
                    mopperCount++;
                }
                debugString.append("Emergency Mopper!");
            }
        }

        debugString.append("\nNeed Moppers: " + needMoppers);
        debugString.append("\nspawnTurn: " + (spawnTurn - startSpawnTurn));
        // spawner logic
        if(rc.getType().getBaseType() == UnitType.LEVEL_ONE_PAINT_TOWER || firstTower) {
            // spawning need moppers code
            if(needMoppers != null && spawnTurn - startSpawnTurn >= 2 && rc.getRoundNum()-emergencyMopperTurn > MOPPER_COOLDOWN){
                if(trySummon(UnitType.MOPPER)){
                    emergencyMopperTurn = rc.getRoundNum();
                    mopperCount++;
                }
                debugString.append("trying to make mopper");
            }
            if(rc.getRoundNum()<400 && rc.getNumberTowers() <= 5) {
                if (allies.length < 3 && (((spawnTurn - startSpawnTurn < 2) && (firstTower || rc.getNumberTowers() > 5)) || rc.getChips() > 2100)) {
                    if (spawnTurn % 3 < 2) {
                        debugString.append("trying to make soldier");
                        if(startSpawnTurn-spawnTurn == 0){
                            if(trySummonInDirection(UnitType.SOLDIER, enemyDirection)){
                                debugString.append("Soldier Spawned 1");
                                spawnTurn++;
                            }
                        }
                        if (trySummon(UnitType.SOLDIER)) {
                            debugString.append("Soldier Spawned 2");
                            spawnTurn++;
                        }
                    }else {
                        debugString.append("trying to make splasher");
                        if (trySummonInDirection(UnitType.SPLASHER, enemyDirection)) {
                            spawnTurn++;
                        }
                    }
                }
//            }else if(rc.getChips()>1000){
            }else if(rc.getChips()>1100){
                if (spawnTurn % 6 < 2) {
                    debugString.append("trying to make soldier");
                    if (trySummon(UnitType.SOLDIER)) {
                        debugString.append("Soldier Spawned 3");
                        spawnTurn++;
                    }
                } else if (spawnTurn % 6 < 4) {
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
                if(Math.random()>0.3) {
                    trySummonInDirection(UnitType.SPLASHER, enemyDirection);
                }else{
//                    trySummonInDirection(UnitType.SOLDIER, enemyDirection);
                    trySummon(UnitType.SOLDIER);
                }
            }else{
//                if (Math.random() < 0.5) {
//                    trySummonInDirection(UnitType.SOLDIER, enemyDirection);
//                } else {
//                    trySummonInDirection(UnitType.MOPPER, enemyDirection);
//                }
                if(needMoppers != null){
                    trySummonInDirection(UnitType.MOPPER, dirTo(needMoppers));
                }else{
                    trySummonInDirection(UnitType.SOLDIER, enemyDirection);
                    debugString.append("Soldier Spawned 5?");
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
            // dont spawn stuff into walls
            if(width-loc.x < 4 || loc.x < 4 || height-loc.y < 4 || loc.y < 4){
                continue;
            }
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
        }else{
            return trySummonInDirection(type, enemyDirection);
        }
    }


    public boolean trySummonInDirection(UnitType type, Direction targetDirection) throws GameActionException{
//        for(Direction dir : fuzzyDirs(targetDirection)){
            if(trySummon(type, rc.getLocation().add(targetDirection).add(targetDirection)) || trySummon(type, rc.getLocation().add(targetDirection))){
                return true;
            }
//        }
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


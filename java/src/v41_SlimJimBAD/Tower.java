package v41_SlimJimBAD;

import v41_SlimJimBAD.Util.Comms;
import battlecode.common.*;

import java.util.Map;

public abstract class Tower extends Robot {
    boolean firstTower = false;
    int TOWER_BUILD_COST = 100;
    Direction enemyDirection;
    Map<Integer, Integer> messagesSent = new java.util.HashMap<>();
    MapLocation[] spawnLocs;
    MapLocation[] enemySpawnLocs;
    UnitType moneyTowerFirstSpawn;

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
        MapLocation myloc = rc.getLocation();
        enemySpawnLocs = new MapLocation[]{new MapLocation(myloc.x, height - 1 - myloc.y),
                new MapLocation(width - 1 - myloc.x, myloc.y),
                new MapLocation(width - 1 - myloc.x, height - 1 - myloc.y),
        };
        moneyTowerFirstSpawn = Math.random() > 0.7 ? UnitType.SOLDIER : UnitType.SPLASHER;
    }

    int spawnTurn = rc.getRoundNum()-1;
    int startSpawnTurn = spawnTurn;
    int emergencyMopperTurn = -1000;
    int needMopperTurn = -1000;
    final int MOPPER_COOLDOWN = 25;
    int mopperCount = 0;
    boolean nearbyPaintFound = false;
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
        if(rc.getChips()>2500 && rc.canUpgradeTower(rc.getLocation()) && (allies.length > 3 || rc.getChips()>3500)){
            debugString.append("Upgrading");
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
        MapInfo[] nearbyTiles = rc.senseNearbyMapInfos(8);
//        boolean enemyPaint = false;
        for(MapInfo tile : nearbyTiles) {
            if (tile.getPaint().isEnemy() && reachableFrom(rc.getLocation(), tile.getMapLocation()) && !nearbyPaintFound) {
//                enemyPaint = true;
                needMoppers = tile.getMapLocation();
                nearbyPaintFound = true;
                break;
            }
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
            if(needMoppers != null && spawnTurn - startSpawnTurn >= 2 && rc.getRoundNum()-emergencyMopperTurn > MOPPER_COOLDOWN){
                if(trySummon(UnitType.MOPPER)){
                    emergencyMopperTurn = rc.getRoundNum();
                    mopperCount++;
                }
                debugString.append("trying to make mopper");
            }
            if(rc.getRoundNum()<400 && rc.getNumberTowers() <= 5) {
                if (allies.length < 3 && (((spawnTurn - startSpawnTurn < 2 || (spawnTurn - startSpawnTurn < 3 && needMoppers != null)) && (firstTower || rc.getNumberTowers() > 5)) || rc.getChips() > 2100)) {
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
            }else if (rc.getChips()>1000){
                if (spawnTurn % 5 < 1) {
                    debugString.append("trying to make soldier");
                    if (trySummon(UnitType.SOLDIER)) {
                        debugString.append("Soldier Spawned 3");
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
        }else if(rc.getType().getBaseType() == UnitType.LEVEL_ONE_MONEY_TOWER && (rc.getNumberTowers() >= 4 || rc.getChips() > 2000)){
            if(rc.getPaint() > UnitType.SPLASHER.paintCost){
                trySummonInDirection(moneyTowerFirstSpawn, enemyDirection);
            }else if(rc.getChips()>1000){
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
            if(loc.x < 3 || loc.y < 3 || loc.x > width - 4 || loc.y > height - 4){
                continue;
            }
            boolean tooCloseToEnemySpawn = false;
            for(MapLocation enemyLoc : enemySpawnLocs){
                if(loc.distanceSquaredTo(enemyLoc) <= rc.getType().actionRadiusSquared){
                    tooCloseToEnemySpawn = true;
                    break;
                }
            }
            if(tooCloseToEnemySpawn){
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
        boolean add1TooClose = false;
        boolean add2TooClose = false;
        for(MapLocation enemyLoc : enemySpawnLocs){
            if(rc.getLocation().add(targetDirection).add(targetDirection).distanceSquaredTo(enemyLoc) <= rc.getType().actionRadiusSquared){
                add2TooClose = true;
            }
            if(rc.getLocation().add(targetDirection).distanceSquaredTo(enemyLoc) <= rc.getType().actionRadiusSquared){
                add1TooClose = true;
            }
        }
        if((!add2TooClose && trySummon(type, rc.getLocation().add(targetDirection).add(targetDirection))) || (!add1TooClose && trySummon(type, rc.getLocation().add(targetDirection)))){
            return true;
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


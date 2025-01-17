package v16_FartSmella;

import v16_FartSmella.Util.Comms;
import battlecode.common.*;

import java.util.Map;

public abstract class Tower extends Robot {
    boolean firstTower = false;
    int TOWER_BUILD_COST = 100;
    Direction enemyDirection;
    Map<Integer, Integer> messagesSent = new java.util.HashMap<>();

    public Tower(RobotController robot) throws GameActionException {
        super(robot);
        if(rc.getRoundNum()<8){
            firstTower = true;
        }

        enemyDirection = rc.getLocation().directionTo(new MapLocation(width/2,height/2));
    }
    int startSpawnTurn = rc.getRoundNum()-1;
    int spawnTurn = rc.getRoundNum()-1;
    public void turn() throws GameActionException {
        RobotInfo[] allies = rc.senseNearbyRobots(-1, rc.getTeam());
        shuffleArray(allies);

        if(rc.getChips()>3000 && rc.canUpgradeTower(rc.getLocation()) && (allies.length > 3 || rc.getChips()>5000)){
            rc.upgradeTower(rc.getLocation());
        }

        communication.parseMessages();

        if(rc.getRoundNum() < 3 && rc.canBroadcastMessage()){
            rc.broadcastMessage(communication.constructMessage(Comms.Codes.TOWER_LOC, rc.getLocation(), rc.getType()));
        }

        RobotInfo[] friendlyTowers = mapData.friendlyTowers.getArray();
        for(RobotInfo ally : allies){
            if(!messagesSent.containsKey(ally.ID)){
                messagesSent.put(ally.ID, 0);
            }
            for(int i = messagesSent.get(ally.ID); i < friendlyTowers.length; i++){
                if(rc.canSendMessage(ally.location)){
                    rc.sendMessage(ally.location, communication.constructMessage(Comms.Codes.TOWER_LOC, friendlyTowers[i].location, friendlyTowers[i].type.getBaseType()));
                    messagesSent.put(ally.ID, i+1);
                }else{
                    break;
                }
            }
        }
        for(RobotInfo tower : mapData.getFriendlyTowers()){
            trySetIndicatorDot(tower.location, 255, 0, 255);
        }

        MapInfo[] nearbyTiles = rc.senseNearbyMapInfos(-1);
        boolean enemyPaint = false;
        for(MapInfo tile : nearbyTiles) {
            if (isEnemyPaint(tile.getPaint())) {
                enemyPaint = true;
                break;
            }
        }
        if(rc.getType().getBaseType() == UnitType.LEVEL_ONE_PAINT_TOWER || firstTower) {
//            if (enemyPaint) {
//                if (trySummon(UnitType.SPLASHER)) {
//                    return;
//                }
//            } else {
                if (allies.length < 3 && (spawnTurn - startSpawnTurn < 3 || rc.getChips() > 1000)) {
                    if (spawnTurn % 5 < 2) {
                        if (trySummon(UnitType.SOLDIER)) {
                            spawnTurn++;
                        }
                    } else if (spawnTurn % 5 < 4) {
                        if (trySummon(UnitType.MOPPER)) {
                            spawnTurn++;
                        }
                    } else {
                        if (trySummonInDirection(UnitType.SPLASHER, enemyDirection)) {
                            spawnTurn++;
                        }
                    }
                }
//            }
        }else if(rc.getType().getBaseType() == UnitType.LEVEL_ONE_MONEY_TOWER){
            if(rc.getPaint() > UnitType.SPLASHER.paintCost){
                trySummonInDirection(UnitType.SPLASHER, enemyDirection);
            }else{
                trySummonAnything();
            }
        }

        tryAttack(null);
        RobotInfo[] enemies = rc.senseNearbyRobots(-1,rc.getTeam().opponent());
        if(enemies.length > 0){
            tryAttack(getClosest(enemies).getLocation());
        }

        if (returnLoc != null) {
            int frontlineMessage = communication.constructMessage(Comms.Codes.FRONTLINE, returnLoc);
            for (RobotInfo friend : allies) {
                if (rc.canSendMessage(friend.getLocation())) rc.sendMessage(friend.getLocation(), frontlineMessage);
            }
        }

//        boolean instaRespawn = true;
//        for(int x = -2; x <= 2; x++){
//            for(int y = -2; y <= 2; y++) {
//                MapInfo info = rc.senseMapInfo(rc.getLocation().translate(x,y));
//                if(info.getMark() != info.getPaint()){
//                    instaRespawn = false;
//                }
//            }
//        }

//        if(rc.getPaint()<50 && rc.getChips() >= TOWER_BUILD_COST && rc.senseNearbyRobots(-1, rc.getTeam()).length > 0 && enemies.length == 0 && rc.senseMapInfo(rc.getLocation()).hasRuin() && !firstTower && instaRespawn){
//            rc.disintegrate();
//        }
    }



    public boolean trySummon(UnitType type, MapLocation loc) throws GameActionException{
        if(rc.canBuildRobot(type, loc)){
            rc.buildRobot(type,loc);
            return true;
        }
        return false;
    }

    public boolean trySummon(UnitType type) throws GameActionException{
        for(MapLocation loc : rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), GameConstants.BUILD_ROBOT_RADIUS_SQUARED)){
            if(trySummon(type, loc)){
                return true;
            }
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


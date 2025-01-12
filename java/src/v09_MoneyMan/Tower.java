package v09_MoneyMan;

import battlecode.common.*;

public abstract class Tower extends Robot {
    boolean firstTower = false;
    int TOWER_BUILD_COST = 100;

    public Tower(RobotController robot) throws GameActionException {
        super(robot);
        if(rc.getRoundNum()<8 && isPaintTower(rc.getType())){
            firstTower = true;
        }
    }
    int startSpawnTurn = rc.getRoundNum()-1;
    int spawnTurn = rc.getRoundNum()-1;
    public void turn() throws GameActionException {
        RobotInfo[] allies = rc.senseNearbyRobots(-1, rc.getTeam());
        if(rc.getChips()>3000 && rc.canUpgradeTower(rc.getLocation()) && allies.length > 3){
            rc.upgradeTower(rc.getLocation());
        }

        MapInfo[] nearbyTiles = rc.senseNearbyMapInfos(-1);
        boolean enemyPaint = false;
        for(MapInfo tile : nearbyTiles) {
            if (isEnemyPaint(tile.getPaint())) {
                enemyPaint = true;
                break;
            }
        }
        if(enemyPaint){
            if(trySummon(UnitType.SPLASHER)){
                return;
            }
        }else {
            if (allies.length < 5 && (spawnTurn - startSpawnTurn < 10 || rc.getChips() > 1000)) {
                if (spawnTurn % 5 < 2) {
                    if (trySummon(UnitType.SOLDIER)) {
                        spawnTurn++;
                    }
                } else if (spawnTurn % 5 < 4) {
                    if (trySummon(UnitType.MOPPER)) {
                        spawnTurn++;
                    }
                } else {
                    if (trySummon(UnitType.SPLASHER)) {
                        spawnTurn++;
                    }
                }
            }
        }

        tryAttack(null);
        RobotInfo[] enemies = rc.senseNearbyRobots(-1,rc.getTeam().opponent());
        if(enemies.length > 0){
            tryAttack(getClosest(enemies).getLocation());
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


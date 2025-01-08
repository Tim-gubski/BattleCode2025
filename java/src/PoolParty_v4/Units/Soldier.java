package PoolParty_v4.Units;

import PoolParty_v4.Unit;
import battlecode.common.*;

// TODO: Dont go into tower range, attack towers maybe?, moppers attack enemy units

public class Soldier extends Unit {
    Direction exploreDir = randomDirection();

    public Soldier(RobotController robot) throws GameActionException {
        super(robot);

    }

    public void turn() throws GameActionException {
//        boolean refillPaint = rc.getPaint() < rc.getType().paintCapacity/4;
//        if(refillPaint){
//            RobotInfo paintTowers = rc.senseNearby
//            fuzzyMove(spawnLoc);
//        }

        if (shouldRefill()) {
            findNearestPaintTower();
            if (nearestPaintTower != null){
                rc.setIndicatorString("Exploring on paint!! Trying to refill at: " + nearestPaintTower);
            } else {
                rc.setIndicatorString("Can't find refill station");
            }
            if(!moveToNearestPaintTower()){
                tryExploreOnPaint();
            }
        } else {
            MapLocation[] nearbyRuins = senseNearbyCompletableTowerlessRuins();
            rc.setIndicatorString("Nearby ruins: " + nearbyRuins.length);
            if (nearbyRuins.length > 0) {
                MapLocation closestRuin = getClosest(nearbyRuins);
                // Check if ruin has already been marked
                if (rc.senseMapInfo(closestRuin.subtract(dirTo(closestRuin))).getMark() == PaintType.EMPTY) {
                    if (!tryMarkTower(randomTowerType(), closestRuin)) {
                        fuzzyMove(closestRuin);
                    }
                    // if ruin has been marked
                } else {
                    fuzzyMove(closestRuin);
                    // prioritize tile we're standing on
                    MapInfo ourTile = rc.senseMapInfo(rc.getLocation());
                    if (ourTile.getMark() != ourTile.getPaint() && ourTile.getMark() != PaintType.EMPTY) {
                        boolean useSecondaryColor = ourTile.getMark() == PaintType.ALLY_SECONDARY;
                        if (rc.canAttack(ourTile.getMapLocation())) {
                            rc.setIndicatorDot(ourTile.getMapLocation(), 255, 0, 0);
                            rc.attack(ourTile.getMapLocation(), useSecondaryColor);
                        }
                    }
                    // fill in correct paint
                    for (MapInfo patternTile : rc.senseNearbyMapInfos(closestRuin, 8)) {
                        if (patternTile.getMark() != patternTile.getPaint() && patternTile.getMark() != PaintType.EMPTY) {
                            boolean useSecondaryColor = patternTile.getMark() == PaintType.ALLY_SECONDARY;
                            if (rc.canAttack(patternTile.getMapLocation())) {
                                rc.attack(patternTile.getMapLocation(), useSecondaryColor);
                                rc.setIndicatorDot(patternTile.getMapLocation(), 255,0,0);
                            }
                        }
                    }
                    // Complete the ruin if we can
                    for (UnitType type : towerTypes) {
                        if (rc.canCompleteTowerPattern(type, closestRuin)) {
                            rc.completeTowerPattern(type, closestRuin);
                            rc.setTimelineMarker("Tower built", 0, 255, 0);
                            System.out.println("Built a tower at " + closestRuin + "!");
                        }
                    }
                }
                // no ruins nearby
            } else {
                // check if enemy tower in range, attack
                RobotInfo tower = inEnemyTowerRange(rc.senseNearbyRobots(-1, rc.getTeam().opponent()));
                if(tower != null) {
                    for (Direction dir : directions) {
                        MapLocation newLocation = rc.getLocation().add(dir);
                        int dist = newLocation.distanceSquaredTo(tower.getLocation());
                        if (dist > tower.type.actionRadiusSquared && dist <= rc.getType().actionRadiusSquared && rc.canMove(dir) && !isEnemyPaint(rc.senseMapInfo(newLocation).getPaint())) {
                            rc.move(dir);
                            break;
                        }
                    }
                    tryAttack(tower.getLocation());
                    // incase we're too close
                    if (rc.getLocation().distanceSquaredTo(tower.getLocation()) <= tower.type.actionRadiusSquared) {
                        fuzzyMove(dirTo(tower.getLocation()).opposite());
                    }
                // otherwise explore
                }else{
                    Direction enemyPaintDirection = getEnemyPaintDirection();
                    if(enemyPaintDirection != null){
                        if(isEnemyPaint(rc.senseMapInfo(rc.getLocation()).getPaint())){
                            fuzzyMove(enemyPaintDirection.opposite());
                        }
                        if(RIGHT){
                            fuzzyMove(enemyPaintDirection.rotateRight().rotateRight());
                        }else{
                            fuzzyMove(enemyPaintDirection.rotateLeft().rotateLeft());
                        }
                    }else{
                        bugNav(explorer.getExploreTarget());
                    }
                }
            }

            // Repaint finished ruins
            MapLocation[] allRuins = rc.senseNearbyRuins(-1);
            if(allRuins.length > 0) {
                MapLocation closestRuin = getClosest(allRuins);

                // repaint
                for (MapInfo patternTile : rc.senseNearbyMapInfos(closestRuin, 8)) {
                    if (patternTile.getMark() != patternTile.getPaint() && patternTile.getMark() != PaintType.EMPTY) {
                        boolean useSecondaryColor = patternTile.getMark() == PaintType.ALLY_SECONDARY;
                        if (rc.canAttack(patternTile.getMapLocation())) {
                            rc.attack(patternTile.getMapLocation(), useSecondaryColor);
                            rc.setIndicatorDot(patternTile.getMapLocation(), 255, 0, 0);
                        }
                    }
                }
            }

            if(!checkAndPaintTile(rc.getLocation())) {
                MapInfo[] attackableTilesInfo = rc.senseNearbyMapInfos(rc.getLocation(), UnitType.SOLDIER.actionRadiusSquared);
                //shuffleArray(attackableTiles);
                for (MapInfo info : attackableTilesInfo) {
                    if (checkAndPaintTile(info)) {
                        break;
                    }
                }
            }
            for (MapLocation loc : getResourcePatternCenterLocations()) {
                if (tryConfirmResourcePattern(loc)) {
                    //rc.setTimelineMarker("Resource pattern confirmed", 0, 255, 0);
                    rc.setIndicatorDot(loc, 0, 255, 0);
                    System.out.println("Resource pattern confirmed at " + loc + "!");
                }
            }
        }

        markNearbyMapData();

    }

//    public MapLocation[] attackableTiles(){
//        MapLocation[] attackable = new MapLocation[69];
//
//    }

}

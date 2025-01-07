package FirstBot_v2.Units;

import FirstBot_v2.Unit;
import battlecode.common.*;
import java.util.Arrays;
import java.util.Collections;

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

        MapLocation[] nearbyRuins = senseNearbyTowerlessRuins();
        if(nearbyRuins.length > 0){
            MapLocation closestRuin = getClosest(nearbyRuins);
            // Check if ruin has already been marked
            if(rc.senseMapInfo(closestRuin.subtract(dirTo(closestRuin))).getMark() == PaintType.EMPTY){
                if(!tryMarkTower(randomTowerType(), closestRuin)){
                    fuzzyMove(closestRuin);
                }
            // if ruin has been marked
            }else{
                fuzzyMove(closestRuin);
                // fill in correct paint
                for (MapInfo patternTile : rc.senseNearbyMapInfos(closestRuin, 8)){
                    if (patternTile.getMark() != patternTile.getPaint() && patternTile.getMark() != PaintType.EMPTY){
                        boolean useSecondaryColor = patternTile.getMark() == PaintType.ALLY_SECONDARY;
                        if (rc.canAttack(patternTile.getMapLocation()))
                            rc.attack(patternTile.getMapLocation(), useSecondaryColor);
                    }
                }
                // Complete the ruin if we can
                for(UnitType type : towerTypes){
                    if (rc.canCompleteTowerPattern(type, closestRuin)){
                        rc.completeTowerPattern(type, closestRuin);
                        rc.setTimelineMarker("Tower built", 0, 255, 0);
                        System.out.println("Built a tower at " + closestRuin + "!");
                    }
                }
            }
        // No ruins nearby, Explore
        }else{
            fuzzyMove(exploreDir);
            if(!rc.onTheMap(rc.getLocation().add(exploreDir).add(exploreDir))){
                exploreDir = randomDirection();
            }
        }

        checkAndPaintTile(rc.getLocation());
        MapLocation[] attackableTiles = rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), UnitType.SOLDIER.actionRadiusSquared);
        shuffleArray(attackableTiles);
        for(MapLocation loc : attackableTiles){
            checkAndPaintTile(loc);
        }
        for(MapLocation loc : getResourcePatternCenterLocations()){
            if(tryConfirmResourcePattern(loc)){
                rc.setTimelineMarker("Resource pattern confirmed", 0, 255, 0);
                rc.setIndicatorDot(loc, 0, 255, 0);
                System.out.println("Resource pattern confirmed at " + loc + "!");
            }
        }

    }
    public void checkAndPaintTile(MapLocation loc) throws GameActionException{
        MapInfo currentTile = rc.senseMapInfo(loc);
        boolean targetColor = getTileTargetColor(currentTile.getMapLocation());
        if ((!currentTile.getPaint().isAlly() || currentTile.getPaint() != PaintType.values()[targetColor ? 0 : 1]) && rc.canAttack(loc)){
            rc.attack(loc, targetColor);
        }
    }

}

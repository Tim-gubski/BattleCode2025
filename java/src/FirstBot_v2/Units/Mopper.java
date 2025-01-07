package FirstBot_v2.Units;

import FirstBot_v2.Unit;
import battlecode.common.*;

public class Mopper extends Unit {
    Direction exploreDir = randomDirection();

    public Mopper(RobotController robot) throws GameActionException {
        super(robot);
    }
    /**
     * Run a single turn for a Mopper.
     * This code is wrapped inside the infinite loop in run(), so it is called once per turn.
     */
    public void turn() throws Exception {
        MapInfo[] nearbyTiles = rc.senseNearbyMapInfos(-1);
        MapLocation closestEnemyPaint = null;
        int closestDist = 999999;
        for(MapInfo tile : nearbyTiles){
            if(tile.getPaint() == PaintType.ENEMY_PRIMARY || tile.getPaint() == PaintType.ENEMY_SECONDARY){
                int dist = rc.getLocation().distanceSquaredTo(tile.getMapLocation());
                if(dist < closestDist){
                    closestDist = dist;
                    closestEnemyPaint = tile.getMapLocation();
                }
            }
        }
        if (closestEnemyPaint != null) {
            if(rc.getLocation().isAdjacentTo(closestEnemyPaint)){
                if(rc.canAttack(closestEnemyPaint)){
                    rc.attack(closestEnemyPaint);
                }
            }else{
                fuzzyMove(rc.getLocation().directionTo(closestEnemyPaint));
            }
        } else {
            fuzzyMove(exploreDir);
            if(!rc.onTheMap(rc.getLocation().add(exploreDir).add(exploreDir))){
                exploreDir = randomDirection();
            }
        }


    }
}

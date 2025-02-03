package v57_Optimaler.Helpers;

import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

/**
 * Class used to select random locations to explore.
 */
public class Explore {

    MapData mapData;

    RobotController rc;
    int myVisionRange;

    int targetRound = -100;
    MapLocation exploreLoc = null;

    MapLocation[] checkLocs = new MapLocation[5];
    //boolean checker = false;

    int w;
    int h;

    public Explore(RobotController rc, int width, int height, MapData mapData){
        this.rc = rc;
        this.w = width;
        this.h = height;
        this.mapData = mapData;
        myVisionRange = GameConstants.VISION_RADIUS_SQUARED;
        generateLocs();
    }

    void generateLocs(){
        checkLocs[0] = new MapLocation(w/2,h/2);
        checkLocs[1] = new MapLocation(0,0);
        checkLocs[2] = new MapLocation(w-1,0);
        checkLocs[3] = new MapLocation(0,h-1);
        checkLocs[4] = new MapLocation(w-1,h-1);
        exploreLoc = checkLocs[rc.getRoundNum()%5];
        targetRound = rc.getRoundNum();
//        targetRound = 0;
    }

    /*void setChecker(int init){
        exploreLoc = checkLocs[init%checkLocs.length];
        checker = true;
    }*/

    void getEmergencyTarget(int tries) {
        //MapLocation myLoc = rc.getLocation();
        int maxX = rc.getMapWidth();
        int maxY = rc.getMapHeight();
        while (tries-- > 0){
            //if (exploreLoc != null) return;
            MapLocation newLoc = new MapLocation((int)(Math.random()*maxX), (int)(Math.random()*maxY));
            //if (checkDanger && Robot.comm.isEnemyTerritoryRadial(newLoc)) continue;
            if (mapData.getMapInfo(newLoc) != null) continue;
            if (exploreLoc != null && rc.getLocation().distanceSquaredTo(exploreLoc) < rc.getLocation().distanceSquaredTo(newLoc)) continue;
            /*if (myLoc.distanceSquaredTo(newLoc) > myVisionRange){
                exploreLoc = newLoc;
            }*/
            exploreLoc = newLoc;
            targetRound = rc.getRoundNum();
        }
        if (exploreLoc == null) {
            exploreLoc = new MapLocation((int)(Math.random()*maxX), (int)(Math.random()*maxY));
        }
    }
    void getCheckerTarget(int tries){
        //MapLocation myLoc = rc.getLocation();
        while (tries-- > 0){
            int checkerIndex = (int)(Math.random()* checkLocs.length);
            MapLocation newLoc = checkLocs[checkerIndex];
            if (mapData.getMapInfo(newLoc) != null) continue;
            if (exploreLoc != null && rc.getLocation().distanceSquaredTo(exploreLoc) < rc.getLocation().distanceSquaredTo(newLoc)) continue;
            exploreLoc = newLoc;
            targetRound = rc.getRoundNum();
        }
        if (exploreLoc == null) getEmergencyTarget(tries);
    }

    public MapLocation getExploreTarget() {
        if (rc.getRoundNum() - targetRound > 40 || (exploreLoc != null && mapData.getMapInfo(exploreLoc) != null)) exploreLoc = null;
        if (exploreLoc == null){
            if (rc.getID()%2 >= 0) getCheckerTarget(15); // big change maybe
            else getEmergencyTarget(15);
        }
        return exploreLoc;
    }

}
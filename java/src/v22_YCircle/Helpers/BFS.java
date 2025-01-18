package v22_YCircle.Helpers;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

/**
 * mildly bytecode-optimized bfs from our spawn zones, treating unexplored tiles as walls. It is used only when bringing an enemy flag.
 * It is run using the remaining bytecoede every turn. Whenever an instance finishes, it starts a new one (since there may be more explored tiles)
 */

public class BFS {
    static int[][] dists;

    static int[][] distsAux;

    static MapLocation[] queue;
    static int queueSize;
    static int queueIndex;

    static int[] mData;

    static RobotController rc;

    static boolean finished = true;

    static void initialize(Robot robot){
        queue = new MapLocation[Robot.H* Robot.W];
        rc = Robot.rc;
        mData = Robot.explore.mapData.mapData;
    }

    static void runNewBFS(){
        if (Clock.getBytecodesLeft() < 7000) return;
        distsAux = new int[Robot.W][Robot.H];
        finished = false;
        queueSize = 0;
        queueIndex = 0;
        MapLocation[] locs = Robot.rc.getAllySpawnLocations();
        for (MapLocation m : locs){
            queue[queueSize++] = m;
            distsAux[m.x][m.y] = 1;
        }
    }

    static void runBFS(){
        if (finished) runNewBFS();
        RobotController rc = Robot.rc;
        while (queueIndex < queueSize){
            if (Clock.getBytecodesLeft() < 500) return;
            MapLocation m = queue[queueIndex];
            int d = distsAux[m.x][m.y];
            MapLocation newLoc = m.add(Direction.NORTHWEST);
            if (rc.onTheMap(newLoc) && (mData[newLoc.x*64 + newLoc.y] & 5) == 1){
                if (distsAux[newLoc.x][newLoc.y] == 0){
                    distsAux[newLoc.x][newLoc.y] = d+1;
                    queue[queueSize++] = newLoc;
                }
            }
            newLoc = m.add(Direction.SOUTHWEST);
            if (rc.onTheMap(newLoc) && (mData[newLoc.x*64 + newLoc.y] & 5) == 1){
                if (distsAux[newLoc.x][newLoc.y] == 0){
                    distsAux[newLoc.x][newLoc.y] = d+1;
                    queue[queueSize++] = newLoc;
                }
            }
            newLoc = m.add(Direction.SOUTHEAST);
            if (rc.onTheMap(newLoc) && (mData[newLoc.x*64 + newLoc.y] & 5) == 1){
                if (distsAux[newLoc.x][newLoc.y] == 0){
                    distsAux[newLoc.x][newLoc.y] = d+1;
                    queue[queueSize++] = newLoc;
                }
            }
            newLoc = m.add(Direction.NORTHEAST);
            if (rc.onTheMap(newLoc) && (mData[newLoc.x*64 + newLoc.y] & 5) == 1){
                if (distsAux[newLoc.x][newLoc.y] == 0){
                    distsAux[newLoc.x][newLoc.y] = d+1;
                    queue[queueSize++] = newLoc;
                }
            }
            newLoc = m.add(Direction.NORTH);
            if (rc.onTheMap(newLoc) && (mData[newLoc.x*64 + newLoc.y] & 5) == 1){
                if (distsAux[newLoc.x][newLoc.y] == 0){
                    distsAux[newLoc.x][newLoc.y] = d+1;
                    queue[queueSize++] = newLoc;
                }
            }
            newLoc = m.add(Direction.SOUTH);
            if (rc.onTheMap(newLoc) && (mData[newLoc.x*64 + newLoc.y] & 5) == 1){
                if (distsAux[newLoc.x][newLoc.y] == 0){
                    distsAux[newLoc.x][newLoc.y] = d+1;
                    queue[queueSize++] = newLoc;
                }
            }
            newLoc = m.add(Direction.EAST);
            if (rc.onTheMap(newLoc) && (mData[newLoc.x*64 + newLoc.y] & 5) == 1){
                if (distsAux[newLoc.x][newLoc.y] == 0){
                    distsAux[newLoc.x][newLoc.y] = d+1;
                    queue[queueSize++] = newLoc;
                }
            }
            newLoc = m.add(Direction.WEST);
            if (rc.onTheMap(newLoc) && (mData[newLoc.x*64 + newLoc.y] & 5) == 1){
                if (distsAux[newLoc.x][newLoc.y] == 0){
                    distsAux[newLoc.x][newLoc.y] = d+1;
                    queue[queueSize++] = newLoc;
                }
            }
        }
        finished = true;
        dists = distsAux;
    }
}
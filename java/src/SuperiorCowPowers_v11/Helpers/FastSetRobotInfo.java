package SuperiorCowPowers_v11.Helpers;


import battlecode.common.*;

public class FastSetRobotInfo {
    RobotInfo[] arr;
    int size;
    int capacity;

    public FastSetRobotInfo(int capacity) {
        this.capacity = capacity;
        arr = new RobotInfo[capacity];
        size = 0;
    }

    public void add(RobotInfo ri) {
        if(size >= capacity){
            System.out.println("Set too small! " + size + " " + capacity);
            return;
        }
        if(!contains(ri)) {
            arr[size++] = ri;
            retChanged = true;
        }
    }

    public void remove(RobotInfo ri) {
        for (int i = 0; i < size; i++) {
            if (arr[i].ID == ri.ID) {
                arr[i] = arr[size-1];
                size--;
                retChanged = true;
                return;
            }
        }
    }

    public boolean contains(RobotInfo ri) {
        for (int i = 0; i < size; i++) {
            if (arr[i].ID == ri.ID) {
                return true;
            }
        }
        return false;
    }

    public void clear() {
        size = 0;
    }

    public int size() {
        return size;
    }

    private RobotInfo[] retArr = null;
    private boolean retChanged = false;
    public RobotInfo[] getArray() {
        if(retChanged) {
            retArr = new RobotInfo[size];
            for (int i = 0; i < size; i++) {
                retArr[i] = arr[i];
            }
            retChanged = false;
        }
        return retArr;
    }

}

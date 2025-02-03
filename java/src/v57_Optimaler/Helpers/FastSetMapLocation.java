package v57_Optimaler.Helpers;


import battlecode.common.MapLocation;

public class FastSetMapLocation {
    MapLocation[] arr;
    int size;
    int capacity;

    public FastSetMapLocation(int capacity) {
        this.capacity = capacity;
        arr = new MapLocation[capacity];
        size = 0;
    }

    public void add(MapLocation loc) {
        if(size >= capacity){
            System.out.println("Set too small! " + size + " " + capacity);
            return;
        }
        if(!contains(loc)) {
            arr[size++] = loc;
            retChanged = true;
        }
    }

    public void remove(MapLocation loc) {
        for (int i = 0; i < size; i++) {
            if (arr[i].x == loc.x && arr[i].y == loc.y) {
                arr[i] = arr[size-1];
                size--;
                retChanged = true;
                return;
            }
        }
    }

    public boolean contains(MapLocation loc) {
        for (int i = 0; i < size; i++) {
            if (arr[i].x == loc.x && arr[i].y == loc.y) {
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

    private MapLocation[] retArr = null;
    private boolean retChanged = false;
    public MapLocation[] getArray() {
        if(retChanged) {
            retArr = new MapLocation[size];
            for (int i = 0; i < size; i++) {
                retArr[i] = arr[i];
            }
            retChanged = false;
        }
        return retArr;
    }

}

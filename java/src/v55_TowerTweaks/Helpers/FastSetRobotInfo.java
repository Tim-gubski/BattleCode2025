package v55_TowerTweaks.Helpers;


import battlecode.common.*;

public class FastSetRobotInfo {
    RobotInfo[] arr;
    int size;
    int capacity;
    public int paintTowers;
    public int moneyTowers;
    public int defenseTowers;

    public FastSetRobotInfo(int capacity) {
        this.capacity = capacity;
        arr = new RobotInfo[capacity];
        size = 0;
    }

    public boolean add(RobotInfo ri) {
        if(size >= capacity){
            System.out.println("Set too small! " + size + " " + capacity);
            return false;
        }
        if(!contains(ri)) {
            arr[size++] = ri;
            if(ri.type.getBaseType() == UnitType.LEVEL_ONE_PAINT_TOWER){
                paintTowers++;
            }else if(ri.type.getBaseType() == UnitType.LEVEL_ONE_MONEY_TOWER){
                moneyTowers++;
            }else if(ri.type.getBaseType() == UnitType.LEVEL_ONE_DEFENSE_TOWER){
                defenseTowers++;
            }
            retChanged = true;
            return true;
        }
        return false;
    }

    public void remove(RobotInfo ri) {
        for (int i = 0; i < size; i++) {
            if (arr[i].location.equals(ri.location) && arr[i].type.getBaseType().equals(ri.type.getBaseType())) {
                arr[i] = arr[size-1];

                if(ri.type.getBaseType() == UnitType.LEVEL_ONE_PAINT_TOWER){
                    paintTowers--;
                }else if(ri.type.getBaseType() == UnitType.LEVEL_ONE_MONEY_TOWER){
                    moneyTowers--;
                }else if(ri.type.getBaseType() == UnitType.LEVEL_ONE_DEFENSE_TOWER){
                    defenseTowers--;
                }

                size--;
                retChanged = true;
                return;
            }
        }
    }

    public boolean contains(RobotInfo ri) {
        for (int i = 0; i < size; i++) {
            if (arr[i].location.equals(ri.location) && arr[i].type.getBaseType().equals(ri.type.getBaseType())) {
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

    private RobotInfo[] retArr = new RobotInfo[0];
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

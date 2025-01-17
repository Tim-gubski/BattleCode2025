package v14_AdultDuck.Util;

import battlecode.common.*;
import v14_AdultDuck.Robot;

public class Comms {
    public enum Codes {
        FRONTLINE("100"),
        SYMMETRY("101"),
        TOWER_LOC("110");
//        TOWERS_HASHES("111")

        final String prefix;

        Codes(String prefix) {
            this.prefix = prefix;
        }

        public String getPrefix() {
            return this.prefix;
        }

        public static Codes fromValue(String prefix) {
            for (Codes code : Codes.values()) {
                if (prefix.equals(code.getPrefix())) {
                    return code;
                }
            }
            throw new RuntimeException("Invalid/unknown code '" + prefix + "'");
        }
    }

    RobotController rc;

    Robot robot;

    public Comms(RobotController rc, Robot robot) {
        this.rc = rc;
        this.robot = robot;
    }

    public void parseMessages() throws GameActionException{
        int round = rc.getRoundNum();
        if (round > 0) {
            for (Message message : rc.readMessages(rc.getRoundNum() - 1)) {
                String bitstring = Integer.toBinaryString(message.getBytes());
                switch (Codes.fromValue(bitstring.substring(0, 3))) {
                    case SYMMETRY -> {
                        String symmetry = bitstring.substring(3,5);
                        switch (symmetry) {
                            case "00" -> robot.symmetry = Symmetry.HORIZONTAL;
                            case "01" -> robot.symmetry = Symmetry.VERTICAL;
                            case "10" -> robot.symmetry = Symmetry.ROTATIONAL;
                            default -> throw new RuntimeException("Invalid symmetry code '" + symmetry + "' for message '" + bitstring + "'");
                        }
                    }
                    case FRONTLINE -> {
                        int x = Integer.parseUnsignedInt(bitstring.substring(3,9), 2);
                        int y = Integer.parseUnsignedInt(bitstring.substring(9, 15), 2);
                        MapLocation newLoc = new MapLocation(x, y);
//                        if ((rc.getType().isTowerType() || robot.returnLoc == null) && rc.getLocation().isWithinDistanceSquared(newLoc, 128)) {
//                            robot.returnLoc = newLoc;
//                        }
                    }
                    case TOWER_LOC -> {
                        int x = Integer.parseUnsignedInt(bitstring.substring(3,9), 2);
                        int y = Integer.parseUnsignedInt(bitstring.substring(9, 15), 2);
                        int towerTypeInt = Integer.parseUnsignedInt(bitstring.substring(15, 17), 2);
                        RobotInfo tower = createRobotInfo(towerTypeInt, x, y);
                        robot.mapData.markFriendlyTower(tower);
                    }
                }
            }
        }
    }

    private RobotInfo createRobotInfo(int towerTypeInt, int x, int y) {
        UnitType towerType;
        if(towerTypeInt == 0){
            towerType = UnitType.LEVEL_ONE_PAINT_TOWER;
        }else if(towerTypeInt == 1){
            towerType = UnitType.LEVEL_ONE_MONEY_TOWER;
        }else{
            towerType = UnitType.LEVEL_ONE_DEFENSE_TOWER;
        }
        return new RobotInfo(0, rc.getTeam(), towerType, 2000,  new MapLocation(x, y), 500);
    }

    public int constructMessage(Codes code, MapLocation loc){
        return constructMessage(code, loc, null);
    }

    public int constructMessage(Codes code, MapLocation loc, UnitType towerType) {
        int message;
        switch (code) {
            case FRONTLINE -> {
                if(loc == null) {
                    loc = rc.getLocation();
                }
                String xBinary = Integer.toBinaryString(loc.x);
                String yBinary = Integer.toBinaryString(loc.y);

                String xPaddedBinary = "0".repeat(6 - xBinary.length()) + xBinary;
                String yPaddedBinary = "0".repeat(6 - yBinary.length()) + yBinary;

                message = combineMessage(Codes.FRONTLINE.getPrefix(), xPaddedBinary, yPaddedBinary);
            }
            case SYMMETRY -> {
                message = combineMessage(Codes.SYMMETRY.getPrefix(), robot.symmetry);
            }
            case TOWER_LOC -> {
                String xBinary = Integer.toBinaryString(loc.x);
                String yBinary = Integer.toBinaryString(loc.y);

                String xPaddedBinary = "0".repeat(6 - xBinary.length()) + xBinary;
                String yPaddedBinary = "0".repeat(6 - yBinary.length()) + yBinary;

                int towerTypeInt;
                if(towerType.getBaseType() == UnitType.LEVEL_ONE_PAINT_TOWER){
                    towerTypeInt = 0;
                }else if(towerType.getBaseType() == UnitType.LEVEL_ONE_MONEY_TOWER){
                    towerTypeInt = 1;
                }else{
                    towerTypeInt = 2;
                }

                String towerTypeBinary = Integer.toBinaryString(towerTypeInt);
                String towerTypePaddedBinary = "0".repeat(2 - towerTypeBinary.length()) + towerTypeBinary;

                message = combineMessage(Codes.TOWER_LOC.getPrefix(), xPaddedBinary, yPaddedBinary, towerTypePaddedBinary);
            }

            default -> throw new RuntimeException("Message failed to construct because code '" + code + "' is not a valid option.");
        }

        return message;
    }

    private int combineMessage(Object... components) {
        StringBuilder builder = new StringBuilder();

        for (Object component : components) {
            builder.append(component);
        }

        builder.append("0".repeat(31 - builder.toString().length()));

        return Integer.parseUnsignedInt(builder.toString(), 2);
    }
}
package v57_Optimaler.Util;

import battlecode.common.*;
import v57_Optimaler.Robot;
import v57_Optimaler.Helpers.Fast.FastIntSet;

public class Comms {
    // 3-bit prefixes (binary -> decimal):
    // FRONTLINE = 100 -> 4
    // SYMMETRY  = 101 -> 5
    // TOWER_LOC = 110 -> 6
    // MOPPER_LOC= 111 -> 7
    public enum Codes {
        FRONTLINE(4),
        SYMMETRY(5),
        TOWER_LOC(6),
        MOPPER_LOC(7);

        final int prefix;
        Codes(int prefix) {
            this.prefix = prefix;
        }
        public int getPrefix() {
            return prefix;
        }
        public static Codes fromValue(int prefix) {
            return switch (prefix) {
                case 4 -> FRONTLINE;
                case 5 -> SYMMETRY;
                case 6 -> TOWER_LOC;
                case 7 -> MOPPER_LOC;
                default -> throw new RuntimeException("Invalid code " + prefix);
            };
        }
    }

    RobotController rc;
    Robot robot;
    FastIntSet seenMessages = new FastIntSet();

    public Comms(RobotController rc, Robot robot) {
        this.rc = rc;
        this.robot = robot;
    }

    public void parseMessages() throws GameActionException {
        int round = rc.getRoundNum();
        if (round > 0) {
            int turnsBack = round - robot.spawnTurn > 2 ? 1 : 2;
            for (Message message : rc.readMessages(round - turnsBack)) {
                int raw = message.getBytes();
//                robot.debugString.append("Message Received ");
                int prefix = (raw >>> 28) & 0x7; // top 3 bits
                switch (Codes.fromValue(prefix)) {
                    case SYMMETRY -> {
                        int symBits = (raw >>> 26) & 0x3; // next 2 bits
                        switch (symBits) {
                            case 0 -> robot.symmetry = Symmetry.HORIZONTAL;
                            case 1 -> robot.symmetry = Symmetry.VERTICAL;
                            case 2 -> robot.symmetry = Symmetry.ROTATIONAL;
                            default -> throw new RuntimeException("Invalid symmetry code");
                        }
                    }
                    case FRONTLINE -> {
                        int x = (raw >>> 22) & 0x3F; // 6 bits
                        int y = (raw >>> 16) & 0x3F; // 6 bits
                        MapLocation newLoc = new MapLocation(x, y);
                        // usage omitted as in original
                    }
                    case TOWER_LOC -> {
                        int x = (raw >>> 22) & 0x3F;
                        int y = (raw >>> 16) & 0x3F;
                        int towerTypeInt = (raw >>> 14) & 0x3; // 2 bits
                        RobotInfo tower = createRobotInfo(towerTypeInt, x, y);
                        robot.mapData.markFriendlyTower(tower);
                    }
                    case MOPPER_LOC -> {
                        int x = (raw >>> 22) & 0x3F;
                        int y = (raw >>> 16) & 0x3F;
                        robot.needMoppers = new MapLocation(x, y);
                    }
                }
            }
        }
    }

    private RobotInfo createRobotInfo(int towerTypeInt, int x, int y) {
        UnitType towerType = switch (towerTypeInt) {
            case 0 -> UnitType.LEVEL_ONE_PAINT_TOWER;
            case 1 -> UnitType.LEVEL_ONE_MONEY_TOWER;
            default -> UnitType.LEVEL_ONE_DEFENSE_TOWER;
        };
        return new RobotInfo(
                0,
                rc.getTeam(),
                towerType,
                2000,
                new MapLocation(x, y),
                500
        );
    }

    public int constructMessage(Codes code, MapLocation loc) {
        return constructMessage(code, loc, null);
    }

    public int constructMessage(Codes code, MapLocation loc, UnitType towerType) {
        if (loc == null) loc = rc.getLocation();
        int prefix = code.getPrefix();
        int x = loc.x & 0x3F; // 6 bits
        int y = loc.y & 0x3F; // 6 bits
        int message = 0;

        switch (code) {
            case FRONTLINE -> {
                // prefix: bits 30..28, x: bits 27..22, y: bits 21..16
                message |= (prefix & 0x7) << 28;
                message |= (x & 0x3F) << 22;
                message |= (y & 0x3F) << 16;
            }
            case SYMMETRY -> {
                // prefix: bits 30..28, sym: bits 27..26
                int symVal = switch (robot.symmetry) {
                    case HORIZONTAL -> 0;
                    case VERTICAL -> 1;
                    case ROTATIONAL -> 2;
                };
                message |= (prefix & 0x7) << 28;
                message |= (symVal & 0x3) << 26;
            }
            case TOWER_LOC -> {
                // prefix: bits 30..28, x: bits 27..22, y: bits 21..16, tower: bits 15..14
                int tType = 2;
                if (towerType != null) {
                    if (towerType.getBaseType() == UnitType.LEVEL_ONE_PAINT_TOWER) tType = 0;
                    else if (towerType.getBaseType() == UnitType.LEVEL_ONE_MONEY_TOWER) tType = 1;
                }
                message |= (prefix & 0x7) << 28;
                message |= (x & 0x3F) << 22;
                message |= (y & 0x3F) << 16;
                message |= (tType & 0x3) << 14;
            }
            case MOPPER_LOC -> {
                message |= (prefix & 0x7) << 28;
                message |= (x & 0x3F) << 22;
                message |= (y & 0x3F) << 16;
            }
            default -> throw new RuntimeException("Invalid code");
        }
        return message;
    }
}

package SuperiorCowPowers_v11.Util;

import SuperiorCowPowers_v11.Robot;
import battlecode.common.MapLocation;
import battlecode.common.Message;
import battlecode.common.RobotController;

public class Comms {
    public enum Codes {
        FRONTLINE("100"),
        SYMMETRY("101");
//        TOWER_LOC("110"),
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

    public void parseMessages() {
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

                        robot.debugString.append(String.format("|symmetry=%s", robot.symmetry));
                    }
                    case FRONTLINE -> {
                        if (rc.getType().isTowerType() || robot.returnLoc == null) {
                            int x = Integer.parseInt(bitstring.substring(3,9), 2);
                            int y = Integer.parseInt(bitstring.substring(9, 15), 2);
                            robot.returnLoc = new MapLocation(x, y);
                        }
                        robot.debugString.append(String.format("|frontline=%s", robot.returnLoc));
                    }
                }
            }
        }
    }

    public int constructMessage(Codes code) {
        int message;
        switch (code) {
            case FRONTLINE -> {
                MapLocation loc = rc.getLocation();
                String xBinary = Integer.toBinaryString(loc.x);
                String yBinary = Integer.toBinaryString(loc.y);

                String xPaddedBinary = "0".repeat(6 - xBinary.length());
                String yPaddedBinary = "0".repeat(6 - yBinary.length());

                message = combineMessage(Codes.FRONTLINE.getPrefix(), xPaddedBinary, yPaddedBinary);
            }
            case SYMMETRY -> {
                message = combineMessage(Codes.SYMMETRY.getPrefix(), robot.symmetry);
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
        robot.debugString.append(String.format("|constructed message %s|", builder));
        return Integer.parseUnsignedInt(builder.toString(), 2);
    }
}
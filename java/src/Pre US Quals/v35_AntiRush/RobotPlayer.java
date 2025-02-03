package v35_AntiRush;

import v35_AntiRush.Towers.DefenseTower;
import v35_AntiRush.Towers.MoneyTower;
import v35_AntiRush.Towers.PaintTower;
import v35_AntiRush.Units.Mopper;
import v35_AntiRush.Units.Soldier;
import v35_AntiRush.Units.Splasher;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;

/**
 * RobotPlayer is the class that describes your main robot strategy.
 * The run() method inside this class is like your main function: this is what we'll call once your robot
 * is created!
 */
public class RobotPlayer {
    static RobotController rc;

    /**
     * We will use this variable to count the number of turns this robot has been alive.
     * You can use static variables like this to save any information you want. Keep in mind that even though
     * these variables are static, in Battlecode they aren't actually shared between your robots.
     */
    static int turnCount = 0;

    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * It is like the main function for your robot. If this method returns, the robot dies!
     *
     * @param rc  The RobotController object. You use it to perform actions from this robot, and to get
     *            information on its current status. Essentially your portal to interacting with the world.
     **/
    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {
        RobotPlayer.rc = rc;

        Robot r = switch (rc.getType()) {
            case MOPPER -> new Mopper(rc);
            case SOLDIER -> new Soldier(rc);
            case SPLASHER -> new Splasher(rc);
            case LEVEL_ONE_PAINT_TOWER, LEVEL_TWO_PAINT_TOWER, LEVEL_THREE_PAINT_TOWER -> new PaintTower(rc);
            case LEVEL_ONE_MONEY_TOWER, LEVEL_TWO_MONEY_TOWER, LEVEL_THREE_MONEY_TOWER -> new MoneyTower(rc);
            case LEVEL_ONE_DEFENSE_TOWER, LEVEL_TWO_DEFENSE_TOWER, LEVEL_THREE_DEFENSE_TOWER -> new DefenseTower(rc);
        };
        r.run();

        // Your code should never reach here (unless it's intentional)! Self-destruction imminent...
    }
}

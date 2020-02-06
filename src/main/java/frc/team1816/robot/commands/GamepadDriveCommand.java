package frc.team1816.robot.commands;

import com.team254.lib.util.CheesyDriveHelper;
import edu.wpi.first.wpilibj2.command.CommandBase;
import frc.team1816.robot.Components;
import frc.team1816.robot.Controls;
import frc.team1816.robot.subsystems.Drivetrain;

import static frc.team1816.robot.Robot.factory;

public class GamepadDriveCommand extends CommandBase {
    public static final String NAME = "gamepaddrivecommand";

    private Drivetrain drivetrain;
    private CheesyDriveHelper cheesyDriveHelper = new CheesyDriveHelper();

    private double prevPowLeft = 0;
    private double prevPowRight = 0;

    public static final double SET_SPEED_DIFF_MAX = 0.08;

    public GamepadDriveCommand() {
        super();
        this.drivetrain = Components.getInstance().drivetrain;
        addRequirements(drivetrain);
    }

    @Override
    public void initialize() {
    }

    @Override
    public void execute() {
        double leftPow = Controls.getInstance().getDriveThrottle();
        double rightPow = leftPow;
        double rotation = Controls.getInstance().getDriveTurn();

        if (factory.getConstant("cheezyDrive") == 0) { // Arcade Drive
            if (rotation == 0 || leftPow != 0) {
                leftPow = limitAcceleration(leftPow, prevPowLeft);
                rightPow = limitAcceleration(rightPow, prevPowRight);
            }

            prevPowLeft = leftPow;
            prevPowRight = rightPow;
        } else { // Cheesy Drive
            boolean quickTurn = Controls.getInstance().getQuickTurn();
            var signal = cheesyDriveHelper.cheesyDrive(leftPow, rotation, quickTurn, false);
            leftPow = signal.getLeft();
            rightPow = signal.getRight();
            // CheezyDrive takes care of rotation so set to 0 to keep or code from adjusting
            rotation = 0;
        }

        drivetrain.setDrivetrainPercent(leftPow, rightPow, rotation);
    }

    @Override
    public boolean isFinished() {
        return false;
    }

    @Override
    public void end(boolean isFinished) {
        drivetrain.setDrivetrainPercent(0, 0);
    }

    private double limitAcceleration(double setPow, double prevPow) {
        if (Math.abs(setPow - prevPow) > SET_SPEED_DIFF_MAX) {
            if (setPow > prevPow) {
                return prevPow + SET_SPEED_DIFF_MAX;
            } else if (setPow < prevPow) {
                return prevPow - SET_SPEED_DIFF_MAX;
            } else {
                return prevPow;
            }
        } else {
            return prevPow;
        }
    }
}

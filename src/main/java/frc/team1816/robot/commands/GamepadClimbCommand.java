package frc.team1816.robot.commands;

import edu.wpi.first.wpilibj.command.Command;
import frc.team1816.robot.Components;
import frc.team1816.robot.Controls;
import frc.team1816.robot.subsystems.Climber;

public class GamepadClimbCommand extends Command {
    private Climber climber;

    public GamepadClimbCommand() {
        super("gamepadclimbcommand");
        this.climber = Components.getInstance().climber;
        requires(climber);
    }

    @Override
    protected void initialize() {
    }

    @Override
    protected void execute() {
        double climberPow;

        if (Controls.getInstance().getDriverClimbUp()) { // FIXME: this logic causes climber motion in disabled
            climberPow = 1.0;
        } else if (Controls.getInstance().getDriverClimbDown()) {
            climberPow = -1.0;
        } else {
            climberPow = Controls.getInstance().getClimbThrottle();
        }

        climber.setClimberPower(climberPow);
    }

    @Override
    protected boolean isFinished() {
        return false;
    }

    @Override
    protected void end() {
        climber.setClimberPower(0);
    }

    @Override
    protected void interrupted() {
        end();
    }
}

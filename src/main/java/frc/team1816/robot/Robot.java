package frc.team1816.robot;

import badlog.lib.BadLog;
import badlog.lib.DataInferMode;

import com.edinarobotics.utils.checker.Checker;
import com.edinarobotics.utils.hardware.RobotFactory;

import edu.wpi.first.networktables.EntryListenerFlags;
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.command.Scheduler;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import frc.team1816.robot.commands.BlinkLedCommand;
import frc.team1816.robot.commands.GamepadClimbCommand;
import frc.team1816.robot.commands.GamepadDriveCommand;
import frc.team1816.robot.subsystems.*;
import frc.team1816.robot.subsystems.LedManager.RobotStatus;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Robot extends TimedRobot {

    public Birdbeak birdbeak;
    public Climber climber;
    public CargoCollector collector;
    public Drivetrain drivetrain;
    public LedManager leds;
    public CargoShooter shooter;

    private boolean autoInitialized;

    private NetworkTableInstance inst;
    private NetworkTable table;
    private NetworkTableEntry widthEntry;
    private NetworkTableEntry heightEntry;
    private NetworkTableEntry xCoordEntry;

    public static RobotState stateInstance = new RobotState();
    private BadLog logger;
    private double loopStart;

    public static class RobotState {
        public double width = 640;
        public double height = 480;
        public double xCoord = -1.0;

        public double getVisionXCoord() {
            return xCoord;
        }

        public double getVisionWidth() {
            return width;
        }

        public double getVisionHeight() {
            return height;
        }
    }

    public static final RobotFactory factory = new RobotFactory(
            System.getenv("ROBOT_NAME") != null ? System.getenv("ROBOT_NAME") : "zeta");
    private SendableChooser<Boolean> drivetrainReverseChooser;

    public Robot() {
        // set the loop timeout in seconds
        super(.02);
    }

    @Override
    public void robotInit() {
        System.out.print("Initializing robot! ");
        System.out.println(0f);
        System.out.println(System.getenv("ROBOT_NAME"));

        Components.getInstance();
        Controls.getInstance();

        birdbeak = Components.getInstance().birdbeak;
        climber = Components.getInstance().climber;
        collector = Components.getInstance().collector;
        drivetrain = Components.getInstance().drivetrain;
        leds = Components.getInstance().ledManager;
        shooter = Components.getInstance().shooter;

        drivetrainReverseChooser = new SendableChooser<>();
        drivetrainReverseChooser.addOption("Cargo Forward", false);
        drivetrainReverseChooser.setDefaultOption("Hatch Forward", true);
        SmartDashboard.putData("InitDriveDir", drivetrainReverseChooser);
        autoInitialized = false;

        if (leds != null) {
            leds.setDefaultCommand(new BlinkLedCommand(0.5));
        }

        inst = NetworkTableInstance.getDefault();
        table = inst.getTable("SmartDashboard");
        widthEntry = table.getEntry("width");
        heightEntry = table.getEntry("height");
        xCoordEntry = table.getEntry("center_x");

        stateInstance.width = widthEntry.getDouble(640.0);
        stateInstance.height = heightEntry.getDouble(480.0);
        stateInstance.xCoord = xCoordEntry.getDouble(-1.0);

        table.addEntryListener("center_x", (table, key, entry, value, flags) -> {stateInstance.xCoord = value.getDouble();}, 
                EntryListenerFlags.kNew | EntryListenerFlags.kUpdate);

        var logFile = new SimpleDateFormat("DDDHHmm").format(new Date());
        logger = BadLog.init("/home/lvuser/" + logFile + ".bag");
        BadLog.createTopic("Timings/RobotLoop", "ms", this::getLastLoop, "hide", "join:Timings");
        BadLog.createTopic("Target Center X", "px", stateInstance::getVisionXCoord, "hide", "join:Vision");
        BadLog.createTopicSubscriber("Lateral Err", "px", DataInferMode.DEFAULT, "hide", "join:Vision");
        BadLog.createTopic("Velocity/Left", "native", drivetrain::getLeftPower, "hide", "join:Velocities");
        BadLog.createTopic("Velocity/Right", "native", drivetrain::getRightPower, "hide", "join:Velocities");
        logger.finishInitialization();
    }

    private Double getLastLoop() {
        return (Timer.getFPGATimestamp() - loopStart) * 1000;
    }


    @Override
    public void disabledInit() {
        autoInitialized = false;
    }

    @Override
    public void autonomousInit() {
        if (climber != null) {
            climber.setDefaultCommand(new GamepadClimbCommand());
        }
        if (drivetrain != null) {
            drivetrain.setDefaultCommand(new GamepadDriveCommand());
            drivetrain.setReverseMode(drivetrainReverseChooser.getSelected());
            // drivetrain.setSlowMode(true);
        }
        autoInitialized = true;
    }

    @Override
    public void teleopInit() {
        if (climber != null) {
            climber.setDefaultCommand(new GamepadClimbCommand());
        }
        if (drivetrain != null) {
            drivetrain.setDefaultCommand(new GamepadDriveCommand());
            drivetrain.setSlowMode(false);
            if (!autoInitialized) {
                drivetrain.setReverseMode(drivetrainReverseChooser.getSelected());
            }
        }
    }

    @Override
    public void testInit() {
        Checker.runTests(factory::isImplemented);
    }

    @Override
    public void disabledPeriodic() {
        loopStart = Timer.getFPGATimestamp();
        if (shooter != null && leds != null) {
            // Check Shooter arm position
            if (shooter.getArmEncoderPosition() > CargoShooter.ARM_POSITION_MID + 100) {
                leds.blinkStatus(RobotStatus.ERROR);
            } else {
                leds.indicateStatus(RobotStatus.DISABLED);
            }
        }
        periodic();
    }

    @Override
    public void autonomousPeriodic() {
        loopStart = Timer.getFPGATimestamp();
        if (leds != null) {
            if (drivetrain.isReverseMode()) {
                leds.indicateStatus(RobotStatus.DRIVETRAIN_FLIPPED);
            } else {
                leds.indicateStatus(RobotStatus.ENABLED);
            }
        }
        periodic();
    }

    @Override
    public void teleopPeriodic() {
        loopStart = Timer.getFPGATimestamp();
        if (drivetrain != null && leds != null && drivetrain.getCurrentCommandName().equals(GamepadDriveCommand.NAME)) {
            if (DriverStation.getInstance().getMatchTime() <= 45 && DriverStation.getInstance().getMatchTime() > 0) {
                leds.blinkStatus(RobotStatus.ENDGAME);
            } else if (drivetrain.isReverseMode()) {
                leds.indicateStatus(RobotStatus.DRIVETRAIN_FLIPPED);
            } else {
                leds.indicateStatus(RobotStatus.ENABLED);
            }
        }
        periodic();
    }

    @Override
    public void testPeriodic() {
        if (leds != null) {
            leds.indicateStatus(RobotStatus.ENABLED);
        }
        periodic();
    }

    private void periodic() {
        Scheduler.getInstance().run();
        if (factory.getConstant("loggingEnabled") > 0) {
            logger.updateTopics();
            logger.log();
        }
    }
}

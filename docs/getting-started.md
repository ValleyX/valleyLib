# Getting Started with ValleyLib

ValleyLib is a command-based library for FTC. It helps you organize your code into **Subsystems** (hardware) and **Commands** (behavior).

## 1. Project Setup

Add the following to your `build.gradle` (Module: teamcode):

```gradle
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.USERNAME:ValleyLib:ftc:Tag'
}
```
*(Replace `USERNAME` with your GitHub username and `Tag` with a release tag like `1.0.0`)*

## 2. Define a Subsystem

Subsystems encapsulate hardware logic.

```java
public class IntakeSubsystem extends FtcSubsystem {
    private final DcMotor motor;

    public IntakeSubsystem(HardwareMap hardwareMap) {
        super(hardwareMap);
        motor = hardwareMap.get(DcMotor.class, "intake");
    }

    public void setPower(double power) {
        motor.setPower(power);
    }
}
```

## 3. Create a RobotContainer

The `RobotContainer` is where you define your subsystems and button bindings.

```java
public class Robot extends RobotContainer {
    public final IntakeSubsystem intake;
    public final CommandGamepad driver;

    public Robot(HardwareMap hwMap, Gamepad gp1) {
        intake = new IntakeSubsystem(hwMap);
        driver = new CommandGamepad(gp1);
        configureBindings();
    }

    @Override
    public void configureBindings() {
        driver.a().whileTrue(Commands.startEnd(() -> intake.setPower(1), () -> intake.setPower(0)));
    }

    @Override
    public Command getAutonomousCommand() {
        return Commands.waitSeconds(1).andThen(() -> intake.setPower(0.5)).withTimeout(2);
    }
}
```

## 4. Create an OpMode

Extend `CommandOpMode` to run your robot.

```java
@TeleOp
public class MainTeleOp extends CommandOpMode {
    private Robot robot;

    @Override
    public void initialize() {
        robot = new Robot(hardwareMap, gamepad1);
    }

    @Override
    public void run() {
        // Optional: Extra telemetry or manual logic
    }
}
```

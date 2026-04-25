package com.vcs.valleylib.ftc.samples.auto;

import com.pedropathing.geometry.BezierLine;
import com.pedropathing.geometry.Pose;
import com.pedropathing.paths.PathChain;
import com.vcs.valleylib.core.command.Command;
import com.vcs.valleylib.ftc.opmode.CommandOpMode;
import com.vcs.valleylib.ftc.pedro.PedroAutoDsl;
import com.vcs.valleylib.ftc.pedro.PedroSubsystem;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;

/**
 * A sample showing the migration of a traditional Pedro Pathing state machine
 * to ValleyLib's command-based system.
 */
@Autonomous(name = "ValleyLib: Pedro Migration", group = "Samples")
public class PedroMigrationSample extends CommandOpMode {

    private PedroSubsystem drive;
    private PathChain mainChain;

    @Override
    protected void initialize() {
        // --- STEP 1: Initialization ---
        // Traditional: follower = Constants.createFollower(hardwareMap);
        // ValleyLib: Encapsulated in PedroSubsystem.
        // drive = new YourDriveSubsystem(hardwareMap);

        // --- STEP 2: Starting Pose ---
        // Traditional: follower.setStartingPose(new Pose(...));
        // ValleyLib: Still set manually during initialization via the subsystem.
        drive.getFollower().setStartingPose(new Pose(72, 8, Math.toRadians(90)));

        // --- STEP 3: Path Building ---
        // Traditional: paths = new Paths(follower);
        // ValleyLib: Defined in initialize() or as class members.
        mainChain = drive.getFollower().pathBuilder()
                .addPath(new BezierLine(new Pose(71.271, 18.885), new Pose(56.162, 34.213)))
                .setLinearHeadingInterpolation(Math.toRadians(90), Math.toRadians(180))
                .addPath(new BezierLine(new Pose(56.162, 34.213), new Pose(15.348, 35.650)))
                .setTangentHeadingInterpolation()
                .addPath(new BezierLine(new Pose(15.348, 35.650), new Pose(62.199, 14.423)))
                .setConstantHeadingInterpolation(Math.toRadians(0))
                .addPath(new BezierLine(new Pose(62.199, 14.423), new Pose(55.000, 58.000)))
                .setTangentHeadingInterpolation()
                .addPath(new BezierLine(new Pose(55.000, 58.000), new Pose(38.455, 83.018)))
                .setTangentHeadingInterpolation()
                .addPath(new BezierLine(new Pose(38.455, 83.018), new Pose(62.000, 82.800)))
                .setTangentHeadingInterpolation()
                .build();

        // --- STEP 4: State Machine Logic ---
        // Traditional: pathState = autonomousPathUpdate(); (plus a switch statement)
        // ValleyLib: Replaced by a linear, readable Command chain.
        Command autoRoutine = PedroAutoDsl.auto(drive, auto -> auto
                .follow(mainChain)           // case 0: followPath
                .waitUntilDriveIdle()        // if(!follower.isBusy())
                .action(() -> telemetryBus.put("Status", "Path Complete"))
        );

        // Schedule it to run when the OpMode starts
        scheduler.schedule(autoRoutine);
    }

    @Override
    protected void run() {
        // --- STEP 5: Loop & Telemetry ---
        // Traditional: follower.update(); panelsTelemetry.update();
        // ValleyLib: Handled automatically by CommandOpMode and PedroSubsystem.
        // You only need to put the data you want to see.
        telemetryBus.put("X", drive.getFollower().getPose().getX());
        telemetryBus.put("Y", drive.getFollower().getPose().getY());
        telemetryBus.put("Heading", drive.getFollower().getPose().getHeading());
    }
}

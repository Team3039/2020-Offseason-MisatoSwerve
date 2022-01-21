package frc.robot.commands;

import edu.wpi.first.wpilibj2.command.CommandBase;
import frc.robot.subsystems.DrivetrainSubsystem;
import frc.robot.subsystems.VisionSubsystem;
import org.frcteam2910.common.control.Path;
import org.frcteam2910.common.control.SimplePathBuilder;
import org.frcteam2910.common.control.Trajectory;
import org.frcteam2910.common.math.RigidTransform2;
import org.frcteam2910.common.math.Rotation2;
import org.frcteam2910.common.math.Vector2;
import org.frcteam2910.common.util.Side;

import java.util.function.Supplier;

public class DriveToLoadingStationCommand extends CommandBase {
    private final DrivetrainSubsystem drivetrain;
    private final VisionSubsystem vision;
    private final Supplier<Side> loadingStationSide;

    private boolean shouldRegeneratePaths = true;

    public DriveToLoadingStationCommand(DrivetrainSubsystem drivetrain, VisionSubsystem vision,
                                        Supplier<Side> loadingStationSide) {
        this.drivetrain = drivetrain;
        this.vision = vision;
        this.loadingStationSide = loadingStationSide;

        addRequirements(drivetrain);
    }

    @Override
    public void initialize() {
        shouldRegeneratePaths = true;
    }

    @Override
    public void execute() {
        if (!vision.doesIntakeHaveTarget() || !shouldRegeneratePaths){
            return;
        }

        Vector2 predictedPosition = vision.getPredictedPostition();
        // We don't want to generate a path at is way too large due to bad vision data, so truncate it to a maximum distance
        if (predictedPosition.length > 240.0) {
            predictedPosition = predictedPosition.normal().scale(240.0);
        }

        if (predictedPosition.x < 40.0) {
            shouldRegeneratePaths = false;
        }

        Vector2 goal = new Vector2(20.0, 0.0);
        switch (loadingStationSide.get()) {
            case LEFT:
                goal = new Vector2(20.0, 22.0);
                break;
            case RIGHT:
                goal = new Vector2(20.0, -22.0);
                break;
        }

        Path path = new SimplePathBuilder(predictedPosition, Rotation2.ZERO)
                .lineTo(goal)
                .build();

        double startingVelocity = drivetrain.getVelocity().length;
        Trajectory.State lastState = drivetrain.getFollower().getLastState();
        if (lastState != null) {
            startingVelocity = lastState.getVelocity();
        }

        Trajectory trajectory = new Trajectory(path, DrivetrainSubsystem.TRAJECTORY_CONSTRAINTS, 12.0, startingVelocity, 0.0);
        drivetrain.resetPose(new RigidTransform2(predictedPosition, drivetrain.getPose().rotation));
        drivetrain.getFollower().follow(trajectory);
    }

    @Override
    public void end(boolean interrupted) {
        drivetrain.getFollower().cancel();
        drivetrain.drive(Vector2.ZERO, 0.0, false);
    }
}

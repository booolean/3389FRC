/* Copyright (c) 2017-2018 FIRST. All Rights Reserved.                        */
/* Open Source Software - may be modified and shared by FRC teams. The code   */
/* must be accompanied by the FIRST BSD license file in the root directory of */
/* the project.                                                               */
/*----------------------------------------------------------------------------*/

package org.usfirst.frc.team3389.robot.commands;

import org.usfirst.frc.team3389.robot.Robot;
import org.usfirst.frc.team3389.robot.subsystems.DriveTrain;
import org.usfirst.frc.team3389.robot.utils.Logger;
import edu.wpi.first.wpilibj.command.Command;

/**
 *
 * @author TEC Tigers
 * @see org.usfirst.frc.team3389.robot.subsystems.DriveTrain
 * 
 */

public class DriveDistance extends Command {

	double distance = 0;
	DriveTrain drive;

	/**
	 * Constructor gains control of the DriveTrain subsystem of the robot.
	 * 
	 * @see org.usfirst.frc.team3389.robot.subsystems.DriveTrain
	 */
	public DriveDistance(double d) {
		// Use requires() here to declare subsystem dependencies
		// requires(Robot.kExampleSubsystem);
		requires(Robot.driveTrain);
		drive = Robot.driveTrain;
		distance = d;
	}

	/**
	 * Grabs the joystick from the OI object of the robot before running the
	 * command.
	 * 
	 * @see org.usfirst.frc.team3389.robot.OIs
	 */
	@Override
	protected void initialize() {
		Robot.robotLogger.log(Logger.DEBUG, this, "enter");
		// do any pre-loop setup here
		drive.resetEncoders();
		drive.drivePosition(distance, distance);
		Robot.robotLogger.log(Logger.DEBUG, this, "exit");
	}

	/**
	 * As the command is run, updates the joystick values and controls the
	 * DriveTrain with them.
	 * 
	 * @see org.usfirst.frc.team3389.robot.subsystems.DriveTrain
	 */
	@Override
	protected void execute() {
		Robot.robotLogger.log(Logger.DEBUG, this, "enter");
		// since the PID drive operation is executing within the talon (firmware), nothing is needed in the execute() loop.
//		drive.drivePosition(distance, distance);
		
		/* TODO option for fine tuning left & right
		 * if there is a way to adjust motor power/speed velocity
		 * then possible test if left or right is lagging and temporarily increase  
		 */
		
		Robot.robotLogger.log(Logger.DEBUG, this, "exit");
	}

	/**
	 * Never allows Drive command to finish on its own terms.
	 */
	@Override
	protected boolean isFinished() {
		// consider the execution finished when we are within 0.5 inches
		if (Math.abs(drive.getPosition() - distance) < 0.5)
			return true;
		return false;
	}

	/**
	 * Stops drivetrain's motion if command is ended with isFinished
	 * 
	 * @see org.usfirst.frc.team3389.robot.commands.Drive#isFinished()
	 */
	@Override
	protected void end() {
		Robot.robotLogger.log(Logger.DEBUG, this, "enter");
		drive.stop();
		Robot.robotLogger.log(Logger.DEBUG, this, "exit");
	}

	/**
	 * Stops drivetrain's motion if another command is ran that needs it.
	 */
	@Override
	protected void interrupted() {
		Robot.robotLogger.log(Logger.DEBUG, this, "enter");
		end();
		Robot.robotLogger.log(Logger.DEBUG, this, "exit");
	}
}

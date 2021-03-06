/* Copyright (c) 2017-2018 FIRST. All Rights Reserved.                        */
/* Open Source Software - may be modified and shared by FRC teams. The code   */
/* must be accompanied by the FIRST BSD license file in the root directory of */
/* the project.                                                               */
/*----------------------------------------------------------------------------*/

package org.usfirst.frc.team3389.robot.commands;

import org.usfirst.frc.team3389.robot.Robot;

import edu.wpi.first.wpilibj.command.Command;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

/**
 * Moves lift to given height using PID loop
 */
public class LiftPIDAuto extends Command {

	// change these
	final double kP = .75, kI = 0.075, kD = 0;

	double targetHeight;
	double error, integral, derivative;
	long lastTime;

	public LiftPIDAuto(double height) {
		requires(Robot.lifter);
		targetHeight = height;
	}

	// Called just before this Command runs the first time
	@Override
	protected void initialize() {
		Robot.lifter.resetEnc();
		lastTime = System.currentTimeMillis();
	}

	// Called repeatedly when this Command is scheduled to run
	@Override
	protected void execute() {
		long currentTime = System.currentTimeMillis();
		double timeElapsed = ((double) (currentTime - lastTime)) / 1000;
		lastTime = currentTime;

		double current = Robot.lifter.getHeight();

		double lastError = error;
		error = targetHeight - current;
		SmartDashboard.putNumber("error", error);
		integral += error * timeElapsed;
		derivative = (error - lastError) / timeElapsed;

		double power = kP * error + kI * integral + kD * derivative;
		SmartDashboard.putNumber("test", power);
		Robot.lifter.driveLift(-power);
	}

	// Make this return true when this Command no longer needs to run execute()
	@Override
	protected boolean isFinished() {
		if (Math.abs(Robot.lifter.getHeight() - targetHeight) < .1) {
			return true;
		}
		return false;
	}

	// Called once after isFinished returns true
	@Override
	protected void end() {
		Robot.lifter.driveLift(-.12);
	}

	// Called when another command which requires one or more of the same
	// subsystems is scheduled to run
	@Override
	protected void interrupted() {
		end();
	}
}

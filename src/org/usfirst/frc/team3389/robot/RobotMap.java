/*----------------------------------------------------------------------------*/
/* Copyright (c) 2017-2018 FIRST. All Rights Reserved.                        */
/* Open Source Software - may be modified and shared by FRC teams. The code   */
/* must be accompanied by the FIRST BSD license file in the root directory of */
/* the project.                                                               */
/*----------------------------------------------------------------------------*/

package org.usfirst.frc.team3389.robot;

/**
 * The RobotMap is a mapping from the ports sensors and actuators are wired into
 * to a variable name. This provides flexibility changing wiring, makes checking
 * the wiring easier and significantly reduces the number of magic numbers
 * floating around.
 */
public class RobotMap {

	/**
	 * CAN IDs of all Talon SRXs used for drive train
	 */
	public final static int DRIVE_LEFTMASTER = 2, DRIVE_LEFTSLAVE = 3, DRIVE_RIGHTMASTER = 0, DRIVE_RIGHTSLAVE = 1;

	public final static int LIFT = 4;

	/**
	 * PWM IDs for intake motor controllers
	 */
	public final static int INTAKE_LEFT = 5, INTAKE_RIGHT = 6;

	public final static int UP_SWITCH_PIN = 6, DOWN_SWITCH_PIN = 7;

	public final static int lTimeoutMs = 10, rTimeoutMs = 10, lSlotIdx = 0, rSlotIdx = 0, lPIDLoopIdx = 0,
			rPIDLoopIdx = 0, cruiseVelocity = 750, accel = 50000;

	/*
	 * Method of computing encoder drive ration: 1) add raw encoder 'ticks' value to
	 * dashboard 2) power robot 3) note initial encoder value 4) manually move (push
	 * / roll) robot a fixed distance 5) note final encoder value ration = (final -
	 * initial) / distance note: the value recorded here is in ticks/inch
	 */
	public static double convRatio = (17148.0) / (108.0);

	public final static int CURRENT_LIMIT = 38;

	public final static double LIFT_TIME = 15.5 / 6; // 15.5 / 8

	public static final int CLIMBER = 0;

	/**
	 *  Max height of lift
	 */
	public static final double MAX_HEIGHT = 42.8;

	public static final double DEADZONE = 0.1;
	public static final int LEFT_DRIVE_STICK = 1 /*5*/, RIGHT_DRIVE_STICK = 4 /*1*/;

	// For example to map the left and right motors, you could define the
	// following variables to use with your DriveTrain subsystem.
	// public static int leftMotor = 1;
	// public static int rightMotor = 2;

	// If you are using multiple modules, make sure to define both the port
	// number and the module. For example you with a rangefinder:
	// public static int rangefinderPort = 1;
	// public static int rangefinderModule = 1;
}

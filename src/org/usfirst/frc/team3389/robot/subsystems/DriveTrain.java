package org.usfirst.frc.team3389.robot.subsystems;

import org.usfirst.frc.team3389.robot.Robot;
import org.usfirst.frc.team3389.robot.RobotMap;
import org.usfirst.frc.team3389.robot.commands.Drive;
import org.usfirst.frc.team3389.robot.subsystems.ioDevices.MPU9250;
import org.usfirst.frc.team3389.robot.utils.Logger;

import com.ctre.phoenix.motorcontrol.ControlMode;
import com.ctre.phoenix.motorcontrol.Faults;
import com.ctre.phoenix.motorcontrol.FeedbackDevice;
import com.ctre.phoenix.motorcontrol.StatusFrameEnhanced;
import com.ctre.phoenix.motorcontrol.StickyFaults;
import com.ctre.phoenix.motorcontrol.can.TalonSRX;

import edu.wpi.first.wpilibj.Encoder;
import edu.wpi.first.wpilibj.command.Subsystem;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

/**
 * Subsystem for the drive train of the robot. Contains the 4 Talons neccessary
 * for the operation of all the motors in order to move the robot.
 * 
 * @author Tec Tigers
 *
 */
public class DriveTrain extends Subsystem {
	public static MPU9250 driveGyro;
	public static TalonSRX leftMaster;
	public static TalonSRX leftSlave;
	public static TalonSRX rightSlave;
	public static TalonSRX rightMaster;
	StickyFaults LFsFaults = new StickyFaults();
	StickyFaults RFsFaults = new StickyFaults();
	StickyFaults LBsFaults = new StickyFaults();
	StickyFaults RBsFaults = new StickyFaults();
	Faults LFFaults = new Faults();
	Faults RFFaults = new Faults();
	Faults LBFaults = new Faults();
	Faults RBFaults = new Faults();

	static double rightTicks; // The number of ticks the motor must travel
	static double leftTicks; // The number of ticks the motor must travel
	// Encoder leftEnc = new Encoder(0, 1, false, Encoder.EncodingType.k4X);
	// Encoder rightEnc = new Encoder(2, 3, false, Encoder.EncodingType.k4X);

	/**
	 * Creates the Drive Train with 4 TalonSRX motor controllers over CAN.
	 */
	public DriveTrain() {
		Robot.robotLogger.log(Logger.DEBUG, this, "enter");
		leftMaster = new TalonSRX(RobotMap.DRIVE_LEFTMASTER);
		leftSlave = new TalonSRX(RobotMap.DRIVE_LEFTSLAVE);
		rightSlave = new TalonSRX(RobotMap.DRIVE_RIGHTSLAVE);
		rightMaster = new TalonSRX(RobotMap.DRIVE_RIGHTMASTER);
		rightSlave.follow(rightMaster);
		leftSlave.follow(leftMaster);
		// encoderInit();
		pidInit();
		driveGyro = new MPU9250(); 
		driveGyro.startUpdatingThread();

		leftMaster.setNeutralMode(com.ctre.phoenix.motorcontrol.NeutralMode.Brake);
		rightMaster.setNeutralMode(com.ctre.phoenix.motorcontrol.NeutralMode.Brake);

		// TODO for PID example @see
		// https://github.com/Team4761/2018-Robot-Code/blob/master/src/org/robockets/robot/drivetrain/Drivetrain.java

		// TODO for PID example @see
		Debug();
		Robot.robotLogger.log(Logger.DEBUG, this, "exit");
		// Outputs Talon debug info

	}

	/**
	 * Initializes the DriveTrain's default command to the Drive command.
	 * 
	 * @see org.usfirst.frc.team3389.robot.commands.Drive
	 */
	@Override
	protected void initDefaultCommand() {
		Robot.robotLogger.log(Logger.DEBUG, this, "enter");

		setDefaultCommand(new Drive());

		Robot.robotLogger.log(Logger.DEBUG, this, "exit");
	}

	/**
	 * Sets the power of the 4 TalonSRX's in percent output mode.
	 * 
	 * @param leftPower
	 *            the power from -1.0 to 1.0 that the left Talons output
	 * @param rightPower
	 *            the power from -1.0 to 1.0 that the right Talons output
	 */
	private void rawDrive(double leftPower, double rightPower) {
		Robot.robotLogger.log(Logger.DEBUG, this, "enter:" + leftPower + ", " + rightPower);
		leftMaster.changeControlMode(TalonControlMode.Speed);
		rightMaster.set(ControlMode.Velocity, rightPower);

		Robot.robotLogger.log(Logger.DEBUG, this, "exit" + leftMaster.getMotorOutputPercent());
		// SmartDashboard.putNumber("encoder1", leftEnc.getDistance());
		// SmartDashboard.putNumber("encoder2", rightEnc.getDistance());

		// Robot.robotLogger.log(Logger.INFO, this, "encoderVal" +
		// leftEnc.getDistance());
		// Robot.robotLogger.log(Logger.INFO, this, "encoderVal" +
		// rightEnc.getDistance());

	}
	
	public static void turnDrive(double speed, double heading) {
		// PID constants and computation variables
		double kP = 0.5, kI = 0.5, kD = 0; // we wont be using derivative data
		double result_speed = 0, integral = 0, derivative = 0, error = 0, previous_error = 0;

		//get initial heading
		double current, initial=driveGyro.getFilteredYaw();	

		//direction calculation variables
		double direction = 1.0, pivot = initial + 180;

		// determine if we it is quicker to turn right or left to get to the new heading
		if(initial > 180) {
			// flip all of the directional variables when operating in the second half of the circle
			pivot = initial;
			initial = initial - 180;
			direction = -(direction);
		}
		
		// loop until robot has turned to new heading
		current= driveGyro.getFilteredYaw();
		while(current < heading) {
			previous_error = error;
		    error = heading - driveGyro.getFilteredYaw(); // Error = Target - Actual
		    integral += (error*.02); // Integral is increased by the error*time (which is .02 seconds using normal IterativeRobot)
		    derivative = (error - previous_error) / .02;
		    result_speed = speed * ((kP*error + kI*integral + kD*derivative) / heading);

			if((heading>initial)&&(heading<=pivot)) {
				driveVelocity((direction*result_speed),-(direction*result_speed));
			}
			else {
				driveVelocity(-(direction*result_speed),(direction*result_speed));
			}
			Robot.robotLogger.log(Logger.INFO, this, "PID turning: target=" + heading + ", current=" + current + ", speed=" + result_speed);
			current = driveGyro.getFilteredYaw();
			// since we will not perfectly tune the PID
			// we have a safety condition to break from the loop if we are 'close enough'
			// increase the constant if we find we are getting stuck in this loop
			if (Math.abs(current - heading) < 0.05)
				break;
		}
	}

	/**
	 * Drives the Drive Train with 2 analog stick's y values like a tank.
	 * 
	 * @param left
	 *            value of left stick from -1.0 to 1.0
	 * @param right
	 *            value of right stick from -1.0 to 1.0
	 */
	public void tankDrive(double left, double right) {
		Robot.robotLogger.log(Logger.DEBUG, this, "enter");

		rawDrive(left, right);

		Robot.robotLogger.log(Logger.DEBUG, this, "exit");
	}

	/**
	 * Stops all Talons.
	 */
	public void stop() {
		Robot.robotLogger.log(Logger.DEBUG, this, "enter");

		rawDrive(0, 0);

		Robot.robotLogger.log(Logger.DEBUG, this, "exit");
	}

	/**
	 * Outputs all data from Talons to robotLogger.
	 */
	private void Debug() {
		Robot.robotLogger.log(Logger.DEBUG, this, "TALON DEBUG\n==================================");
		Robot.robotLogger.log(Logger.DEBUG, this, "Output Current");
		Robot.robotLogger.log(Logger.DEBUG, this, "leftFront : " + leftMaster.getOutputCurrent());
		Robot.robotLogger.log(Logger.DEBUG, this, "rightFront: " + rightSlave.getOutputCurrent());
		Robot.robotLogger.log(Logger.DEBUG, this, "leftBack  : " + leftSlave.getOutputCurrent());
		Robot.robotLogger.log(Logger.DEBUG, this, "rightBack : " + rightMaster.getOutputCurrent());

		Robot.robotLogger.log(Logger.DEBUG, this, "Output Voltage");
		Robot.robotLogger.log(Logger.DEBUG, this, "leftFront : " + leftMaster.getMotorOutputVoltage());
		Robot.robotLogger.log(Logger.DEBUG, this, "rightFront: " + rightSlave.getMotorOutputVoltage());
		Robot.robotLogger.log(Logger.DEBUG, this, "leftBack  : " + leftSlave.getMotorOutputVoltage());
		Robot.robotLogger.log(Logger.DEBUG, this, "rightBack : " + rightMaster.getMotorOutputVoltage());

		Robot.robotLogger.log(Logger.DEBUG, this, "Bus Voltage");
		Robot.robotLogger.log(Logger.DEBUG, this, "leftFront : " + leftMaster.getBusVoltage());
		Robot.robotLogger.log(Logger.DEBUG, this, "rightFront: " + rightSlave.getBusVoltage());
		Robot.robotLogger.log(Logger.DEBUG, this, "leftBack  : " + leftSlave.getBusVoltage());
		Robot.robotLogger.log(Logger.DEBUG, this, "rightBack : " + rightMaster.getBusVoltage());

		Robot.robotLogger.log(Logger.DEBUG, this, "Output Percent");
		Robot.robotLogger.log(Logger.DEBUG, this, "leftFront : " + leftMaster.getMotorOutputPercent());
		Robot.robotLogger.log(Logger.DEBUG, this, "rightFront: " + rightSlave.getMotorOutputPercent());
		Robot.robotLogger.log(Logger.DEBUG, this, "leftBack  : " + leftSlave.getMotorOutputPercent());
		Robot.robotLogger.log(Logger.DEBUG, this, "rightBack : " + rightMaster.getMotorOutputPercent());
		Robot.robotLogger.log(Logger.DEBUG, this, "exit");

		// Talon Faults
		Robot.robotLogger.log(Logger.DEBUG, this, leftMaster.getFaults(LFFaults).toString());
		Robot.robotLogger.log(Logger.DEBUG, this, rightSlave.getFaults(RFFaults).toString());
		Robot.robotLogger.log(Logger.DEBUG, this, leftSlave.getFaults(LBFaults).toString());
		Robot.robotLogger.log(Logger.DEBUG, this, rightMaster.getFaults(RBFaults).toString());
		// Talon Stick Faults
		Robot.robotLogger.log(Logger.DEBUG, this, leftMaster.getStickyFaults(LFsFaults).toString());
		Robot.robotLogger.log(Logger.DEBUG, this, rightSlave.getStickyFaults(RFsFaults).toString());
		Robot.robotLogger.log(Logger.DEBUG, this, leftSlave.getStickyFaults(LBsFaults).toString());
		Robot.robotLogger.log(Logger.DEBUG, this, rightMaster.getStickyFaults(RBsFaults).toString());

		clearStickyFaults();
	}

	private void clearStickyFaults() {
		leftMaster.clearStickyFaults(0);
		rightSlave.clearStickyFaults(0);
		leftSlave.clearStickyFaults(0);
		rightMaster.clearStickyFaults(0);
	}

	public void resetEncoders() {
		leftMaster.setSelectedSensorPosition(0, 0, 0);
		rightMaster.setSelectedSensorPosition(0, 0, 0);
	}
	//
	// public void encoderInit() {
	// leftEnc.setMaxPeriod(1);
	// leftEnc.setMinRate(10);
	// leftEnc.setDistancePerPulse(5);
	// leftEnc.setReverseDirection(true);
	// leftEnc.setSamplesToAverage(7);
	// rightEnc.setMaxPeriod(1);
	// rightEnc.setMinRate(10);
	// rightEnc.setDistancePerPulse(5);
	// rightEnc.setReverseDirection(true);
	// rightEnc.setSamplesToAverage(7);
	// }

	private void pidInit() {


		leftMaster.configSelectedFeedbackSensor(FeedbackDevice.QuadEncoder, RobotMap.lPIDLoopIdx, RobotMap.rTimeoutMs); /* PIDLoop=0,timeoutMs=0 */
		// leftBack.configSelectedFeedbackSensor(FeedbackDevice.QuadEncoder, 0, 0); /*
		// PIDLoop=0,timeoutMs=0 */
		rightMaster.configSelectedFeedbackSensor(FeedbackDevice.QuadEncoder, RobotMap.rPIDLoopIdx, RobotMap.rTimeoutMs); /* PIDLoop=0,timeoutMs=0 */
		// rightBack.configSelectedFeedbackSensor(FeedbackDevice.QuadEncoder, 0, 0); /*
		// PIDLoop=0,timeoutMs=0 */

		leftMaster.setSensorPhase(true);
		rightMaster.setSensorPhase(true);
		leftMaster.setInverted(false);
		rightMaster.setInverted(false);
		
		leftMaster.setStatusFramePeriod(StatusFrameEnhanced.Status_13_Base_PIDF0, 10,
				RobotMap.lTimeoutMs);
		leftMaster.setStatusFramePeriod(StatusFrameEnhanced.Status_10_MotionMagic, 10,
				RobotMap.lTimeoutMs);

		rightMaster.setStatusFramePeriod(StatusFrameEnhanced.Status_13_Base_PIDF0, 10,
				RobotMap.rTimeoutMs);
		rightMaster.setStatusFramePeriod(StatusFrameEnhanced.Status_10_MotionMagic, 10,
				RobotMap.rTimeoutMs);

		leftMaster.configNominalOutputForward(0, RobotMap.lTimeoutMs);
		leftMaster.configNominalOutputReverse(0, RobotMap.lTimeoutMs);
		leftMaster.configPeakOutputForward(1, RobotMap.lTimeoutMs);
		leftMaster.configPeakOutputReverse(-1, RobotMap.lTimeoutMs);
		
		rightMaster.configNominalOutputForward(0, RobotMap.rTimeoutMs);
		rightMaster.configNominalOutputReverse(0, RobotMap.rTimeoutMs);
		rightMaster.configPeakOutputForward(1, RobotMap.rTimeoutMs);
		rightMaster.configPeakOutputReverse(-1, RobotMap.rTimeoutMs);
		
		
		leftMaster.selectProfileSlot(RobotMap.lSlotIdx, RobotMap.lPIDLoopIdx);
		leftMaster.config_kF(0, 0.2, RobotMap.lTimeoutMs);
		leftMaster.config_kP(0, 0.2, RobotMap.lTimeoutMs);
		leftMaster.config_kI(0, 0, RobotMap.lTimeoutMs);
		leftMaster.config_kD(0, 0, RobotMap.lTimeoutMs);
		
		rightMaster.selectProfileSlot(RobotMap.rSlotIdx, RobotMap.rPIDLoopIdx);
		rightMaster.config_kF(0, 0.2, RobotMap.rTimeoutMs);
		rightMaster.config_kP(0, 0.2, RobotMap.rTimeoutMs);
		rightMaster.config_kI(0, 0, RobotMap.rTimeoutMs);
		rightMaster.config_kD(0, 0, RobotMap.rTimeoutMs);
		
		leftMaster.configMotionCruiseVelocity(RobotMap.cruiseVelocity, RobotMap.lTimeoutMs);
		leftMaster.configMotionAcceleration(RobotMap.accel, RobotMap.lTimeoutMs);
		
		rightMaster.configMotionCruiseVelocity(15000, RobotMap.rTimeoutMs);
		rightMaster.configMotionAcceleration(6000, RobotMap.rTimeoutMs);
		
		leftMaster.setSelectedSensorPosition(0, RobotMap.lPIDLoopIdx, RobotMap.lTimeoutMs);
		rightMaster.setSelectedSensorPosition(0, RobotMap.rPIDLoopIdx, RobotMap.rTimeoutMs);

			}

	public static void driveVelocity(double leftVelocity, double rightVelocity) {
		rightMaster.set(ControlMode.Velocity, rightVelocity);
		leftMaster.set(ControlMode.Velocity, leftVelocity);
	}

	public static void drivePosition(int leftPosition, int rightPosition) {

		rightTicks = RobotMap.convRatio * rightPosition;
		leftTicks = RobotMap.convRatio * leftPosition;
		rightMaster.set(ControlMode.MotionMagic, rightTicks);
		leftMaster.set(ControlMode.MotionMagic, leftTicks);
	}

	public static double getLeftTicks() {
		return leftTicks;

	}

	public static double getRightTicks() {
		return rightTicks;
	}
	public static void drivePercent(int leftPercent,int rightPercent) {
		leftMaster.set(ControlMode.PercentOutput, leftPercent);
		rightMaster.set(ControlMode.PercentOutput, rightPercent);
	}
}
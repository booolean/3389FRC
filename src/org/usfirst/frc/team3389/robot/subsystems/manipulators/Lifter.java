package org.usfirst.frc.team3389.robot.subsystems.manipulators;

import org.usfirst.frc.team3389.robot.Robot;
import org.usfirst.frc.team3389.robot.RobotMap;
import org.usfirst.frc.team3389.robot.commands.LiftStick;
import org.usfirst.frc.team3389.robot.subsystems.ioDevices.QuadEncoder;
import org.usfirst.frc.team3389.robot.utils.Logger;

import com.ctre.phoenix.motorcontrol.ControlMode;
import com.ctre.phoenix.motorcontrol.Faults;
import com.ctre.phoenix.motorcontrol.StickyFaults;
import com.ctre.phoenix.motorcontrol.can.TalonSRX;

import edu.wpi.first.wpilibj.DigitalInput;
import edu.wpi.first.wpilibj.Encoder;
import edu.wpi.first.wpilibj.command.Subsystem;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

public class Lifter extends Subsystem{
	TalonSRX lift;
	StickyFaults liftSFaults = new StickyFaults();
	Faults LiftFaults = new Faults();
	Encoder enc = new Encoder(4, 5, false, Encoder.EncodingType.k4X);
	DigitalInput limitOne;
	DigitalInput limitTwo;
	
	private double height;
	private double radius;
	
	public Lifter() {
		Robot.robotLogger.log(Logger.DEBUG, this, "enter");
		limitOne= new DigitalInput(6);
		limitTwo= new DigitalInput(7);
		lift = new TalonSRX(RobotMap.LIFT);
		lift.setNeutralMode(com.ctre.phoenix.motorcontrol.NeutralMode.Brake);
		Debug();
		Robot.robotLogger.log(Logger.DEBUG, this, "exit");
	}
	
	public void driveLift(double power) {
		Robot.robotLogger.log(Logger.DEBUG, this, "enter:\t" + power);
		
		lift.set(ControlMode.PercentOutput, power);
		Robot.robotLogger.log(Logger.DEBUG, this, "limOneStatus: "+limitOne.get());
		Robot.robotLogger.log(Logger.DEBUG, this, "limTwoStatus: "+limitTwo.get());
		SmartDashboard.putBoolean("Limit One", limitOne.get());
		SmartDashboard.putBoolean("Limit Two", limitTwo.get());
		Robot.robotLogger.log(Logger.DEBUG, this, "exit");	
	}
	
	protected void initDefaultCommand() {
		Robot.robotLogger.log(Logger.DEBUG, this, "enter");
		
		setDefaultCommand(new LiftStick());
		
		Robot.robotLogger.log(Logger.DEBUG, this, "exit");
	}	
	
	public void stop() {
		Robot.robotLogger.log(Logger.DEBUG, this, "enter");
		
		driveLift(0);
		
		Robot.robotLogger.log(Logger.DEBUG, this, "exit");
	}
	
	public double getHeight() {
		height=radius*(enc.get()/360);
		return height;
	}
	public void gotoHeight(int inches) {
		getHeight();
		double wantedInch = inches;
		while(!((height>=wantedInch-1)&&(height<=wantedInch+1))){
			getHeight();
			if(wantedInch>height) {
				//TODO Make motor go up
			}
			if(wantedInch<height) {
				//TODO Make motor go down
			}
		}
		
		
		
	}
	private void Debug() {
		Robot.robotLogger.log(Logger.DEBUG, this, "TALON DEBUG\n==================================");
		Robot.robotLogger.log(Logger.DEBUG, this, "Output Current");
		Robot.robotLogger.log(Logger.DEBUG, this, "lift : " + lift.getOutputCurrent());

		Robot.robotLogger.log(Logger.DEBUG, this, "Output Voltage");
		Robot.robotLogger.log(Logger.DEBUG, this, "lift : " + lift.getMotorOutputVoltage());

		Robot.robotLogger.log(Logger.DEBUG, this, "Bus Voltage");
		Robot.robotLogger.log(Logger.DEBUG, this, "lift : " + lift.getBusVoltage());
		

		Robot.robotLogger.log(Logger.DEBUG, this, "Output Percent");
		Robot.robotLogger.log(Logger.DEBUG, this, "lift: " + lift.getMotorOutputPercent());
		Robot.robotLogger.log(Logger.DEBUG, this, "exit");

		// Talon Faults
		Robot.robotLogger.log(Logger.DEBUG, this, lift.getFaults(LiftFaults).toString());
		
		// Talon Stick Faults
		Robot.robotLogger.log(Logger.DEBUG, this, lift.getStickyFaults(liftSFaults).toString());

		clearStickyFaults();
	}

	private void clearStickyFaults() {
		lift.clearStickyFaults(0);
	}
}

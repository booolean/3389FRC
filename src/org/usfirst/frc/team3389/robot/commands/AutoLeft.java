/*
* Copyright (c) 2017-2018 FRC TEAM 3389. All Rights Reserved.
* Open Source Software - may be modified and shared by FRC teams. The code
* must be accompanied by the FIRST BSD license file in the root directory of
* the project.
*/


package org.usfirst.frc.team3389.robot.commands;

import edu.wpi.first.wpilibj.command.CommandGroup;

public class AutoLeft extends CommandGroup {
	// private properties here
	
	public AutoLeft(int gameData) {
		
		if(gameData == 0 || gameData == 2) {
			addParallel(new LiftAuto(3.5));
			addSequential(new DriveDistance(324.6));
			addSequential(new DriveTurn(.5, 90));
			addSequential(new IntakeAuto(-1, 2));
		}
		else if(gameData == 1 || gameData == 3) {
			addSequential(new DriveDistance(223.2));
			addSequential(new DriveTurn(.5, 90));
			addParallel(new LiftAuto(3.5));
			addSequential(new DriveDistance(207.1));
			addSequential(new DriveTurn(.5, -90));
			addSequential(new DriveDistance(41.6));
		}
		
		
		
		// add any combination of commands in sequential or parallel execution
		// for example:
		//   addSequential(new CommandA());
		//   addParallel(new CommandB());
		//   addParallel(new CommandC());
		//   addSequential(new CommandD());
		addSequential(new DriveDistance(36));
//		addSequential(new DriveTurn(.3, 90));
	}
}

/*----------------------------------------------------------------------------*/
/* Copyright (c) 2017-2018 FIRST. All Rights Reserved.                        */
/* Open Source Software - may be modified and shared by FRC teams. The code   */
/* must be accompanied by the FIRST BSD license file in the root directory of */
/* the project.                                                               */
/*----------------------------------------------------------------------------*/

// https://github.com/FRC-Team-Vern/VL53L0X_Example/blob/master/src/org/usfirst/frc/team5461/robot/sensors/VL53L0X.java

package org.usfirst.frc.team3389.robot.ioDevices.VL53L0X;

import java.nio.ByteBuffer;

import org.usfirst.frc.team3389.robot.Robot;
import org.usfirst.frc.team3389.robot.ioDevices.I2CUpdatableAddress;
import org.usfirst.frc.team3389.robot.utils.Logger;

import edu.wpi.first.wpilibj.RobotController;

/**
 * 
 * @author FRC Team 5461
 *
 */
public class VL53L0X extends I2CUpdatableAddress {

	// Store address given when the class is initialized.

	private static final int defaultAddress = 0x29;
	// The value of the address above the default address.
	private int deviceAddressOffset;
	private byte stop_variable;
	private int measurement_timing_budget_us;
	private long timeout_start_ms;
	private long io_timeout = 1000000; // microseconds
	private boolean did_timeout;

	private enum BYTE_SIZE {
		SINGLE(1), DOUBLE(2);

		public int value;

		private BYTE_SIZE(int value) {
			this.value = value;
		}
	};

	private enum vcselPeriodType {
		VcselPeriodPreRange, VcselPeriodFinalRange
	};

	private class SequenceStepEnables {
		byte tcc, msrc, dss, pre_range, final_range;
	};

	private class SequenceStepTimeouts {
		short pre_range_vcsel_period_pclks, final_range_vcsel_period_pclks;

		short msrc_dss_tcc_mclks, pre_range_mclks, final_range_mclks;
		int msrc_dss_tcc_us, pre_range_us, final_range_us;
	};

	private class BooleanCarrier {
		public boolean value = false;

		public BooleanCarrier(boolean inValue) {
			this.value = inValue;
		}
	}

	public VL53L0X(int offset) {
		super(Port.kMXP, defaultAddress);
		Robot.robotLogger.log(Logger.DEBUG, this, "enter");
		this.deviceAddressOffset = offset;
		this.did_timeout = false;
		Robot.robotLogger.log(Logger.DEBUG, this, "exit");
	}

	public final boolean init(boolean io_2v8) {
		Robot.robotLogger.log(Logger.DEBUG, this, "enter");
		// Start by changing to new address. This is required after every power up.
		// setAddress(defaultAddress + deviceAddressOffset);
		// sensor uses 1V8 mode for I/O by default; switch to 2V8 mode if necessary
		if (io_2v8) {
			write(VL53L0X_Constants.VHV_CONFIG_PAD_SCL_SDA__EXTSUP_HV.value,
					readByteNoErr(VL53L0X_Constants.VHV_CONFIG_PAD_SCL_SDA__EXTSUP_HV.value) | 0x01); // set bit 0
		}

		// "Set I2C standard mode"
		write(0x88, 0x00);

		write(0x80, 0x01);
		write(0xFF, 0x01);
		write(0x00, 0x00);
		stop_variable = readByteNoErr(0x91);
		write(0x00, 0x01);
		write(0xFF, 0x00);
		write(0x80, 0x00);

		// disable SIGNAL_RATE_MSRC (bit 1) and SIGNAL_RATE_PRE_RANGE (bit 4) limit
		// checks
		write(VL53L0X_Constants.MSRC_CONFIG_CONTROL.value,
				readByteNoErr(VL53L0X_Constants.MSRC_CONFIG_CONTROL.value) | 0x12);

		// set final range signal rate limit to 0.25 MCPS (million counts per second)
		setSignalRateLimit(0.25f);

		write(VL53L0X_Constants.SYSTEM_SEQUENCE_CONFIG.value, 0xFF);

		byte[] spad_count = new byte[1];
		BooleanCarrier spad_type_is_aperture = new BooleanCarrier(false);
		if (!getSpadInfo(spad_count, spad_type_is_aperture)) {
			Robot.robotLogger.log(Logger.ERROR, this, "unable to get spad info");
			return false;
		}

		// The SPAD map (RefGoodSpadMap) is read by VL53L0X_get_info_from_device() in
		// the API, but the same data seems to be more easily readable from
		// GLOBAL_CONFIG_SPAD_ENABLES_REF_0 through _6, so read it from there
		ByteBuffer ref_spad_map = ByteBuffer.allocateDirect(6);
		read(VL53L0X_Constants.GLOBAL_CONFIG_SPAD_ENABLES_REF_0.value, 6, ref_spad_map);

		write(0xFF, 0x01);
		write(VL53L0X_Constants.DYNAMIC_SPAD_REF_EN_START_OFFSET.value, 0x00);
		write(VL53L0X_Constants.DYNAMIC_SPAD_NUM_REQUESTED_REF_SPAD.value, 0x2C);
		write(0xFF, 0x00);
		write(VL53L0X_Constants.GLOBAL_CONFIG_REF_EN_START_SELECT.value, 0xB4);

		byte first_spad_to_enable = (byte) (spad_type_is_aperture.value ? 12 : 0); // 12 is the first aperture spad
		byte spads_enabled = 0;

		byte[] ref_spad_map_array = new byte[6];
		ref_spad_map.get(ref_spad_map_array);
		for (byte i = 0; i < 48; i++) {
			if (i < first_spad_to_enable || spads_enabled == spad_count[0]) {
				// This bit is lower than the first one that should be enabled, or
				// (reference_spad_count) bits have already been enabled, so zero this bit
				ref_spad_map_array[i / 8] &= ~(1 << (i % 8));
			} else if (((ref_spad_map_array[i / 8] >> (i % 8)) & 0x1) == 0x01) {
				spads_enabled++;
			}
		}

		ByteBuffer ref_spad_map2 = ByteBuffer.allocateDirect(6);
		ref_spad_map2.put(ref_spad_map_array);
		writeBulkAddress(VL53L0X_Constants.GLOBAL_CONFIG_SPAD_ENABLES_REF_0.value, ref_spad_map2, 6);

		write(0xFF, 0x01);
		write(0x00, 0x00);

		write(0xFF, 0x00);
		write(0x09, 0x00);
		write(0x10, 0x00);
		write(0x11, 0x00);

		write(0x24, 0x01);
		write(0x25, 0xFF);
		write(0x75, 0x00);

		write(0xFF, 0x01);
		write(0x4E, 0x2C);
		write(0x48, 0x00);
		write(0x30, 0x20);

		write(0xFF, 0x00);
		write(0x30, 0x09);
		write(0x54, 0x00);
		write(0x31, 0x04);
		write(0x32, 0x03);
		write(0x40, 0x83);
		write(0x46, 0x25);
		write(0x60, 0x00);
		write(0x27, 0x00);
		write(0x50, 0x06);
		write(0x51, 0x00);
		write(0x52, 0x96);
		write(0x56, 0x08);
		write(0x57, 0x30);
		write(0x61, 0x00);
		write(0x62, 0x00);
		write(0x64, 0x00);
		write(0x65, 0x00);
		write(0x66, 0xA0);

		write(0xFF, 0x01);
		write(0x22, 0x32);
		write(0x47, 0x14);
		write(0x49, 0xFF);
		write(0x4A, 0x00);

		write(0xFF, 0x00);
		write(0x7A, 0x0A);
		write(0x7B, 0x00);
		write(0x78, 0x21);

		write(0xFF, 0x01);
		write(0x23, 0x34);
		write(0x42, 0x00);
		write(0x44, 0xFF);
		write(0x45, 0x26);
		write(0x46, 0x05);
		write(0x40, 0x40);
		write(0x0E, 0x06);
		write(0x20, 0x1A);
		write(0x43, 0x40);

		write(0xFF, 0x00);
		write(0x34, 0x03);
		write(0x35, 0x44);

		write(0xFF, 0x01);
		write(0x31, 0x04);
		write(0x4B, 0x09);
		write(0x4C, 0x05);
		write(0x4D, 0x04);

		write(0xFF, 0x00);
		write(0x44, 0x00);
		write(0x45, 0x20);
		write(0x47, 0x08);
		write(0x48, 0x28);
		write(0x67, 0x00);
		write(0x70, 0x04);
		write(0x71, 0x01);
		write(0x72, 0xFE);
		write(0x76, 0x00);
		write(0x77, 0x00);

		write(0xFF, 0x01);
		write(0x0D, 0x01);

		write(0xFF, 0x00);
		write(0x80, 0x01);
		write(0x01, 0xF8);

		write(0xFF, 0x01);
		write(0x8E, 0x01);
		write(0x00, 0x01);
		write(0xFF, 0x00);
		write(0x80, 0x00);

		write(VL53L0X_Constants.SYSTEM_INTERRUPT_CONFIG_GPIO.value, 0x04);
		write(VL53L0X_Constants.GPIO_HV_MUX_ACTIVE_HIGH.value, readByteNoErr(VL53L0X_Constants.GPIO_HV_MUX_ACTIVE_HIGH.value) & ~0x10); // active low
		write(VL53L0X_Constants.SYSTEM_INTERRUPT_CLEAR.value, 0x01);

		// -- VL53L0X_SetGpioConfig() end

		measurement_timing_budget_us = getMeasurementTimingBudget();

		// "Disable MSRC and TCC by default"
		// MSRC = Minimum Signal Rate Check
		// TCC = Target CentreCheck
		// -- VL53L0X_SetSequenceStepEnable() begin

		write(VL53L0X_Constants.SYSTEM_SEQUENCE_CONFIG.value, 0xE8);

		// -- VL53L0X_SetSequenceStepEnable() end

		// improve accuracy slightly by not attempting to measure faster than 50ms
		//if (measurement_timing_budget_us < 50000)
		//	measurement_timing_budget_us = 50000;

		// "Recalculate timing budget"
		setMeasurementTimingBudget(measurement_timing_budget_us);

		// VL53L0X_StaticInit() end

		// VL53L0X_PerformRefCalibration() begin (VL53L0X_perform_ref_calibration())

		// -- VL53L0X_perform_vhv_calibration() begin

		write(VL53L0X_Constants.SYSTEM_SEQUENCE_CONFIG.value, 0x01);
		if (!performSingleRefCalibration((byte) 0x40)) {
			Robot.robotLogger.log(Logger.ERROR, this, "failed vhv performance calibration");
			return false;
		}

		// -- VL53L0X_perform_vhv_calibration() end

		// -- VL53L0X_perform_phase_calibration() begin

		write(VL53L0X_Constants.SYSTEM_SEQUENCE_CONFIG.value, 0x02);
		if (!performSingleRefCalibration((byte) 0x00)) {
			Robot.robotLogger.log(Logger.ERROR, this, "failed phase performance calibration");
			return false;
		}

		// -- VL53L0X_perform_phase_calibration() end

		// "restore the previous Sequence Config"
		write(VL53L0X_Constants.SYSTEM_SEQUENCE_CONFIG.value, 0xE8);

		// VL53L0X_PerformRefCalibration() end

		Robot.robotLogger.log(Logger.DEBUG, this, "exit");
		return true;
	}

	// Performs a single-shot range measurement and returns the reading in
	// millimeters
	// based on VL53L0X_PerformSingleRangingMeasurement()
	public int readRangeSingleMillimeters() {
		write(0x80, 0x01);
		write(0xFF, 0x01);
		write(0x00, 0x00);
		write(0x91, stop_variable);
		write(0x00, 0x01);
		write(0xFF, 0x00);
		write(0x80, 0x00);

		write(VL53L0X_Constants.SYSRANGE_START.value, 0x01);

		// "Wait until start bit has been cleared"
		startTimeout();
		while ((readByteNoErr(VL53L0X_Constants.SYSRANGE_START.value) & 0x01) == 0x01) {
			if (checkTimeoutExpired()) {
				did_timeout = true;
				Robot.robotLogger.log(Logger.WARNING, this, "timeout reading single range");
				return 65535;
			}
		}

		return readRangeContinuousMillimeters();
	}

	// Returns a range reading in millimeters when continuous mode is active
	// (readRangeSingleMillimeters() also calls this function after starting a
	// single-shot range measurement)
	public int readRangeContinuousMillimeters() {
		Robot.robotLogger.log(Logger.DEBUG, this, "enter");
		startTimeout();
		while ((readByteNoErr(VL53L0X_Constants.RESULT_INTERRUPT_STATUS.value) & 0x07) == 0) {
			if (checkTimeoutExpired()) {
				did_timeout = true;
				Robot.robotLogger.log(Logger.INFO, this, "timeout reading continuous range");
				return 65535;
			}
		}

		// assumptions: Linearity Corrective Gain is 1000 (default);
		// fractional ranging is not enabled
		// ByteBuffer byte_buffer_range =
		// read16(VL53L0X_Constants.RESULT_RANGE_STATUS.value + 10);

		short range = readShortNoErr(VL53L0X_Constants.RESULT_RANGE_STATUS.value + 10);

		write(VL53L0X_Constants.SYSTEM_INTERRUPT_CLEAR.value, 0x01);
		// byte_buffer_range.clear();
		Robot.robotLogger.log(Logger.DEBUG, this, "exit");
		return range;
	}

	public final int getAddressFromDevice() {
		Robot.robotLogger.log(Logger.DEBUG, this, "enter");
		int val = (int) readByteNoErr(VL53L0X_Constants.I2C_SLAVE_DEVICE_ADDRESS.value);
		Robot.robotLogger.log(Logger.DEBUG, this, "current address is 0x" + Integer.toHexString(val));
		Robot.robotLogger.log(Logger.DEBUG, this, "exit");
		return val;
	}

	public synchronized boolean writeBulkAddress(int registerAddress, ByteBuffer data, int size) {
		ByteBuffer registerWithDataToSendBuffer = ByteBuffer.allocateDirect(size + 1);
		registerWithDataToSendBuffer.put(data);
		return writeBulk(registerWithDataToSendBuffer, size + 1);
	}

	// Set the return signal rate limit check value in units of MCPS (mega counts
	// per second). "This represents the amplitude of the signal reflected from the
	// target and detected by the device"; setting this limit presumably determines
	// the minimum measurement necessary for the sensor to report a valid reading.
	// Setting a lower limit increases the potential range of the sensor but also
	// seems to increase the likelihood of getting an inaccurate reading because of
	// unwanted reflections from objects other than the intended target.
	// Defaults to 0.25 MCPS as initialized by the ST API and this library.
	private boolean setSignalRateLimit(float limit_Mcps) {
		if (limit_Mcps < 0 || limit_Mcps > 511.99) {
			return false;
		}
		writeShort(VL53L0X_Constants.FINAL_RANGE_CONFIG_MIN_COUNT_RATE_RTN_LIMIT.value, (int) (limit_Mcps * (1 << 7)));
		return true;
	}

	// Get reference SPAD (single photon avalanche diode) count and type
	// based on VL53L0X_get_info_from_device(),
	// but only gets reference SPAD count and type
	private boolean getSpadInfo(byte[] count, BooleanCarrier type_is_aperture) {
		byte tmp_byte = 0x00; // ByteBuffer.allocateDirect(BYTE_SIZE.SINGLE.value);

		write(0x80, 0x01);
		write(0xFF, 0x01);
		write(0x00, 0x00);

		write(0xFF, 0x06);
		write(0x83, readByteNoErr(0x83) | 0x04);
		write(0xFF, 0x07);
		write(0x81, 0x01);

		write(0x80, 0x01);

		write(0x94, 0x6b);
		write(0x83, 0x00);
		startTimeout();
		while (readByteNoErr(0x83) == 0x00) {
			if (checkTimeoutExpired()) {
				return false;
			}
		}
		write(0x83, 0x01);
		tmp_byte = readByteNoErr(0x92);

		count[0] = (byte) (tmp_byte & 0x7f);
		// count.put(0, count_byte);
		boolean type_is_aperture_boolean = (((tmp_byte) & 0x01) == 0x01);
		type_is_aperture.value = type_is_aperture_boolean;

		write(0x81, 0x00);
		write(0xFF, 0x06);
		write(0x83, readByteNoErr(0x83 & ~0x04));
		write(0xFF, 0x01);
		write(0x00, 0x01);

		write(0xFF, 0x00);
		write(0x80, 0x00);

		return true;
	}

	// Get the measurement timing budget in microseconds
	// based on VL53L0X_get_measurement_timing_budget_micro_seconds()
	// in us
	private int getMeasurementTimingBudget() {
		SequenceStepEnables enables = new SequenceStepEnables();
		SequenceStepTimeouts timeouts = new SequenceStepTimeouts();

		final short StartOverhead = 1910; // note that this is different than the value in set_
		final short EndOverhead = 960;
		final short MsrcOverhead = 660;
		final short TccOverhead = 590;
		final short DssOverhead = 690;
		final short PreRangeOverhead = 660;
		final short FinalRangeOverhead = 550;

		// "Start and end overhead times always present"
		int budget_us = StartOverhead + EndOverhead;

		getSequenceStepEnables(enables);
		getSequenceStepTimeouts(enables, timeouts);

		if (enables.tcc == 0x01) {
			budget_us += (timeouts.msrc_dss_tcc_us + TccOverhead);
		}

		if (enables.dss == 0x01) {
			budget_us += 2 * (timeouts.msrc_dss_tcc_us + DssOverhead);
		} else if (enables.msrc == 0x01) {
			budget_us += (timeouts.msrc_dss_tcc_us + MsrcOverhead);
		}

		if (enables.pre_range == 0x01) {
			budget_us += (timeouts.pre_range_us + PreRangeOverhead);
		}

		if (enables.final_range == 0x01) {
			budget_us += (timeouts.final_range_us + FinalRangeOverhead);
		}

		measurement_timing_budget_us = budget_us; // store for internal reuse
		return budget_us;
	}

	// Set the measurement timing budget in microseconds, which is the time allowed
	// for one measurement; the ST API and this library take care of splitting the
	// timing budget among the sub-steps in the ranging sequence. A longer timing
	// budget allows for more accurate measurements. Increasing the budget by a
	// factor of N decreases the range measurement standard deviation by a factor of
	// sqrt(N). Defaults to about 33 milliseconds; the minimum is 20 ms.
	// based on VL53L0X_set_measurement_timing_budget_micro_seconds()
	private boolean setMeasurementTimingBudget(int budget_us) {
		SequenceStepEnables enables = new SequenceStepEnables();
		SequenceStepTimeouts timeouts = new SequenceStepTimeouts();

		final short StartOverhead = 1320; // note that this is different than the value in get_
		final short EndOverhead = 960;
		final short MsrcOverhead = 660;
		final short TccOverhead = 590;
		final short DssOverhead = 690;
		final short PreRangeOverhead = 660;
		final short FinalRangeOverhead = 550;

		final int MinTimingBudget = 20000;

		if (budget_us < MinTimingBudget) {
			return false;
		}

		int used_budget_us = StartOverhead + EndOverhead;

		getSequenceStepEnables(enables);
		getSequenceStepTimeouts(enables, timeouts);

		if (enables.tcc == 0x01) {
			used_budget_us += (timeouts.msrc_dss_tcc_us + TccOverhead);
		}

		if (enables.dss == 0x01) {
			used_budget_us += 2 * (timeouts.msrc_dss_tcc_us + DssOverhead);
		} else if (enables.msrc == 0x01) {
			used_budget_us += (timeouts.msrc_dss_tcc_us + MsrcOverhead);
		}

		if (enables.pre_range == 0x01) {
			used_budget_us += (timeouts.pre_range_us + PreRangeOverhead);
		}

		if (enables.final_range == 0x01) {
			used_budget_us += FinalRangeOverhead;

			// "Note that the final range timeout is determined by the timing
			// budget and the sum of all other timeouts within the sequence.
			// If there is no room for the final range timeout, then an error
			// will be set. Otherwise the remaining time will be applied to
			// the final range."

			if (used_budget_us > budget_us) {
				// "Requested timeout too big."
				return false;
			}

			int final_range_timeout_us = budget_us - used_budget_us;

			// set_sequence_step_timeout() begin
			// (SequenceStepId == VL53L0X_SEQUENCESTEP_FINAL_RANGE)

			// "For the final range timeout, the pre-range timeout
			// must be added. To do this both final and pre-range
			// timeouts must be expressed in macro periods MClks
			// because they have different vcsel periods."
			short final_range_timeout_mclks = (short) timeoutMicrosecondsToMclks(final_range_timeout_us,
					(byte) timeouts.final_range_vcsel_period_pclks);

			if (enables.pre_range == 0x01) {
				final_range_timeout_mclks += timeouts.pre_range_mclks;
			}

			writeShort(VL53L0X_Constants.FINAL_RANGE_CONFIG_TIMEOUT_MACROP_HI.value,
					encodeTimeout(final_range_timeout_mclks));

			// set_sequence_step_timeout() end

			measurement_timing_budget_us = budget_us; // store for internal reuse
		}
		return true;
	}

	// Get sequence step enables
	// based on VL53L0X_GetSequenceStepEnables()
	private void getSequenceStepEnables(SequenceStepEnables enables) {
		byte sequence_config = readByteNoErr(VL53L0X_Constants.SYSTEM_SEQUENCE_CONFIG.value);

		enables.tcc = (byte) ((sequence_config >> 4) & 0x1);
		enables.dss = (byte) ((sequence_config >> 3) & 0x1);
		enables.msrc = (byte) ((sequence_config >> 2) & 0x1);
		enables.pre_range = (byte) ((sequence_config >> 6) & 0x1);
		enables.final_range = (byte) ((sequence_config >> 7) & 0x1);
	}

	// Convert sequence step timeout from microseconds to MCLKs with given VCSEL
	// period in PCLKs
	// based on VL53L0X_calc_timeout_mclks()
	private int timeoutMicrosecondsToMclks(int timeout_period_us, byte vcsel_period_pclks) {
		int macro_period_ns = calcMacroPeriod(vcsel_period_pclks);

		return (((timeout_period_us * 1000) + (macro_period_ns / 2)) / macro_period_ns);
	}

	// Get sequence step timeouts
	// based on get_sequence_step_timeout(),
	// but gets all timeouts instead of just the requested one, and also stores
	// intermediate values
	private void getSequenceStepTimeouts(SequenceStepEnables enables, SequenceStepTimeouts timeouts) {
		timeouts.pre_range_vcsel_period_pclks = getVcselPulsePeriod(vcselPeriodType.VcselPeriodPreRange);

		timeouts.msrc_dss_tcc_mclks = (short) (readByteNoErr(VL53L0X_Constants.MSRC_CONFIG_TIMEOUT_MACROP.value) + 1);
		timeouts.msrc_dss_tcc_us = timeoutMclksToMicroseconds(timeouts.msrc_dss_tcc_mclks,
				timeouts.pre_range_vcsel_period_pclks);

		short result;
		result = readShortNoErr(VL53L0X_Constants.PRE_RANGE_CONFIG_TIMEOUT_MACROP_HI.value);
		timeouts.pre_range_mclks = decodeTimeout(result);
		timeouts.pre_range_us = timeoutMclksToMicroseconds(timeouts.pre_range_mclks,
				timeouts.pre_range_vcsel_period_pclks);

		timeouts.final_range_vcsel_period_pclks = getVcselPulsePeriod(vcselPeriodType.VcselPeriodFinalRange);
		result = readShortNoErr(VL53L0X_Constants.FINAL_RANGE_CONFIG_TIMEOUT_MACROP_HI.value);
		timeouts.final_range_mclks = decodeTimeout(result);

		if (enables.pre_range == 0x01) {
			timeouts.final_range_mclks -= timeouts.pre_range_mclks;
		}

		timeouts.final_range_us = timeoutMclksToMicroseconds(timeouts.final_range_mclks,
				timeouts.final_range_vcsel_period_pclks);
	}

	// Convert sequence step timeout from MCLKs to microseconds with given VCSEL
	// period in PCLKs
	// based on VL53L0X_calc_timeout_us()
	int timeoutMclksToMicroseconds(short timeout_period_mclks, short vcsel_period_pclks) {
		int macro_period_ns = calcMacroPeriod(vcsel_period_pclks);

		return ((timeout_period_mclks * macro_period_ns) + (macro_period_ns / 2)) / 1000;
	}

	private int calcMacroPeriod(int vcsel_period_pclks) {
		return ((((int) 2304 * (vcsel_period_pclks) * 1655) + 500) / 1000);
	}

	// Decode VCSEL (vertical cavity surface emitting laser) pulse period in PCLKs
	// from register value
	// based on VL53L0X_decode_vcsel_period()
	private int decodeVcselPeriod(int reg_val) {
		return (((reg_val) + 1) << 1);
	}

	// Record the current time to check an upcoming timeout against
	private long startTimeout() {
		long now  = RobotController.getFPGATime();
		timeout_start_ms = now;
		return (now);
	}

	// Check if timeout is enabled (set to nonzero value) and has expired
	private boolean checkTimeoutExpired() {
		long now  = RobotController.getFPGATime();
		return (io_timeout > 0 && (now - timeout_start_ms) > io_timeout);
	}

	// Get the VCSEL pulse period in PCLKs for the given period type.
	// based on VL53L0X_get_vcsel_pulse_period()
	byte getVcselPulsePeriod(vcselPeriodType type) {
		if (type == vcselPeriodType.VcselPeriodPreRange) {
			return (byte) decodeVcselPeriod(readByteNoErr(VL53L0X_Constants.PRE_RANGE_CONFIG_VCSEL_PERIOD.value));
		} else if (type == vcselPeriodType.VcselPeriodFinalRange) {
			return (byte) decodeVcselPeriod(readByteNoErr(VL53L0X_Constants.FINAL_RANGE_CONFIG_VCSEL_PERIOD.value));
		} else {
			return (byte) 255;
		}
	}

	// Encode sequence step timeout register value from timeout in MCLKs
	// based on VL53L0X_encode_timeout()
	// Note: the original function took a uint16_t, but the argument passed to it
	// is always a uint16_t.
	short encodeTimeout(short timeout_mclks) {
		// format: "(LSByte * 2^MSByte) + 1"
		int ls_byte = 0;
		short ms_byte = 0;

		if (timeout_mclks > 0) {
			ls_byte = timeout_mclks - 1;

			while ((ls_byte & 0xFFFFFF00) > 0) {
				ls_byte >>= 1;
				ms_byte++;
			}
			return (short) ((ms_byte << 8) | (ls_byte & 0xFF));
		} else {
			return 0;
		}
	}

	// Decode sequence step timeout in MCLKs from register value
	// based on VL53L0X_decode_timeout()
	// Note: the original function returned a uint32_t, but the return value is
	// always stored in a uint16_t.
	short decodeTimeout(short reg_val) {
		// format: "(LSByte * 2^MSByte) + 1"
		return (short) ((short) ((reg_val & 0x00FF) << (short) ((reg_val & 0xFF00) >> 8)) + 1);
	}

	// based on VL53L0X_perform_single_ref_calibration()
	boolean performSingleRefCalibration(byte vhv_init_byte) {
		write(VL53L0X_Constants.SYSRANGE_START.value, 0x01 | vhv_init_byte); // VL53L0X_REG_SYSRANGE_MODE_START_STOP

		startTimeout();
		while ((readByteNoErr(VL53L0X_Constants.RESULT_INTERRUPT_STATUS.value) & 0x07) == 0) {
			if (checkTimeoutExpired()) {
				return false;
			}
		}

		write(VL53L0X_Constants.SYSTEM_INTERRUPT_CLEAR.value, 0x01);

		write(VL53L0X_Constants.SYSRANGE_START.value, 0x00);

		return true;
	}

	// local private helper methods which ignore error handling
	
	public byte readByteNoErr(int registerAddress) {
    	byte[] data = new byte[1];
    	readByte(registerAddress, data);
    	return data[0];
    }
	
    public short readShortNoErr(int registerAddress) {
    	short[] data = new short[1];
    	readShort(registerAddress, data);
    	return data[0];
    }
	
}

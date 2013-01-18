/**
 * VStar: a statistical analysis tool for variable star data.
 * Copyright (C) 2010  AAVSO (http://www.aavso.org/)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>. 
 */
package org.aavso.tools.vstar.util;

import junit.framework.TestCase;

import org.aavso.tools.vstar.util.coords.DecInfo;
import org.aavso.tools.vstar.util.coords.RAInfo;
import org.aavso.tools.vstar.util.date.HJDConverter;

/**
 * HJDConverter unit tests.
 */
public class HJDConverterTest extends TestCase {

	private static final double JD = 2445239.4;
	private static final int PRECISION = 8;

	public HJDConverterTest(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	// Test cases

	public void testConversion1() {
		RAInfo ra = new RAInfo(1950, 0, 0, 0);
		DecInfo dec = new DecInfo(1950, 0, 0, 0);
		double hjd = HJDConverter.convert(JD, ra, dec);
		String hjdStr = getNumToPrecision(hjd, PRECISION);
		assertEquals(getNumToPrecision(2445239.40578294, PRECISION), hjdStr);
	}

	public void testConversion2() {
		RAInfo ra = new RAInfo(1950, 15, 2, 3.6);
		DecInfo dec = new DecInfo(1950, -25, 45, 3);
		double hjd = HJDConverter.convert(JD, ra, dec);
		String hjdStr = getNumToPrecision(hjd, PRECISION);
		assertEquals(getNumToPrecision(2445239.39611801, PRECISION), hjdStr);
	}

	public void testConversion3() {
		RAInfo ra = new RAInfo(1950, 12, 0, 0);
		DecInfo dec = new DecInfo(1950, 2, 0, 0);
		double hjd = HJDConverter.convert(JD, ra, dec);
		String hjdStr = getNumToPrecision(hjd, PRECISION);
		assertEquals(getNumToPrecision(2445239.39422482, PRECISION), hjdStr);
	}

	// Helpers

	private String getNumToPrecision(double n, int precision) {
		return String.format("%1." + precision + "f", n);
	}
}

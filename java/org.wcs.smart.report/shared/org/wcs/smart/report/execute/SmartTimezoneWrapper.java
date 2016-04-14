/*
 * Copyright (C) 2012 Wildlife Conservation Society
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.wcs.smart.report.execute;

import java.util.Date;

import org.wcs.smart.ca.ConservationArea;

import com.ibm.icu.util.TimeZone;

/**
 * This is a hack to be able to provide the current conservation
 * area and user to the SMART Birt Functions.
 * 
 * @author Emily
 *
 */
public class SmartTimezoneWrapper extends TimeZone{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private TimeZone wrapper;
	private ConservationArea ca;
	private String user;
	
	/**
	 * Create new timezone
	 * @param wrapper
	 * @param ca
	 * @param user
	 */
	public SmartTimezoneWrapper(TimeZone wrapper, ConservationArea ca, String user) {
		this.wrapper = wrapper;
		this.ca = ca;
		this.user = user;
	}
	
	/**
	 * The conservation area
	 * @return
	 */
	public ConservationArea getConservationArea(){
		return this.ca;
	}
	
	/**
	 * The smart user label
	 * @return
	 */
	public String getUser(){
		return this.user;
	}
	
	@Override
	public int getOffset(int era, int year, int month, int day, int dayOfWeek,
			int milliseconds) {
		return wrapper.getOffset(era, year, month, day, dayOfWeek, milliseconds);
	}

	@Override
	public void setRawOffset(int offsetMillis) {
		wrapper.setRawOffset(offsetMillis);
	}

	@Override
	public int getRawOffset() {
		return wrapper.getRawOffset();
	}

	@Override
	public boolean useDaylightTime() {
		return wrapper.useDaylightTime();
	}

	@Override
	public boolean inDaylightTime(Date date) {
		return wrapper.inDaylightTime(date);
	}
	
}
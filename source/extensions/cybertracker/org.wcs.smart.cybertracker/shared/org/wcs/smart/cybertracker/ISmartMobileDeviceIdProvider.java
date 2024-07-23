/*
 * Copyright (C) 2024 Wildlife Conservation Society
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
package org.wcs.smart.cybertracker;

import java.util.List;

import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;

/**
 * Device id provider for providing smart mobile device ids from 
 * saved data
 */
public interface ISmartMobileDeviceIdProvider {

	public static final String EXT_ID = "org.wcs.smart.cybertracker.device"; //$NON-NLS-1$
	public static final String EXT_NAME = "device_id_provider"; //$NON-NLS-1$
	
	/**
	 * Get the device ids for associated with any data managed by
	 * this plugin for the conservation area.
	 * 
	 * @param session
	 * @param ca
	 * @return
	 */
	public List<String> getDeviceIds(Session session, ConservationArea ca);
	
}

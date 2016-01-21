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
package org.wcs.smart.cybertracker.model;

import org.wcs.smart.cybertracker.internal.Messages;

/**
 * Projection format details for cybertracker profiles.
 * @author Emily
 *
 */
public enum ProjectionFormat {
	DEGREE_MIN_SEC(Messages.CyberTrackerProperties_ProjectonFormat_DegreeMinSec, 0),
	DECEMAL_DEGREE(Messages.CyberTrackerProperties_ProjectonFormat_DecemalDegree, 1),
	UTM(Messages.CyberTrackerProperties_ProjectonFormat_UTM, 2);
	private String guiName;
	private int id;
	ProjectionFormat(String guiName, int id) {
		this.guiName = guiName;
		this.id = id;
	}
	public String getGuiName() {
		return this.guiName;
	}
	public int getId() {
		return id;
	}
	public static Integer[] getIds() {
		ProjectionFormat[] values = ProjectionFormat.values();
		Integer[] ids = new Integer[values.length];
		for (int i = 0; i < values.length; i++) {
			ids[i] = values[i].getId();
		}
		return ids;
	}
}
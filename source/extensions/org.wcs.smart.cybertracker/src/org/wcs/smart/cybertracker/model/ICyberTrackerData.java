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

import java.util.Date;
import java.util.List;

import org.wcs.smart.cybertracker.model.data.Data.Sightings.S;

import com.vividsolutions.jts.geom.Coordinate;

/**
/**
 * Model representing single set of data imported from CyberTracker application
 * 
 * @author elitvin
 * @since 4.0.0
 */
public interface ICyberTrackerData {

	public Date getStartDate();

	public Date getEndDate();

	public String getType();

	public String getDisplayType();
	
	public String getDetails();
	
	public List<S> getSData();
	
	public List<Coordinate> getTimerTrackList();

	public void setTimerTrackList(List<Coordinate> timerTrackList);
	
}

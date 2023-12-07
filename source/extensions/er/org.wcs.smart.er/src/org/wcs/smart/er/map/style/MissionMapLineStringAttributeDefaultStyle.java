/*
 * Copyright (C) 2023 Wildlife Conservation Society
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
package org.wcs.smart.er.map.style;

import org.wcs.smart.er.internal.Messages;
import org.wcs.smart.udig.style.IMapLayerDefaultStyle;

/**
 * Patrol map page default waypoint style
 * 
 * @author Emily
 *
 */
public class MissionMapLineStringAttributeDefaultStyle implements IMapLayerDefaultStyle {

	public static final String KEY = "org.wcs.smart.mission.map.attribute.linestring";  //$NON-NLS-1$
	
	@Override
	public String getKey() {
		return KEY;
	}

	@Override
	public String getMapName() {
		return Messages.MissionMapWaypointDefaultStyle_MapName;
	}

	@Override
	public String getLayerName() {
		return "LineString Attributes";
	}

}

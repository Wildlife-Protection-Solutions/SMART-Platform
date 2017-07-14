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
package org.wcs.smart.cybertracker.properties;

import java.util.Comparator;

import org.wcs.smart.cybertracker.model.CyberTrackerPropertiesProfile;

/**
 * Comparator for sorting a list of {@link CyberTrackerPropertiesProfile} objects based
 * on their isDefault state and name.
 * 
 * @author elitvin
 * @since 4.0.0
 */
public class CtProfileDefaultNameComparator implements Comparator<CyberTrackerPropertiesProfile> {

	@Override
	public int compare(CyberTrackerPropertiesProfile p1, CyberTrackerPropertiesProfile p2) {
		if (p1.isDefault()) {
			return -1; //by design we have only one default profile and we place it at the beginning
		}
		if (p1.getName() == null) {
			return p2.getName() == null ? 0 : -1;
		}
		return p1.getName().compareTo(p2.getName());
	}

}

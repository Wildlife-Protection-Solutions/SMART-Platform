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
package org.wcs.smart.cybertracker.patrol.query;

import org.wcs.smart.patrol.query.ext.IExtensionGroupBy;
import org.wcs.smart.query.model.filter.IGroupByVisitor;

/**
 * SMART Mobile Device ID group by extension
 * 
 * @author Emily
 * @since 8.1.0
 *
 */
public class MobileDeviceIdParolGroupBy implements IExtensionGroupBy {

	public static final String KEY = "patrol:contribution:smdeviceid"; //$NON-NLS-1$
	
	@Override
	public String asString() {
		return getKeyPart();
	}

	@Override
	public String getKeyPart() {
		return KEY;
	}

	@Override
	public GroupByType getType() {
		return GroupByType.KEY;
	}

	@Override
	public void visit(IGroupByVisitor visitor) {
		visitor.visit(this);
	}

	@Override
	public IExtensionGroupBy createGroupBy(String key) {
		if (key.equals(KEY + ":")){ //$NON-NLS-1$
			return new MobileDeviceIdParolGroupBy();
		}
		return null;
	}
}

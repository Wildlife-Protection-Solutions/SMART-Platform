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
package org.wcs.smart.patrol.query.model;

import java.util.Locale;
import java.util.UUID;

import org.hibernate.Session;

/**
 * Blank implementation for some of {@link IPatrolQueryOption} methods 
 * which is suitable for most contribution items
 * 
 * @author elitvin
 * @since 1.0.0
 */
public abstract class AbstractEmptyPatrolQueryOption implements IPatrolQueryOption {

	
	@Override
	public Class<?> getSourceClass() {
		return this.getClass();
	}

	@Override
	public boolean isEmployeeItem() {
		return false;
	}
	
	@Override
	public String getName(Session session, UUID uuid, Locale l) {
		return ""; //$NON-NLS-1$
	}

	@Override
	public String[] getNames(Session session, UUID uuid, Locale l) {
		return new String[0];
	}

	@Override
	public Object getObject(Session session, UUID uuid) {
		return null;
	}

}

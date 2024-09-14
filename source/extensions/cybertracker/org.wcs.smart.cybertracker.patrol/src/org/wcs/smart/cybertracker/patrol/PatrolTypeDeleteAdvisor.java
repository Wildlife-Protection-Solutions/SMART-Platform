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
package org.wcs.smart.cybertracker.patrol;

import org.hibernate.Session;
import org.wcs.smart.ca.advisors.IDeleteAdvisor;
import org.wcs.smart.patrol.model.PatrolType;

import com.ibm.icu.text.MessageFormat;

/**
 * Delete advisor for patrol types (track types) that are now linked
 * to patrol packages
 * @since 8.1
 */
public class PatrolTypeDeleteAdvisor implements IDeleteAdvisor {

	public PatrolTypeDeleteAdvisor() {
	}

	@Override
	public String canDelete(Object object, Session session) {
		if (!(object instanceof PatrolType)) return "Invalid object type"; //$NON-NLS-1$
		
		PatrolType pt = (PatrolType)object;
		
		long numpackages = session.createQuery("SELECT count(*) FROM PatrolCtPackage WHERE trackType = :type", Long.class) //$NON-NLS-1$
		.setParameter("type", pt) //$NON-NLS-1$
		.uniqueResult();
		
		if (numpackages > 0) {
			return MessageFormat.format("There are {0} SMART Mobile patrol packages associated with the ''{1}'' track type. These must be deleted before you can delete the track type.", numpackages, pt.getName());
		}
		
		return null;
	}

}

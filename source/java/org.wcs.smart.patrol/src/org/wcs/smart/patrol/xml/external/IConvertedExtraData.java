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
package org.wcs.smart.patrol.xml.external;

import java.util.List;

import org.hibernate.Session;
import org.wcs.smart.patrol.model.Patrol;

/**
 * Represents the patrol extra-data converted object.
 * 
 * @author elitvin
 * @since 1.0.0
 */
public interface IConvertedExtraData {
	
	/**
	 * @return any warnings generated during the import process
	 */
	public List<String> getWarnings();
	
	/**
	 * Perform actual save operation of converted extra-data.
	 * 
	 * @param session
	 * @param patrol
	 * @return <true> if data successfully saved in database
	 */
	public boolean saveInTransaction(Session session, Patrol patrol);

}

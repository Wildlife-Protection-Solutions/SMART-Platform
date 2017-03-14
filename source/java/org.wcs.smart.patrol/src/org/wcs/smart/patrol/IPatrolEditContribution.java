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
package org.wcs.smart.patrol;

import org.hibernate.Session;
import org.wcs.smart.patrol.model.Patrol;


/**
 * Contribution management when a patrol is split or merged.
 * 
 * @author Emily
 *
 */
public interface IPatrolEditContribution {

	public static final String EXTENSION_ID = "org.wcs.smart.patrol.contribution"; //$NON-NLS-1$
	
	/**
	 * Called when the patrol is split from one patrol into mutliple patrols.
	 * 
	 * @param originalPatrol the original patrol
	 * @param newPatrols the new patrol
	 * 
	 */
	public void splitPatrol(Session s, Patrol originalPatrol, Patrol newPatrol);
	

}

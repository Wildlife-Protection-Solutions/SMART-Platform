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
package org.wcs.smart.changetracking;

import org.hibernate.Session;

/**
 * Abstract change log tracker that creates and drop triggers provided.
 * 
 * @author Emily
 *
 */
public abstract class AbstractChangeTrackerInstaller implements IChangeTrackerInstaller {

	@Override
	public void installChangeTracking(Session session) throws Exception {
		for (String[] trigger : getTriggers()){
			DerbyTriggerManager.INSTANCE.createTriggerIfNotExists(trigger[0], trigger[1], session);
		}
	}

	@Override
	public void removeChangeTracking(Session session) throws Exception {
		for (String[] trigger : getTriggers()){
			DerbyTriggerManager.INSTANCE.dropIfExists(trigger[0], session);
		}
	}
	
	/**
	 * Return an array of all triggers that are required for tracking
	 * changes. { {triggername, sqldefinition} {triggername, sqldefinition} ...}
	 * @return
	 */
	public abstract String[][] getTriggers();

}

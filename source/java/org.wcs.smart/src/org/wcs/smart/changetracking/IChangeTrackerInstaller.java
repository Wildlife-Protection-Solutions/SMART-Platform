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
 * Installs change tracking for a given plugin.
 * 
 * @author Emily
 *
 */
public interface IChangeTrackerInstaller {

	/**
	 * Change log installer extension id
	 */
	public static final String EXTENSION_ID = "org.wcs.smart.changelog.installer"; //$NON-NLS-1$
	
	/**
	 * Should create triggers for all tables for the CURRENT version of
	 * the plugin.  IF the database version is not the current version
	 * this should exit immediately and return false.
	 * 
	 * <p>When plugins are installed/upgraded all existing change log
	 * tracking is disabled (by calling removeChangeTracking), then the
	 * plugin updates/installations are made, then change log tracking is
	 * reinstalled. This ensures that the plugins don't have to worry about
	 * triggers or other table requirements causing upgrade scripts to fail.
	 * As a result this function should ONLY install change tracking if the
	 * database state is current.  If its not current it should do nothing as
	 * when it is made current the change tracking will be installed.
	 * </p>
	 * 
	 * @return true if installed false if database is not up-to-date and could
	 * not be installed
	 * @param session with active transaction
	 */
	public boolean installChangeTracking(Session session) throws Exception;
	
	/**
	 * Should remove all triggers ever created.  This will be called before
	 * any upgrade scripts to ensure triggers don't cause issues with upgrade scripts.
	 * <p>This must be able to deal with previous versions of the database for a given plugin.
	 * For example if Version 1 has Table A and Version 2 has Table A & B this function
	 * must be able to handle removing change tracking for both Version 1 and Version 2.</p>
	 * 
	 * @param session with active transaction
	 */
	public void removeChangeTracking(Session session) throws Exception;
}

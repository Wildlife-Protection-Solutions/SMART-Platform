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
package org.wcs.smart.report.model;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Platform;

/**
 * A root folder in the report tree.
 * @author egouge
 * @since 1.0.0
 */
public class RootReportFolder implements IAdaptable{

	/**
	 * Conservation area root folder
	 */
	public static final RootReportFolder CA_ROOT_FOLDER = new RootReportFolder("Conservation Area Reports", true);
	/**
	 * User root folder
	 */
	public static final RootReportFolder USER_ROOT_FOLDER = new RootReportFolder("My Reports", false);
	
	private String name;
	private boolean isShared = false;
	
	/**
	 * Creates a new root folder
	 * @param name name
	 * @param isShared if shared between users
	 */
	private RootReportFolder(String name, boolean isShared){
		this.name = name;
		this.isShared = isShared;
	}
	
	/**
	 * @return folder name
	 */
	public String getName(){
		return this.name;
	}
	
	/**
	 * @return if folder shared between users
	 */
	public boolean isShared(){
		return this.isShared;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public Object getAdapter(Class adapter) {
		return Platform.getAdapterManager().getAdapter(this, adapter);
	}
}

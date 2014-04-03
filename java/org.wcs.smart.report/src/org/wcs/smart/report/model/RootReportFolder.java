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
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.report.internal.Messages;

/**
 * A root folder in the report tree.
 * @author egouge
 * @since 1.0.0
 */
public class RootReportFolder implements IAdaptable{

	/**
	 * Conservation area root folder
	 */
	public static final RootReportFolder CA_ROOT_FOLDER = new RootReportFolder(Messages.RootReportFolder_CaRootReportFolderName, true, SmartDB.getCurrentConservationArea());
	/**
	 * User root folder
	 */
	public static final RootReportFolder USER_ROOT_FOLDER = new RootReportFolder(Messages.RootReportFolder_UserRootReportFolderName, false, SmartDB.getCurrentConservationArea());
	
	private String name;
	private Boolean isShared = false;
	private ConservationArea ca;
	
	public static RootReportFolder createCaRootFolder(ConservationArea ca){
		return new RootReportFolder(Messages.RootReportFolder_CaRootReportFolderName, true, ca);
	}
	
	public static RootReportFolder createUserRootFolder(ConservationArea ca){
		return new RootReportFolder(Messages.RootReportFolder_UserRootReportFolderName, false, ca);
	}
	
	/**
	 * Creates a new root folder
	 * @param name name
	 * @param isShared if shared between users
	 */
	private RootReportFolder(String name, boolean isShared, ConservationArea ca){
		this.name = name;
		this.isShared = isShared;
		this.ca = ca;
	}
	
	/**
	 * @return Conservation Area associated with root folder 
	 */
	public ConservationArea getConservationArea(){
		return this.ca;
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
	
	@Override
	public boolean equals(Object other){
		if (other instanceof RootReportFolder){
			RootReportFolder o = (RootReportFolder) other;
			return ca.equals(o.ca) && isShared == o.isShared; 
		}
		return false;
	}
	
	@Override
	public int hashCode(){
		final int prime = 31;
		int result = 1;
		result = prime * result + ca.hashCode();
		result = prime * result + isShared.hashCode();
		return result;
	}
}

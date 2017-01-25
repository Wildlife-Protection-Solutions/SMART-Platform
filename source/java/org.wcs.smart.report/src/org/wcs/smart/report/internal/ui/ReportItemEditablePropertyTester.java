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
package org.wcs.smart.report.internal.ui;

import java.text.Collator;

import org.eclipse.core.expressions.PropertyTester;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.report.manger.ReportManager;
import org.wcs.smart.report.model.Report;
import org.wcs.smart.report.model.ReportFolder;
import org.wcs.smart.report.model.RootReportFolder;
import org.wcs.smart.user.UserLevelManager;

/**
 * Property tester for determining if a report item (report or report folder) 
 * has permission to perform a given action.
 * <p>Supported Actions: RENAME, DELETE, EDIT, NEWFOLDER, EXPORT</p>
 * @author egouge
 * @since 1.0.0
 */
public class ReportItemEditablePropertyTester extends PropertyTester {

	/**
	 * if the selection can be renamed
	 */
	public static final String RENAME = "rename"; //$NON-NLS-1$
	/**
	 * if the selection can be deleted
	 */
	public static final String DELETE = "delete"; //$NON-NLS-1$
	/**
	 * if the selection can be modified (editing report; not renaming) 
	 */
	public static final String EDIT = "edit"; //$NON-NLS-1$
	/**
	 * if the selection can have a new report added to it 
	 */
	public static final String NEWREPORT = "newreport"; //$NON-NLS-1$
	/**
	 * if the selection can have a child folder added to it
	 */
	public static final String NEWFOLDER = "newfolder"; //$NON-NLS-1$
	/**
	 * if the selection can be modified (editing report; not renaming) 
	 */
	public static final String EXPORT = "export"; //$NON-NLS-1$
	
	/**
	 * Creates a new property tester
	 */
	public ReportItemEditablePropertyTester() {
	}

	@Override
	public boolean test(Object receiver, String property, Object[] args,
			Object expectedValue) {

		if (args.length != 1){
			return false;
		}
		
		if (!UserLevelManager.INSTANCE.supportsUser(SmartDB.getCurrentEmployee(), UserLevelManager.ADMIN,UserLevelManager.ANALYST, UserLevelManager.MANAGER)){
			//data entry can't do anything with reports
			return false;
		}
		String operator = (String)args[0];
		if (receiver instanceof RootReportFolder){
			if (operator.equals(NEWFOLDER) || operator.equals(NEWREPORT)){
				if (((RootReportFolder)receiver) == RootReportFolder.CA_ROOT_FOLDER){
					//ca only modifiable by admin/managers
					return ReportManager.canModifyCaReports();
				}else{
					return true;
				}
			}
			return false;
		}
		if (receiver instanceof ReportFolder){
			ReportFolder folder = (ReportFolder)receiver;
			
			if (operator.equals(EDIT) || operator.equals(EXPORT)){
				return false;
			}
			
			
			//rename/delete/newfolder
			if (folder.getEmployee() == null){
				//ca folder
				if (ReportManager.canModifyCaReports()){
					return true;
				}else{
					return false;
				}
			}else{
				if (SmartDB.isMultipleAnalysis()){
					return Collator.getInstance().equals(folder.getEmployee().getSmartUserId(), SmartDB.getCurrentEmployee().getSmartUserId());
				}else{
					return folder.getEmployee().equals(SmartDB.getCurrentEmployee());
				}
			}
			
		}
		if (receiver instanceof Report){
			if (operator.equals(NEWFOLDER) ||  operator.equals(NEWREPORT)){
				return false;
			}
			
			Report r = (Report)receiver;
			if (operator.equals(EXPORT)){
				return true;
			}
			if (r.getShared()){
				if (ReportManager.canModifyCaReports()){
					return true;
				}else{
					return false;
				}
			}else{
				if (SmartDB.isMultipleAnalysis()){
					return Collator.getInstance().equals(r.getOwner().getSmartUserId(), SmartDB.getCurrentEmployee().getSmartUserId());
				}else{
					return r.getOwner().equals(SmartDB.getCurrentEmployee());
				}
			}
			
		}
		return false;
		
	}

}

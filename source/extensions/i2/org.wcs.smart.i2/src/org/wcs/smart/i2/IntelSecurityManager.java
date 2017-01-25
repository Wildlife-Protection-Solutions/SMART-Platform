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
package org.wcs.smart.i2;

import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.user.UserLevelManager;

public enum IntelSecurityManager {

	INSTANCE;
	
	/**
	 * Determine if the current user can edit entities records
	 * @return
	 */
	public boolean canDeleteRecord(){
		return UserLevelManager.INSTANCE.supportsUser(SmartDB.getCurrentEmployee(), IntelAnalystUserLevel.INSTANCE);
	}
	
	/**
	 * Determine if the current user can edit a query
	 * @return
	 */
	public boolean canEditQuery(){
		return true;
	}
	
	/**
	 * True if current user can link attachments to entities
	 * @return
	 */
	public boolean canLinkAttachmentsToEntities(){
		return UserLevelManager.INSTANCE.supportsUser(SmartDB.getCurrentEmployee(), IntelAnalystUserLevel.INSTANCE);
	}
	/**
	 * True if current user can link locations to entities
	 * @return
	 */
	public boolean canLinkLocationsToEntities(){
		return UserLevelManager.INSTANCE.supportsUser(SmartDB.getCurrentEmployee(), IntelAnalystUserLevel.INSTANCE);
	}
	/**
	 * Determine if the current user can edit entities records
	 * @return
	 */
	public boolean canEditEntity(){
		return UserLevelManager.INSTANCE.supportsUser(SmartDB.getCurrentEmployee(), IntelAnalystUserLevel.INSTANCE);
	}
	
	/**
	 * Determine if the current user can create entities
	 * @return
	 */
	public boolean canCreateEntity(){
		return UserLevelManager.INSTANCE.supportsUser(SmartDB.getCurrentEmployee(), IntelAnalystUserLevel.INSTANCE);
	}
	
	/**
	 * Determine if the current user can view and modify
	 * working sets
	 * 
	 * @return
	 */
	public boolean canViewWorkingSets(){
		return UserLevelManager.INSTANCE.supportsUser(SmartDB.getCurrentEmployee(), IntelAnalystUserLevel.INSTANCE);
	}
	
	/**
	 * Determine if the current user can view and modify queries
	 * 
	 * @return
	 */
	public boolean canViewQueries(){
		return UserLevelManager.INSTANCE.supportsUser(SmartDB.getCurrentEmployee(), IntelAnalystUserLevel.INSTANCE);
	}
}

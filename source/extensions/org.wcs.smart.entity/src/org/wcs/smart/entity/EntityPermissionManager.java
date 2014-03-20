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
package org.wcs.smart.entity;

import org.wcs.smart.ca.Employee.SmartUserLevel;
import org.wcs.smart.hibernate.SmartDB;

/**
 * Utility for determining which users have permissions
 * for which functions.
 * 
 * @author Emily
 *
 */
public class EntityPermissionManager {

	/**
	 * If current user can modify entities
	 * @return
	 */
	public static boolean canCreateEditDeleteEntities(){
		if (SmartDB.isMultipleAnalysis()){
			return false;
		}
		if (SmartDB.getCurrentEmployee().getSmartUserLevel() == SmartUserLevel.ANALYST){
			return false;
		}
		return true;
	}
	
	/**
	 * If current user can modify entity types
	 * @return
	 */
	public static boolean canCreateEditDeleteTypes(){
		if (SmartDB.isMultipleAnalysis()){
			return false;
		}
		return SmartDB.getCurrentEmployee().getSmartUserLevel() == SmartUserLevel.ADMIN;
	}
	
	/**
	 * If the current user can view entity sightings
	 * @return
	 */
	public static boolean canViewSightings(){
		if (SmartDB.isMultipleAnalysis()){
			return true;
		}
		return SmartDB.getCurrentEmployee().getSmartUserLevel() != SmartUserLevel.DATA_ENTRY;
	}
}

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
package org.wcs.smart.i2.security;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.model.IntelPermission;
import org.wcs.smart.i2.model.IntelProfile;
import org.wcs.smart.user.UserLevelManager;

public enum IntelSecurityManager {

	INSTANCE;
	
	/**
	 * Map from profile uuid to permission
	 */
	private volatile HashMap<UUID, Integer> permissions = null;
	
	private void loadPermissions() {
		if (permissions != null) return;
		synchronized (INSTANCE) {
			if (permissions != null) return;
			
			HashMap<UUID, Integer> temp = new HashMap<>();
			try(Session session = HibernateManager.openSession()){
				
				if (SmartDB.isMultipleAnalysis()) {
					String query = "FROM IntelPermission p WHERE id.employee.smartUserId = :userid "; //$NON-NLS-1$
					List<IntelPermission> items = session.createQuery(query, IntelPermission.class)
							.setParameter("userid", SmartDB.getCurrentEmployee().getSmartUserId()).list(); //$NON-NLS-1$
					for (IntelPermission p : items) temp.put(p.getProfile().getUuid(), p.getPermission());
				}else {
					List<IntelPermission> items = QueryFactory.buildQuery(session, IntelPermission.class, 
						new Object[] {"id.employee", SmartDB.getCurrentEmployee()}).list(); //$NON-NLS-1$
					for (IntelPermission p : items) temp.put(p.getProfile().getUuid(), p.getPermission());
				}
			}
			permissions = temp;
		}
		
	}
	
	public void clearCache() {
		synchronized (INSTANCE) {
			permissions = null;	
		}
	}
	
	private boolean supportsPermission(UUID profileUuid, int permission) {
		if (permissions == null) loadPermissions();
		
		if (!permissions.containsKey(profileUuid)) return false;
		if ((permissions.get(profileUuid) & permission) != 0) return true;
		return false;
	}
	
	private boolean supportsPermissionAny(int permission) {
		if (permissions == null) loadPermissions();
		for (Integer p : permissions.values()) {
			if ( (p.intValue() & permission) != 0 ) return true;
		}
		return false;
	}
	
	/**
	 * Determine if the current user can configure the intelligence module; this includes items such
	 * as configuring attributes, entity types, relationship types etc.
	 * @return
	 */
	public boolean canConfigure(IntelProfile p) {
		return canConfigure(p.getUuid());
	}
	public boolean canConfigure(UUID profileUuid) {
		return supportsPermission(profileUuid, IntelPermission.ADMIN);
	}
	
	
	public boolean canConfigureAny() {
		return supportsPermissionAny(IntelPermission.ADMIN);
	}
	
	
	/**
	 * Determine if the current user can view records
	 * @return
	 */
	public boolean canViewRecords(IntelProfile p){
		return canViewRecords(p.getUuid());				
	}
	
	public boolean canViewRecords(UUID profileUuid){
		return supportsPermission(profileUuid, IntelPermission.ADMIN) ||
				supportsPermission(profileUuid, IntelPermission.READ_ONLY) ||
				supportsPermission(profileUuid, IntelPermission.RECORD_EDIT_ALL) ||
				supportsPermission(profileUuid, IntelPermission.RECORD_EDIT_NOTSTATUS) ||
				supportsPermission(profileUuid, IntelPermission.RECORD_VIEW) ||
				supportsPermission(profileUuid, IntelPermission.RECORD_DELETE);

		
//		return UserLevelManager.INSTANCE.supportsUser(SmartDB.getCurrentEmployee(), IntelAnalystUserLevel.INSTANCE) ||
//				UserLevelManager.INSTANCE.supportsUser(SmartDB.getCurrentEmployee(), IntelViewRecordsUserLevel.INSTANCE) ||
//				UserLevelManager.INSTANCE.supportsUser(SmartDB.getCurrentEmployee(),  IntelEditRecordUserLevel.INSTANCE) ||
//				UserLevelManager.INSTANCE.supportsUser(SmartDB.getCurrentEmployee(),  IntelEditRecordWithStatusUserLevel.INSTANCE) ||
//				UserLevelManager.INSTANCE.supportsUser(SmartDB.getCurrentEmployee(),  IntelDeleteRecordUserLevel.INSTANCE) ||
//				UserLevelManager.INSTANCE.supportsUser(SmartDB.getCurrentEmployee(), IntelReadOnlyUserLevel.INSTANCE);
				
	}
	
	/**
	 * Determine if the current user can view entities
	 * @return
	 */
	public boolean canViewEntities(UUID profileUuid){
		return supportsPermission(profileUuid, IntelPermission.ADMIN) ||
				supportsPermission(profileUuid, IntelPermission.READ_ONLY) ||
				supportsPermission(profileUuid, IntelPermission.ENTITY_VIEW) ||
				supportsPermission(profileUuid, IntelPermission.ENTITY_EDIT) ||
				supportsPermission(profileUuid, IntelPermission.ENTITY_DELETE);
		
//		return UserLevelManager.INSTANCE.supportsUser(SmartDB.getCurrentEmployee(), IntelAnalystUserLevel.INSTANCE) ||
//				UserLevelManager.INSTANCE.supportsUser(SmartDB.getCurrentEmployee(), IntelViewEntityUserLevel.INSTANCE) ||
//				UserLevelManager.INSTANCE.supportsUser(SmartDB.getCurrentEmployee(), IntelEditEntityUserLevel.INSTANCE) ||
//				UserLevelManager.INSTANCE.supportsUser(SmartDB.getCurrentEmployee(), IntelDeleteEntityUserLevel.INSTANCE) ||
//				UserLevelManager.INSTANCE.supportsUser(SmartDB.getCurrentEmployee(), IntelReadOnlyUserLevel.INSTANCE);
		
	}
	public boolean canViewEntities(IntelProfile p){
		return canViewEntities(p.getUuid());	
	}
	
	/**
	 * Determine if the current user can delete records
	 * @return
	 */
	public boolean canDeleteRecord(IntelProfile p){
		return canDeleteRecord(p.getUuid());
	}
	public boolean canDeleteRecord(UUID profileUuid){
		return supportsPermission(profileUuid, IntelPermission.ADMIN) ||
				supportsPermission(profileUuid, IntelPermission.RECORD_DELETE);
	}
	
	/**
	 * Determine if the current user can delete entities
	 * @return
	 */
	public boolean canDeleteEntity(IntelProfile p){
		return canDeleteEntity(p.getUuid());
	}
	/**
	 * Determine if the current user can delete entities
	 * @return
	 */
	public boolean canDeleteEntity(UUID profileUuid){
		return supportsPermission(profileUuid, IntelPermission.ADMIN) ||
				supportsPermission(profileUuid, IntelPermission.ENTITY_DELETE);
	}
	/**
	 * Determine if the current user can edit working set
	 * @return
	 */
	public boolean canEditWorkingSet(){
		return supportsPermissionAny(IntelPermission.ADMIN); 
	}
	
	/**
	 * Determine if the current user can edit a query
	 * @return
	 */
	public boolean canEditQuery(){
		return supportsPermissionAny(IntelPermission.ADMIN) ||
				supportsPermissionAny(IntelPermission.QUERY);
	}
	
	/**
	 * True if current user can link attachments to entities
	 * @return
	 */
	public boolean canLinkAttachmentsToEntities(){
		return supportsPermissionAny(IntelPermission.ADMIN);
	}
	
	/**
	 * True if current user can link locations to entities
	 * @return
	 */
	public boolean canLinkLocationsToEntities(){
		return supportsPermissionAny(IntelPermission.ADMIN) ||
				supportsPermissionAny(IntelPermission.RECORD_EDIT_ALL) ||
				supportsPermissionAny(IntelPermission.RECORD_EDIT_NOTSTATUS);
	}
	/**
	 * Determine if the current user can edit entities records
	 * @return
	 */
	public boolean canEditEntity(IntelProfile p ){
		return canEditEntity(p.getUuid());
	}
	/**
	 * Determine if the current user can edit entities records
	 * @return
	 */
	public boolean canEditEntity(UUID profileUuid){
		return supportsPermission(profileUuid, IntelPermission.ADMIN) ||
				supportsPermission(profileUuid, IntelPermission.ENTITY_EDIT);
	}
	
	/**
	 * Determine if the current user can create entities
	 * @return
	 */
	public boolean canCreateEntity(IntelProfile p){
		return canCreateEntity(p.getUuid());
	}
	public boolean canCreateEntity(UUID profileUuid){
		return supportsPermission(profileUuid, IntelPermission.ADMIN) ||
				supportsPermission(profileUuid, IntelPermission.ENTITY_CREATE);
	}
	
	
	/**
	 * Determine if the current user can create query
	 * @return
	 */
	public boolean canCreateQueryAny(){
		return supportsPermissionAny(IntelPermission.ADMIN) ||
				supportsPermissionAny(IntelPermission.QUERY);
	}
	
	/**
	 * Determine if the current user can create record
	 * @return
	 */
	public boolean canCreateRecord(IntelProfile p){
		return canCreateRecord(p.getUuid());
	}
	public boolean canCreateRecord(UUID profileUuid){
		return supportsPermission(profileUuid, IntelPermission.ADMIN) ||
				supportsPermission(profileUuid, IntelPermission.RECORD_CREATE);
	}
	
	public boolean canEditRecordAny(){
		return supportsPermissionAny(IntelPermission.ADMIN) ||
				supportsPermissionAny(IntelPermission.RECORD_EDIT_ALL) ;
	}
	public boolean canCreateRecordAny(){
		return supportsPermissionAny(IntelPermission.ADMIN) ||
				supportsPermissionAny(IntelPermission.RECORD_CREATE);
	}
	
	public boolean canDeleteRecordAny(){
		return supportsPermissionAny(IntelPermission.ADMIN) ||
				supportsPermissionAny(IntelPermission.RECORD_DELETE);
	}
	
	public boolean canViewRecordAny(){
		return supportsPermissionAny(IntelPermission.ADMIN) ||
				supportsPermissionAny(IntelPermission.READ_ONLY) ||
				supportsPermissionAny(IntelPermission.RECORD_VIEW);
	}
	
	public boolean canCreateEntityAny(){
		return supportsPermissionAny(IntelPermission.ADMIN) ||
				supportsPermissionAny(IntelPermission.ENTITY_CREATE);
	}
	
	public boolean canDeleteEntityAny(){
		return supportsPermissionAny(IntelPermission.ADMIN) ||
				supportsPermissionAny(IntelPermission.ENTITY_DELETE);
	}
	
	public boolean canViewEntityAny(){
		return supportsPermissionAny(IntelPermission.ADMIN) ||
				supportsPermissionAny(IntelPermission.READ_ONLY) ||
				supportsPermissionAny(IntelPermission.ENTITY_VIEW);
	}
	public boolean canEditEntityAny(){
		return supportsPermissionAny(IntelPermission.ADMIN) ||
				supportsPermissionAny(IntelPermission.ENTITY_EDIT);
	}
	/**
	 * Determine if the current user can view and modify
	 * working sets
	 * 
	 * @return
	 */
	public boolean canViewWorkingSets(){
		return supportsPermissionAny(IntelPermission.ADMIN) ||
				supportsPermissionAny(IntelPermission.READ_ONLY);
	}
	
	/**
	 * Determine if the current user can view and modify queries
	 * 
	 * @return
	 */
	public boolean canViewQueryAny(){
		return supportsPermissionAny(IntelPermission.ADMIN) ||
				supportsPermissionAny(IntelPermission.READ_ONLY) ||
				supportsPermissionAny(IntelPermission.QUERY);
	}
	
	/**
	 * Determine if the current user can view and modify queries
	 * 
	 * @return
	 */
	public boolean canViewQuery(IntelProfile p){
		return supportsPermission(p.getUuid(), IntelPermission.ADMIN) ||
				supportsPermission(p.getUuid(), IntelPermission.READ_ONLY) ||
				supportsPermission(p.getUuid(), IntelPermission.QUERY);
	}
	
	
	/**
	 * Determine if the current user can
	 * edit the record status
	 * 
	 * @return
	 */
	public boolean canEditRecordStatus(IntelProfile p){
		return canEditRecordStatus(p.getUuid());
	}
	
	public boolean canEditRecordStatus(UUID profileUuid){
		
		return supportsPermission(profileUuid, IntelPermission.ADMIN) ||
				supportsPermission(profileUuid, IntelPermission.RECORD_EDIT_ALL);
	}
	/**
	 * Determine if the current user can
	 * edit the record 
	 * 
	 * @return
	 */
	public boolean canEditRecord(IntelProfile p){
		return canEditRecord(p.getUuid());
	}
	
	public boolean canEditRecord(UUID profileUuid){
		return supportsPermission(profileUuid, IntelPermission.ADMIN) ||
				supportsPermission(profileUuid, IntelPermission.RECORD_EDIT_NOTSTATUS) ||
				supportsPermission(profileUuid, IntelPermission.RECORD_EDIT_ALL);
		
	}
	public boolean canAccessFieldData(){
		return UserLevelManager.INSTANCE.supportsUser(SmartDB.getCurrentEmployee(), UserLevelManager.ADMIN) ||
				UserLevelManager.INSTANCE.supportsUser(SmartDB.getCurrentEmployee(), UserLevelManager.ANALYST) ||
				UserLevelManager.INSTANCE.supportsUser(SmartDB.getCurrentEmployee(),  UserLevelManager.MANAGER)||
				UserLevelManager.INSTANCE.supportsUser(SmartDB.getCurrentEmployee(),  UserLevelManager.DATA_ENTRY); 
	}
}

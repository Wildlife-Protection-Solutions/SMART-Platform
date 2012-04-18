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
package org.wcs.smart.patrol.model;

import java.io.File;
import java.io.Serializable;

import org.hibernate.EmptyInterceptor;
import org.hibernate.type.Type;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.util.SmartUtils;

/**
 * An interceptor for waypoint attachment that copies the file into the filestore
 * if necessary or deletes the file from the filestore.
 * 
 * Must be applied to any hibernate session that modifies waypoints inorder for
 * waypoint files to be removed correctly from the data store.
 *  
 * @author Emily
 * @since 1.0.0
 */
public class WaypointAttachmentInterceptor extends EmptyInterceptor{

	/**
	 * When a waypoint is deleted it also deletes the file on disk.
	 */
    public void onDelete(Object entity,
            Serializable id,
            Object[] state,
            String[] propertyNames,
            Type[] types) {
    	
    	if (entity instanceof WaypointAttachment){
    		WaypointAttachment wa = (WaypointAttachment)entity;
    		wa.getFullFile().delete();
    		wa.setWaypoint(null);
    	}
    	
    }
    
    
    /**
	 * When a waypoint is deleted it also deletes the file on disk.
	 */
    public boolean onSave(Object entity,
            Serializable id,
            Object[] state,
            String[] propertyNames,
            Type[] types) {
//    	
    	if (entity instanceof WaypointAttachment){
    		WaypointAttachment attachment = (WaypointAttachment)entity;
    		
    		if (attachment.getCopyFromLocation() != null){
    		
    			File f = new File(SmartDB.getCurrentConservationArea().getFileDataStoreLocation() + File.separator + attachment.getWaypoint().getPatrolLegDay().getPatrolLeg().getPatrol().getPatrolDatastorePath() );
    			if (!f.exists()){
    				SmartUtils.createDirectory(f);
    			}
    			File to = new File(f.getAbsoluteFile() + File.separator + attachment.getFilename());
    			int counter = 1;
    			while(to.exists()){
    				String name = (counter++) + "_" + attachment.getFilename();
    				to = new File(f.getAbsoluteFile() + File.separator + name);
    			}
    			if (!SmartUtils.copyFile(attachment.getCopyFromLocation(), to)){
    				throw new RuntimeException("Patrol modifications could not be saved because attachment could not be copied.  Ensure write permissions to directory or remove attachment.");
    			}else{
    				attachment.setFilename(to.getName());
    				attachment.setCopyFromLocation(null);
    			}
    		}
    		
    	}
    	
    	return true;
    	
    }

}

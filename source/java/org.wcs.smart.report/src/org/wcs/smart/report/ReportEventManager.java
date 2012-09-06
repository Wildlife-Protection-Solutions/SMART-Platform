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
package org.wcs.smart.report;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.birt.report.model.api.ReportDesignHandle;
import org.wcs.smart.report.in.IReportImportHandler;
import org.wcs.smart.report.model.Report;
import org.wcs.smart.report.model.ReportFolder;

/**
 * Event manager for managing report events.
 * 
 * @author egouge
 * @since 1.0.0
 */
public class ReportEventManager {

	/**
	 * Report event types
	 * 
	 */
	public enum EventType{
		REPORT_ADDED,
		REPORT_UPDATED,
		REPORT_DELETED,
		FOLDER_ADDED,
		FOLDER_UPDATED,
		FOLDER_DELETED
	};

	private  List<IReportListener> listeners = new ArrayList<IReportListener>();
	private List<IReportImportHandler> importHandlers = new ArrayList<IReportImportHandler>();
	
	private static ReportEventManager instance = null;
	
	/**
	 * Creates a new event manager
	 */
	private ReportEventManager(){
		
	}
	/**
	 * 
	 * @return the current event manager instance
	 */
	public static ReportEventManager getInstance(){
		if (instance == null){
			instance = new ReportEventManager();
		}
		return instance;
	}
	
	/**
	 * Adds a report event listener
	 * @param listener
	 */
	public void addReportListener(IReportListener listener){
		listeners.add(listener);
	}
	
	/**
	 * Removes a report event listener
	 * @param listener
	 */
	public void removeReportListener(IReportListener listener){
		listeners.remove(listener);
	}
	
	/*
	 * Fires all listeners of a given event type
	 */
	private void fireEvents(Object o, EventType type){
		List<IReportListener> values = new ArrayList<IReportListener>();
		values.addAll(listeners);
		for (IReportListener listener : values){
			listener.reportEvent(o, type);
		}
	}
	
	/**
	 * Fire report added listeners
	 * @param r report added
	 */
	public void fireReportAdded(Report r){
		fireEvents(r, EventType.REPORT_ADDED);
	}
	/**
	 * Fire report updated listeners 
	 * @param r the updated report
	 */
	public void fireReportUpdated(Report r){
		fireEvents(r, EventType.REPORT_UPDATED);
	}
	/**
	 * Fire report deleted listeners
	 * @param r the deleted report
	 */
	public void fireReportDeleted(Report r){
		fireEvents(r, EventType.REPORT_DELETED);
	}
	/**
	 * Fire folder added events
	 * @param f the folder added
	 */
	public void fireReportFolderAdded(ReportFolder f){
		fireEvents(f, EventType.FOLDER_ADDED);
	}
	/**
	 * Fire folder delete events
	 * @param f the deleted folder
	 */
	public void fireReportFolderDeleted(ReportFolder f){
		fireEvents(f, EventType.FOLDER_DELETED);
	}
	/**
	 * Fire folder modified events
	 * @param f the modified folder
	 */
	public void fireReportFolderModified(ReportFolder f){
		fireEvents(f, EventType.FOLDER_UPDATED);
	}
	
	/**
	 * Adds a report import handler
	 * @param handler
	 */
	public void addImportHandler(IReportImportHandler handler){
		this.importHandlers.add(handler);
	}
	
	/**
	 * Removes the report import handler
	 * @param handler
	 */
	public void removeImportHandler(IReportImportHandler handler){
		this.importHandlers.remove(handler);
	}
	
	/**
	 * Fires all registered report import handlers
	 * @param rdh
	 * @param oldToNewQuery
	 * @throws Exception
	 */
	public void fireReportImportHandlers(ReportDesignHandle rdh, HashMap<String, String> oldToNewQuery) throws Exception{
		for (IReportImportHandler handler: this.importHandlers){
			handler.reportImported(rdh, oldToNewQuery);
		}
	}
}

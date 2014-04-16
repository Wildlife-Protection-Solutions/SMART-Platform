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
package org.wcs.smart.patrol.internal.ui;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolLegMember;

/**
 * A table the display the patrol legs.  Used for editing patrol legs.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class PatrolLegTable {
	private static final DateFormat DATE_TIME_FORMAT = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM);
	
	private TableViewer patrolLegViewer  = null;
	private HashMap<TableViewerColumn, LegTableProvider> columns = null;
	private TableViewerColumn  pilotColumn = null;
	
	/*
	 * Table columns
	 */
	private enum LegColumn {
		LEGID(Messages.PatrolLegTable_LegId_ColumnName), 
		STARTDATE(Messages.PatrolLegTable_LegStartDate_ColumnName), 
		ENDDATE(Messages.PatrolLegTable_LegEndDate_ColumnName), 
		TRANSPORTTYPE(Messages.PatrolLegTable_LegTranportType_ColumnName), 
		LEADER(Messages.PatrolLegTable_LegLeader_ColumnName), 
		PILOT(Messages.PatrolLegTable_LegPilot_ColumnName), 
		MEMBERS(Messages.PatrolLegTable_LegMembers_ColumnName);

		private String guiName;

		private LegColumn(String guiName) {
			this.guiName = guiName;
		}
	}
	
	/*
	 * Creates a new patrol leg table
	 */
	public PatrolLegTable(){
		
	}
	/**
	 * Sets the visiblity of the pilot column.
	 * @param visible if the pilot column should be visible or not
	 */
	public void showPilotColum(boolean visible){
		if (visible){
			pilotColumn.getColumn().setWidth(100);
			pilotColumn.getColumn().setResizable(true);
		}else{
			pilotColumn.getColumn().setWidth(0);
			pilotColumn.getColumn().setResizable(false);
		}
	}
	
	/**
	 * Refreshes table.
	 */
	public void refresh(){
		if (patrolLegViewer != null){
			patrolLegViewer.refresh();
		}
	}
	
	/**
	 * 
	 * @return the underlying table viewer
	 */
	public TableViewer getTable(){
		return patrolLegViewer;
	}
	
	/**
	 * Sets the table viewer input and updates column width.
	 * @param legs list of patrol legs
	 */
	public void setInput(ArrayList<PatrolLeg> legs){
		patrolLegViewer.setInput(legs);
		
		//update column widths
		GC gc = new GC(patrolLegViewer.getTable().getDisplay());
		try{
		gc.setFont(patrolLegViewer.getTable().getFont());
		for (Iterator<?> iterator = columns.entrySet().iterator(); iterator.hasNext();) {
			Entry<TableViewerColumn, LegTableProvider> column = (Entry<TableViewerColumn, LegTableProvider>) iterator.next();
			if (column.getKey().getColumn().getWidth() == 0){
				continue;
			}
			int width = 100;
			for (Iterator<?> iter = legs.iterator(); iter.hasNext();) {
				PatrolLeg leg = (PatrolLeg) iter.next();
				width = Math.max(100, gc.textExtent(column.getValue().getText(leg)).x + 30);
			}
			column.getKey().getColumn().setWidth(Math.min(width, 500));
		}
		}finally{
			gc.dispose();
		}
	}
	/**
	 * 
	 * @return the leg table selection
	 */
	public IStructuredSelection getSelection(){
		return (IStructuredSelection) patrolLegViewer.getSelection();
	}
	

	/**
	 * Creates the table viewer
	 * @param parent the parent composite
	 * @return the created table viewer
	 */
	public TableViewer createTable(Composite parent){
		
		patrolLegViewer = new TableViewer(parent, SWT.FULL_SELECTION | SWT.BORDER );
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.widthHint = 20;
		gd.heightHint = 20;
		
		patrolLegViewer.getTable().setLayoutData(gd);
		patrolLegViewer.setContentProvider(ArrayContentProvider.getInstance());
		patrolLegViewer.getTable().setHeaderVisible(true);
		patrolLegViewer.getTable().setLinesVisible(true);
		
		columns = new HashMap<TableViewerColumn, LegTableProvider>();
		
		for (int i = 0; i < LegColumn.values().length; i ++){
			TableViewerColumn column = new TableViewerColumn(patrolLegViewer, SWT.NONE);
			column.getColumn().setResizable(true);
			column.getColumn().setMoveable(false);
			column.getColumn().setWidth(100);
			column.getColumn().setText(LegColumn.values()[i].guiName);
			LegTableProvider labelProvider = new LegTableProvider(LegColumn.values()[i]);
			column.setLabelProvider(labelProvider);
			
			if (LegColumn.values()[i] == LegColumn.PILOT){		
				pilotColumn = column;
			}
			columns.put(column, labelProvider);
		}

		return patrolLegViewer;
	}
	
	/**
	 * Label provider for patrol leg table.
	 * 
	 * @author Emily
	 * @since 1.0.0
	 */
	class LegTableProvider extends ColumnLabelProvider{
		
		private LegColumn column;
		public LegTableProvider(LegColumn column){
			this.column = column;
		}
		/**
		 * @see org.eclipse.jface.viewers.ILabelProvider#getText(java.lang.Object)
		 */
		public String getText(Object element) {
			PatrolLeg pld = (PatrolLeg)element;
			if (this.column == LegColumn.LEGID){
				return pld.getId();
			}else if (this.column == LegColumn.LEADER){
				if (pld.getLeader() == null){
					return ""; //$NON-NLS-1$
				}
				return pld.getLeader().getMember().getFullLabel();
			}else if (this.column == LegColumn.ENDDATE){
				return DATE_TIME_FORMAT.format( pld.getEndDate() );
			}else if (this.column == LegColumn.PILOT){
				if (pld.getPilot() == null){
					return ""; //$NON-NLS-1$
				}
				return pld.getPilot().getMember().getFullLabel();
			}else if (this.column == LegColumn.STARTDATE){
				return DATE_TIME_FORMAT.format(pld.getStartDate() );
			}else if (this.column == LegColumn.TRANSPORTTYPE){
				return pld.getType().getName();
			}else if (this.column == LegColumn.MEMBERS){
				StringBuilder sb = new StringBuilder();
				sb.append(pld.getMembers().size() + ": "); //$NON-NLS-1$
				for(PatrolLegMember member: pld.getMembers()){
					sb.append(member.getMember().getFullLabel());
					sb.append("; "); //$NON-NLS-1$
				}
				return sb.toString();
			}
			return ""; //$NON-NLS-1$
		}

	}
}

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
package org.wcs.smart.connect.ui.server.configure;

import java.text.SimpleDateFormat;
import java.util.List;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.connect.ConnectHibernateManager;
import org.wcs.smart.connect.model.ConnectServer;
import org.wcs.smart.connect.model.ConnectServerStatus;
import org.wcs.smart.connect.model.ConnectSyncHistoryRecord;
import org.wcs.smart.connect.replication.DerbyReplicationManager;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.util.UuidUtils;

/**
 * Dialog for displaying replication information and history.
 * 
 * @author Emily
 *
 */
public class ReplicationInfoDialog extends TitleAreaDialog {

	private Label lblServerVersion;
	private Label lblServerRevision;
	private Label lblLocalChanges;
	private Label lblEnabled;
	
	private TableViewer syncHistory;
	
	private enum SyncHistoryColumn{
		DATETIME("Date/Time", 150),
		TYPE("Type", 85),
		STATUS("Status", 55),
		STARTREVISION("Start Revision", 85),
		ENDREVISION("End Revision", 85),
		STATUSURL("Status URL", 300);
		
		private String name;
		private int size;
		
		private SyncHistoryColumn(String name, int size){
			this.name = name;
			this.size = size;
		}
		public String getGuiName(){
			return this.name;
		}
		public int getSize(){
			return this.size;
		}
		public String getValue(ConnectSyncHistoryRecord record){
			if (this == DATETIME){
				return SimpleDateFormat.getDateTimeInstance().format(record.getDatetime());
			}else if (this == TYPE){
				return record.getType().name();
			}else if (this == STATUS){
				return record.getStatus().name();
			}else if (this == STARTREVISION){
				return String.valueOf(record.getStartRevision());
			}else if (this == ENDREVISION){
				return String.valueOf(record.getEndRevision());
			}else if (this == STATUSURL){
				return record.getStatusUrl();
			}
			return null;
		}
	}
	/**
	 * Default constructor
	 */
	public ReplicationInfoDialog(Shell parent) {
		super(parent);
	}

	@Override
	public Point getInitialSize(){
		Point p = super.getInitialSize();
		p.x = 700;
		return p;
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		parent = (Composite) super.createDialogArea(parent);
		
		final Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout());
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Group g = new Group(main, SWT.FLAT );
		g.setText("Replication Information");
		g.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		g.setLayout(new GridLayout(2, false));
		
		Label l = new Label(g, SWT.NONE);
		l.setText("Replication State:");
		lblEnabled = new Label(g, SWT.NONE);
		lblEnabled .setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false,1,1));
		
		l = new Label(g, SWT.NONE);
		l.setText("Server Version:");
		lblServerVersion = new Label(g, SWT.NONE);
		lblServerVersion.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false,1,1));
		
		l = new Label(g, SWT.NONE);
		l.setText("Last Server Revision:");
		lblServerRevision = new Label(g, SWT.NONE);
		lblServerRevision.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false,1,1));
		
		l = new Label(g, SWT.NONE);
		l.setText("Local Changes:");
		lblLocalChanges = new Label(g, SWT.NONE);
		lblLocalChanges.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false,1,1));
		
		syncHistory = new TableViewer(main, SWT.FULL_SELECTION | SWT.BORDER);
		syncHistory.getTable().setHeaderVisible(true);
		syncHistory.getTable().setLinesVisible(true);
		syncHistory.setContentProvider(ArrayContentProvider.getInstance());
		syncHistory.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridData)syncHistory.getTable().getLayoutData()).heightHint = 150;
		
		for (final SyncHistoryColumn col : SyncHistoryColumn.values()){
			TableViewerColumn vcol = new TableViewerColumn(syncHistory, SWT.NONE);
			vcol.getColumn().setText(col.getGuiName());
			vcol.setLabelProvider(new ColumnLabelProvider(){
				@Override
				public String getText(Object element){
					if (element instanceof ConnectSyncHistoryRecord){
						return col.getValue((ConnectSyncHistoryRecord)element);
					}
					return super.getText(element);
				}
			});
			vcol.getColumn().setWidth(col.getSize());
		}
		
		initControls();
		
		setTitle("SMART Replication Details");
		getShell().setText("SMART Replication Details");
		setMessage("Details on replication history.");
		
		return main;
	}
	
	private void initControls(){

		Session session = HibernateManager.openSession();
		try{
			if (DerbyReplicationManager.INSTANCE.isReplicationEnabled(session)){
				lblEnabled.setText("Enabled");
			}else{
				lblEnabled.setText("Disabled");
			}
			ConnectServer server = ConnectHibernateManager.getConnectServer(session);
			
			if (server == null){
				return;
			}
			ConnectServerStatus status = (ConnectServerStatus) session.get(ConnectServerStatus.class, SmartDB.getCurrentConservationArea().getUuid());
			if (status == null){
				lblLocalChanges.setText("unknown");
				lblServerVersion.setText("unknown");
				lblServerRevision.setText("unknown");
			}else{
				lblServerVersion.setText( UuidUtils.uuidToString(status.getVersion()));
				lblServerRevision.setText( status.getServerRevision().toString() );
					
				Boolean hasChanges = DerbyReplicationManager.INSTANCE.hasLocalChanges(session);
				if (hasChanges == null){
					lblLocalChanges.setText("Error");
				}else if (hasChanges){
					lblLocalChanges.setText("Yes");
				}else{
					lblLocalChanges.setText("No");
				}	
			}
			
			List<?> input = session.createCriteria(ConnectSyncHistoryRecord.class).add(Restrictions.eq("conservationArea", server.getConservationArea())).list();
			syncHistory.setInput(input);
		
			
		}finally{
			session.close();
		}
	}
	
	
	@Override
	public boolean isResizable(){
		return true;
	}
	
}

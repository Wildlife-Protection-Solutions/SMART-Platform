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
package org.wcs.smart.ui.internal.userlog;

import java.util.Calendar;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.wcs.smart.LoginLogEntry;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.internal.Messages;



/**
 * Shows the Log of User-login activity 
 * 
 * @author jeffloun
 *
 */
public class LoginLogDialog extends Dialog {

	private TableViewer viewer;
	private LoginLogTableComparator comparator;
	
	public LoginLogDialog(Shell parentShell) {
		super(parentShell);
		setShellStyle(SWT.CLOSE | SWT.PRIMARY_MODAL | SWT.BORDER | SWT.TITLE |SWT.RESIZE);

	}
	
	@Override
	public Point getInitialSize() {
		Point p = super.getInitialSize();
		if (p.y > 500) p.y = 500;
		return p;
	}
	
	/**
	 * Create contents of the dialog.
	 */
	@Override
	public Control createDialogArea(Composite parent){
		parent = (Composite)super.createDialogArea(parent);
		createViewer(parent);
		Button button = new Button(parent, SWT.PUSH);
        button.setText(Messages.LoginLogDialog_4);
        button.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                boolean confirm = MessageDialog.openConfirm(getShell(), Messages.LoginLogDialog_5, Messages.LoginLogDialog_6);
                if(confirm == false) return;
                
                try(Session s = HibernateManager.openSession()){
                	s.beginTransaction();
	                try{
	                	Calendar cal = Calendar.getInstance();
	                	cal.add(Calendar.YEAR, -1);
	                	
	                	String hql = "delete from LoginLogEntry where loginTimestamp < :time"; //$NON-NLS-1$
						Query<?> query = s.createQuery(hql);
	               	  	query.setParameter("time",  cal.getTime()); //$NON-NLS-1$
	               	  	query.executeUpdate();
	
	                	s.getTransaction().commit();
	                } catch (Throwable t) {
	                	s.getTransaction().rollback();
	                	throw t;
	                }
               	}
                viewer.setInput(LoginLogModelProvider.INSTANCE.getLog());
                viewer.refresh();
            }
        });
        
		return parent;
	}
	
	@Override
	protected void configureShell(Shell newShell){
	  super.configureShell(newShell);
	  newShell.setText(Messages.LogOfLogins_Dialog_Title);
	}
	
    private void createViewer(Composite parent) {
        viewer = new TableViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER);
        createColumns(parent, viewer);
        final Table table = viewer.getTable();
        table.setHeaderVisible(true);
        table.setLinesVisible(true);

        viewer.setContentProvider(new ArrayContentProvider());
        viewer.setInput(LoginLogModelProvider.INSTANCE.getLog());
        
        
        // set the sorter for the table
        comparator = new LoginLogTableComparator();
        viewer.setComparator(comparator);

        comparator.setColumn(3);
        viewer.getTable().setSortDirection(SWT.DOWN);
        viewer.getTable().setSortColumn(viewer.getTable().getColumn(3));
		viewer.refresh();
		
        // define layout for the viewer
        GridData gridData = new GridData();
        gridData.verticalAlignment = GridData.FILL;
        gridData.horizontalSpan = 2;
        gridData.grabExcessHorizontalSpace = true;
        gridData.grabExcessVerticalSpace = true;
        gridData.horizontalAlignment = GridData.FILL;
        viewer.getControl().setLayoutData(gridData);

    }

    // create the columns for the table
    private void createColumns(final Composite parent, final TableViewer viewer) {
        String[] titles = { Messages.LoginLogDialog_0, Messages.LoginLogDialog_1, Messages.LoginLogDialog_2, Messages.LoginLogDialog_3 };
        int[] bounds = { 200, 150, 200, 200 }; //column widths

        // first column is for the CA name
        TableViewerColumn col = createTableViewerColumn(titles[0], bounds[0], 0);
        col.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                LoginLogEntry p = (LoginLogEntry) element;
                return p.getCaName();
            }
        });

        // second column is for the User ID
        col = createTableViewerColumn(titles[1], bounds[1], 1);
        col.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                LoginLogEntry p = (LoginLogEntry) element;
                return p.getSmartUserId();
            }
        });

        // Permission levels at the time of login
        col = createTableViewerColumn(titles[2], bounds[2], 2);
        col.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                LoginLogEntry p = (LoginLogEntry) element;
                return p.getUserLevels();
            }
        });

        // The login date and time
        col = createTableViewerColumn(titles[3], bounds[3], 3);
        col.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                LoginLogEntry p = (LoginLogEntry) element;
                return p.getLoginTimestamp().toString();
            }
        });

    }

    private TableViewerColumn createTableViewerColumn(String title, int bound, final int colNumber) {
        final TableViewerColumn viewerColumn = new TableViewerColumn(viewer, SWT.NONE);
        final TableColumn column = viewerColumn.getColumn();
        column.setText(title);
        column.setWidth(bound);
        column.setResizable(true);
        column.setMoveable(true);
        column.addSelectionListener(getSelectionAdapter(column, colNumber));
        return viewerColumn;
    }

    public void setFocus() {
        viewer.getControl().setFocus();
    }

    @Override
    protected boolean isResizable() {
        return true;
    }
    
    private SelectionAdapter getSelectionAdapter(final TableColumn column,
            final int index) {
        SelectionAdapter selectionAdapter = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                comparator.setColumn(index);
                int dir = comparator.getDirection();
                viewer.getTable().setSortDirection(dir);
                viewer.getTable().setSortColumn(column);
                viewer.refresh();
            }
        };
        return selectionAdapter;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
		// create OK and Cancel buttons by default
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
	}
}


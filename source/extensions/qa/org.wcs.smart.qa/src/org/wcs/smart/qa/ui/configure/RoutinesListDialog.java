/*
 * Copyright (C) 2017 Wildlife Conservation Society
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
package org.wcs.smart.qa.ui.configure;

import java.lang.reflect.InvocationTargetException;
import java.text.Collator;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.qa.QaPlugIn;
import org.wcs.smart.qa.RoutineExtensionManager;
import org.wcs.smart.qa.internal.Messages;
import org.wcs.smart.qa.model.IQaDataProvider;
import org.wcs.smart.qa.model.IQaRoutineType;
import org.wcs.smart.qa.model.QaRoutine;
import org.wcs.smart.qa.ui.configure.create.EditRoutineDialog;
import org.wcs.smart.qa.ui.configure.create.NewRoutineWizard;
import org.wcs.smart.ui.SmartLabelProvider;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Dialog listing all QA Routines. 
 * 
 * @author Emily
 *
 */
public class RoutinesListDialog extends TitleAreaDialog {

	private TableViewer tblRoutines;
	private Font boldFont;
	private Composite descriptionArea;
	
	private RoutineColumn sortColumn;
	private int sortDirection = 1;
	private Object lastSelection = null;
	
	private Listener resizeListener = null;
	
	private ViewerComparator sorter = new ViewerComparator(){
		@Override
		public int compare(Viewer viewer, Object e1, Object e2) {
			 if (sortColumn == null) return 0;
			 
			 String s1 = getValue(sortColumn, e1);
			 String s2 = getValue(sortColumn, e2);
			 if (s1==null) s1 = ""; //$NON-NLS-1$
			 if (s2==null) s2 = ""; //$NON-NLS-1$
			 return Collator.getInstance().compare(s1,s2) * sortDirection;
		}
	};
	
	public enum RoutineColumn{
		NAME(Messages.RoutinesListDialog_RoutineNameColumnName, Messages.RoutinesListDialog_RoutineTooltipColumnName, 260),
		TYPE(Messages.RoutinesListDialog_RoutineTypeColumnName, Messages.RoutinesListDialog_RoutineTypeTooltip, 150),
		AUTO(Messages.RoutinesListDialog_RoutineAutoExecuteColumnName, Messages.RoutinesListDialog_RoutineAutoExecuteTooltip, 100),
		DESC(Messages.RoutinesListDialog_RoutineDescriptionColumnName, Messages.RoutinesListDialog_RoutineDescriptionTooltip,10),
		PARAMETERS(Messages.RoutinesListDialog_RoutineParametersColumnName, Messages.RoutinesListDialog_RoutineParametersTooltip,10);
		
		
		public String guiName;
		public String tooltip;
		public int width;
		RoutineColumn(String guiName, String tooltip, int width){
			this.guiName = guiName;
			this.tooltip = tooltip;
			this.width = width;
		}
	}
	
	public RoutinesListDialog(Shell parentShell) {
		super(parentShell);
	}

	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CLOSE_LABEL, true);
	}

	@Override
	public Point getInitialSize(){
		return new Point(800, 400);
	}
	
	
	protected void createDescriptionPanel(){
		for (Control c : descriptionArea.getChildren()){
			c.dispose();
		}
		if (resizeListener != null){
			descriptionArea.removeListener(SWT.Resize,resizeListener);
			resizeListener = null;
		}
		
		if (tblRoutines.getSelection().isEmpty()) return;
		Object x = ((IStructuredSelection)tblRoutines.getSelection()).getFirstElement();
		if (!(x instanceof WrappedQaRoutine)) return;
		WrappedQaRoutine routine = (WrappedQaRoutine) x;
		QaRoutine r = routine.routine;
		if (r == null) return;
		
		descriptionArea.setLayout(new GridLayout());

		Label l = new Label(descriptionArea, SWT.WRAP);
		l.setFont(boldFont);
		l.setText(r.getName());
		l.setBackground(getShell().getDisplay().getSystemColor(SWT.COLOR_WHITE));
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)l.getLayoutData()).widthHint = 200;
		
		ScrolledComposite scroll = new ScrolledComposite(descriptionArea, SWT.V_SCROLL );
		scroll.setExpandVertical(true);
//		scroll.setExpandHorizontal(true);
		scroll.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		//((GridData)scroll.getLayoutData()).widthHint = 150;
		scroll.setBackground(getShell().getDisplay().getSystemColor(SWT.COLOR_WHITE));

		Composite textArea =new Composite(scroll, SWT.NONE);
		scroll.setContent(textArea);
		textArea.setLayout(new GridLayout());
		((GridLayout)textArea.getLayout()).marginWidth = 0;
		((GridLayout)textArea.getLayout()).marginHeight= 0;
		textArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		textArea.setBackground(getShell().getDisplay().getSystemColor(SWT.COLOR_WHITE));
		
		l = new Label(textArea, SWT.WRAP);
		l.setText(r.getRoutineType().getName(Locale.getDefault()));
		l.setBackground(getShell().getDisplay().getSystemColor(SWT.COLOR_WHITE));		
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)l.getLayoutData()).widthHint = 150;

		if (r.getDescription() != null && !r.getDescription().isEmpty()){
			l = new Label(textArea, SWT.WRAP);
			l.setText("\n" + Messages.RoutinesListDialog_DescriptionLbl + "\n" + r.getDescription()); //$NON-NLS-1$ //$NON-NLS-2$ 
			l.setBackground(getShell().getDisplay().getSystemColor(SWT.COLOR_WHITE));
			l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			((GridData)l.getLayoutData()).widthHint = 200;
		}
		String params = routine.parameterDescription;
		if (params != null && !params.isEmpty()){
			l = new Label(textArea, SWT.WRAP);
			l.setText("\n" + Messages.RoutinesListDialog_ParameterLbl + "\n" + params); //$NON-NLS-1$ //$NON-NLS-2$ 
			l.setBackground(getShell().getDisplay().getSystemColor(SWT.COLOR_WHITE));
			l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			((GridData)l.getLayoutData()).widthHint = 200;
		}
		
		StringBuilder sb = new StringBuilder();
		for (IQaDataProvider provider : RoutineExtensionManager.INSTANCE.getDataProviders()){
			if (provider.supportsRoutine(r.getRoutineType())){
				sb.append(provider.getName(Locale.getDefault()));
				sb.append("\n"); //$NON-NLS-1$
			}
		}
		if (sb.length() > 0){
			sb.deleteCharAt(sb.length() - 1);
			
			l = new Label(textArea, SWT.WRAP);
			l.setText("\n" + Messages.RoutinesListDialog_DataTypesLbl + "\n" + sb.toString()); //$NON-NLS-1$ //$NON-NLS-2$
			l.setBackground(getShell().getDisplay().getSystemColor(SWT.COLOR_WHITE));
			l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			((GridData)l.getLayoutData()).widthHint = 200;
		}
		
		resizeListener = new Listener(){
			@Override
			public void handleEvent(Event event) {
				if (scroll.isDisposed()){
					descriptionArea.removeListener(SWT.Resize, this);
					return;
				}
				int width = descriptionArea.getSize().x - scroll.getVerticalBar().getSize().x - 15;
				textArea.setSize(textArea.computeSize(width, SWT.DEFAULT));
				scroll.setMinSize(textArea.computeSize(width, SWT.DEFAULT));
					
			}
		};
		descriptionArea.addListener(SWT.Resize,resizeListener);
		
		getShell().layout(true, true);
		int width = descriptionArea.getSize().x - scroll.getVerticalBar().getSize().x - 15;
		textArea.setSize(textArea.computeSize(width, SWT.DEFAULT));
		scroll.setMinSize(textArea.computeSize(width, SWT.DEFAULT));
	}
	 
	
	
	@Override
	protected Control createDialogArea(Composite parent) {
		parent = (Composite)super.createDialogArea(parent);
		
		FontData fd = parent.getFont().getFontData()[0];
		fd.setStyle(SWT.BOLD);
		boldFont = new Font(parent.getDisplay(), fd);
		parent.addDisposeListener(e->boldFont.dispose());
		
		Composite outer = new Composite(parent, SWT.NONE);
		outer.setLayout(new GridLayout(2, false));
		outer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		outer.setBackground(outer.getDisplay().getSystemColor(SWT.COLOR_WHITE));

		descriptionArea = new Composite(outer, SWT.BORDER);
		descriptionArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridData)descriptionArea.getLayoutData()).widthHint = 200;
		descriptionArea.setBackground(outer.getDisplay().getSystemColor(SWT.COLOR_WHITE));
		
		Composite tableComp = new Composite(outer, SWT.NONE);
		tableComp.setLayout(new GridLayout(2, false));
		tableComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		tableComp.setBackground(outer.getDisplay().getSystemColor(SWT.COLOR_WHITE));
		((GridLayout)tableComp.getLayout()).marginWidth = 0;
		((GridLayout)tableComp.getLayout()).marginHeight= 0;
		
		tblRoutines = new TableViewer(tableComp, SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
		tblRoutines.setContentProvider(ArrayContentProvider.getInstance());
		tblRoutines.setInput(new String[]{DialogConstants.LOADING_TEXT});
		tblRoutines.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		for (RoutineColumn c : RoutineColumn.values()){
			if (c == RoutineColumn.PARAMETERS || c == RoutineColumn.DESC) continue;
			TableViewerColumn typeColumn = new TableViewerColumn(tblRoutines, SWT.NONE);
			typeColumn.getColumn().setText(c.guiName);
			typeColumn.getColumn().setToolTipText(c.tooltip);
			typeColumn.getColumn().setMoveable(true);
			typeColumn.setLabelProvider(new ColumnLabelProvider(){
				@Override
				public String getText(Object element) {
					return getValue(c, element);
				}
				
				@Override
				public Image getImage(Object element) {
					if (c == RoutineColumn.NAME){
						return QaPlugIn.getDefault().getImageRegistry().get(QaPlugIn.ICON_QA);
					}
					return null;
				}
			});
			typeColumn.getColumn().setWidth(c.width);
			
			typeColumn.getColumn().addSelectionListener(new SelectionListener() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					if (c == sortColumn){
						sortDirection = sortDirection * -1;
					}else{
						sortColumn = c;
					}
					tblRoutines.getTable().setSortColumn(typeColumn.getColumn());
					tblRoutines.getTable().setSortDirection(sortDirection == 1 ? SWT.UP : SWT.DOWN);
					tblRoutines.refresh();
				}
				
				@Override
				public void widgetDefaultSelected(SelectionEvent e) { }
			});
		}
//		tblRoutines.getTable().setLinesVisible(true);
		tblRoutines.setComparator(sorter);
		tblRoutines.getTable().setHeaderVisible(true);
		tblRoutines.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				edit();
			}
		});
		tblRoutines.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				createDescriptionPanel();
			}
		});
		
		Composite buttonPnl = new Composite(tableComp, SWT.NONE);
		buttonPnl.setLayout(new GridLayout());
		((GridLayout)buttonPnl.getLayout()).marginWidth = 0;
		((GridLayout)buttonPnl.getLayout()).marginHeight = 0;
		buttonPnl.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		buttonPnl.setBackground(outer.getDisplay().getSystemColor(SWT.COLOR_WHITE));

		
		Button btnNew = new Button(buttonPnl, SWT.PUSH);
		btnNew.setText(DialogConstants.ADD_BUTTON_TEXT);
		btnNew.addListener(SWT.Selection, e->add());
		btnNew.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Button btnEdit = new Button(buttonPnl, SWT.PUSH);
		btnEdit.setText(DialogConstants.EDIT_BUTTON_TEXT);
		btnEdit.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		btnEdit.addListener(SWT.Selection, e->edit());
		btnEdit.setEnabled(false);
		
		Button btnDelete = new Button(buttonPnl, SWT.PUSH);
		btnDelete.setText(DialogConstants.DELETE_BUTTON_TEXT);
		btnDelete.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		btnDelete.addListener(SWT.Selection, e->delete());
		btnDelete.setEnabled(false);
		
		Menu mnu = new Menu(tblRoutines.getControl());
		tblRoutines.getControl().setMenu(mnu);
		MenuItem mnuNew = new MenuItem(mnu, SWT.PUSH);
		mnuNew.setText(DialogConstants.ADD_BUTTON_TEXT);
		mnuNew.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		mnuNew.addListener(SWT.Selection, e->add());
		
		MenuItem mnuEdit = new MenuItem(mnu, SWT.PUSH);
		mnuEdit.setText(DialogConstants.EDIT_BUTTON_TEXT);
		mnuEdit.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EDIT_ICON));
		mnuEdit.addListener(SWT.Selection, e->edit());
		mnuEdit.setEnabled(false);
		
		MenuItem mnuDelete = new MenuItem(mnu, SWT.PUSH);
		mnuDelete.setText(DialogConstants.DELETE_BUTTON_TEXT);
		mnuDelete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		mnuDelete.addListener(SWT.Selection, e->delete());
		mnuDelete.setEnabled(false);

		tblRoutines.addSelectionChangedListener(e->{
			boolean enabled = false;
			if (!tblRoutines.getSelection().isEmpty()){
				if ( ((IStructuredSelection)tblRoutines.getSelection()).getFirstElement() instanceof WrappedQaRoutine){
					enabled = true;
				}
			}
			mnuDelete.setEnabled(enabled);
			mnuEdit.setEnabled(enabled);
			btnDelete.setEnabled(enabled);
			btnEdit.setEnabled(enabled);
			
		});
		
		refresh();
		
		getShell().setText(Messages.RoutinesListDialog_ShellTitle);
		setTitle(Messages.RoutinesListDialog_DialogTitle);
		setMessage(Messages.RoutinesListDialog_DialogMessage);
		return outer;
	}
	
	private void add(){
		WizardDialog ws = new WizardDialog(getShell(), new NewRoutineWizard());
		if (ws.open() == Window.OK){
			refresh();
		}
	}
	
	private void edit(){
		if (tblRoutines.getSelection().isEmpty()) return;
		Object x = ((IStructuredSelection)tblRoutines.getSelection()).getFirstElement();
		if (!(x instanceof WrappedQaRoutine)) return;
		QaRoutine r = ((WrappedQaRoutine)x).routine;
		EditRoutineDialog dialog = new EditRoutineDialog(getShell(),r);
		if (dialog.open() == Window.OK){
			refresh();
		}
	}
	
	private void delete(){
		final List<QaRoutine> toDelete = new ArrayList<QaRoutine>();
		for (Iterator<?> iterator = ((IStructuredSelection)tblRoutines.getSelection()).iterator(); iterator.hasNext();) {
			Object x = (Object) iterator.next();
			if (x instanceof WrappedQaRoutine){
				toDelete.add(((WrappedQaRoutine)x).routine);
			}
		}
		if (toDelete.isEmpty()) return;
		if (!MessageDialog.openConfirm(getShell(), Messages.RoutinesListDialog_DeleteTitle, MessageFormat.format(Messages.RoutinesListDialog_DeleteConfirmMsg, toDelete.size()))){
			return;
		}
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(getShell());
		try{
		pmd.run(true, false, new IRunnableWithProgress() {
			
			@Override
			public void run(IProgressMonitor monitor) throws InvocationTargetException,
					InterruptedException {
				monitor.beginTask(Messages.RoutinesListDialog_DeleteTaskName, toDelete.size());
				try(Session s = HibernateManager.openSession()){
					s.getTransaction().begin();
					try{
						for (QaRoutine r : toDelete){
							Query<?> q = s.createQuery("DELETE FROM QaError where qaRoutine = :r"); //$NON-NLS-1$
							q.setParameter("r", r); //$NON-NLS-1$
							q.executeUpdate();
							
							s.delete(r);
							monitor.worked(1);
						}
						
						s.getTransaction().commit();
					}catch (Exception ex){
						s.getTransaction().rollback();
						throw new InvocationTargetException(ex);
					}
				}
				monitor.done();
			}
		});
		}catch (Exception ex){
			String msg = ex.getMessage();
			if (msg == null && ex.getCause() != null) msg = ex.getCause().getMessage();
			QaPlugIn.displayLog(Messages.RoutinesListDialog_DeleteError + msg, ex);
		}
		refresh();
	}
	
	@Override
	public boolean isResizable(){
		return true;
	}
	
	
	private String getValue(RoutineColumn field, Object element){
		if (element == null) return ""; //$NON-NLS-1$
		if (!(element instanceof WrappedQaRoutine)) return element.toString();
		QaRoutine r = ((WrappedQaRoutine)element).routine;
		switch(field){
			case AUTO:
				if (r.getAutoCheck()){
					return SmartLabelProvider.BOOLEAN_TRUE_LABEL;
				}else{
					return SmartLabelProvider.BOOLEAN_FALSE_LABEL;
				}
			case NAME:
				return r.getName();
			case TYPE:
				IQaRoutineType type = r.getRoutineType();
				if (type == null) return Messages.RoutinesListDialog_Undefined;
				return type.getName(Locale.getDefault());
		}
		return ""; //$NON-NLS-1$
	}

	
	private void refresh(){
		lastSelection = null;
		if (!tblRoutines.getSelection().isEmpty())
			lastSelection = ((IStructuredSelection)tblRoutines.getSelection()).getFirstElement();
	
		tblRoutines.setInput(new String[]{DialogConstants.LOADING_TEXT});
		refreshJob.setSystem(true);
		refreshJob.schedule();
	}
	
	private Job refreshJob = new Job("refresh"){ //$NON-NLS-1$

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			List<WrappedQaRoutine> routines = new ArrayList<>();
			
			
			try(Session s = HibernateManager.openSession()){
				List<QaRoutine> thisroutines = QueryFactory.buildQuery(s, QaRoutine.class, "conservationArea", SmartDB.getCurrentConservationArea()).getResultList(); //$NON-NLS-1$
				for (QaRoutine r : thisroutines){
					r.getParameters().size();
					String parameterSummary = r.getRoutineType().getParameterSummary(r, Locale.getDefault(), s);
					routines.add(new WrappedQaRoutine(r, parameterSummary));
				}
			}
			Display.getDefault().asyncExec(()->{
				tblRoutines.setInput(routines);
				if (lastSelection == null && !routines.isEmpty()){
					tblRoutines.setSelection(new StructuredSelection(routines.get(0)));
				}else{
					tblRoutines.setSelection(new StructuredSelection(lastSelection));
				}
				
			});
			return Status.OK_STATUS;
		}
		
	};
	
	private class WrappedQaRoutine {
		QaRoutine routine;
		String parameterDescription;
		
		public WrappedQaRoutine(QaRoutine routine, String parameterDescription){
			this.routine = routine;
			this.parameterDescription = parameterDescription;
		}
	}
}

package org.wcs.smart.i2.ui.dialogs;

import java.text.Collator;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.WorkingSetManager;
import org.wcs.smart.i2.event.IntelEvents;
import org.wcs.smart.i2.model.IntelWorkingSet;
import org.wcs.smart.i2.ui.WorkingSetLabelProvider;
import org.wcs.smart.ui.TranslateSimpleListItemDialog;
import org.wcs.smart.ui.properties.DialogConstants;

public class WorkingSetListDialog extends TitleAreaDialog {

	@Inject
	private IEventBroker eventBroker;
	
	private ListViewer lstViewer;
	private IStructuredSelection lastSelection;
	private IntelWorkingSet selection;
	
	@Inject
	public WorkingSetListDialog(@Named(IServiceConstants.ACTIVE_SHELL) Shell parentShell) {
		super(parentShell);
		lastSelection = null;
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		parent = (Composite) super.createDialogArea(parent);
		parent = new Composite(parent, SWT.NONE);
		parent.setLayout(new GridLayout(2, false));
		parent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		lstViewer = new ListViewer(parent, SWT.BORDER | SWT.V_SCROLL);
		lstViewer.setLabelProvider(WorkingSetLabelProvider.INSTANCE);
		lstViewer.setContentProvider(ArrayContentProvider.getInstance());
		lstViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		lstViewer.setInput(new String[]{DialogConstants.LOADING_TEXT});
		lstViewer.addDoubleClickListener(new IDoubleClickListener() {
			
			@Override
			public void doubleClick(DoubleClickEvent event) {
				okPressed();
			}
		});
		Menu menu =  new Menu(lstViewer.getControl());
		lstViewer.getControl().setMenu(menu);
		
		MenuItem renameItem = new MenuItem(menu, SWT.PUSH);
		renameItem.setText("Rename");
//		renameItem.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.));
		renameItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				renameItem();
			}
		});
		
		MenuItem deleteItem = new MenuItem(menu, SWT.PUSH);
		deleteItem.setText(DialogConstants.DELETE_BUTTON_TEXT);
		deleteItem.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		deleteItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				deleteItem();
			}
		});
		menu.addMenuListener(new MenuListener() {
			
			@Override
			public void menuShown(MenuEvent e) {
				boolean hasSelection = getSelectedItem() != null;
				deleteItem.setEnabled(hasSelection);
				renameItem.setEnabled(hasSelection);
			}
			
			@Override
			public void menuHidden(MenuEvent e) {}
		});
		
		setTitle("Working Sets");
		getShell().setText("Working Sets");
		setMessage("Select the working set to use");
		
		loadWorkingsets.setSystem(true);
		loadWorkingsets.schedule();
		return parent;
	}
	
	private IntelWorkingSet getSelectedItem(){
		if (lstViewer.getSelection().isEmpty()) return null;
		Object x = ((IStructuredSelection)lstViewer.getSelection()).getFirstElement();
		if (x instanceof IntelWorkingSet) return (IntelWorkingSet) x;
		return null;
	}
	
	private void renameItem(){
		IntelWorkingSet toRename = getSelectedItem();
		if (toRename == null) return;
		
		Session s = HibernateManager.openSession();
		try{
			toRename = (IntelWorkingSet) s.get(IntelWorkingSet.class, toRename.getUuid());
			toRename.getNames().size();
		}finally{
			s.close();
		}
		
		TranslateSimpleListItemDialog dialog = new TranslateSimpleListItemDialog(getShell(), toRename);
		if (dialog.open() == Window.OK){
			s = HibernateManager.openSession();
			try{
				s.beginTransaction();
				s.saveOrUpdate(toRename);
				s.getTransaction().commit();
			}catch (Exception ex){
				s.getTransaction().rollback();
				Intelligence2PlugIn.displayLog(MessageFormat.format("Error renaming working set ''{0}''. {1}", toRename.getName(), ex.getMessage()), ex);
			}finally{
				s.close();
			}
			lastSelection = new StructuredSelection(toRename);
			loadWorkingsets.schedule();
		}
	}
	private void deleteItem(){
		IntelWorkingSet toDelete = getSelectedItem();
		if (toDelete == null) return;
		
		boolean deleteok = false;
		Session s = HibernateManager.openSession();
		try{
			s.beginTransaction();
			WorkingSetManager.INSTANCE.deleteWorkingSet(s, toDelete);
			s.getTransaction().commit();
			deleteok = true;
		}catch (Exception ex){
			s.getTransaction().rollback();
			Intelligence2PlugIn.displayLog(MessageFormat.format("Error deleting working set ''{0}''. {1}", toDelete.getName(), ex.getMessage()), ex);
		}finally{
			s.close();
		}
		if (deleteok) eventBroker.send(IntelEvents.WS_DELETE, toDelete);
		
		loadWorkingsets.schedule();	
	}
	
	@Override
	protected void okPressed() {
		selection = getSelectedItem();
		super.okPressed();
	}
	
	public IntelWorkingSet getSelection(){
		return this.selection;
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, true);
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
	}
	
	
	
	@Override
	public boolean isResizable(){
		return true;
	}

	Job loadWorkingsets = new Job("load working sets"){

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			Display.getDefault().syncExec(()->	lstViewer.setInput(new String[]{DialogConstants.LOADING_TEXT}));
			List<IntelWorkingSet> sets = null;
			Session s = HibernateManager.openSession();
			try{
				sets = s.createCriteria(IntelWorkingSet.class)
				.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea()))
				.list();
			}finally{
				s.close();
			}
			
			Collections.sort(sets, (a,b)->Collator.getInstance().compare(a.getName(), b.getName()));
			final List<IntelWorkingSet> newSets = sets;
			Display.getDefault().syncExec(()->	{
				lstViewer.setInput(newSets);
				if (lastSelection != null) lstViewer.setSelection(lastSelection);
			});
			
			return Status.OK_STATUS;
		}
		
	};
}

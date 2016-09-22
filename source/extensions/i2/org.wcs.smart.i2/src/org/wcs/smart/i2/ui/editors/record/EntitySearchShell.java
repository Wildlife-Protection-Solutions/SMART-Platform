package org.wcs.smart.i2.ui.editors.record;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.search.BasicEntitySearch;
import org.wcs.smart.i2.ui.EntityTypeLabelProvider;
import org.wcs.smart.ui.properties.DialogConstants;

public class EntitySearchShell implements Listener {

	private Shell shell;
	private Shell hiddenParent;
	
	private TableViewer tblEntityList;
	private Text txtSearch;
	
	private RecordEditor editor;
	
	public EntitySearchShell(Display ownerDisplay, String searchString, RecordEditor editor){
		this.editor = editor;
		hiddenParent = new Shell(ownerDisplay);
		
		shell = new Shell(hiddenParent, SWT.NO_TRIM );
		
		shell.setLayout(new GridLayout());
		((GridLayout)shell.getLayout()).marginWidth = 0;
		((GridLayout)shell.getLayout()).marginHeight = 0;
		shell.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		
		shell.addListener(SWT.Dispose, this);
		shell.addListener(SWT.Deactivate, this);

		Composite owner = new Composite(shell, SWT.BORDER);
		GridLayout gd = new GridLayout();
		
		owner.setLayout(gd);
		owner.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		owner.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		
		txtSearch = new Text(owner, SWT.SEARCH | SWT.ICON_SEARCH | SWT.ICON_CANCEL);
		txtSearch.setText(searchString.trim());
		txtSearch.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		txtSearch.addModifyListener(new ModifyListener() {
			
			@Override
			public void modifyText(ModifyEvent e) {
				searchEntityJob.schedule(500);
			}
		});
		tblEntityList = new TableViewer(owner, SWT.BORDER);
		tblEntityList.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		tblEntityList.setContentProvider(ArrayContentProvider.getInstance());
		tblEntityList.setLabelProvider(new LabelProvider(){
			@Override
			public String getText(Object element){
				if (element instanceof IntelEntity) return ((IntelEntity) element).getIdAttributeAsText();
				return super.getText(element);
			}
			
			@Override
			public Image getImage(Object element){
				if (element instanceof IntelEntity) return EntityTypeLabelProvider.INSTANCE.getImage(((IntelEntity) element).getEntityType());
				return super.getImage(element);
			}
		});
		
		tblEntityList.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				linkSelection();
				shell.close();
			}
		});
		
		Menu mnu = new Menu(tblEntityList.getTable());
		tblEntityList.getTable().setMenu(mnu);
		
		MenuItem addToRecord = new MenuItem(mnu, SWT.PUSH);
		addToRecord.setText("Add to Record");
		addToRecord.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				linkSelection();
			}
		});
		searchEntityJob.schedule();
		shell.setSize(400, 200);		
	}
	
	public Shell getShell(){
		return shell;
	}
	
	private void linkSelection(){
		Object x = ((IStructuredSelection)tblEntityList.getSelection()).getFirstElement();
		if (x instanceof IntelEntity){
			IntelEntity entity = (IntelEntity) x;
			editor.linkEntity(entity);
		}
	}

	@Override
	public void handleEvent(Event event) {
		if (event.type == SWT.Dispose){
			hiddenParent.dispose();
			return;
		}
		if (event.type == SWT.Deactivate){
			getShell().close();
			return;
		}
	}
	
	private Job searchEntityJob = new Job("search entity"){

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			String[] searchText = new String[]{""};
			Display.getDefault().syncExec(()->{
				searchText[0] = txtSearch.getText();
				tblEntityList.setInput(new String[]{DialogConstants.LOADING_TEXT});
			});
		
			
			List<IntelEntity> entities = new ArrayList<IntelEntity>();
			Session s = HibernateManager.openSession();
			try{
				BasicEntitySearch search = new BasicEntitySearch(searchText[0], 50);
				entities.addAll(search.doSearch(s));
			}finally{
				s.close();
			}
			Display.getDefault().syncExec(() -> {
				if (tblEntityList.getTable().isDisposed()) return;
				
				if (entities.isEmpty()){
					tblEntityList.setInput(new String[]{"No results found"});
				}else{
					tblEntityList.setInput(entities);
				}
				
			});
			return Status.OK_STATUS;
		}
		
	};
	

}

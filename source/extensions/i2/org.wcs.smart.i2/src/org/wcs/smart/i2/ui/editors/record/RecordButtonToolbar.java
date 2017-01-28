package org.wcs.smart.i2.ui.editors.record;

import org.eclipse.birt.core.framework.IConfigurationElement;
import org.eclipse.birt.report.designer.internal.ui.util.UIHelper;
import org.eclipse.birt.report.engine.api.EmitterInfo;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.osgi.framework.Bundle;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.birt.ui.ReportEngineManager;
import org.wcs.smart.i2.IntelSecurityManager;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.RecordManager;
import org.wcs.smart.i2.WorkingSetManager;
import org.wcs.smart.i2.birt.IntelReportManager;

public class RecordButtonToolbar extends Composite{


	private ToolItem wsetItem;
	private ToolItem deleteItem;
	private ToolItem editItem;
	private ToolItem printItem;
	private ToolItem saveItem;
	
	private RecordEditor recordEditor;
	
	public RecordButtonToolbar(Composite parent, RecordEditor editor, FormToolkit toolkit) {
		super(parent, SWT.NONE);
		setLayout(new GridLayout());
		((GridLayout)getLayout()).marginWidth = 0;
		((GridLayout)getLayout()).marginHeight = 0;
		this.recordEditor = editor;
		createBar(toolkit);
		toolkit.adapt(this);
	}
	
	public void enableWs(boolean enable){
		wsetItem.setEnabled(enable);
	}
	
	public void setEditMode(boolean editMode){		
		if (editItem.isDisposed()) return;
		editItem.setSelection(editMode);
		if (IntelSecurityManager.INSTANCE.canDeleteRecord()){
			deleteItem.setEnabled(editMode);		
		}
	}
	
	private void createBar(FormToolkit toolkit){
	
		ToolBar buttonBar = new ToolBar(this, SWT.HORIZONTAL | SWT.FLAT);
		buttonBar.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false));
				
		Menu formatsOpMenu = new Menu(getShell(), SWT.POP_UP);
		buttonBar.addListener(SWT.Dispose, e->formatsOpMenu.dispose());
		EmitterInfo pdfEmitter = null;
		for (EmitterInfo einfo : ReportEngineManager.getBirtReportEngine().getEmitterInfo()){
			if (einfo.getFormat().equalsIgnoreCase("PDF")){
				pdfEmitter = einfo;
			}
			MenuItem mi = new MenuItem(formatsOpMenu,SWT.PUSH);
			mi.setText(einfo.getFormat());
			if (einfo.getIcon() != null){
				IConfigurationElement confElem = einfo.getEmitter();
				if ( confElem != null ){
					String pluginId = confElem.getDeclaringExtension( ).getNamespace( );
					Bundle bundle = Platform.getBundle( pluginId );
					mi.setImage( UIHelper.getImage( bundle, einfo.getIcon(), false ));
				}
			}
			
			mi.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					IntelReportManager.INSTANCE.exportRecord(recordEditor.getRecord(), einfo);
				}
			});
		}
		saveItem = new ToolItem(buttonBar, SWT.PUSH);
		saveItem.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_ETOOL_SAVE_EDIT));
		saveItem.setToolTipText("save");
		saveItem.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent event){
				recordEditor.getSite().getPage().saveEditor(recordEditor, false);
			}
		});
		saveItem.setEnabled(recordEditor.isDirty());
		recordEditor.addPropertyListener((source, propId) -> {
			if (propId == IEditorPart.PROP_DIRTY){
				Display.getDefault().syncExec(()->saveItem.setEnabled(recordEditor.isDirty()));
			}
		});
		
		final EmitterInfo pdfFormat = pdfEmitter;
		printItem = new ToolItem(buttonBar, SWT.DROP_DOWN);
		printItem.setImage(Intelligence2PlugIn.getDefault().getImageRegistry().get(Intelligence2PlugIn.ICON_PDF));
		printItem.setToolTipText("print to pdf");
		printItem.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent event){
				 if (event.detail == SWT.ARROW) {
			          Rectangle rect = printItem.getBounds();
			          Point pt = new Point(rect.x, rect.y + rect.height);
			          pt = buttonBar.toDisplay(pt);
			          formatsOpMenu.setLocation(pt.x, pt.y);
			          formatsOpMenu.setVisible(true);
			    }else{
			    	if (pdfFormat != null){
			    		IntelReportManager.INSTANCE.exportRecord(recordEditor.getRecord(), pdfFormat);
			    	}else{	
			    		MessageDialog.openError(recordEditor.getSite().getShell(), "Error", "Could not find PDF exporter.");
			    	}
			    }
			}	
		});
		
		ToolItem refreshItem = new ToolItem(buttonBar, SWT.PUSH);
		refreshItem.setImage(Intelligence2PlugIn.getDefault().getImageRegistry().get(Intelligence2PlugIn.ICON_REFRESH));
		refreshItem.setToolTipText("refresh record");
		refreshItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				boolean doAction = true;
				if (recordEditor.isDirty()){
					if (!MessageDialog.openConfirm(recordEditor.getSite().getShell(), "Refresh", "Changes will be lost.  Are you sure you want to refresh?")){
						doAction = false;
					}
				}
				if (doAction){
					recordEditor.setDirty(false);
					recordEditor.refresh();				
				}
			}
		});
		
		deleteItem = new ToolItem(buttonBar, SWT.PUSH);
		deleteItem.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		deleteItem.setToolTipText("delete record");
		deleteItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (MessageDialog.openConfirm(recordEditor.getSite().getShell(), "Delete", "Are you sure you want to delete this record.  This action cannot be undone.")){
					RecordManager.INSTANCE.deleteRecord(recordEditor.getRecord(), recordEditor.getContext());
				}
			}
		});
		if (IntelSecurityManager.INSTANCE.canDeleteRecord()){
			deleteItem.setEnabled(recordEditor.getEditMode());	
		}else{
			deleteItem.setEnabled(false);
		}
		
		wsetItem = new ToolItem(buttonBar, SWT.PUSH);
		wsetItem.setImage(Intelligence2PlugIn.getDefault().getImageRegistry().get(Intelligence2PlugIn.ICON_WORKINGSET_NEW));
		wsetItem.setToolTipText("add to current working set");
		wsetItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				WorkingSetManager.INSTANCE.addToActiveWorkingSet(recordEditor.getRecord(), recordEditor.getContext());
			}
		});
		wsetItem.setEnabled(false);
		
		editItem = new ToolItem(buttonBar, SWT.CHECK);
		editItem.setImage(Intelligence2PlugIn.getDefault().getImageRegistry().get(Intelligence2PlugIn.ICON_EDIT));
		editItem.setToolTipText("enable or disable editing of record");
		editItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				recordEditor.setEditMode(!recordEditor.getEditMode());
				
			}
		});
	}
	
	

}

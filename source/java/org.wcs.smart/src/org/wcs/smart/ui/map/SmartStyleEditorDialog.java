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
package org.wcs.smart.ui.map;

import java.io.IOException;
import java.text.Collator;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.geotools.styling.Style;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.locationtech.udig.project.IStyleBlackboard;
import org.locationtech.udig.project.internal.ProjectFactory;
import org.locationtech.udig.project.internal.StyleBlackboard;
import org.locationtech.udig.project.internal.StyleEntry;
import org.locationtech.udig.style.sld.SLDContent;
import org.locationtech.udig.style.sld.editor.EditorPageManager;
import org.locationtech.udig.style.sld.editor.StyleEditorDialog;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.SmartStyle;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.udig.style.SmartLayerStyle;
import org.wcs.smart.udig.style.StyleManager;
import org.wcs.smart.ui.TranslateNamesHandler;
import org.wcs.smart.ui.TranslateSimpleListItemDialog;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.util.SmartUtils;

/**
 * A customized style dialog to allow users to select
 * styles from the smart style list. 
 * 
 * @author Emily
 *
 */
public class SmartStyleEditorDialog extends StyleEditorDialog implements Listener{

    public final static int SAVE_ID = 39;
    
	private static final String[] LOADING = new String[]{Messages.SmartSavedStylePage_LoadingText};
	private static final String NOT_SELECTED = Messages.SmartStyleEditorDialog_CustomStyleLabel;
	
	private TableViewer lstSmart;
	private Job loadStylesJob;
	private SmartStyle lastSelectedSs;
	private byte[] currentLayerSs;

	/**
	 * Image descriptor for enabled clear button.
	 */
	private static final String SAVE_ICON = "org.wsc.smart.SAVE_ICON"; //$NON-NLS-1$
	
	/**
	 * Get image descriptors for the clear button.
	 */
	static {
		ImageDescriptor descriptor = AbstractUIPlugin.imageDescriptorFromPlugin(PlatformUI.PLUGIN_ID,"$nl$/icons/full/etool16/save_edit.png"); //$NON-NLS-1$
		if (descriptor != null) {
			JFaceResources.getImageRegistry().put(SAVE_ICON, descriptor);
		}
	}
	
	public SmartStyleEditorDialog(Shell parentShell,
			EditorPageManager manager) {
		super(parentShell, manager);
	}

	
    @Override
    protected Control createButtonBar( Composite parent ) {
        Composite composite = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout(2, false);
        layout.marginWidth = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
        layout.marginHeight = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
        layout.horizontalSpacing = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
        layout.verticalSpacing = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
        composite.setLayout(layout);
        GridData data = new GridData(SWT.FILL, SWT.CENTER, true, false);
        composite.setLayoutData(data);
        composite.setFont(parent.getFont());
        // add apply/revert/close buttons
        addOkCancelRevertApplyButtons(parent, composite);

        return composite;
    }
    
    /* (non-Javadoc)
	 * @see org.eclipse.jface.Editor.EditorDialog#createTreeAreaContents(org.eclipse.swt.widgets.Composite)
	 */
	@Override
    protected Control createTreeAreaContents(Composite parent) {
		getShell().setText(Messages.SmartStyleEditorDialog_ShellTitle);
		
		Composite outer = new Composite(parent, SWT.NONE);
		outer.setLayout(new GridLayout());
		outer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		outer.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));

		// -- SMART Saved Styles area --
		Group smartArea = new Group(outer, SWT.DEFAULT);
		smartArea.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
		smartArea.setText(Messages.SmartStyleEditorDialog_SaveStylesLabel);
		smartArea.setLayout(new GridLayout());
		smartArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridLayout)smartArea.getLayout()).marginHeight = 0;
		((GridLayout)smartArea.getLayout()).marginWidth = 0;
		
		
		ToolBar comptool = new ToolBar(smartArea, SWT.FLAT);
		comptool.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
		comptool.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false));
	
		ToolItem save = new ToolItem(comptool, SWT.FLAT);
		save.setImage(JFaceResources.getImage(SAVE_ICON));
		save.setToolTipText(Messages.SmartStyleEditorDialog_saveTooltip);
		save.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e){
				saveStyle();
			}
		});
		
		ToolItem manage = new ToolItem(comptool, SWT.FLAT);
		manage.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		manage.setToolTipText(Messages.SmartStyleEditorDialog_deleteTooltip);
		manage.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				deleteStyle();
			}
		});
		ToolItem rename = new ToolItem(comptool, SWT.FLAT);
		rename.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.RENAME_ICON));
		rename.setToolTipText(Messages.SmartStyleEditorDialog_renameTooltip);
		rename.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				renameStyle();
			}
		});
		
		lstSmart = new TableViewer(smartArea, SWT.NONE);
		lstSmart.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridData)lstSmart.getControl().getLayoutData()).heightHint = 50;
		lstSmart.setContentProvider(ArrayContentProvider.getInstance());
		lstSmart.setLabelProvider(new SmartStyleLabelProvider(){
			@Override
			public String getText(Object element){
				String value = super.getText(element);
				byte[] uuid = currentLayerSs;
				if (uuid != null && element instanceof SmartStyle && Arrays.equals(((SmartStyle)element).getUuid(), uuid)){
						return value + "**"; //$NON-NLS-1$
				}else if ((uuid == null || uuid.length == 0) && element == NOT_SELECTED){
					return value + "**"; //$NON-NLS-1$
				}
				return value;
			}
		});
		lstSmart.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				updateBlackboard();
			}
		});
		
		MenuManager mm = new MenuManager();
		mm.add(new Action(DialogConstants.DELETE_BUTTON_TEXT, SmartPlugIn.getDefault().getImageRegistry().getDescriptor(SmartPlugIn.DELETE_ICON)) {
			@Override
			public void run(){
				deleteStyle();
			}
		});
		mm.add(new Action(Messages.SmartStyleEditorDialog_RenameButton, SmartPlugIn.getDefault().getImageRegistry().getDescriptor(SmartPlugIn.RENAME_ICON)) {
			@Override
			public void run(){
				renameStyle();
			}
		});
		lstSmart.getControl().setMenu(mm.createContextMenu(lstSmart.getControl()));
		
		
		// -- Custom style configuration area --
		Group leftArea = new Group(outer, SWT.DEFAULT);
		leftArea.setText(Messages.SmartStyleEditorDialog_CustomConfigLabel);
		leftArea.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
		leftArea.setFont(parent.getFont());
		GridLayout leftLayout = new GridLayout();
		leftLayout.numColumns = 1;
		leftLayout.marginHeight = 0;
		leftLayout.marginTop = IDialogConstants.VERTICAL_MARGIN;
		leftLayout.marginWidth = 0;
		leftLayout.marginLeft = IDialogConstants.HORIZONTAL_MARGIN;
		leftLayout.horizontalSpacing = 0;
		leftLayout.verticalSpacing = 0;
		leftArea.setLayout(leftLayout);
		leftArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		// Build the tree an put it into the composite.
		TreeViewer viewer = createTreeViewer(leftArea);
		setTreeViewer(viewer);
		
		updateTreeFont(JFaceResources.getDialogFont());
		viewer.getControl().getParent().setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true));
		viewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true));
		((GridData)viewer.getControl().getLayoutData()).heightHint = 50;
		
		
		// -- load styles -- 
		loadStylesJob = new LoadStylesJob(lstSmart, true);
		loadStylesJob.addJobChangeListener(new JobChangeAdapter(){
			boolean first = true;
			@Override
			public void done(IJobChangeEvent event) {
				if (first){
					//the first time we also want to create the glyph icons
					//do this as a separate job as it may take a long time
					configureIcons.setSystem(true);
					configureIcons.schedule();
					first = false;
				}
				
				//select the correct style from the list
				getShell().getDisplay().asyncExec(new Runnable(){

					@Override
					public void run() {
						updateStyleSelectionToMatchBlackboard();
					}});
			}
		});
		loadStylesJob.schedule();
		
		currentLayerSs = (byte[]) getSelectedLayer().getStyleBlackboard().get(SmartLayerStyle.STYLE_ID);
		return leftArea;
	}
	
	private void renameStyle(){
		try {
			(new TranslateNamesHandler()).execute(lstSmart.getSelection(), getShell());
			lstSmart.refresh();
		} catch (ExecutionException e1) {
			SmartPlugIn.displayLog(Messages.SmartStyleEditorDialog_renameError, e1);
		}
	}
	
	private void updateStyleSelectionToMatchBlackboard(){
		byte[] existingStyleUuid = (byte[]) getSelectedLayer().getStyleBlackboard().get(SmartLayerStyle.STYLE_ID);
		if (existingStyleUuid == null || existingStyleUuid.length == 0){
			lstSmart.setSelection(new StructuredSelection(NOT_SELECTED), true);
		}else{
			SmartStyle temp = new SmartStyle();
			temp.setUuid(existingStyleUuid);
			
			if ( ((Collection<?>)lstSmart.getInput()).contains(temp) ){
				lstSmart.setSelection(new StructuredSelection(temp), true);
			}else{
				//this style uuid no longer exists
				lstSmart.setSelection(new StructuredSelection(NOT_SELECTED), true);
			}
			
		}
	}
	
	/*
	 * updates the style blackboard with the current selected 
	 * smart style
	 */
	protected void updateBlackboard(){
//		setMessage(null);
		lastSelectedSs = null;
		
		if (lstSmart.getSelection().isEmpty()) return;
		Object x = ((IStructuredSelection)lstSmart.getSelection()).getFirstElement();
		StyleBlackboard sb = getSelectedLayer().getStyleBlackboard();
		if (x instanceof SmartStyle){
			SmartStyle style = (SmartStyle)x;
			lastSelectedSs = style;
			try {
				//clear blackboard and apply style
				sb.clear();
				StyleBlackboard parsed = StyleManager.INSTANCE.fromString(((SmartStyle) style).getStyleString());
				sb.addAll(parsed);
				sb.put(SmartLayerStyle.STYLE_ID, ((SmartStyle) style).getUuid());
			} catch (Exception ex) {
				SmartPlugIn.log(MessageFormat.format(Messages.SmartSavedStylePage_ApplyError, ex.getMessage()), ex);
			}
			getCurrentPage().refresh();
		}
	}
   

    @Override
    public void updateButtons() {
        getButton(APPLY_ID).setEnabled(true);
        getButton(REVERT_ID).setEnabled(true);
        getButton(OK_ID).setEnabled(true);
        getButton(CANCEL_ID).setEnabled(true);
        getButton(SAVE_ID).setEnabled(true);
    }
    
    private void addOkCancelRevertApplyButtons( Composite parent, Composite composite ) {
        GridLayout layout = new GridLayout(3, true);
        
        Composite compRight = new Composite(composite, SWT.NONE);
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        layout.horizontalSpacing = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
        layout.verticalSpacing = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
        
        compRight.setLayout(layout);
        compRight.setLayoutData(new GridData(SWT.END, SWT.CENTER, true, false));
        compRight.setFont(parent.getFont());

        Button saveButton = createButton(compRight, SAVE_ID, DialogConstants.SAVE_TEXT, false); 
        saveButton.setEnabled(false);
        saveButton.addListener(SWT.Selection, this);
        
        Button revertButton = createButton(compRight, REVERT_ID, Messages.SmartStyleEditorDialog_RevertButton, false); 
        revertButton.setEnabled(false);
        revertButton.addListener(SWT.Selection, this);
        
        Button applyButton = createButton(compRight, APPLY_ID, Messages.SmartStyleEditorDialog_ApplyButton, false); 
        applyButton.setEnabled(false);
        applyButton.addListener(SWT.Selection, this);
                
        new Label(compRight, SWT.NONE);
        
        Button closeButton = createButton(compRight, CANCEL_ID, IDialogConstants.CANCEL_LABEL, false); 
        closeButton.setEnabled(true);
        closeButton.addListener(SWT.Selection, this);
        
        Button okButton = createButton(compRight, OK_ID, IDialogConstants.OK_LABEL, false); 
        okButton.setEnabled(true);
        okButton.addListener(SWT.Selection, this);
        
        layout.numColumns=3;
    }
    
    
    
    /**
     * Will dispatch the even to the correct method (doApply, doRevert, etc...).
     */
    @Override
    public void handleEvent( Event event ) {
        
        int buttonId = (Integer) event.widget.getData();
        
        switch( buttonId ) {
        case StyleEditorDialog.APPLY_ID:
            doApply(true);
            break;
        case StyleEditorDialog.REVERT_ID:
            doRevert();
            break;
        case StyleEditorDialog.OK_ID:
            if( doApply(true) ){
                close();
            }
            break;
        case StyleEditorDialog.CANCEL_ID:
            close();
            break;
        case SAVE_ID:
        	saveStyle();
        	break;
        default:
            break;
        }
        
    }

    private void saveStyle(){
    	setMessage(null);
    	
    	SmartStyle newStyle = new SmartStyle();
		newStyle.setConservationArea(SmartDB.getCurrentConservationArea());
		
		String name = MessageFormat.format(Messages.SmartSavedStylePage_NewStyleDefaultName, super.getSelectedLayer().getName());
		newStyle.updateName(SmartDB.getCurrentConservationArea().getDefaultLanguage(), name);
		newStyle.updateName(SmartDB.getCurrentLanguage(), name);
		newStyle.setName(name);
		
    	SaveNameDialog nameDialog = new SaveNameDialog(getShell(), newStyle, lastSelectedSs);
    	if (nameDialog.open() == Window.CANCEL){
    		return;
    	}
    	
    	newStyle = nameDialog.getStyleToUpdate();
    	
    	doApply(false);
    	//get the current blackboard and remove the Smart Layer Style id
		IStyleBlackboard sb = ProjectFactory.eINSTANCE.createStyleBlackboard();
		for (String key : getSelectedLayer().getStyleBlackboard().keySet()){
			sb.put(key, getSelectedLayer().getStyleBlackboard().get(key));
		}
		
		
		String stringStyle = null;
		try {
			stringStyle = StyleManager.INSTANCE.asString((StyleBlackboard)sb);
		} catch (IOException ex) {
			SmartPlugIn.displayLog(MessageFormat.format(Messages.SmartSavedStylePage_SaveError1, ex.getMessage()), ex);
			return;
		}
		if (stringStyle == null){
			SmartPlugIn.displayLog(Messages.SmartSavedStylePage_SaveError2, null);
		}
		newStyle.setStyleString(stringStyle);
		
			
		
		final SmartStyle toSave = newStyle;
		lstSmart.setInput(LOADING);
		Job saveJob = new Job(Messages.SmartSavedStylePage_SaveJobName){

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				Session s = HibernateManager.openSession();
				s.beginTransaction();
				Exception ex = null;
				try {
					s.saveOrUpdate(toSave);
					s.getTransaction().commit();
				} catch (Exception ex1) {
					ex = ex1;
					s.getTransaction().rollback();
				} finally {
					s.close();
				}
				if (ex == null){
					setSmartLayerStyle(toSave);
				}
				final Exception fex = ex;
				getShell().getDisplay().syncExec(new Runnable(){

					@Override
					public void run() {
						if (fex != null){
							SmartPlugIn.displayLog(MessageFormat.format(Messages.SmartSavedStylePage_SaveError3, fex.getMessage()), fex);
							return;
						}
						setMessage(MessageFormat.format(Messages.SmartStyleEditorDialog_SavedStyle, toSave.getName()), IMessageProvider.INFORMATION);
						
						try{
							//update glyph
							((SmartStyleLabelProvider)lstSmart.getLabelProvider()).setGlyph(toSave, null);
							StyleBlackboard parsed = StyleManager.INSTANCE.fromString(toSave.getStyleString());
							Style sld = (Style)parsed.get(SLDContent.ID);
							if (sld != null){
								((SmartStyleLabelProvider)lstSmart.getLabelProvider()).setGlyph(toSave, StyleManager.INSTANCE.createImage(sld));
							}
						}catch (Exception ex){
							
						}
				}});
				
				loadStylesJob.schedule();
				return Status.OK_STATUS;
			}

		};
		saveJob.schedule();		
    }
    
    private void setSmartLayerStyle(SmartStyle style){
    	if (style == null){
    		getSelectedLayer().getStyleBlackboard().put(SmartLayerStyle.STYLE_ID, null);
    	}else{
    		getSelectedLayer().getStyleBlackboard().put(SmartLayerStyle.STYLE_ID, style.getUuid());
    	}
    }
    
    
    private boolean doApply(boolean updateLayer) {
    	
    	SmartStyle currentSelectedStyle = null;
    	if(updateLayer) currentLayerSs = null;
    	
    	IStructuredSelection sel = (IStructuredSelection) lstSmart.getSelection();
    	if (!sel.isEmpty() && sel.getFirstElement() instanceof SmartStyle){
    		currentSelectedStyle = (SmartStyle) sel.getFirstElement();
    	}else{
    		setSmartLayerStyle(null); //custom	
    	}
    	
        if( getCurrentPage() == null){
            return false;
        }
        if (getCurrentPage().performApply()) {
            setExitButtonState();
           
            
            Object selection = NOT_SELECTED;
            //compare the blackboards of the current style with the applied style
            //if they are the same we can set the smart style key; otherwise
            //this is now a custom style and we want to clear the smart style key
            if (currentSelectedStyle != null){
            	try{
            		StyleBlackboard z = StyleManager.INSTANCE.fromString(currentSelectedStyle.getStyleString());
            		StyleBlackboard y = getSelectedLayer().getStyleBlackboard();
            		
            		if (blackboardsEqual(z,y)){
            			setSmartLayerStyle(currentSelectedStyle);
            			selection = currentSelectedStyle;
            			if (updateLayer) currentLayerSs = currentSelectedStyle.getUuid();
            		}else{
            			setSmartLayerStyle(null);
            		}
            	}catch (Exception ex){
            		SmartPlugIn.log(ex.getMessage(), ex);
            	}
            }
            
            if (updateLayer){
            	getSelectedLayer().apply();
            
            	lstSmart.setSelection(new StructuredSelection(selection));
            	lstSmart.refresh();
            }
            return true;
        }
        
        return false;
    }
    
    private boolean blackboardsEqual(StyleBlackboard a, StyleBlackboard b){
    	if (a.getContent().size() != b.getContent().size()){
    		return false;
    	}
    	
    	for (StyleEntry se : a.getContent()){
    		if (se.getID().equals(SmartLayerStyle.STYLE_ID)) continue;	
    		StyleEntry fnd = null;
    		for (StyleEntry sb: b.getContent()){
    			if (se.getID().equals(sb.getID())){
    				fnd = sb;
    				break;
    			}
    		}
    		if (fnd == null) return false;
    		
    		if (!se.getMemento().equals(fnd.getMemento())) return false;
    	}
    	return true;
    }
    
    private void doRevert() {
        //return to the blackboard state before we loaded the dialog
        getSelectedLayer().revertAll();
        getSelectedLayer().apply();
        if (getSelectedLayer().getMap().getRenderManager() != null){
        	getSelectedLayer().getMap().getRenderManager().refresh(getSelectedLayer(), null);
        }
        
        //move listeners to new sld
        //StyledLayerDescriptor newSLD = this.styleEditorDialog.getSLD();
        setExitButtonState();
        
        //update button states, page updates
        getCurrentPage().refresh();
        
        updateStyleSelectionToMatchBlackboard();
    }
    
   
    private void deleteStyle(){
    	setMessage(null);
		
		final List<SmartStyle> toDelete = new ArrayList<SmartStyle>();
		for (Iterator<?> iterator = ((StructuredSelection)lstSmart.getSelection()).iterator(); iterator.hasNext();) {
			Object type = iterator.next();
			if (type instanceof SmartStyle){
				toDelete.add((SmartStyle)type);
			}
		}
		if (toDelete.size() == 0){
			setMessage(Messages.SmartStyleEditorDialog_NothingToDelete, IMessageProvider.ERROR);
			return;
		}
		
		StringBuilder sb = new StringBuilder(0);
		for (SmartStyle ss : toDelete){
			sb.append("'"); //$NON-NLS-1$
			sb.append(ss.getName());
			sb.append("',"); //$NON-NLS-1$
		}
		sb.deleteCharAt(sb.length() - 1);
		
		if (!MessageDialog.openQuestion(getShell(), Messages.SmartStyleEditorDialog_DeleteShell, 
				MessageFormat.format(Messages.SmartStyleEditorDialog_DeleteMessage, sb.toString() ))){
			return;
		}
		
		Job deleteJob = new Job(Messages.SmartSavedStylePage_DeleteJobName){

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				Session s = HibernateManager.openSession();
				s.beginTransaction();
				try{
					for (SmartStyle style : toDelete){
						s.delete(style);
					}
					s.getTransaction().commit();
					
					Display.getDefault().syncExec(new Runnable(){

						@Override
						public void run() {
							setMessage(MessageFormat.format(Messages.SmartStyleEditorDialog_DeleteCompleteMessage, toDelete.size()), IMessageProvider.INFORMATION);
						}});

				}catch (Exception ex){
					try{
						s.getTransaction().rollback();
					}catch(Exception ex2){
						SmartPlugIn.log(ex2.getMessage(), ex2);
					}
					SmartPlugIn.displayLog(MessageFormat.format(Messages.SmartSavedStylePage_DeleteError, ex.getMessage()), ex);
				}finally{
					s.close();
				}
				loadStylesJob.schedule();
				return Status.OK_STATUS;
			}
			
		};
		lstSmart.setInput(LOADING);
		deleteJob.schedule();
	}
    
    
    /*
     * job loads smart style glyphs
     */
    private Job configureIcons = new Job(Messages.SmartStyleEditorDialog_LoadingIconJobName){

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			final SmartStyleLabelProvider lprovider = (SmartStyleLabelProvider) lstSmart.getLabelProvider();
			Object[] elements = ((ArrayContentProvider)lstSmart.getContentProvider()).getElements(lstSmart.getInput());
			for (Object x : elements){
				if (x instanceof SmartStyle){
					final SmartStyle ss = (SmartStyle)x;
					Style sld = null;
					lprovider.setGlyph(ss, null);
					try{
						StyleBlackboard parsed = StyleManager.INSTANCE.fromString(ss.getStyleString());
						sld = (Style)parsed.get(SLDContent.ID);
						if (sld != null){
							final Style lsld = sld;
							getShell().getDisplay().syncExec(new Runnable(){
								@Override
								public void run() {
									lprovider.setGlyph(ss, StyleManager.INSTANCE.createImage(lsld));
									if (!lstSmart.getControl().isDisposed()) lstSmart.refresh(ss);
								}});
							
						}
					}catch (Exception ex){
					}
				}
			}

	
			
			return Status.OK_STATUS;
		}
    	
    };
    
    
    /*
     * job for loading smart styles from the database and
     * update the given viewer
     */
    private class LoadStylesJob extends Job{

    	private Viewer toUpdate;
    	private boolean addNoneOp;
    	
    	/**
    	 * 
    	 * @param toUpdate viewer to update
    	 * @param addNoneOp true if custom option should be added to style list
    	 */
		public LoadStylesJob(Viewer toUpdate, boolean addNoneOp) {
			super(Messages.SmartSavedStylePage_LoadingStyleJobName);
			this.toUpdate = toUpdate;
			this.addNoneOp = addNoneOp;
		}

		@SuppressWarnings("unchecked")
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			List<Object> items = Collections.emptyList();
			Session session = HibernateManager.openSession();
			try{
				items = session.createCriteria(SmartStyle.class)
					.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea())) //$NON-NLS-1$
					.list();
			}finally{
				session.close();
			}			
			Collections.sort(items, new Comparator<Object>() {
				@Override
				public int compare(Object o1, Object o2) {
					return Collator.getInstance().compare(
							((SmartStyle)o1).getName(),
							((SmartStyle)o2).getName());
				}
			});
			final List<Object> optionItems = new ArrayList<Object>(items);
			if (addNoneOp){
				optionItems.add(0,NOT_SELECTED);
			}
			getShell().getDisplay().syncExec(new Runnable() {
				@Override
				public void run() {
					if (toUpdate != null && !toUpdate.getControl().isDisposed()){
						toUpdate.setInput(optionItems);
					}
				}
			});
			
			return Status.OK_STATUS;
		}
    	
    }
	
	
    /*
     * Save style dialog
     */
	private class SaveNameDialog extends TitleAreaDialog{

		private Button btnCreateNew;
		private Button btnOverwrite;
		private Label lblCreateNew;
		private Label lblOverwrite;
		private ComboViewer cmbStyles;
		
		private Text txtName;
		
		private SmartStyle ss;	
		private SmartStyle update;
		private SmartStyle defaultOverwrite;
		
		protected SaveNameDialog(Shell parentShell, SmartStyle ss, SmartStyle defaultOverwrite) {
			super(parentShell);
			
			this.ss = ss;
			this.update = null;
			this.defaultOverwrite = defaultOverwrite;
		}
		
		public SmartStyle getStyleToUpdate(){
			if (update == null){
				return ss;
			}else{
				return update;
			}
		}
		@Override
		protected Control createDialogArea(Composite parent) {
			parent = (Composite) super.createDialogArea(parent);
			getShell().setText(Messages.SmartStyleEditorDialog_SaveShell);
			setTitle(Messages.SmartStyleEditorDialog_SaveTitle);
			setMessage(Messages.SmartStyleEditorDialog_SaveMessage);

			Composite main = new Composite(parent, SWT.NONE);

			main.setLayout(new GridLayout(1, false));
			main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

			SelectionAdapter enableListener = new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					enableElements();
				}
			};
			Listener validateListener = new Listener() {
				
				@Override
				public void handleEvent(Event event) {
					validate();				
				}
			};
			
			btnCreateNew = new Button(main, SWT.RADIO);
			btnCreateNew.setSelection(false);
			btnCreateNew.setText(Messages.SmartStyleEditorDialog_SaveAsNewOp);
			btnCreateNew.addSelectionListener(enableListener);
			
			Composite compNew = new Composite(main, SWT.NONE);
			compNew.setLayout(new GridLayout(3, false));
			lblCreateNew = new Label(compNew, SWT.NONE);
			
			Language defaultLanguage = SmartDB.getCurrentConservationArea().getDefaultLanguage();
			lblCreateNew.setText(Messages.SmartStyleEditorDialog_StyleNameLabel + " [" + defaultLanguage.getCode() + "]:");   //$NON-NLS-1$//$NON-NLS-2$
			lblCreateNew.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
			
			txtName = new Text(compNew, SWT.BORDER);
			txtName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			if (ss.getName() != null){
				txtName.setText(ss.getName());
			}
			txtName.addListener(SWT.Modify, validateListener);

			
			GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
			gd.horizontalIndent = 20;
			compNew.setLayoutData(gd);

			Button btnTranslate = new Button(compNew, SWT.PUSH);
			btnTranslate.setText(Messages.SmartStyleEditorDialog_TranslateButton);
			btnTranslate.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
			btnTranslate.addSelectionListener(new SelectionAdapter(){
				@Override
				public void widgetSelected(SelectionEvent e) {
					ss.updateName(SmartDB.getCurrentConservationArea().getDefaultLanguage(), txtName.getText());
					TranslateSimpleListItemDialog d = new TranslateSimpleListItemDialog(getShell(), ss);
					if (d.open() == TranslateSimpleListItemDialog.OK){
						txtName.setText(ss.findName(SmartDB.getCurrentConservationArea().getDefaultLanguage()));
					}
							
				}
			});
			
			
			btnOverwrite = new Button(main, SWT.RADIO);
			btnOverwrite.setSelection(false);
			btnOverwrite.setText(Messages.SmartStyleEditorDialog_OverwriteOp);
			btnOverwrite.addSelectionListener(enableListener);
			
			Composite compList = new Composite(main, SWT.NONE);
			compList.setLayout(new GridLayout(2, false));
			lblOverwrite = new Label(compList, SWT.NONE);
			lblOverwrite.setText(Messages.SmartStyleEditorDialog_StyleLabel);
			lblOverwrite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
			
			cmbStyles = new ComboViewer(compList, SWT.DROP_DOWN | SWT.READ_ONLY );
			cmbStyles.setLabelProvider(new SmartStyleLabelProvider());
			cmbStyles.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			cmbStyles.setContentProvider(ArrayContentProvider.getInstance());
			cmbStyles.setInput(LOADING);
			cmbStyles.getControl().addListener(SWT.Selection, validateListener);
			
			gd = new GridData(SWT.FILL, SWT.FILL, false, false);
			gd.horizontalIndent = 20;
			gd.heightHint = 100;
			compList.setLayoutData(gd);
			
			btnCreateNew.setSelection(true);
			btnOverwrite.setSelection(false);
			enableElements();
						
			Job j = new LoadStylesJob(cmbStyles, false);
			j.schedule();
			if (defaultOverwrite != null){
				j.addJobChangeListener(new JobChangeAdapter() {
					public void done(IJobChangeEvent event) {
						getShell().getDisplay().asyncExec(new Runnable(){
							@Override
							public void run() {
								if (!cmbStyles.getControl().isDisposed()){
									cmbStyles.setSelection(new StructuredSelection(defaultOverwrite));
									btnCreateNew.setSelection(false);
									btnOverwrite.setSelection(true);
									enableElements();
								}		
							}							
						});
						
					}
				});
			}
			
			return main;
		}
		
		private void enableElements(){
			boolean enabled = btnCreateNew.getSelection();
			txtName.setEnabled(enabled);
			lblCreateNew.setEnabled(enabled);
			
			cmbStyles.getControl().setEnabled(!enabled);
			lblOverwrite.setEnabled(!enabled);
			
			validate();
		}
		
		/*
		 * Validate the user input
		 */
		private void validate() {
			boolean ok = true;
			setErrorMessage(null);
			if(btnOverwrite.getSelection()){
				IStructuredSelection sel = (IStructuredSelection) cmbStyles.getSelection();
				if (sel.isEmpty() ){
					setErrorMessage(Messages.SaveBasemapDialog_Error_NoBasemap);
					ok = false;
				}
				update = (SmartStyle)sel.getFirstElement();
			}else{
				update = null;
				String name = txtName.getText();
				if (name.trim().length() == 0){
					setErrorMessage(Messages.SaveBasemapDialog_Error_NoName);
					ok = false;
				}
				if (!SmartUtils.isSimpleString(name, SmartUtils.RegExLevel.ALLOWED_CHARS_COMPLEX_REGEX, org.wcs.smart.ca.Label.MAX_LENGTH )){
					setErrorMessage(
							MessageFormat.format(
									Messages.SaveBasemapDialog_Error_BasemapName,
									new Object[]{SmartUtils.RegExLevel.ALLOWED_CHARS_COMPLEX_REGEX.textDesc, org.wcs.smart.ca.Label.MAX_LENGTH }));
					ok = false;
				}
				
				ss.updateName(SmartDB.getCurrentConservationArea().getDefaultLanguage(), name);
				ss.setName(name);
			}
			
			Button btn = getButton(IDialogConstants.OK_ID);
			if (btn != null){
				btn.setEnabled(ok);
			}
		}
	}
	
}

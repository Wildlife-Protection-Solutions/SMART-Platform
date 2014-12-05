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
package org.wcs.smart.udig;

import java.io.IOException;
import java.text.Collator;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import net.refractions.udig.project.BlackboardEvent;
import net.refractions.udig.project.IBlackboard;
import net.refractions.udig.project.IBlackboardListener;
import net.refractions.udig.project.IStyleBlackboard;
import net.refractions.udig.project.internal.ProjectFactory;
import net.refractions.udig.project.internal.StyleBlackboard;
import net.refractions.udig.style.sld.IEditorPageContainer;
import net.refractions.udig.style.sld.IStyleEditorPageContainer;
import net.refractions.udig.style.sld.editor.StyleEditorPage;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IPageChangedListener;
import org.eclipse.jface.dialogs.PageChangedEvent;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.SmartStyle;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.udig.style.SmartLayerStyle;
import org.wcs.smart.udig.style.StyleManager;
import org.wcs.smart.ui.TranslateSimpleListItemDialog;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Style page for picking a SMART Saved style or saving the current style
 * for later use.
 * 
 * @author Emily
 *
 */
public class SmartSavedStylePage extends StyleEditorPage implements SelectionListener{
	
	private static final String[] LOADING = new String[]{Messages.SmartSavedStylePage_LoadingText};
	private static final String NOT_SELECTED = Messages.SmartSavedStylePage_NoStyleOption;
	private ListViewer lstViewer;
	private ComboViewer cmbViewer;
	
	private Button btnSave;
	private Button btnDelete;
	private Button btnReplace;
	private Button btnRename;
	
	private Label lblStatusMessage;
	private Label lblStatusIcon;
	
	private Composite info;
	
	private byte[] currentUuid = null;
	
	//if we are on this page or not; if not we want to clear the saved smart style when change is made
	private boolean listenOtherStyles = true;
	
	private LabelProvider styleLabelProvider = new LabelProvider(){
		@Override
		public String getText(Object element){
			if (element instanceof SmartStyle){
				return ((SmartStyle)element).getName();
			}
			return super.getText(element);
		}
	};
	
	public SmartSavedStylePage() {		
	}

	
	@Override
	public void setContainer(IEditorPageContainer container) {
		super.setContainer(container);
		
		getSelectedLayer().getStyleBlackboard().addListener(new IBlackboardListener() {
			@Override
			public void blackBoardCleared(IBlackboard source) {
			}
			
			@Override
			public void blackBoardChanged(BlackboardEvent event) {
				if (listenOtherStyles){
					if(getStyleBlackboard() != null){
						getStyleBlackboard().remove(SmartLayerStyle.STYLE_ID);
					}
				}	
			}
		});
		
		((IStyleEditorPageContainer) container).addPageChangedListener(new IPageChangedListener() {
			public void pageChanged(PageChangedEvent event) {
				if (getEditorPage() == event.getSelectedPage()) {
					listenOtherStyles = false;
				} else {
					listenOtherStyles = true;
				}
			}
		});
	}
	
	
	
	@Override
	public boolean okToLeave() {
		return true;
	}

	@Override
	public boolean performOk() {
		return performApply();
	}

	@Override
	public boolean performApply() {
		if (cmbViewer.getSelection().isEmpty()){
			getStyleBlackboard().remove(SmartLayerStyle.STYLE_ID);
			return true;
		}else{
			Object style = ((IStructuredSelection)cmbViewer.getSelection()).getFirstElement();
			if (style instanceof SmartStyle){
				try {
					//clear blackboard and apply style
					getStyleBlackboard().clear();
					
					StyleBlackboard parsed = StyleManager.INSTANCE.fromString(((SmartStyle) style).getStyleString());
					for (String key : parsed.keySet()){
						getStyleBlackboard().put(key, parsed.get(key));
					}
					getStyleBlackboard().put(SmartLayerStyle.STYLE_ID, ((SmartStyle) style).getUuid());
				} catch (Exception ex) {
					SmartPlugIn.log(MessageFormat.format(Messages.SmartSavedStylePage_ApplyError, ex.getMessage()), ex);
					return false;
				}
			}else{
				getStyleBlackboard().remove(SmartLayerStyle.STYLE_ID);	
			}
		}
		return true;
	}

	@Override
	public void refresh() {
		updateSelection();
	}

	@Override
	public void createPageContent(Composite parent) {
		Label l;
		
		parent.setLayout(new GridLayout());
		
		Composite top = new Composite(parent, SWT.NONE);
		top.setLayout(new GridLayout(2, false));
		top.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		l = new Label(top, SWT.NONE);
		l.setText(Messages.SmartSavedStylePage_LayerStyleLabel);
		
		cmbViewer = new ComboViewer(top, SWT.DROP_DOWN | SWT.READ_ONLY);
		cmbViewer.setLabelProvider(styleLabelProvider);
		cmbViewer.setContentProvider(ArrayContentProvider.getInstance());
		cmbViewer.setInput(LOADING);
		cmbViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Composite warn = new Composite(top, SWT.NONE);
		warn.setLayout(new GridLayout(2, false));
		warn.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		l = new Label(warn, SWT.NONE);
		l.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		l.setImage(getShell().getDisplay().getSystemImage(SWT.ICON_WARNING));
		
		l = new Label(warn, SWT.WRAP);
		l.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		((GridData)l.getLayoutData()).widthHint = 100;
		l.setText(Messages.SmartSavedStylePage_Warning);
		
//		//spacer
//		l = new Label(top, SWT.NONE);
//		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Group configure = new Group(parent, SWT.NONE);
		configure.setText(Messages.SmartSavedStylePage_ConfigureLabel);
		configure.setLayout(new GridLayout(2,false));
		configure.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
				
		info = new Composite(configure, SWT.NONE);
		info.setLayout(new GridLayout(2, false));
		((GridLayout)info.getLayout()).marginWidth = 0;
		((GridLayout)info.getLayout()).marginHeight = 0;
		info.setVisible(false);
		info.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		
		lblStatusIcon = new Label(info, SWT.NONE);
		lblStatusIcon.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.INFO_ICON));
				
		lblStatusMessage = new Label(info, SWT.WRAP);
		lblStatusMessage.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		lstViewer = new ListViewer(configure, SWT.SINGLE | SWT.BORDER);
		lstViewer.setContentProvider(ArrayContentProvider.getInstance());
		lstViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		lstViewer.setLabelProvider(styleLabelProvider);
		lstViewer.setInput(LOADING);
				
		Composite buttonPanel = new Composite(configure, SWT.NONE);
		buttonPanel.setLayout(new GridLayout());
		buttonPanel.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		
		btnSave = new Button(buttonPanel, SWT.PUSH);
		btnSave.setText(Messages.SmartSavedStylePage_SaveNewButton);
		btnSave.addSelectionListener(this);
		btnSave.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		btnSave.setToolTipText(Messages.SmartSavedStylePage_SaveNewTooltip);
		
		btnReplace = new Button(buttonPanel, SWT.PUSH);
		btnReplace.setText(Messages.SmartSavedStylePage_ReplaceButton);
		btnReplace.addSelectionListener(this);
		btnReplace.setToolTipText(Messages.SmartSavedStylePage_replaceTooltip);
		btnReplace.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		btnDelete = new Button(buttonPanel, SWT.PUSH);
		btnDelete.setText(DialogConstants.DELETE_BUTTON_TEXT);
		btnDelete.addSelectionListener(this);
		btnDelete.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		btnDelete.setToolTipText(Messages.SmartSavedStylePage_DeleteTooltip);
		
		btnRename = new Button(buttonPanel, SWT.PUSH);
		btnRename.setText(Messages.SmartSavedStylePage_RenameButton);
		btnRename.addSelectionListener(this);
		btnRename.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		btnRename.setToolTipText(Messages.SmartSavedStylePage_Renametooltip);
		
		
		loadStylesJob.schedule();

	}

	@Override
	public String getLabel() {
		return Messages.SmartSavedStylePage_Title;
	}

	@Override
	public boolean performCancel() {
		return true;
	}

	@Override
	public String getErrorMessage() {
		return null;
	}

	@Override
	public void gotFocus() {
		refresh();
	}

	@Override
	public void styleChanged(Object source) {
		refresh();
	}

	private StyleBlackboard getStyleBlackboard(){
		if (getContainer() != null){
			return super.getSelectedLayer().getStyleBlackboard();
		}
		return null;
	}
	
	private void setInfoMessage(int type, String infoMessage){
		if (infoMessage == null){
			info.setVisible(false);
		}else{
			this.lblStatusMessage.setText(infoMessage);
		
			if (type == SWT.ICON_ERROR){
				lblStatusIcon.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ERROR_ICON));
			}else if (type == SWT.ICON_INFORMATION){
				lblStatusIcon.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.INFO_ICON));
			}
				
			info.setVisible(true);
		}
	}
	/*
	 * updates the selected style combo list viewer with the current value on the 
	 * style blackboard
	 */
	private void updateSelection(){
		currentUuid = null;
		currentUuid = (byte[]) getStyleBlackboard().get(SmartLayerStyle.STYLE_ID);
		
		if (currentUuid == null){
			cmbViewer.setSelection(new StructuredSelection(NOT_SELECTED));
		}else{
			//find SmartStyle that matched the uuid
			byte[] thisUuid = currentUuid;
			boolean found = false;
			if (thisUuid != null){
				Object[] values = ((ArrayContentProvider)cmbViewer.getContentProvider()).getElements(cmbViewer.getInput());
				for (Object v : values){
					if (v instanceof SmartStyle && 
							Arrays.equals(thisUuid, ((SmartStyle)v).getUuid())){
						cmbViewer.setSelection(new StructuredSelection(v));
						found = true;
						break;
					}
				}
			}
			if (!found){
				cmbViewer.setSelection(new StructuredSelection(NOT_SELECTED));
			}
		}
	}
	
	/*
	 * saves the current style blackboard as a new smart style
	 */
	private void saveStyle(){
		setInfoMessage(SWT.ICON_ERROR, null);
		//get the current blackboard and remove the Smart Layer Style id
		IStyleBlackboard sb = ProjectFactory.eINSTANCE.createStyleBlackboard();
		for (String key : getStyleBlackboard().keySet()){
			sb.put(key, getStyleBlackboard().get(key));
		}
		sb.remove(SmartLayerStyle.STYLE_ID);
		
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
		
		final SmartStyle newStyle = new SmartStyle();
		newStyle.setConservationArea(SmartDB.getCurrentConservationArea());
		newStyle.setStyleString(stringStyle);
			
		String name = MessageFormat.format(Messages.SmartSavedStylePage_NewStyleDefaultName, super.getSelectedLayer().getName());
		newStyle.updateName(SmartDB.getCurrentConservationArea().getDefaultLanguage(), name);
		newStyle.updateName(SmartDB.getCurrentLanguage(), name);
			
			
		lstViewer.setInput(LOADING);
		Job saveJob = new Job(Messages.SmartSavedStylePage_SaveJobName){

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				Session s = HibernateManager.openSession();
				s.beginTransaction();
				try {
					s.save(newStyle);
					s.getTransaction().commit();
					
					Display.getDefault().syncExec(new Runnable(){

						@Override
						public void run() {
							setInfoMessage(SWT.ICON_INFORMATION, MessageFormat.format("New style saved:''{0}''", newStyle.getName()));
						}});

					
				} catch (Exception ex) {
					try {
						s.getTransaction().rollback();
					} catch (Exception ex2) {
						SmartPlugIn.log(ex2.getMessage(), ex2);
					}
					SmartPlugIn.displayLog(MessageFormat.format(Messages.SmartSavedStylePage_SaveError3, ex.getMessage()), ex);
				} finally {
					s.close();
				}
				loadStylesJob.schedule();
				return Status.OK_STATUS;
			}

		};
		saveJob.schedule();		
	}
	
	/*
	 * deletes all selected styles
	 */
	private void deleteStyle(){
		setInfoMessage(SWT.ICON_ERROR, null);
		final List<SmartStyle> toDelete = new ArrayList<SmartStyle>();
		for (Iterator<?> iterator = ((StructuredSelection)lstViewer.getSelection()).iterator(); iterator.hasNext();) {
			Object type = iterator.next();
			if (type instanceof SmartStyle){
				toDelete.add((SmartStyle)type);
			}
		}
		if (toDelete.size() == 0){
			setInfoMessage(SWT.ICON_ERROR, "Nothing selected to delete");
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
							setInfoMessage(SWT.ICON_INFORMATION, MessageFormat.format("{0} styles deleted", toDelete.size()));
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
		lstViewer.setInput(LOADING);
		deleteJob.schedule();
		
	}

	private SmartStyle findFirstSelectedStyle(){
		for (Iterator<?> iterator = ((StructuredSelection)lstViewer.getSelection()).iterator(); iterator.hasNext();) {
			Object type = iterator.next();
			if (type instanceof SmartStyle){
				return (SmartStyle)type;
			}
		}
		return null;
	}
	
	/*
	 * replaces the first selected style in the list with the style on the blackboard
	 */
	private void replaceStyle(){
		setInfoMessage(SWT.ICON_ERROR, null);
		final SmartStyle toUpdate = findFirstSelectedStyle();
		if (toUpdate == null){
			setInfoMessage(SWT.ICON_ERROR,"No style selected to update");
			return;
		}
	
		Job replaceJob = new Job(Messages.SmartSavedStylePage_UpdateJobName){

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				Session s = HibernateManager.openSession();
				s.beginTransaction();
				try{
					String styleString = StyleManager.INSTANCE.asString(getStyleBlackboard());
					toUpdate.setStyleString(styleString);
					s.saveOrUpdate(toUpdate);
					s.getTransaction().commit();
					
					Display.getDefault().syncExec(new Runnable(){

						@Override
						public void run() {
							setInfoMessage(SWT.ICON_INFORMATION, MessageFormat.format("Style ''{0}'' updated", toUpdate.getName()));
						}});

				}catch (Exception ex){
					try{
						s.getTransaction().rollback();
					}catch(Exception ex2){
						SmartPlugIn.log(ex2.getMessage(), ex2);
					}
					SmartPlugIn.displayLog(MessageFormat.format(Messages.SmartSavedStylePage_UpdateError, ex.getMessage()), ex);
				}finally{
					s.close();
				}
				loadStylesJob.schedule();
				return Status.OK_STATUS;
			}
			
		};
		lstViewer.setInput(LOADING);
		replaceJob.schedule();
	}
	
	public void rename(){
		setInfoMessage(SWT.ICON_INFORMATION, null);
		SmartStyle ss = findFirstSelectedStyle();
		if (ss == null) return;
		
		Session s = HibernateManager.openSession();
		try{
			s.beginTransaction();
			s.saveOrUpdate(ss);
			TranslateSimpleListItemDialog dialog = new TranslateSimpleListItemDialog(getShell(), ss);
			if (dialog.open() == TranslateSimpleListItemDialog.OK){
				s.getTransaction().commit();
			}else{
				s.getTransaction().rollback();
			}
		}catch (Exception ex){
			SmartPlugIn.displayLog(Messages.TranslateNamesHandler_Error_TranslatingName, ex);
		}finally{
			s.close();
		}
		cmbViewer.refresh();
		lstViewer.refresh();
	}

	@Override
	public void widgetDefaultSelected(SelectionEvent e) {
		
	}
	
	

	@Override
	public void widgetSelected(SelectionEvent e) {
		if (e.widget == btnSave){
			saveStyle();
		}else if (e.widget == btnDelete){
			deleteStyle();
		}else if (e.widget == btnReplace){
			replaceStyle();
		}else if (e.widget == btnRename){
			rename();
		}
	}

	
	Job loadStylesJob = new Job(Messages.SmartSavedStylePage_LoadingStyleJobName){
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
			final List<Object> lItems = items;
			final List<Object> optionItems = new ArrayList<Object>(items);
			optionItems.add(0,NOT_SELECTED);
			Display.getDefault().syncExec(new Runnable() {
				@Override
				public void run() {
					if (!lstViewer.getControl().isDisposed()){
						lstViewer.setInput(lItems);
						cmbViewer.setInput(optionItems);
						updateSelection();
					}
				}
			});
			
			return Status.OK_STATUS;
		}
	};
}

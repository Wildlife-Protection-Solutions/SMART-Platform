/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.i2.ui.views;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.tools.compat.parts.DIViewPart;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.e4.ui.di.Persist;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.e4.ui.model.application.ui.MDirtyable;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.UIEvents;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.ISaveablePart;
import org.eclipse.ui.IViewPart;
import org.osgi.service.event.Event;
import org.wcs.smart.PerspectiveEditorTracker;
import org.wcs.smart.i2.IntelSecurityManager;
import org.wcs.smart.i2.event.IntelEvents;
import org.wcs.smart.i2.model.IntelRecord;
import org.wcs.smart.i2.ui.IntelDataAnalysisPerspective;
import org.wcs.smart.i2.ui.IntelDataAssessmentPerspective;
import org.wcs.smart.i2.ui.dialogs.NewEntityDialog;
import org.wcs.smart.i2.ui.editors.record.EntitySearchShell;
import org.wcs.smart.i2.ui.editors.record.RecordEditor;

/**
 * View for display record narratives or scratchpads.  This are open only from an record editor
 * and are linked directly to the editor;
 * 
 * @author Emily
 *
 */
public class RecordNarrativeView {

	public static final String ID = "org.wcs.smart.i2.record.editor.narrative";
	
	public static final String TYPE_KEY = ID + ".type";
	public static final String EDITOR_KEY = ID + ".editor";
	
	public enum FieldType{NARRATIVE,SCRATCHPAD};
	
	private RecordEditor recordEditor;
	private FieldType type;
	
	private Label lblHeader;
	private Text txt;
	
	@Inject
	private Shell parentShell;
	@Inject
	private IViewPart viewPart;
	@Inject
	private MPart part;
	@Inject
	private EPartService ePartService;
	@Inject
	private MDirtyable dirty;
	
	private ModifyListener textListener = new ModifyListener() {
		@Override
		public void modifyText(ModifyEvent e) {
			recordEditor.updateText(type, txt.getText());
			recordEditor.setDirty(true);
			setDirty(true);
		}
	};
	
	@Inject
	@Optional
	public void partLifeCycle(@UIEventTopic(IntelEvents.RECORD_SAVED) IntelRecord r){
		if (r.equals(recordEditor.getRecord())){
			setDirty(false);
		}
	}
	@Inject
	@Optional
	public void partClosed(@UIEventTopic(UIEvents.UIElement.TOPIC_WIDGET) Event event){
		if (event.getProperty(UIEvents.EventTags.NEW_VALUE)==null &&
				event.getProperty(UIEvents.EventTags.ELEMENT).equals(recordEditor.getContext().get(MPart.class))){
			//close me
			ePartService.hidePart(part, true);
		}
		
	}
	
	public void setEditMode(boolean editMode){
		txt.setEditable(editMode);
	}
	
	public boolean isDisposed(){
		return txt.isDisposed();
	}
	private void setDirty(boolean isDirty){
		this.dirty.setDirty(isDirty);
		((RecordsNarrativeViewWrapper)viewPart).setDirty();
	}
	
	@PostConstruct
	public void createPartControl(final Composite parent) {
		
//		part.getTags().add(EPartService.REMOVE_ON_HIDE_TAG);
		if (!part.getTags().contains(PerspectiveEditorTracker.EDITOR_TAG)) part.getTags().add(PerspectiveEditorTracker.EDITOR_TAG);
		if (!part.getTags().contains(IntelDataAssessmentPerspective.ID)) part.getTags().add(IntelDataAssessmentPerspective.ID);
		if (!part.getTags().contains(IntelDataAnalysisPerspective.ID)) part.getTags().add(IntelDataAnalysisPerspective.ID);
		part.getTags().remove("View");
		
		this.type = (FieldType) part.getTransientData().get(TYPE_KEY);
		this.recordEditor = (RecordEditor) part.getTransientData().get(EDITOR_KEY);
		
		if (type == null || recordEditor == null) throw new IllegalStateException("No type or intelligence record provided.");
		
		parent.setLayout(new GridLayout());
		parent.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
		
		lblHeader = new Label(parent, SWT.NONE);
		StringBuilder sb = new StringBuilder();
		sb.append(recordEditor.getRecord().getTitle());
		sb.append(" - ");
		sb.append(type == FieldType.NARRATIVE ? "Narrative" : "Scratchpad");
		
		lblHeader.setText(sb.toString());
		lblHeader.setBackground(lblHeader.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
		FontData fd = lblHeader.getFont().getFontData()[0];
		fd.setStyle(SWT.BOLD);
		fd.setHeight(fd.getHeight() + 1);
		Font headerFont = new Font(parent.getShell().getDisplay(), fd);
		lblHeader.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				headerFont.dispose();	
			}
		});
		lblHeader.setFont(headerFont);
		lblHeader.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		txt = new Text(parent, SWT.MULTI | SWT.WRAP | SWT.V_SCROLL | SWT.BORDER);
		txt.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		if (type == FieldType.NARRATIVE){
			if (recordEditor.getRecord().getDescription() == null){
				txt.setText("");
			}else{
				txt.setText(recordEditor.getRecord().getDescription());
			}
		}else if (type == FieldType.SCRATCHPAD){
			if (recordEditor.getRecord().getComment() == null){
				txt.setText("");
			}else{
				txt.setText(recordEditor.getRecord().getComment());
			}
		}
		if (recordEditor.getEditMode()){
			txt.addModifyListener(textListener);
			
			createMenu(txt);
		}else{
			txt.setEditable(false);
		}
		txt.setBackground(txt.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
	}
	
	public void updateText(String text){
		txt.removeModifyListener(textListener);
		try{
			if (text == null) text = "";
			txt.setText(text);
		}finally{
			txt.addModifyListener(textListener);
		}
		setDirty(true);
	}
	
	//cannot be called inside createPartControl as it doesn't work in there
	public void configureName(){
		part.setLabel(lblHeader.getText());
	}
	
	@Persist
	public void save(){
		setDirty(false);
		recordEditor.getSite().getPage().saveEditor(recordEditor,  false);
	}
	
	@Focus
	public void setFocus() {
		txt.setFocus();
	}

	@PreDestroy
	public void dispose() {
	}
	
	
	private void createMenu(Text text){
		Menu menu = new Menu(text);
		text.setMenu(menu);
		
		MenuItem search = new MenuItem(menu, SWT.CASCADE);
		search.setText("Search Entity -> ");
		search.addSelectionListener(new SelectionAdapter() {			
			@Override
			public void widgetSelected(SelectionEvent e) {				
				EntitySearchShell shell = new EntitySearchShell(parentShell, text.getSelectionText(), recordEditor);
				shell.open(Display.getDefault().getCursorLocation());
			}
		});
		
		if (IntelSecurityManager.INSTANCE.canCreateEntity()){
			MenuItem newEntity = new MenuItem(menu, SWT.CASCADE);
			newEntity.setText("New Entity ... ");
			newEntity.addSelectionListener(new SelectionAdapter() {			
				@Override
				public void widgetSelected(SelectionEvent e) {				
					NewEntityDialog dialog = new NewEntityDialog(parentShell);
					ContextInjectionFactory.inject(dialog, recordEditor.getContext());
					if (dialog.open() == NewEntityDialog.OK){
						recordEditor.linkEntity(dialog.getNewEntity());
					}
				}
			});
		}
		
		new MenuItem(menu, SWT.SEPARATOR);
		
		MenuItem cut = new MenuItem(menu, SWT.PUSH);
		cut.setText("Cut");
		cut.addSelectionListener(new SelectionAdapter() {			
			@Override
			public void widgetSelected(SelectionEvent e) {
				text.cut();
			}
		});
		MenuItem copy = new MenuItem(menu, SWT.PUSH);
		copy.setText("Copy");
		copy.addSelectionListener(new SelectionAdapter() {			
			@Override
			public void widgetSelected(SelectionEvent e) {
				text.copy();
			}
		});
		MenuItem paste = new MenuItem(menu, SWT.PUSH);
		paste.setText("Paste");
		paste.addSelectionListener(new SelectionAdapter() {			
			@Override
			public void widgetSelected(SelectionEvent e) {
				text.paste();
			}
		});
		
		MenuItem delete = new MenuItem(menu, SWT.PUSH);
		delete.setText("Delete");
		delete.addSelectionListener(new SelectionAdapter() {			
			@Override
			public void widgetSelected(SelectionEvent e) {
				Point sel = text.getSelection();
				int from = sel.x;
				int to = sel.y;
				text.setText(text.getText().substring(0, from) + text.getText().substring(to));
			}
		});
		
		new MenuItem(menu, SWT.SEPARATOR);

		MenuItem selectAll = new MenuItem(menu, SWT.PUSH);
		selectAll.setText("Select All");
		selectAll.addSelectionListener(new SelectionAdapter() {			
			@Override
			public void widgetSelected(SelectionEvent e) {
				text.selectAll();
			}
		});
		
		menu.addMenuListener(new MenuListener() {
			
			@Override
			public void menuShown(MenuEvent e) {
				delete.setEnabled(!text.getSelectionText().isEmpty());
				cut.setEnabled(!text.getSelectionText().isEmpty());
				copy.setEnabled(!text.getSelectionText().isEmpty());
			}
			
			@Override
			public void menuHidden(MenuEvent e) {
				
			}
		});
	}
	
	
	public static class RecordsNarrativeViewWrapper extends DIViewPart<RecordNarrativeView> implements ISaveablePart {
		public RecordsNarrativeViewWrapper() {
			super(RecordNarrativeView.class);
		}
		
		/* to allow save function to work on this view */
		@Override
		public void doSave(IProgressMonitor monitor) {
			getComponent().save();
		}
		
		@Override
		public void doSaveAs() {
		}
		@Override
		public boolean isDirty() {
			return getComponent().dirty.isDirty();
		}
		public void setDirty(){
			firePropertyChange(PROP_DIRTY);
		}
		@Override
		public boolean isSaveAsAllowed() {
			return false;
		}
		@Override
		public boolean isSaveOnCloseNeeded() {
			return false;
		}
	}
	
	
}

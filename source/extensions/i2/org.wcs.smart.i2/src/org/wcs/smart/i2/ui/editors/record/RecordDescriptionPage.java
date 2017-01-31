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
package org.wcs.smart.i2.ui.editors.record;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.part.EditorPart;
import org.wcs.smart.i2.IntelSecurityManager;
import org.wcs.smart.i2.WorkingSetManager;
import org.wcs.smart.i2.ui.SmartSection;
import org.wcs.smart.i2.ui.dialogs.NewEntityDialog;
import org.wcs.smart.i2.ui.views.RecordNarrativeView;
import org.wcs.smart.i2.ui.views.RecordNarrativeView.FieldType;

/**
 * Record editor page for description and scratchpad fields 
 * 
 * @author Emily
 *
 */
public class RecordDescriptionPage extends EditorPart{

	private FormToolkit  toolkit;

	private RecordButtonToolbar buttontoolbar;
	
	private RecordEditor recordEditor;
	
	private SashForm sashForm;
	
	private Composite narrativePart;
	private Composite scratchpadPart;
	private Text txtDescription;
	private Text txtScratchpad;
	
	
	public RecordDescriptionPage(RecordEditor parent){
		this.recordEditor =  parent;
	}
	
	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		super.setSite(site);
	}

	public void enableWs(boolean enable){
		buttontoolbar.enableWs(enable);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void createPartControl(Composite parent) {
		
		toolkit = new FormToolkit(parent.getDisplay());
		
		parent.setLayout(new GridLayout());
		((GridLayout)parent.getLayout()).marginWidth= 0;
		((GridLayout)parent.getLayout()).marginHeight = 0;
		((GridLayout)parent.getLayout()).verticalSpacing= 0;
				
		Composite buttonComp = toolkit.createComposite(parent, SWT.NONE);
		buttonComp.setLayout(new GridLayout());
		buttonComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		buttontoolbar = new RecordButtonToolbar(buttonComp, recordEditor, toolkit);
		buttontoolbar.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, true, false));
		
		sashForm = new SashForm(parent, SWT.VERTICAL);
		sashForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		SmartSection narrativeSection = new SmartSection(sashForm, toolkit, "Narrative"){
			public void populateHeaderAdditions(Composite parent){
				Hyperlink l = createHyperlink(parent, FieldType.NARRATIVE);
				l.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
				l.setToolTipText("Opens narrative in separate window");
			}
		};
		
		toolkit.adapt(narrativeSection);
		narrativePart = toolkit.createComposite(narrativeSection, SWT.NONE);
		narrativePart.setLayout(new GridLayout());
		narrativePart.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		SmartSection expEntities = new SmartSection(sashForm, toolkit, "Scratchpad"){
			public void populateHeaderAdditions(Composite parent){
				Hyperlink l = createHyperlink(parent, FieldType.SCRATCHPAD);
				l.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
				l.setToolTipText("Opens scratchpad in separate window");
			}
		};
		
		scratchpadPart = toolkit.createComposite(expEntities, SWT.NONE);
		scratchpadPart.setLayout(new GridLayout());
		scratchpadPart.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		sashForm.addListener(SWT.Resize, (e)->{
			List<SmartSection> sections = (List<SmartSection>) sashForm.getData(SmartSection.KIDS_KEY);
			int max = (Integer)sashForm.getData(SmartSection.MAX_KEY);
			if ( max >= 0){
				sections.get(max).maximize();
				
			}else{
				//resize all so that the minimum size is respected
				sections.forEach(s->s.resizeMinSize());
			}
		});
		
	}

	
	public void setEditMode(boolean editMode){		
		buttontoolbar.setEditMode(editMode);
	}
	
	public void initPage(){
		if (narrativePart != null){
			for (Control c : narrativePart.getChildren()){
				c.dispose();
			}
		}
		if (scratchpadPart != null){
			for (Control c : scratchpadPart.getChildren()){
				c.dispose();
			}
		}
		
		txtDescription = toolkit.createText(narrativePart, recordEditor.getRecord().getDescription(), SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
		txtDescription.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		if (recordEditor.getEditMode()){
			txtDescription.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent e) {
					recordEditor.getRecord().setDescription(txtDescription.getText());
					recordEditor.updateTextLink(RecordNarrativeView.FieldType.NARRATIVE, recordEditor.getRecord().getDescription());
					recordEditor.setDirty(true);
				}
			});
			
			createMenu(txtDescription);
		}else{
			txtDescription.setEditable(false);
		}
		
		txtScratchpad = toolkit.createText(scratchpadPart, recordEditor.getRecord().getComment(), SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
		txtScratchpad.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		if (recordEditor.getEditMode()){
			txtScratchpad.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent e) {
					recordEditor.getRecord().setComment(txtScratchpad.getText());
					recordEditor.updateTextLink(RecordNarrativeView.FieldType.SCRATCHPAD, recordEditor.getRecord().getComment());
					recordEditor.setDirty(true);
				}
			});
			createMenu(txtScratchpad);
		}else{
			txtScratchpad.setEditable(false);
		}	
		
		narrativePart.layout();
		scratchpadPart.layout();
		
		enableWs(WorkingSetManager.INSTANCE.isSet() && recordEditor.getRecord().getUuid() != null);
	}
	

	
	private Hyperlink createHyperlink(Composite parent, RecordNarrativeView.FieldType type){
		Hyperlink open = toolkit.createHyperlink(parent, "open...", SWT.NONE);
		open.setBackground(parent.getBackground());
		open.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				recordEditor.openExternalText(type);
			}
		});
		return open;
	}
	
	private void createMenu(Text text){
		Menu menu = new Menu(text);
		text.setMenu(menu);
		
		MenuItem search = new MenuItem(menu, SWT.CASCADE);
		search.setText("Search Entity -> ");
		search.addSelectionListener(new SelectionAdapter() {			
			@Override
			public void widgetSelected(SelectionEvent e) {				
				EntitySearchShell shell = new EntitySearchShell(getSite().getShell(), text.getSelectionText(), recordEditor);
				shell.open(Display.getDefault().getCursorLocation());
			}
		});
		
		if (IntelSecurityManager.INSTANCE.canCreateEntity()){
			MenuItem newEntity = new MenuItem(menu, SWT.CASCADE);
			newEntity.setText("New Entity ... ");
			newEntity.addSelectionListener(new SelectionAdapter() {			
				@Override
				public void widgetSelected(SelectionEvent e) {				
					NewEntityDialog dialog = new NewEntityDialog(getSite().getShell());
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
	
	public void updateText (RecordNarrativeView.FieldType type, String text){
		Text txt = null;
		if (type == FieldType.NARRATIVE){
			txt = txtDescription;
			recordEditor.getRecord().setDescription(text);
		}else if (type == FieldType.SCRATCHPAD){
			txt = txtScratchpad;
			recordEditor.getRecord().setComment(text);
		}
		
		Listener[] ls = txt.getListeners(SWT.Modify);
		for (Listener l : ls){
			txt.removeListener(SWT.Modify, l);
		}
		txt.setText(text);
		for (Listener l : ls){
			txt.addListener(SWT.Modify, l);
		}
	}
	
	@Override
	public void setFocus() {
		narrativePart.setFocus();
	}

	
	@Override
	public void dispose(){
		if (toolkit != null) toolkit.dispose();
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
	}

	@Override
	public void doSaveAs() {
	}

	@Override
	public boolean isDirty() {
		return recordEditor.isDirty();
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	
}
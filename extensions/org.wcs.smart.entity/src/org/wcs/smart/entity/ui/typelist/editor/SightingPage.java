package org.wcs.smart.entity.ui.typelist.editor;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.part.EditorPart;
import org.hibernate.Session;
import org.wcs.smart.entity.ui.typelist.editor.sightings.EntityFilterComposite;

public class SightingPage extends EditorPart implements IEntityTypeEditorPage {


	private EntityTypeEditor parentEditor;
	
	private EntityFilterComposite entityFilter;
	
	
	public SightingPage(EntityTypeEditor editor){
		this.parentEditor = editor;
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
	
	}

	@Override
	public void doSaveAs() {

	}

	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		super.setSite(site);
		super.setInput(input);
	}

	@Override
	public boolean isDirty() {
		return false;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	@Override
	public void createPartControl(Composite parent) {
		FormToolkit toolkit = new FormToolkit(Display.getCurrent());
		toolkit.setBorderStyle(SWT.BORDER);
		
		Form form = toolkit.createForm(parent);
		form.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		

		GridLayout glayout = new GridLayout();
		glayout.verticalSpacing = 0;
		glayout.marginHeight = 0;
		form.getBody().setLayout(glayout);
		form.setText("Sightings");
		
		
		Group g = new Group(form.getBody(), SWT.NONE);
		toolkit.adapt(g);
		g.setText("Filters");
		g.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		g.setLayout(new GridLayout(2, false));
		
		Label l1 = toolkit.createLabel(g, "Date Filter:");
		Label l3 = toolkit.createLabel(g, "Date Filter:");
		
		Label l2 = toolkit.createLabel(g, "Entity Filter:");
		
		entityFilter = new EntityFilterComposite(g);
		entityFilter.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		toolkit.adapt(entityFilter);
		toolkit.adapt(entityFilter.getLabel(), false, false);
		
		Button btnRefresh = toolkit.createButton(g, "Update Table", SWT.PUSH);
	}

	
	private void createSightingsTable(Composite parent){
		TableViewer sightingTable = new TableViewer(parent, SWT.BORDER | SWT.FULL_SELECTION);
	
		
	}
	
	
	
	@Override
	public void setFocus() {
		
	}
	

	@Override
	public void updatePage(Session currentSession, boolean typeModified) {
		entityFilter.setEntities(parentEditor.getEntityType().getEntities());
	}

}

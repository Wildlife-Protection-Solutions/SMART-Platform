package org.wcs.smart.entity.ui.newwizard;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.hibernate.Session;
import org.wcs.smart.entity.model.EntityType;
import org.wcs.smart.entity.model.EntityType.Status;

/**
 * Composite for entity type status field
 * @author Emily
 *
 */
public class StatusComposite extends AbstractEntityComposite {

	private ComboViewer statusCombo ;
	@Override
	public String getName() {
		return "Entity Type Status";
	}

	@Override
	public String getDescription() {
		return "Modify the status of the entity type.";
	}

	@Override
	public String validate() {
		return null;
	}

	@Override
	public Composite createComposite(Composite parent) {
		Composite part = new Composite(parent, SWT.NONE);
		part.setLayout(new GridLayout(2, false));
		((GridLayout)part.getLayout()).marginWidth = 20;
		part.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
		
		Label l = new Label(part, SWT.NONE);
		l.setText("Status:");
		
		statusCombo = new ComboViewer(part, SWT.DROP_DOWN | SWT.READ_ONLY);
		statusCombo.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		statusCombo.setContentProvider(ArrayContentProvider.getInstance());
		statusCombo.setLabelProvider(new LabelProvider(){
			public String getText(Object element){
				return ((EntityType.Status)element).getGuiName();
			}
		});
		statusCombo.setInput(EntityType.Status.values());
		statusCombo.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				fireChange(new Event());
			}
		});
		return part;
	}

	@Override
	public void updateEntityType(EntityType entityType) {
		entityType.setStatus( (Status) ((IStructuredSelection)statusCombo.getSelection()).getFirstElement() );

	}

	@Override
	public void initFields(EntityType entityType, Session session) {
		statusCombo.setSelection(new StructuredSelection(entityType.getStatus()));
	}

}

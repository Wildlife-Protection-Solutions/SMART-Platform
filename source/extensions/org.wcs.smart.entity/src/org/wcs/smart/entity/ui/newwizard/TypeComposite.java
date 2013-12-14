package org.wcs.smart.entity.ui.newwizard;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.hibernate.Session;
import org.wcs.smart.entity.model.EntityType;
import org.wcs.smart.entity.model.EntityType.Type;

public class TypeComposite extends AbstractEntityComposite{

	private ComboViewer typeviewer;
	
	@Override
	public String getName() {
		return "Entity Type";
	}

	@Override
	public String getDescription() {
		return "Select the type of entity. Entities can either be fixed (waterholes) or transient (animals).";
	}

	@Override
	public String validate() {
		return null;
	}

	@Override
	public Composite createComposite(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(2, false));

		Label l = new Label(main, SWT.NONE);
		l.setText("Type:");
		
		typeviewer = new ComboViewer(parent, SWT.DROP_DOWN | SWT.READ_ONLY | SWT.BORDER);
		typeviewer.setContentProvider(ArrayContentProvider.getInstance());
		typeviewer.setLabelProvider(new LabelProvider(){
			public String getText(Object element){
				return ((EntityType.Type)element).getGuiName();
			}
		});
		typeviewer.setInput(EntityType.Type.values());
		typeviewer.setSelection(new StructuredSelection(EntityType.Type.TRANSIENT));

		typeviewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				fireChange(new Event());
			}
		});
		return main;
	}

	@Override
	public void updateEntityType(EntityType entityType) {
		EntityType.Type type = (Type) ((IStructuredSelection)typeviewer.getSelection()).getFirstElement();
		entityType.setType(type);
		
	}

	@Override
	public void initFields(EntityType entityType, Session session) {
		if (entityType.getType() == null){
			typeviewer.setSelection(new StructuredSelection(EntityType.Type.TRANSIENT));
		}else{
			typeviewer.setSelection(new StructuredSelection(entityType.getType()));
		}
		
	}

}

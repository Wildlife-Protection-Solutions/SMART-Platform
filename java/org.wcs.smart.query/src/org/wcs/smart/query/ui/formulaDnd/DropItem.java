package org.wcs.smart.query.ui.formulaDnd;


import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

public class DropItem extends Composite {

	private Label lbl;
	private String text;

	public DropItem(Composite parent, String test) {
		super(parent, SWT.NONE);
		this.text = test;
		createComposite(parent);
		
	}
	
	
	
	
	public String getText(){
		return this.lbl.getText();
	}
	
	public String asQueryPart(){
		return "";
	};
	public String asValidationQueryPart(){
		return "";
	}
	
	public void createComposite(Composite parent){
		super.setLayout(new GridLayout(1, false));
		GridData gd = new GridData();
		gd.horizontalIndent = 5;
		gd.verticalIndent = 0;
		
		super.setData(gd);
		
		Composite inner = new Composite(this, SWT.BORDER);
		inner.setLayout(new GridLayout(1, false));
		lbl = new Label(inner, SWT.NONE);
		lbl.setText(this.text);
		
		initDrag(this, DropTargetPanel.types);
		initDrag(lbl, DropTargetPanel.types);
		initDrag(inner, DropTargetPanel.types);
	}
	
	

	private void initDrag(Control comp, Transfer[] types){
		DragSource dsource = new DragSource(comp, DND.DROP_MOVE);
		dsource.setTransfer(types);
		dsource.addDragListener(new DragSourceListener() {
			
			@Override
			public void dragStart(DragSourceEvent event) {
				// TODO Auto-generated method stub
				LocalSelectionTransfer.getTransfer().setSelection(new StructuredSelection(DropItem.this));
				event.doit = true;

			}
			
			@Override
			public void dragSetData(DragSourceEvent event) {
				// TODO Auto-generated method stub
				if (LocalSelectionTransfer.getTransfer()
						.isSupportedType(event.dataType)) {
					//event.data = viewer.getSelection();
					event.data = DropItem.this;
				}
			}
			
			@Override
			public void dragFinished(DragSourceEvent event) {
				LocalSelectionTransfer.getTransfer().setSelection(null);
//				viewer.refresh();
				DropItem.this.redraw();
			}
		});		
	}
	
	
	
	
}

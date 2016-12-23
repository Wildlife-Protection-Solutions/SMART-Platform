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
package org.wcs.smart.i2.ui.views.query.dropitem;

import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.wcs.smart.util.SmartUtils;

/**
 * A super class for a query drag and drop item.
 * 
 * @author Emily
 * @since 1.0.0
 */
public abstract class DropItem {

	private IDefinitionPanel targetPanel = null;
	
	private Composite widget;
	
	private String notAllowedMessage = ""; //$NON-NLS-1$
	
	/**
	 * Creates a new drop item
	 */
	public DropItem(){
	}
	
	/**
	 * Create the widget for the drop item 
	 * attached to the given panel.
	 * 
	 * @param panel target drop panel for the drop item widget 
	 */
	public void createWidget(IDefinitionPanel panel, Composite parent){
		this.targetPanel = panel;
		widget = createCompositeInternal(parent);
	}
	
	/**
	 * Disposes of the drop items widget
	 */
	public void dispose(){
		if (widget != null && !widget.isDisposed()){
			widget.dispose();
			widget = null;
		}
	}
	
//	/**
//	 * Moves this drop item to a new panel.
//	 * 
//	 * @param newPanel new drop item panel
//	 */
//	public void moveParent(IDefinitionPanel newPanel){
//		//TODO: not necessarily supported on all os's
//		widget.setParent(newPanel.getDropTargetComposite());
//		targetPanel = newPanel;
//	}
	
	/**
	 * @return the drop item widget or null
	 * if widget not created
	 */
	public Composite getWidget(){
		return this.widget;
	}
	
	/**
	 * @return a text representation of the drop item. 
	 * This is used to approximate the size of the drop item when dragging
	 */
	public abstract String getText();
	
	/**
	 * @return the query representation of the drop item
	 */
	public abstract String asQueryPart();
	
	
	/**
	 * Initializes the selected values
	 * associated with the drop item. Each drop
	 * item specifies the expected data object
	 * 
	 * @param data
	 */
	public abstract void initializeData(Object data);
	
	/**
	 * Called when a drop item modified the queries.
	 * <p>This function notifies the required components
	 * that the query has changed.</p>
	 */
	protected void queryChanged(){
		targetPanel.validate();
		targetPanel.fireQueryChangedListeners();
	}
	
	/**
	 * @param parent parent composite
	 */
	protected Composite createCompositeInternal(Composite parent){
		Composite main = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(1, false);
		layout.marginBottom = 2;
		layout.marginLeft = 4;
		layout.marginRight = 4;
		layout.marginTop = 2;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 0;
		main.setLayout(layout);
		main.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		
		
		Composite inner = new Composite(main, SWT.BORDER);
		inner.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		//((GridData)inner.getLayoutData()).heightHint = 23;

		layout = new GridLayout(2, false);
		layout.marginBottom = 1;
		layout.marginLeft = 4;
		layout.marginRight = 0;
		layout.marginTop = 1;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.horizontalSpacing = 4;
		layout.verticalSpacing = 0;
		inner.setLayout(layout);
		
		createComposite(inner);
		
		Label lblX = new Label(inner, SWT.NONE);
		lblX.setToolTipText("Delete Item");
		lblX.setLayoutData(new GridData(SWT.TOP, SWT.RIGHT, false, true));
		lblX.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_ETOOL_DELETE));
		lblX.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseUp(MouseEvent e) {
				targetPanel.removeItem(DropItem.this);
				targetPanel.fireQueryChangedListeners();
			}
			
		});
		
		initDrag(main);
		initDrag(inner);
		
		return main;
	}

	/**
	 * Creates the internals of the drop item
	 * @param parent
	 */
	protected abstract void createComposite(Composite parent);
	

	/**
	 * Registers a control for dragging.  Any sub-elements
	 * that you want to be able to drag need to be registered.
	 * 
	 * @param comp control to register
	 */
	protected void initDrag(Control comp){
		DragSource dsource = new DragSource(comp, DND.DROP_MOVE);
		dsource.setTransfer(IDefinitionPanel.DND_TYPES);
		dsource.addDragListener(new DragSourceListener() {
			@Override
			public void dragStart(DragSourceEvent event) {
				LocalSelectionTransfer.getTransfer().setSelection(new StructuredSelection(DropItem.this));
				event.doit = true;
			}
			
			@Override
			public void dragSetData(DragSourceEvent event) {
			}
			
			@Override
			public void dragFinished(DragSourceEvent event) {
				LocalSelectionTransfer.getTransfer().setSelection(null);
				if (targetPanel == null){
					DropItem.this.dispose();
				}else{
					targetPanel.finishDrag(DropItem.this);
				}
			}
		});		
	}
	
	/**
	 * Formats string for diaplay in label.
	 * Specifically replaces & with &&
	 * @param text
	 * @return
	 */
	protected String formatStringForLabel(String text){
		return SmartUtils.formatStringForLabel(text);
	}
	
	/**
	 * Determines if this drop item is allowed to be added to definition view.
	 * Some restrictions may be applied to particular drop items (validation, permissions, etc.)
	 * that do not allow to add this item to query. Reason will be obtained via getNotAllowedMessage() 
	 * 
	 * @return
	 */
	public boolean isAllowed() {
		return true;
	}
	
	public String getNotAllowedMessage() {
		return notAllowedMessage;
	}
	
	protected void setNotAllowedMessage(String notAllowedMessage) {
		this.notAllowedMessage = notAllowedMessage;
	}
	
	public IDefinitionPanel getTargetPanel(){
		return this.targetPanel;
	}
	
	public void setTargetPanel(IDefinitionPanel target){
		this.targetPanel = target;
	}
}

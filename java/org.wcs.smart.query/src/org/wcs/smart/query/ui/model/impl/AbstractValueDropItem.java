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
package org.wcs.smart.query.ui.model.impl;

import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.wcs.smart.query.common.ui.EncounterRateDialog;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.ui.model.DropItem;
import org.wcs.smart.query.ui.model.IValueDropItem;

/**
 * An abstract value drop item that includes the ability
 * to add encounter ratios.
 * 
 * @author egouge
 * @since 1.0.0
 */
public abstract class AbstractValueDropItem extends DropItem implements IValueDropItem{

	public static final String PER_LABEL = Messages.AbstractValueDropItem_EncounterRatePerRatio;
	
	protected IValueDropItem encounterRatio;
	private Font smallerFont;
	private Composite main;
	private IValueDropItem[] encounterRateOptions;
		
	protected boolean isEncounter;
	
	protected AbstractValueDropItem(boolean isEncounter){
		this.isEncounter = isEncounter;
	}
	/**
	 * @return the value query part
	 */
	protected abstract String getValueQueryPart();
	/**
	 * @return the value query test
	 */
	protected abstract String getValueText();
	/**
	 * @param parent the value composite part
	 */
	protected abstract void createValueComposite(Composite parent);
	/**
	 * @param data initializes the value data
	 */
	protected abstract void initializeValueData(Object data);
	
	/**
	 * Sets the encounter ratio
	 * @param dropItem the patrol value option to be used as the 
	 * encounter rate 
	 */
	public void setEncounterRatio(IValueDropItem dropItem){
		this.encounterRatio = dropItem;
	}
	
	/**
	 * True if value drop item has encounter ratio
	 * @return
	 */
	public boolean hasEncounterRatio(){
		return this.encounterRatio != null;
	}
	
	/**
	 * Sets the encounter rate ratio options
	 * @param options the patrol value options to be used as the 
	 * encounter rate choices
	 */
	public void setEncounterRateOptions(IValueDropItem[] options){
		encounterRateOptions = options;
	}
	
	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#getText()
	 */
	@Override
	public String getText() {
		if (isEncounter){
			return getValueText();
		}
		StringBuilder sb = new StringBuilder();
		sb.append(getValueText());
		if (hasEncounterRatio()){
			sb.append( " " + PER_LABEL + " "); //$NON-NLS-1$ //$NON-NLS-2$
			sb.append(((DropItem)encounterRatio).getText());
		}
		
		return sb.toString();
	}
	
	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#dispose()
	 */
	@Override
	public void dispose(){
		super.dispose();
		if (smallerFont != null){
			smallerFont.dispose();
			smallerFont = null;
		}
	}
	
	/**
	 * @param data - if a single object, the single object
	 * is passed to the underlying value item.
	 * If an array object, then the first element is
	 * any data to be passed to underlying value item and the second item
	 * is null or the enconterRatio drop item
	 * 
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#initializeData(java.lang.Object)
	 */
	@Override
	public void initializeData(Object data) {
		if (data == null){
			return;
		}
		if (data.getClass().isArray()){
			isEncounter = true;
			Object[] d = (Object[])data;
			initializeValueData(d[0]);
			this.encounterRatio = (IValueDropItem)d[1];
		}else{
			initializeValueData(data);
		}
	}
	
	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#asQueryPart()
	 */
	@Override
	public String asQueryPart() {
		StringBuilder sb = new StringBuilder();
		sb.append(getValueQueryPart());
		
		if (encounterRatio != null){
			sb.append("/"); //$NON-NLS-1$
			sb.append(((DropItem)encounterRatio).asQueryPart());
		}
		return sb.toString();
	}
	
	/*
	 * Creates the ui element
	 */
	private void updateUi(){
		Control[] kids = main.getChildren();
		for (int i = 0; i < kids.length; i ++){
			kids[i].dispose();
		}
		
		createValueComposite(main);
		
		if (encounterRatio != null){
			main.setLayout(new GridLayout(1, false));
			
			Label lbl = new Label(main, SWT.NONE);
			lbl.setText( " " + PER_LABEL + " "); //$NON-NLS-1$ //$NON-NLS-2$
			initDrag(lbl);
			
			Label lbl2 = new Label(main, SWT.NONE);
			lbl2.setText( formatStringForLabel(((DropItem)encounterRatio).getText()));
			
			createRateLink(main, Messages.AbstractValueDropItem_ChangeRate);
		}else{
			main.setLayout(new GridLayout(2, false));
			createRateLink(main, Messages.AbstractValueDropItem_ComputeRate);
		}
		
		main.layout();
		main.redraw();
		
	}
	
	/**
	 * Creates the compute/change rate link
	 * @param parent
	 */
	private void createRateLink(Composite parent, String text) {
		final Hyperlink link = new Hyperlink(parent,  SWT.NONE);
		
		link.setUnderlined(true);
		link.setForeground( main.getShell().getDisplay().getSystemColor(SWT.COLOR_BLUE) );
		link.setText(formatStringForLabel(text));
		
		if (smallerFont != null){
			FontData fd = (link.getFont().getFontData()[0]);
			fd.setHeight(fd.getHeight() - 1);
			smallerFont = new Font(Display.getCurrent(), fd);
		}
		
		link.setFont(smallerFont);
		link.setLayoutData(new GridData(SWT.RIGHT, SWT.BOTTOM, true, false));
		link.addListener(SWT.MouseUp, new Listener() {
			@Override
			public void handleEvent(Event event) {
				showRateDialog();
			}
		});
	}
	
	/**
	 * Displays the rate dialog and updates the encounter ratio as
	 * required.
	 */
	private void showRateDialog() {
		EncounterRateDialog dialog = new EncounterRateDialog(main.getShell(), encounterRateOptions, encounterRatio);
		if (dialog.open() == Window.OK){
			encounterRatio = dialog.getSelectedItems();
			updateUi();
			getTargetPanel().redraw();
			queryChanged();
		}
		
	}
	
	/* (non-Javadoc)
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#createComposite(org.eclipse.swt.widgets.Composite)
	 */
	public void createComposite(Composite parent){
		main = new Composite(parent, SWT.NONE);
		initDrag(main);
		if (isEncounter){
			updateUi();
		}else{
			main.setLayout(new GridLayout());
			createValueComposite(main);
		}
	}
}

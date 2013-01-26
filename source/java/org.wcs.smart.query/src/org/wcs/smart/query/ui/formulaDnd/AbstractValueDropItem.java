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
package org.wcs.smart.query.ui.formulaDnd;

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
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.parser.PatrolQueryOptions.PatrolValueOption;

/**
 * An abstract value drop item that includes the ability
 * to add encounter ratios.
 * 
 * @author egouge
 * @since 1.0.0
 */
public abstract class AbstractValueDropItem extends DropItem {

	private static final String PER_LABEL = Messages.AbstractValueDropItem_RateofChangePerLabel;
	
	protected PatrolValueOption encounterRatio;
	private Font smallerFont;
	private Composite main;
	private PatrolValueOption[] encounterRateOptions;
		
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
	public void setEncounterRatio(PatrolValueOption dropItem){
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
	public void setEncounterRateOptions(PatrolValueOption[] options){
		
		encounterRateOptions = options;
	}
	
	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#getText()
	 */
	@Override
	public String getText() {
		StringBuilder sb = new StringBuilder();
		sb.append(getValueText());
		if (hasEncounterRatio()){
			sb.append( " " + PER_LABEL + " "); //$NON-NLS-1$ //$NON-NLS-2$
			sb.append(encounterRatio.getGuiName());
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
	 * @param data - must be an object array where the first element is
	 * any data to be passed to underlying value item and the second item
	 * is null or the enconterRatio patrolvalueoption
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#initializeData(java.lang.Object)
	 */
	@Override
	public void initializeData(Object data) {
		if (data == null){
			return;
		}
		Object[] d = (Object[])data;
		initializeValueData(d[0]);
		this.encounterRatio = (PatrolValueOption)d[1];		
	}
	
	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#asQueryPart()
	 */
	@Override
	public String asQueryPart() {
		StringBuilder sb = new StringBuilder();
		sb.append(getValueQueryPart());
		
		if (encounterRatio != null){
			sb.append("/patrol:sum:"); //$NON-NLS-1$
			sb.append(encounterRatio.getKeyPart());
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
			lbl2.setText( formatStringForLabel(encounterRatio.getGuiName()));
			
			createRateLink(main, Messages.AbstractValueDropItem_ChangeRankLink);
		}else{
			main.setLayout(new GridLayout(2, false));
			createRateLink(main, Messages.AbstractValueDropItem_ComputeRateLink);
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
		EncounterRateDialog dialog = new EncounterRateDialog(main.getShell(), encounterRateOptions);
		if (dialog.open() == Window.OK){
			encounterRatio = dialog.getSelectedItems();
			updateUi();
			targetPanel.layout();
			queryChanged();
		}
		
	}
	
	/* (non-Javadoc)
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#createComposite(org.eclipse.swt.widgets.Composite)
	 */
	public void createComposite(Composite parent){
		main = new Composite(parent, SWT.NONE);
		initDrag(main);
		updateUi();
	}
	

	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#isValueItem()
	 */
	@Override
	public boolean isValueItem(){
		return true;
	}
	
	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#isFilterItem()
	 */
	@Override
	public boolean isFilterItem(){
		return false;
	}

	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#isGroupByItem()
	 */
	@Override
	public boolean isGroupByItem(){
		return false;
	}
}

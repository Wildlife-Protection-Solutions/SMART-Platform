package org.wcs.smart.i2.ui.views.query.dropitem;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

public class TextDropItem extends DropItem {

	private String name;
	private String queryKey;
	
	
	/**
	 * Creates a new are drop item that has 
	 * single text field label
	 * 
	 */
	public TextDropItem(String name, String queryKey){
		this.name = name;
		this.queryKey = queryKey;
	}
	
	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#getText()
	 */
	@Override
	public String getText() {
		return name;
	}

	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#asQueryPart()
	 */
	@Override
	public String asQueryPart() {
		return queryKey;
	}

	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#createComposite(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected void createComposite(Composite parent) {
		Label lbl = new Label(parent, SWT.NONE);
		lbl.setText( formatStringForLabel(getText()));  //$NON-NLS-1$//$NON-NLS-2$
		initDrag(lbl);

	}

}

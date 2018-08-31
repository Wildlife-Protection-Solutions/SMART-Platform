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

import java.text.Collator;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.query.ListItem;
import org.wcs.smart.i2.query.observation.filter.GroupByItem;
import org.wcs.smart.util.UuidUtils;
/**
 * Conservation area group by drop item for profiles
 * 
 * @author Emily
 *
 */
public class ConservationAreaGroupByDropItem  extends DropItem implements IGroupByDropItem, ICombinableDropItem {

	private List<ListItem> filteredValues = new ArrayList<>();
	
	private Label caLabel;
	private Link link;
	
	/**
	 * Creates a new drop item
	 * @param type
	 */
	public ConservationAreaGroupByDropItem(){
	}
	
	
	@Override
	public void dispose(){
		super.dispose();
	}
	/* (non-Javadoc)
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#getText()
	 */
	@Override
	public String getText() {
		return Messages.ConservationAreaGroupByDropItem_CaGroupByLabel;
	}

	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#asQueryPart()
	 */
	@Override
	public String asQueryPart() {
		StringBuilder queryPart = new StringBuilder();
		queryPart.append(GroupByItem.GroupByType.CA.getKey());
		queryPart.append(":"); //$NON-NLS-1$
		for (int i = 0; i < filteredValues.size(); i ++){
			 //this is a hack to get the javacc complier to accept ca and entity type group by options
			queryPart.append( "z" ); //$NON-NLS-1$
			queryPart.append( filteredValues.get(i).getKeyId() );
			if (i != filteredValues.size() -1){
				queryPart.append(":"); //$NON-NLS-1$
			}	
		}
		if (!filteredValues.isEmpty()) queryPart.append(":"); //$NON-NLS-1$
		
		return queryPart.toString();
	}

	/**
	 * @param data - must be a (ListItem[]) that represents
	 * the selected group by options or null if all selected
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#initializeData(java.lang.Object)
	 */
	
	public void addConservationArea(ConservationArea ca) {
		filteredValues.add(new ListItem(UuidUtils.uuidToString(ca.getUuid()), ca.getNameLabel(), ca.getNameLabel()));
		updateLabel();
	}

	
	
	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#createComposite(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected void createComposite(Composite parent) {
		Composite comp = new Composite(parent, SWT.NONE);
		comp.setLayout(new GridLayout(2, false));
		
		caLabel = new Label(comp, SWT.NONE);
		caLabel.setText( formatStringForLabel(getText()));
		initDrag(caLabel);
		
		link = new Link(comp, SWT.NONE);
		link.setLayoutData(new GridData(SWT.LEFT, SWT.BOTTOM, false, false));
		link.setText("<a>...</a>"); //$NON-NLS-1$
		link.addListener(SWT.Selection, e->{
			OptionDialog dialog = new OptionDialog(parent.getShell());
			dialog.setGroupByItem(this, filteredValues);
			if (dialog.open() == OptionDialog.OK) {
				filteredValues.clear();
				if (dialog.getSelectedItems() == null) {
					//select all
				}else {
					for (ListItem i : dialog.getSelectedItems()) {
						filteredValues.add(i);
					}
				}
				updateLabel();
				super.queryChanged();
			}
		});
		updateToolTipMessage();
	}
	
	private void updateToolTipMessage(){
		StringBuilder tipStr = new StringBuilder();
		if (filteredValues.isEmpty()){
			tipStr.append(Messages.ConservationAreaGroupByDropItem_AllItems);
		}else{
			for (ListItem item: filteredValues){
				tipStr.append("'" + item.getName() + "'" + "\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
		}
		link.setToolTipText(tipStr.toString());
	}
	
	@Override
	public List<ListItem> getListOptions() {
		List<ListItem> items = new ArrayList<ListItem>();
		for (ConservationArea ca : SmartDB.getConservationAreaConfiguration().getConservationAreas()){
			items.add(new ListItem(UuidUtils.uuidToString(ca.getUuid()), ca.getNameLabel()));
		}
		items.sort((a,b)->Collator.getInstance().compare(a.getName(), b.getName()));
		return items;

	}

	private void updateLabel() {
		if (caLabel != null) {
			caLabel.setText( formatStringForLabel(getText()));
			updateToolTipMessage();
			super.getTargetPanel().redraw();
		}
	}

	@Override
	public boolean addItem(DropItem item) {
		if (!(item instanceof ConservationAreaGroupByDropItem)) return false;
		this.filteredValues.addAll( ((ConservationAreaGroupByDropItem)item).filteredValues);
		updateLabel();
		
		return true;
	}

}

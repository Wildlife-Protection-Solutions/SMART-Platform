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
package org.wcs.smart.i2.ui.views.query;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.IntelAttribute;
import org.wcs.smart.i2.model.IntelAttributeListItem;
import org.wcs.smart.i2.model.IntelEntityTypeAttribute;
import org.wcs.smart.i2.query.IntelQueryColumnProvider;
import org.wcs.smart.i2.query.observation.filter.IQueryFilter;
import org.wcs.smart.i2.ui.views.query.dropitem.DateDropItem;
import org.wcs.smart.i2.ui.views.query.dropitem.DropItem;
import org.wcs.smart.i2.ui.views.query.dropitem.DropItemFactory;
import org.wcs.smart.i2.ui.views.query.dropitem.OptionDropItem;
import org.wcs.smart.i2.ui.views.query.dropitem.TextBoxDropItem;
import org.wcs.smart.i2.ui.views.query.dropitem.TextDropItem;

/**
 * Intelligence attribute tree filter item.
 * 
 * @author Emily
 *
 */
public class AttributeTreeFilterItem extends BasicTreeFilterItem {

	private UUID attributeUuid;
	private IntelAttribute.AttributeType type;
	private String dropItemName = null;
	private String queryKey = ""; //$NON-NLS-1$
	
	/**
	 * Creates a new attribute filter for an entity type attribute
	 * @param attribute
	 */
	public AttributeTreeFilterItem(IntelEntityTypeAttribute attribute) {
		super(attribute.getAttribute().getName());
		this.attributeUuid = attribute.getAttribute().getUuid();
		type = attribute.getAttribute().getType();
		dropItemName = IntelQueryColumnProvider.generateName(attribute.getAttribute(),  attribute.getEntityType());
		queryKey = "e_attribute:" + attribute.getAttribute().getType().key + ":" + attribute.getAttribute().getKeyId() + ":" + attribute.getEntityType().getKeyId(); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	/**
	 * Create a new attribute filter for an attribute across all entity types
	 * @param attribute
	 */
	public AttributeTreeFilterItem(IntelAttribute attribute) {
		super(attribute.getName());
		this.attributeUuid = attribute.getUuid();
		type = attribute.getType();
		dropItemName = IntelQueryColumnProvider.generateName(attribute, null);
		queryKey = "e_attribute:" + attribute.getType().key + ":" + attribute.getKeyId() + ":" ; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	public IntelAttribute.AttributeType getType(){
		return this.type;
	}
	
	@Override
	public DropItem[] asDropItem() {
		
		switch(type){
		case BOOLEAN:
			return new DropItem[]{new TextDropItem(dropItemName, queryKey)};
		case DATE:
			return new DropItem[]{new DateDropItem(dropItemName, queryKey)};
		case LIST:
			final List<String> labels = new ArrayList<String>();
			final List<String> keys = new ArrayList<String>();
			labels.add(DropItemFactory.ANY_LABEL);
			keys.add(IQueryFilter.ANY_OPTION_KEY);
			Job j = new Job("creating attribute drop item"){ //$NON-NLS-1$

				@Override
				protected IStatus run(IProgressMonitor monitor) {
					Session s = HibernateManager.openSession();
					try{
						IntelAttribute a = (IntelAttribute) s.get(IntelAttribute.class, attributeUuid);
						if (a.getAttributeList() != null){
							for (IntelAttributeListItem i : a.getAttributeList()){
								labels.add(i.getName());
								keys.add(i.getKeyId());
							}
						}
					}finally{
						s.close();
					}
					
					return Status.OK_STATUS;
				}
			};
			j.schedule();
			try {
				j.join();
			} catch (InterruptedException e) {
				Intelligence2PlugIn.displayLog(Messages.AttributeTreeFilterItem_ErrorMsg, e);
			}
			
			return new DropItem[]{new OptionDropItem(dropItemName, queryKey, labels.toArray(new String[labels.size()]), keys.toArray(new String[keys.size()]))};
		case NUMERIC:
			return new DropItem[]{new TextBoxDropItem(dropItemName, queryKey, TextBoxDropItem.InputType.NUMERIC)};
		case TEXT:
			return new DropItem[]{new TextBoxDropItem(dropItemName, queryKey, TextBoxDropItem.InputType.TEXT)};
		default:
			break;
			
		}
		return null;
	}
}

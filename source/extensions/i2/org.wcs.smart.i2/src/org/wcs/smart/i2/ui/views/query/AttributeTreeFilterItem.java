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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.hibernate.Session;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.InternalQueryManager;
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
import org.wcs.smart.ui.SmartLabelProvider;
import org.wcs.smart.util.UuidUtils;

/**
 * Intelligence attribute tree filter item.
 * 
 * @author Emily
 *
 */
public class AttributeTreeFilterItem extends BasicTreeFilterItem {

	private IntelAttribute.AttributeType type;
	private String dropItemName = null;
	private String queryKey = ""; //$NON-NLS-1$
	private String attributeKey = null;
	
	/**
	 * Creates a new attribute filter for an entity type attribute
	 * @param attribute
	 */
	public AttributeTreeFilterItem(IntelEntityTypeAttribute attribute) {
		super(attribute.getAttribute().getName());
		type = attribute.getAttribute().getType();
		attributeKey = attribute.getAttribute().getKeyId();
		dropItemName = IntelQueryColumnProvider.generateName(attribute.getAttribute(),  attribute.getEntityType());
		queryKey = "e_attribute:" + attribute.getAttribute().getType().key + ":" + attribute.getAttribute().getKeyId() + ":" + attribute.getEntityType().getKeyId(); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	/**
	 * Create a new attribute filter for an attribute across all entity types
	 * @param attribute
	 */
	public AttributeTreeFilterItem(IntelAttribute attribute) {
		super(attribute.getName());
		type = attribute.getType();
		attributeKey = attribute.getKeyId();
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
					try(Session s = HibernateManager.openSession()){
						for (IntelAttributeListItem item : InternalQueryManager.INSTANCE.getQueryItemProvider().getAttributeListItems(attributeKey, s)) {
							labels.add(item.getName());
							keys.add(item.getKeyId());
						}
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
		case EMPLOYEE:
			final List<String> elabels = new ArrayList<String>();
			final List<String> ekeys = new ArrayList<String>();
			
			elabels.add(DropItemFactory.ANY_LABEL);
			ekeys.add(IQueryFilter.ANY_OPTION_KEY);
			
			Job j2 = new Job("creating employee drop item"){ //$NON-NLS-1$

				@Override
				protected IStatus run(IProgressMonitor monitor) {
					try(Session s = HibernateManager.openSession()){
						List<Employee> emps = InternalQueryManager.INSTANCE.getQueryItemProvider().getEmployees(s);
						for (Employee e : emps) {
							elabels.add(SmartLabelProvider.getFullLabel(e));
							ekeys.add(UuidUtils.uuidToString(e.getUuid()));
						}
					}
					return Status.OK_STATUS;
				}
			};
			j2.schedule();
			try {
				j2.join();
			} catch (InterruptedException e) {
				Intelligence2PlugIn.displayLog(Messages.AttributeTreeFilterItem_employeeNotFound, e);
			}
			
			return new DropItem[]{new OptionDropItem(dropItemName, queryKey, elabels.toArray(new String[elabels.size()]), ekeys.toArray(new String[ekeys.size()]))};
		case NUMERIC:
			return new DropItem[]{new TextBoxDropItem(dropItemName, queryKey, TextBoxDropItem.InputType.NUMERIC)};
		case TEXT:
			return new DropItem[]{new TextBoxDropItem(dropItemName, queryKey, TextBoxDropItem.InputType.TEXT)};
		}
		return null;
	}
}

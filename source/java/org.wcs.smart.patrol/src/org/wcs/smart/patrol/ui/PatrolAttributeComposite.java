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
package org.wcs.smart.patrol.ui;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.patrol.PatrolEventManager;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.internal.ui.PatrolItemComposite;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolAttribute;
import org.wcs.smart.patrol.model.PatrolAttributeListItem;
import org.wcs.smart.patrol.model.PatrolAttributeValue;
import org.wcs.smart.ui.ca.datamodel.AttributeFieldFactory;
import org.wcs.smart.ui.ca.datamodel.IAttributeField;

/**
 * Composite for collecting custom patrol attributes
 * 
 * @author Emily
 * @since 7.0.0
 *
 */
public class PatrolAttributeComposite extends PatrolItemComposite {

	private List<PatrolAttribute> attributes;
	private List<IAttributeField<?>> fields;
	private HashMap<IAttributeField<?>, PatrolAttribute> field2attribute;
	
	public PatrolAttributeComposite(List<PatrolAttribute> attributes) {
		super();
		this.attributes = attributes;
	}
	
	@Override
	public Composite createComponent(Composite parent, int style) {
		
		Composite ctemp = new Composite(parent, SWT.NONE);
		ctemp.setLayout(new GridLayout(2, false));
		ctemp.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		fields = new ArrayList<>();
		field2attribute = new HashMap<>();
		
		for(PatrolAttribute pa : attributes) {
			Attribute temp = new Attribute();
			temp.setName(pa.getName());
			if (pa.getName().length() > 25) {
				temp.setName(pa.getName().substring(0,25) + "..."); //$NON-NLS-1$
			}
			temp.setType(pa.getType());
			temp.setKeyId(pa.getKeyId());
			if (pa.getAttributeList() != null) {
				temp.setAttributeList(new ArrayList<>());
				for (PatrolAttributeListItem item : pa.getAttributeList()) {
					AttributeListItem clone = new AttributeListItem();
					clone.setAttribute(temp);
					clone.setIsActive(true);
					clone.setUuid(item.getUuid());
					temp.getAttributeList().add(clone);
					clone.setKeyId(item.getKeyId());
					clone.setName(item.getName());
				}
			}
			IAttributeField<?> field = AttributeFieldFactory.findAttributeField(temp);
			field2attribute.put(field, pa);
			field.addModifyListener(e->modified());
			field.createComposite(ctemp);
			ctemp.addListener(SWT.Dispose, e->field.dispose());
			fields.add(field);
			
		}
		return ctemp;
	}
	private void modified() {
		setErrorMessage(null);
		for (IAttributeField<?> field : fields) {
			String error = field.validate();
			if (error != null) setErrorMessage(error);
		}
		super.fireChangeListeners();
	}
	
	@Override
	public void setValues(Patrol p, Session session) {
		if (p.getCustomAttributes() == null) p.setCustomAttributes(new ArrayList<>());
		for (IAttributeField<?> field : fields) {
			for (PatrolAttributeValue v : p.getCustomAttributes()) {
				if (v.getPatrolAttribute().getKeyId().equals(field.getAttribute().getKeyId())) {
					Object x = v.getAttributeValue();
					if (x instanceof PatrolAttributeListItem) {
						AttributeListItem temp = new AttributeListItem();
						temp.setName(((PatrolAttributeListItem) x).getName());
						temp.setKeyId(((PatrolAttributeListItem) x).getKeyId());
						temp.setUuid(((PatrolAttributeListItem) x).getUuid());
						x = temp;
					}
					field.setValue(x);
					break;
				}
			}
		}
	}

	public boolean isValid() {
		if (fields == null) return false;
		for (IAttributeField<?> field : fields) {
			String error = field.validate();
			if (error != null) {
				return false;
			}
		}
		return true;
	}
	
	@Override
	public boolean updatePatrol(Patrol p, Session session) {
		for (IAttributeField<?> field : fields) {
			String error = field.validate();
			if (error != null) {
				MessageDialog.openError(Display.getDefault().getActiveShell(),Messages.PatrolAttributeComposite_errorTitle, MessageFormat.format(Messages.PatrolAttributeComposite_ErrorMessage, field.getAttribute().getName(), error));
				return false;
			}
		}
		
		for (IAttributeField<?> field : fields) {
			PatrolAttributeValue value = null;
			for (PatrolAttributeValue v : p.getCustomAttributes()) {
				if (v.getPatrolAttribute().getKeyId().equals(field.getAttribute().getKeyId())) {
					value = v;
					break;
				}
			}
			boolean isnew = false;
			if (value == null) {
				value = new PatrolAttributeValue();
				value.setPatrol(p);
				value.setPatrolAttribute(field2attribute.get(field));
				p.getCustomAttributes().add(value);
				isnew = true;
			}
			Object v = field.getValue();
			if (v != null) {
				if (v instanceof AttributeListItem) {
					for (PatrolAttributeListItem li : value.getPatrolAttribute().getAttributeList()) {
						if (li.getKeyId().equals(((AttributeListItem) v).getKeyId())) {
							v = li;
							break;
						}
					}
				}
				value.setAttributeValue(v);
			}else {
				p.getCustomAttributes().remove(value);
				if (!isnew) session.delete(value);
			}
		}
		return true;
	}

	@Override
	public String getTitle() {
		return Messages.PatrolAttributeComposite_Title;
	}

	@Override
	public int getAttribute() {
		return PatrolEventManager.PATROL_CUSTOM_ATTRIBUTE;
	}

}

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

import java.text.Collator;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.hibernate.Session;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.patrol.PatrolEventManager;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.internal.ui.PatrolItemComposite;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolAttribute;
import org.wcs.smart.patrol.model.PatrolAttributeListItem;
import org.wcs.smart.patrol.model.PatrolAttributePatrolType;
import org.wcs.smart.patrol.model.PatrolAttributeTreeNode;
import org.wcs.smart.patrol.model.PatrolAttributeValue;
import org.wcs.smart.patrol.model.PatrolType;
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

	private List<IAttributeField<?>> fields;
	private HashMap<IAttributeField<?>, PatrolAttribute> field2attribute;
	
	private Composite ctemp;
	
	private ScrolledComposite scrolled;
	private boolean includeDisabled = true;
	
	public PatrolAttributeComposite(boolean includeDisabled ) {
		super();
		this.includeDisabled = includeDisabled;
	}
	
	@Override
	public Composite createComponent(Composite parent, int style) {
		
		scrolled = new ScrolledComposite(parent, SWT.V_SCROLL);
		scrolled.setLayout(new GridLayout());
		scrolled.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		ctemp = new Composite(scrolled, SWT.NONE);
		ctemp.setLayout(new GridLayout(2, false));
		ctemp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		
		scrolled.setContent(ctemp);
		
		return scrolled;
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
		for (Control c : ctemp.getChildren()) c.dispose();
		
		fields = new ArrayList<>();
		field2attribute = new HashMap<>();
		
		PatrolType pt = session.get(PatrolType.class, p.getPatrolType().getUuid());
		if (pt.getCustomAttributes().isEmpty()) {
			Label l = new Label(ctemp, SWT.NONE);
			l.setText(MessageFormat.format("No custom attributes defined for track type {0}", pt.getName()));
			l.setBackground(l.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		}
		
		List<PatrolAttributePatrolType> sorted = new ArrayList<>(pt.getCustomAttributes());
		if (!includeDisabled) {
			for (Iterator<PatrolAttributePatrolType> iterator = sorted.iterator(); iterator.hasNext();) {
				PatrolAttributePatrolType a =  iterator.next();
				if (!a.getIsActive()) iterator.remove();
				
			}
		}
		sorted.sort((a,b)->Collator.getInstance().compare(a.getPatrolAttribute().getName(), b.getPatrolAttribute().getName()));
		//create ui fields
		for (PatrolAttributePatrolType pat : sorted) {
			PatrolAttribute pa = pat.getPatrolAttribute();
			
			//convert patrol attribute to data model attribute so we can use the
			//attribute fields setup for editing
			Attribute temp = new Attribute();
			temp.setName(pa.getName());
			if (pa.getName().length() > 25) {
				temp.setName(pa.getName().substring(0,25) + "..."); //$NON-NLS-1$
			}
			temp.setType(pa.getType());
			temp.setKeyId(pa.getKeyId());
			temp.setIcon(pa.getIcon());
			if (pa.getType() == Attribute.AttributeType.LIST) {
				temp.setAttributeList(new ArrayList<>());
				for (PatrolAttributeListItem item : pa.getAttributeList()) {
					AttributeListItem clone = new AttributeListItem();
					clone.setAttribute(temp);
					clone.setIsActive(true);
					clone.setUuid(item.getUuid());
					temp.getAttributeList().add(clone);
					clone.setKeyId(item.getKeyId());
					clone.setName(item.getName());
					clone.setIcon(item.getIcon());
				}
			}
			if (pa.getType() == Attribute.AttributeType.TREE) {
				temp.setTree(new ArrayList<>());
				temp.setActiveTreeNodes(new ArrayList<>());
				List<PatrolAttributeTreeNode> toProcess = new ArrayList<>();
				toProcess.addAll(pa.getAttributeTree());
				
				HashMap<PatrolAttributeTreeNode, AttributeTreeNode> cloneMap = new HashMap<>();
				
				while(!toProcess.isEmpty()) {
					PatrolAttributeTreeNode n = toProcess.remove(0);
					
					AttributeTreeNode clone = new AttributeTreeNode();
					clone.setAttribute(temp);
					clone.setIsActive(n.getIsActive());
					clone.setUuid(n.getUuid());
					clone.setKeyId(n.getKeyId());
					clone.setHkey(n.getHkey());
					clone.setIcon(n.getIcon());
					clone.setName(n.getName());
					clone.setActiveChildren(new ArrayList<>());
					clone.setChildren(new ArrayList<>());
					cloneMap.put(n,  clone);
					
					if (n.getParent() == null) {
						temp.getTree().add(clone);
						if (clone.getIsActive()) temp.getActiveTreeNodes().add(clone);
					}else {
						AttributeTreeNode tparent = cloneMap.get(n.getParent());
						clone.setParent(tparent);
						tparent.getChildren().add(clone);
						if (clone.getIsActive()) tparent.getActiveChildren().add(clone);
					}
					toProcess.addAll(n.getChildren());
					
				}
			}
			
			IAttributeField<?> field = AttributeFieldFactory.findAttributeField(temp);
			field2attribute.put(field, pa);
			field.createComposite(ctemp);
			field.addModifyListener(e->modified());
			field.addResizeListener(e->ctemp.getParent().notifyListeners(SWT.Resize, e));
			ctemp.addListener(SWT.Dispose, e->field.dispose());
			fields.add(field);
		}
		
		ctemp.layout(true);
		scrolled.setExpandHorizontal(true);
		ctemp.setSize(ctemp.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		scrolled.getParent().layout(true, true);
		
		//initial ui values
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
					if (x instanceof PatrolAttributeTreeNode node) {
						AttributeTreeNode temp = new AttributeTreeNode();
						temp.setName(node.getName());
						temp.setKeyId(node.getKeyId());
						temp.setUuid(node.getUuid());
						temp.setHkey(node.getHkey());
						temp.setIcon(node.getIcon());
						x=temp;
						
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
					PatrolAttribute temp = session.get(PatrolAttribute.class, value.getPatrolAttribute().getUuid());
					for (PatrolAttributeListItem li : temp.getAttributeList()) {
						if (li.getKeyId().equals(((AttributeListItem) v).getKeyId())) {
							v = li;
							break;
						}
					}
				}else if (v instanceof AttributeTreeNode node) {
					PatrolAttributeTreeNode temp = session.get(PatrolAttributeTreeNode.class, node.getUuid());
					v = temp;
				}
				value.setAttributeValue(v);
			}else {
				p.getCustomAttributes().remove(value);
				if (!isnew) session.remove(value);
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

/*
 * Copyright (C) 2021 Wildlife Conservation Society
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
package org.wcs.smart.dataentry.dialog.composite;

import java.io.StringReader;
import java.text.MessageFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.DataModel;
import org.wcs.smart.common.control.SmartUiUtils;
import org.wcs.smart.dataentry.internal.Messages;
import org.wcs.smart.dataentry.model.CmAttribute;
import org.wcs.smart.dataentry.model.CmAttributeOption;
import org.wcs.smart.dataentry.visiblewhen.filter.Parser;
import org.wcs.smart.filter.AttributeFilter;
import org.wcs.smart.filter.BooleanFilter;
import org.wcs.smart.filter.BracketFilter;
import org.wcs.smart.filter.IFilter;
import org.wcs.smart.filter.NotFilter;
import org.wcs.smart.filter.Operator;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.NamedItemLabelProvider;
import org.wcs.smart.ui.SmartStyledTitleDialog;
import org.wcs.smart.ui.ca.datamodel.dropitem.AttributeDropItem;
import org.wcs.smart.ui.ca.datamodel.dropitem.AttributeListDropItem;
import org.wcs.smart.ui.ca.datamodel.dropitem.AttributeMListDropItem;
import org.wcs.smart.ui.ca.datamodel.dropitem.AttributeTreeDropItem;
import org.wcs.smart.ui.ca.datamodel.dropitem.BooleanOpDropItem;
import org.wcs.smart.ui.ca.datamodel.dropitem.BracketDropItem;
import org.wcs.smart.ui.ca.datamodel.dropitem.BracketDropItem.BracketType;
import org.wcs.smart.util.SmartUtils;
import org.wcs.smart.ui.ca.datamodel.dropitem.DropItem;
import org.wcs.smart.ui.ca.datamodel.dropitem.ErrorDropItem;
import org.wcs.smart.ui.ca.datamodel.dropitem.IDropItemFactory;
import org.wcs.smart.ui.ca.datamodel.dropitem.ListItem;
import org.wcs.smart.ui.ca.datamodel.dropitem.NotDropItem;

/**
 * Dialog for configuring visible when expression for configurable model
 * 
 * @author Emily
 *
 */
public class VisibleWhenDialog extends SmartStyledTitleDialog {

	private static final String DROPITEM_KEY = "DropItem"; //$NON-NLS-1$

	private CmAttribute attribute;
	
	private ComboViewer cmbAttribute;
	private Composite main;
	private Composite expression;
	
	private Composite advanced;
	
	private DefinitionPanel definitionPanel;
	
	public String stringExpression;
	
	public VisibleWhenDialog(Shell parent, CmAttribute attribute) {
		super(parent);
		this.attribute = attribute;
	}
	
	@Override
	public void okPressed() {
		if (!validate()) return;
		
		stringExpression = getQuery();
		super.okPressed();
	}
	
	/**
	 * 
	 * @return the query string representing the 
	 * visible when expression
	 */
	public String getQueryExpression() {
		return this.stringExpression;
	}
	
	@Override
	public Control createDialogArea(Composite parent){
		setTitle(MessageFormat.format(Messages.VisibleWhenDialog_Title, attribute.getName()));
		getShell().setText(Messages.VisibleWhenDialog_ShellTitle);
		setMessage(Messages.VisibleWhenDialog_Message);
		
		definitionPanel = new DefinitionPanel(parent.getShell()) {
			@Override
			public void fireQueryChangedListeners(){
				modified();
			}
		};
		
		Composite composite = (Composite)super.createDialogArea(parent);
		
		main = new Composite(composite, SWT.NONE);
		main.setLayout(new GridLayout(1, false));
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
				
		expression = new Composite(main, SWT.NONE);
		expression.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		expression.setLayout(new GridLayout());
		((GridLayout)expression.getLayout()).marginWidth = 0;
		((GridLayout)expression.getLayout()).marginHeight = 0;
		
		Link lnkAdvanced = new Link(main, SWT.NONE);
		lnkAdvanced.setText("<a>" + Messages.VisibleWhenDialog_AdvancedOption + "</a>"); //$NON-NLS-1$ //$NON-NLS-2$
		lnkAdvanced.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, true, false));
		
		lnkAdvanced.addListener(SWT.Selection, e->{
			
			if (advanced == null) {
				showAdvanced();
				lnkAdvanced.setText("<a>" + Messages.VisibleWhenDialog_BasicOption + "</a>"); //$NON-NLS-1$ //$NON-NLS-2$
			}else {
				showBasic();
				lnkAdvanced.setText("<a>" + Messages.VisibleWhenDialog_AdvancedOption + "</a>"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			
			expression.getParent().layout(true);	
			expression.layout(true);
			getShell().layout(true);
		});

		String query = attribute.getCmAttributeOptions().get(CmAttributeOption.ID_IS_VISIBLE).getStringValue();
		if (query != null && !query.isBlank()) {
			showAdvanced();
			parseQuery(query);
			validate();
			lnkAdvanced.setText("<a>" + Messages.VisibleWhenDialog_BasicOption + "</a>"); //$NON-NLS-1$ //$NON-NLS-2$
		}else {
			showBasic();
		}
		return composite; 
	}

	private void showBasic() {
		if (advanced != null) {
			definitionPanel.clear();
			advanced.dispose();
			advanced = null;
		}
		for (Control c : expression.getChildren()) {
			if (c instanceof Composite) ((Composite)c).dispose();
		}
		createBasicPanel(expression);
		getShell().layout(true, true);
	}

	private void showAdvanced() {
		for (Control c : expression.getChildren()) {
			if (c instanceof Composite) ((Composite)c).dispose();
		}
		advanced = createAdvancedPanel(expression);
		getShell().layout(true, true);
	}

	private void parseQuery(String query) {
		definitionPanel.clear();
		Parser parser = new Parser(new StringReader(query));
		try {
			List<DropItem> items = new ArrayList<>();
			IFilter part = parser.ParseQuery();
			try(Session session = HibernateManager.openSession()){
				generateDropItem(part, items, session);
			}
			definitionPanel.addItems(items);
		}catch (Exception ex) {
			definitionPanel.addItem(new ErrorDropItem(Messages.VisibleWhenDialog_ParseQueryError + ex.getMessage()));
		}
	}
	
	private void generateDropItem(IFilter filter, List<DropItem> items, Session session){
		if (filter instanceof BracketFilter) {
			items.add(new BracketDropItem(BracketType.OPEN));
			generateDropItem(((BracketFilter)filter).getFilter(), items, session);
			items.add(new BracketDropItem(BracketType.CLOSE));
		}else if (filter instanceof NotFilter) {
			items.add(new NotDropItem());
			generateDropItem(((NotFilter)filter).getFilter(), items, session);
		}else if (filter instanceof BooleanFilter) {
			BooleanFilter bfilter = (BooleanFilter)filter;
			generateDropItem(bfilter.getFilter1(),items, session);
			DropItem di = new BooleanOpDropItem();
			di.initializeData(bfilter.getOperator());
			items.add(di);
			generateDropItem(bfilter.getFilter2(), items, session);
		}else if (filter instanceof AttributeFilter) {
			AttributeFilter afilter = (AttributeFilter)filter;
			items.add(generateAttributeDropItem(afilter, session));
		}else {
			//filter not supported
			items.add(new ErrorDropItem(Messages.VisibleWhenDialog_FilterNotSupported));
		}
	}
	
	private DropItem generateAttributeDropItem(AttributeFilter afilter, Session session) {
		Attribute dattribute = QueryFactory.buildQuery(session, Attribute.class, 
				new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}, //$NON-NLS-1$
				new Object[] {"keyId", afilter.getAttributeKey()}).uniqueResult(); //$NON-NLS-1$
		
		if (dattribute == null) {
			return new ErrorDropItem(MessageFormat.format(Messages.VisibleWhenDialog_AttributeNotFound, afilter.getAttributeKey()));
		}else if (!dattribute.getType().equals( afilter.getAttributeType()) ) {
			return new ErrorDropItem(MessageFormat.format(Messages.VisibleWhenDialog_AttributeTypeDifferent, afilter.getAttributeKey()));
		}else {
			Category c = attribute.getNode().getCategory();
			List<Attribute> all = new ArrayList<>();
			c.getAllAttribute(all, null);
			if (!all.contains(dattribute)) {
				return new ErrorDropItem(MessageFormat.format(Messages.VisibleWhenDialog_AttributeNotAssociatedWithCategory,  dattribute.getName(), c.getName()));
			}else {
				DropItem di = generateDropItem(dattribute);
				
				switch(dattribute.getType()) {
				case BOOLEAN:
					break;
				case DATE:
					di.initializeData(new Object[] { afilter.getValue(), afilter.getValue2(), afilter.getOperator() });
					break;
				case LIST:
					String key = (String) afilter.getValue();
					ListItem init = null;
					if (key.equals(AttributeFilter.ANY_OPTION_KEY)) {
						init = IDropItemFactory.ANY_OPTION;
					}else {
						for (AttributeListItem li : dattribute.getAttributeList()) {
							if (li.getKeyId().equals(key)) {
								init = new ListItem(li.getUuid(), li.getName(), li.getKeyId());
							}
						}
					}
					if (init == null) {
						di = new ErrorDropItem(MessageFormat.format(Messages.VisibleWhenDialog_ListItemNotFound, dattribute.getName(), key));
					}else {
						di.initializeData(init);
					}
					break;
				case MLIST:
					String[] keys = ((String) afilter.getValue()).split(AttributeFilter.MLIST_SEPERATOR);
					List<ListItem> inits = new ArrayList<>();
					for (String ikey : keys) {
						init = null;
						for (AttributeListItem li : dattribute.getAttributeList()) {
							if (li.getKeyId().equals(ikey)) {
								init = new ListItem(li.getUuid(), li.getName(), li.getKeyId());
							}
						}
						if (init == null) {
							di = new ErrorDropItem(MessageFormat.format(Messages.VisibleWhenDialog_ListItemNotFound2, dattribute.getName(), ikey));
							break;
						}else {
							inits.add(init);
						}
					}
					di.initializeData(new Object[] {afilter.getOperator(), inits});
					break;
				case NUMERIC:
					di.initializeData(new String[] { afilter.getOperator().asSmartValue(), ((Double) afilter.getValue()).toString()});
					break;
				case TEXT:
					di.initializeData(new String[] { afilter.getOperator().asSmartValue(), (String) afilter.getValue()});
					break;
				case TREE:
					String hkey = (String) afilter.getValue();
					ArrayDeque<AttributeTreeNode> nodes = new ArrayDeque<>();
					nodes.addAll(dattribute.getTree());
					boolean ok = false;
					while(!nodes.isEmpty()) {
						AttributeTreeNode node  = nodes.remove();
						if (node.getHkey().equalsIgnoreCase(hkey)) {
							di.initializeData(node);
							ok = true;
							break;
						}
						nodes.addAll(node.getChildren());
					}
					if (!ok) di = new ErrorDropItem(MessageFormat.format(Messages.VisibleWhenDialog_NotTreeNodeFound, hkey, dattribute.getName()));
					break;
				default:
					break;
				
				}
				return di;
			}
		}
	}
		
	private String getQuery() {
		if (advanced != null) {
			return definitionPanel.getQueryPart();
		}else {
			DropItem it = (DropItem) expression.getData(DROPITEM_KEY);
			if (it != null) {
				return it.asQueryPart();
			}
			return ""; //$NON-NLS-1$
		}
	}
	
	private boolean validate() {
		String query = getQuery();
		
		Parser parser = new Parser(new StringReader(query));
		try {
			parser.ParseQuery();
			setErrorMessage(null);
			return true;
		}catch (Exception ex) {
			setErrorMessage(ex.getMessage());
			return false;
		}

	}
	private void modified() {
		boolean enabled = validate();
		if (getButton(IDialogConstants.OK_ID) != null) getButton(IDialogConstants.OK_ID).setEnabled(enabled);
	}
	
	private void updateAttributeSelection() {
		cmbAttribute.getControl().setParent(main);
		for (Control c : expression.getChildren()) {
			if (c instanceof Composite) ((Composite)c).dispose();
		}
		
		CmAttribute a = (CmAttribute) cmbAttribute.getStructuredSelection().getFirstElement();
		if (a == null) return;
		
		DropItem i = generateDropItem(a.getAttribute());
		expression.setData(DROPITEM_KEY, i);
		Composite widget = i.createWidget(definitionPanel, expression);
		
		widget.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
		
		ArrayDeque<Control> c = new ArrayDeque<>();
		c.add(widget);
		while(!c.isEmpty()) {
			Control cc = c.remove();
			if (cc instanceof Label) {
				Label l = (Label) cc;
				if (l.getText() != null && !l.getText().trim().isEmpty()) {
					l.setText(l.getText().replaceAll(SmartUtils.formatStringForLabel(a.getAttribute().getName()), "")); //$NON-NLS-1$
					((GridLayout)l.getParent().getLayout()).numColumns = ((GridLayout)l.getParent().getLayout()).numColumns + 1; 
					cmbAttribute.getControl().setParent(l.getParent());
					cmbAttribute.getControl().moveAbove(l);
				}
				if (l.getImage() != null){
					l.dispose();
				}
			}else if (cc instanceof Composite) {
				for (Control kid : ((Composite)cc).getChildren()) c.add(kid);
			}
		}
	
		cmbAttribute.getControl().setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		expression.getParent().layout(true, true);	
//		expression.layout(true);
	}
	
	private void createBasicPanel(Composite parent) {
		cmbAttribute = new ComboViewer(parent, SWT.READ_ONLY | SWT.DROP_DOWN);
		cmbAttribute.setContentProvider(ArrayContentProvider.getInstance());
		cmbAttribute.setLabelProvider(new NamedItemLabelProvider());
		cmbAttribute.getControl().setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));

		List<CmAttribute> cmattributes = new ArrayList<>(attribute.getNode().getCmAttributes());
		cmattributes.remove(attribute);
		cmbAttribute.setInput(cmattributes);
		if (cmattributes.size() > 0) cmbAttribute.setSelection(new StructuredSelection(cmattributes.get(0)));
		
		cmbAttribute.addPostSelectionChangedListener(e->{
			updateAttributeSelection();
			modified();
		});
		updateAttributeSelection();
		
		SmartUiUtils.makeTransparent(parent);
	}
	
	private Composite createAdvancedPanel(Composite parent) {
		
		SashForm outer = new SashForm(parent, SWT.NONE);
		outer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite leftPart = new Composite(outer, SWT.BORDER);
		leftPart.setLayout(new GridLayout());
		leftPart.setBackground(outer.getDisplay().getSystemColor(SWT.COLOR_WHITE));
		
		Composite cc = definitionPanel.createComposite(leftPart);
		
		cc.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		cc.setBackground(leftPart.getBackground());
		((GridData)cc.getLayoutData()).heightHint = 200;
		
		Composite rightPart = new Composite(outer, SWT.BORDER);
		rightPart.setLayout(new GridLayout());
		rightPart.setBackground(leftPart.getBackground());
		((GridLayout)rightPart.getLayout()).marginWidth = 0;
		((GridLayout)rightPart.getLayout()).marginHeight = 0;
		
		
		TableViewer lstOptions = new TableViewer(rightPart, SWT.V_SCROLL);
		lstOptions.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		//((GridData)lstOptions.getControl().getLayoutData()).heightHint = 250;
		lstOptions.setContentProvider(ArrayContentProvider.getInstance());
		lstOptions.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof CmAttribute) return ((CmAttribute) element).getAttribute().getName();
				if (element instanceof Operator) return ((Operator)element).getGuiValue();
				return super.getText(element);
			}
			@Override
			public Image getImage(Object element) {
				if (element instanceof CmAttribute) return DataModel.getAttributeImage(((CmAttribute)element).getAttribute().getType());
				return super.getImage(element);
			}
		});
		lstOptions.addDoubleClickListener(event -> {
			for (Iterator<?> iterator = lstOptions.getStructuredSelection().iterator(); iterator.hasNext();) {
				Object type = iterator.next();
				DropItem di = null;
				if (type instanceof CmAttribute) {
					CmAttribute cm = (CmAttribute)type;
					di = generateDropItem(cm.getAttribute());
				}else if (type instanceof Operator) {
					if (type == Operator.BRACKETS) {
						definitionPanel.addItem(new BracketDropItem(BracketType.OPEN));
						definitionPanel.addItem(new BracketDropItem(BracketType.CLOSE));
					}else if (type == Operator.NOT) {
						di = new NotDropItem();
					}
				}
				
				if (di != null) definitionPanel.addItem(di);
				
			}
		});
		
		List<Object> cmattributes = new ArrayList<>(attribute.getNode().getCmAttributes());
		cmattributes.remove(attribute);
		cmattributes.add(Operator.NOT);
		cmattributes.add(Operator.BRACKETS);
		lstOptions.setInput(cmattributes);
		outer.setWeights(new int[] {5,3});
		
		return outer;
	}
	
	private DropItem generateDropItem(Attribute attribute) {
		switch(attribute.getType()) {
		case BOOLEAN:
		case DATE:
		case NUMERIC:
		case TEXT:
			return new AttributeDropItem(attribute);
		case LIST:
			AttributeListDropItem di = new AttributeListDropItem(attribute);
			di.setOnlyActive(true);
			return di;
		case MLIST:
			AttributeMListDropItem mdi = new AttributeMListDropItem(attribute);
			mdi.setOnlyActive(true);
			return mdi;
		case TREE:
			AttributeTreeDropItem tdi = new AttributeTreeDropItem(attribute);
			tdi.setOnlyActive(true);
			return tdi;
		};
		throw new IllegalStateException(MessageFormat.format(Messages.VisibleWhenDialog_InvalidType, attribute.getType().getName(Locale.getDefault())));
	}
}

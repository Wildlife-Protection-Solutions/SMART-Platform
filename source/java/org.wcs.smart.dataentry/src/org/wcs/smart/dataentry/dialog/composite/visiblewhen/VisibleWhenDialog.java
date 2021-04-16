package org.wcs.smart.dataentry.dialog.composite.visiblewhen;

import java.text.MessageFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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
import org.wcs.smart.ca.datamodel.DataModel;
import org.wcs.smart.common.control.SmartUiUtils;
import org.wcs.smart.dataentry.model.CmAttribute;
import org.wcs.smart.filter.Operator;
import org.wcs.smart.ui.NamedItemLabelProvider;
import org.wcs.smart.ui.SmartStyledTitleDialog;
import org.wcs.smart.ui.ca.datamodel.dropitem.AttributeDropItem;
import org.wcs.smart.ui.ca.datamodel.dropitem.AttributeListDropItem;
import org.wcs.smart.ui.ca.datamodel.dropitem.AttributeMListDropItem;
import org.wcs.smart.ui.ca.datamodel.dropitem.AttributeTreeDropItem;
import org.wcs.smart.ui.ca.datamodel.dropitem.BracketDropItem;
import org.wcs.smart.ui.ca.datamodel.dropitem.BracketDropItem.BracketType;
import org.wcs.smart.ui.ca.datamodel.dropitem.DropItem;
import org.wcs.smart.ui.ca.datamodel.dropitem.IDefinitionPanel;
import org.wcs.smart.ui.ca.datamodel.dropitem.NotDropItem;

public class VisibleWhenDialog extends SmartStyledTitleDialog {

	private CmAttribute attribute;
	
	private ComboViewer cmbAttribute;
	private Composite main;
	private Composite expression;
	
	private Composite advanced;
	
	private IDefinitionPanel definitionPanel;
	
	public VisibleWhenDialog(Shell parent, CmAttribute attribute) {
		super(parent);
		
		this.attribute = attribute;
		
		definitionPanel = new DefinitionPanel(parent) {
			@Override
			public void fireQueryChangedListeners(){
				modified();
			}
		};
	}
	
	@Override
	public Control createDialogArea(Composite parent){
		setTitle(MessageFormat.format("{0} - Enabled When", attribute.getName()));
		getShell().setText("Enabled When");
		setMessage("Configure expression for when this attribute is enabled in the UI or not");
		
		Composite composite = (Composite)super.createDialogArea(parent);
		
		main = new Composite(composite, SWT.NONE);
		main.setLayout(new GridLayout(1, false));
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
				
		expression = new Composite(main, SWT.NONE);
		expression.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		expression.setLayout(new GridLayout());
		((GridLayout)expression.getLayout()).marginWidth = 0;
		((GridLayout)expression.getLayout()).marginHeight = 0;
		
		createBasicPanel(expression);
		
		Link lnkAdvanced = new Link(main, SWT.NONE);
		lnkAdvanced.setText("<a>" + "Advanced..." + "</a>");
		lnkAdvanced.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, true, false));
		
		lnkAdvanced.addListener(SWT.Selection, e->{
			
			if (advanced == null) {
				for (Control c : expression.getChildren()) {
					if (c instanceof Composite) ((Composite)c).dispose();
				}
				advanced = createAdvancedPanel(expression);
				lnkAdvanced.setText("<a>" + "Basic..." + "</a>");
			}else {
				definitionPanel.clear();
				advanced.dispose();
				advanced = null;
				for (Control c : expression.getChildren()) {
					if (c instanceof Composite) ((Composite)c).dispose();
				}
				createBasicPanel(expression);
				lnkAdvanced.setText("<a>" + "Advanced..." + "</a>");
			}
			
			expression.getParent().layout(true);	
			expression.layout(true);
			getShell().layout(true);
		});

		return composite; 
	}

	private String getQuery() {
		if (advanced != null) {
			return definitionPanel.getQueryPart();
		}else {
//			CmAttribute a = (CmAttribute) cmbAttribute.getStructuredSelection().getFirstElement();
			DropItem it = (DropItem) expression.getData("DropItem");
			if (it != null) {
				return it.asQueryPart();
			}
			return "";
		}
	}
	
	private void modified() {
		System.out.println("Modified:");
		System.out.println(getQuery());
	}
	private void updateAttributeSelection() {
		cmbAttribute.getControl().setParent(main);
		for (Control c : expression.getChildren()) {
			if (c instanceof Composite) ((Composite)c).dispose();
		}
		
		CmAttribute a = (CmAttribute) cmbAttribute.getStructuredSelection().getFirstElement();
		if (a == null) return;
		
		DropItem i = null;
		
		switch(a.getAttribute().getType()) {
		case BOOLEAN:
		case NUMERIC:
		case DATE:
		case TEXT:
			i = new AttributeDropItem(a.getAttribute());
			break;
		case LIST:
			i = new AttributeListDropItem(a.getAttribute());
			break;
		case MLIST:
			i = new AttributeMListDropItem(a.getAttribute());
			break;
		case TREE:
			i = new AttributeTreeDropItem(a.getAttribute());
			break;
		}
		expression.setData("DropItem", i);
		Composite widget = i.createWidget(definitionPanel, expression);
		
		widget.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		ArrayDeque<Control> c = new ArrayDeque<>();
		c.add(widget);
		while(!c.isEmpty()) {
			Control cc = c.remove();
			if (cc instanceof Label) {
				Label l = (Label) cc;
				if (l.getText() != null && !l.getText().trim().isEmpty()) {
					l.setText(l.getText().replaceAll(a.getAttribute().getName(), ""));
					
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
		expression.getParent().layout(true);	
		expression.layout(true);
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
		
		Composite rightPart = new Composite(outer, SWT.BORDER);
		rightPart.setLayout(new GridLayout());
		rightPart.setBackground(leftPart.getBackground());
		((GridLayout)rightPart.getLayout()).marginWidth = 0;
		((GridLayout)rightPart.getLayout()).marginHeight = 0;
		
		TableViewer lstOptions = new TableViewer(rightPart, SWT.V_SCROLL);
		lstOptions.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
//		((GridData)lstOptions.getControl().getLayoutData()).heightHint = 250;
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
					switch(cm.getAttribute().getType()) {
					case BOOLEAN:
					case DATE:
					case NUMERIC:
					case TEXT:
						di = new AttributeDropItem(cm.getAttribute());
						break;
					case LIST:
						di = new AttributeListDropItem(cm.getAttribute());
						break;
					case MLIST:
						di = new AttributeMListDropItem(cm.getAttribute());
						break;
					case TREE:
						di = new AttributeTreeDropItem(cm.getAttribute());
						break;
					}
					
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
}

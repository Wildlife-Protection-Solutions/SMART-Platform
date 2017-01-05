package org.wcs.smart.i2.ui.views.query.dropitem;

import java.util.Locale;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.wcs.smart.i2.query.Operator;


public class TextBoxDropItem extends DropItem {

	public enum InputType{TEXT,NUMERIC};
	
	private String name;
	private String queryKeyPart;
	
	private InputType type;
	
	private Text value;
	private ComboViewer operators;
	
	private Operator currentOperator;
	private String currentValue;
	
	/**
	 * Creates a new are drop item that has 
	 * single text field label
	 * 
	 */
	public TextBoxDropItem(String name, String queryKeyPart, InputType type){
		this.name = name;
		this.queryKeyPart = queryKeyPart;
		this.type = type;
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
		if (type == InputType.TEXT){
			String strValue = value.getText().replaceAll("\"", "");
			return queryKeyPart + " " + ((Operator) ((IStructuredSelection)operators.getSelection()).getFirstElement()).getKey() + " \"" + strValue + "\"";
		}else{
			return queryKeyPart + " " + ((Operator) ((IStructuredSelection)operators.getSelection()).getFirstElement()).getKey() + " " + value.getText();
		}
	}

	public void setInitialValue(Operator op, String data){
		this.currentOperator = op;
		this.currentValue = data;
	}

	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#createComposite(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected void createComposite(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(4, false);
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		main.setLayout(layout);
		main.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, true));
		
		
		Label lbl = new Label(main, SWT.NONE);
		lbl.setText( formatStringForLabel(getText()));  //$NON-NLS-1$//$NON-NLS-2$
		initDrag(lbl);

		operators = new ComboViewer(main, SWT.DROP_DOWN | SWT.READ_ONLY);
		operators.setContentProvider(ArrayContentProvider.getInstance());
		operators.setLabelProvider(new OperatorLabelProvider());
		if (type == InputType.NUMERIC) {
			operators.setInput(Operator.NUMERIC_OPS);
		} else if (type == InputType.TEXT) {
			operators.setInput(Operator.STRING_OPS);
		}
		FontData fd = (operators.getControl().getFont().getFontData()[0]);
		fd.setHeight(fd.getHeight() - 1);
		Font smallerFont = new Font(Display.getCurrent(), fd);
		operators.getControl().setFont(smallerFont);
		operators.getControl().addListener(SWT.Dispose, e->smallerFont.dispose());
		
		operators.getControl().addListener(SWT.Modify, e->{
			Operator current = (Operator) ((IStructuredSelection)operators.getSelection()).getFirstElement();
			if (current != null && !current.equals(currentOperator)){
				currentOperator = current;
				queryChanged();
			}
		});		

		value = new Text(main, SWT.BORDER);
		value.addListener(SWT.Modify, e->{
			queryChanged();
		});

		GridData gd = new GridData();
		gd.minimumWidth = 50;
		gd.widthHint = 100;
		value.setLayoutData(gd);

		if (currentOperator != null){
			operators.setSelection(new StructuredSelection(currentOperator));
		}else{
			operators.setSelection(new StructuredSelection(((Object[])operators.getInput())[0]));
		}
		if (currentValue != null){
			value.setText(currentValue);
		}
		
	}

}

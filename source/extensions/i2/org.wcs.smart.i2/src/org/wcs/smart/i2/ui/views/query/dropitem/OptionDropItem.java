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
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.wcs.smart.i2.query.Operator;

public class OptionDropItem extends DropItem {

	public static OptionDropItem createAndOrDropItem(){
		return new OptionDropItem(new String[]{Operator.AND.getLabel(Locale.getDefault()), Operator.OR.getLabel(Locale.getDefault())}, new String[]{Operator.AND.getKey(), Operator.OR.getKey()});
	}
	private Option[] options;
	
	private ComboViewer combo;
	private Option currentOption;
	
	private String label;
	private String key;
	
	public OptionDropItem(String[] labels, String[] queryPart){
		this(null, null, labels, queryPart);
	}
	
	public OptionDropItem(String label, String key, String[] labels, String[] queryPart){
		this.label = label;
		this.key = key;
		options = new Option[labels.length];
		for (int i= 0; i < labels.length; i ++){
			options[i] = new Option(labels[i], queryPart[i]);
		}
	}
	
	private Option getSelection(){
		return (Option) ((IStructuredSelection) combo.getSelection()).getFirstElement();
	}
	
	@Override
	public String getText() {
		if (label != null){
			return label + " = ";
		}else{
			Option x = getSelection();
			if (x == null) return "";
			return x.label;
		}
	}

	@Override
	public String asQueryPart() {
		StringBuilder sb = new StringBuilder();
		if (key  != null){
			sb.append(key);
			sb.append(Operator.EQUALS.getKey());
		}
		Option x = getSelection();
		if (x != null){
			sb.append(x.key);
		}
		return sb.toString();
	}

	@Override
	public void initializeData(Object data) {
	}

	@Override
	protected void createComposite(Composite parent) {
		parent = new Composite(parent, SWT.NONE);
		parent.setLayout(new GridLayout(label==null?1:2, false));
		((GridLayout)parent.getLayout()).marginWidth = 0;
		((GridLayout)parent.getLayout()).marginHeight = 0;
		
		if (this.label != null){
			Label l = new Label(parent, SWT.NONE);
			l.setText(formatStringForLabel(label + " = "));
		}
		combo = new ComboViewer(parent, SWT.DROP_DOWN | SWT.READ_ONLY);
		combo.setContentProvider(ArrayContentProvider.getInstance());
		combo.setLabelProvider(new LabelProvider(){
			public String getText(Object element){
				if (element instanceof Option){
					return ((Option) element).label;
				}
				return super.getText(element);
			}
		});
		combo.setInput(options);
		
		FontData fd = (combo.getControl().getFont().getFontData()[0]);
		fd.setHeight(fd.getHeight() - 1);
		Font smallerFont = new Font(Display.getCurrent(), fd);
		combo.getControl().setFont(smallerFont);
		combo.getControl().addListener(SWT.Dispose, e->smallerFont.dispose());
		combo.getControl().addListener(SWT.Modify, e->{
			Option selection = getSelection();
			if (selection != null && !selection.equals(currentOption)){
				queryChanged();
			}
		});
		if (currentOption == null){
			currentOption = options[0];
		}
		
		combo.setSelection(new StructuredSelection(currentOption));
	}

	private class Option{
		public String label;
		public String key;
		public Option(String label, String key){
			this.label = label;
			this.key = key;
		}
		
	}
}

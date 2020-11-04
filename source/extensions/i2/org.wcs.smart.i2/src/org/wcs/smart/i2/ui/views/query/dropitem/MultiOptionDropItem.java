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
package org.wcs.smart.i2.ui.views.query.dropitem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.wcs.smart.i2.query.Operator;
import org.wcs.smart.i2.query.observation.filter.DataModelFilter;
import org.wcs.smart.ui.CheckBoxDropDown;

/**
 * Drop item that displays a combo box where users can select
 * a value.
 * 
 * @author Emily
 *
 */
public class MultiOptionDropItem extends DropItem {

	public static MultiOptionDropItem createAndOrDropItem(boolean canEdit){
		return new MultiOptionDropItem(new String[]{
				Operator.AND.getLabel(Locale.getDefault()), 
				Operator.OR.getLabel(Locale.getDefault())}, 
				new String[]{Operator.AND.getKey(), Operator.OR.getKey()}, canEdit);
	}
	
	private Option[] options;
	
	
	protected CheckBoxDropDown listViewer;
	protected ComboViewer opViewer;

	
	protected Collection<Option> currentOptions = null;
	protected Operator currentOp = null;
	
	private String label;
	private String key;
	
	private boolean canEdit;
	
	public MultiOptionDropItem(String[] labels, String[] queryPart, boolean canEdit){
		this(null, null, labels, queryPart, canEdit);
	}
	
	public MultiOptionDropItem(String label, String key, String[] labels, String[] queryPart, boolean canEdit){
		this.label = label;
		this.key = key;
		this.canEdit = canEdit;
		options = new Option[labels.length];
		for (int i= 0; i < labels.length; i ++){
			options[i] = new Option(labels[i], queryPart[i]);
		}
		
		currentOp = Operator.MULTI_LIST_OPERATORS[0];
	}
	
	
	@Override
	public String getText() {
		if (label != null){
			return label + " = "; //$NON-NLS-1$
		}else{
			Option x = currentOptions.size() > 0 ? currentOptions.iterator().next() : null;
			if (x == null) return ""; //$NON-NLS-1$
			return x.label;
		}
	}

	@Override
	public String asQueryPart() {
		StringBuilder sb = new StringBuilder();
		if (key  != null){
			sb.append(key);
			sb.append(" "); //$NON-NLS-1$
			sb.append(currentOp.getKey());
			sb.append(" "); //$NON-NLS-1$
		}
		if (currentOptions == null) return sb.toString();
		
		for (Option o : currentOptions) {
			sb.append(o.key);
			sb.append(DataModelFilter.MLIST_SEPERATOR);
		}
		
		return sb.substring(0, sb.length() - 1);
	}

	public void setInitialValue(Collection<String> key) {
		this.currentOptions = new ArrayList<>();
		for (String k : key) {
			for (Option o : options){
				if (o.key.equalsIgnoreCase(k)){
					currentOptions.add(o);
					break;
				}
			}
		}
	}

	@Override
	protected void createComposite(Composite parent) {
		parent = new Composite(parent, SWT.NONE);
		parent.setLayout(new GridLayout(label==null ? 2 : 3, false));
		((GridLayout)parent.getLayout()).marginWidth = 0;
		((GridLayout)parent.getLayout()).marginHeight = 0;
		
		if (this.label != null){
			Label l = new Label(parent, SWT.NONE);
			l.setText(formatStringForLabel(label + " = ")); //$NON-NLS-1$
			initDrag(l);
		}

		opViewer = new ComboViewer(parent, SWT.DROP_DOWN | SWT.READ_ONLY);
		opViewer.setContentProvider(ArrayContentProvider.getInstance());
		opViewer.setLabelProvider(new OperatorLabelProvider());
		opViewer.setInput(Operator.MULTI_LIST_OPERATORS);
		opViewer.getControl().setEnabled(canEdit);
		opViewer.setSelection(new StructuredSelection(Operator.MULTI_LIST_OPERATORS[0]));
		if (currentOp != null) {
			opViewer.setSelection(new StructuredSelection(currentOp));
		}
		opViewer.addSelectionChangedListener(e->{
			currentOp = (Operator) opViewer.getStructuredSelection().getFirstElement();
			queryChanged();	
		});

		Composite color = new Composite(parent, SWT.NONE);
		color.setLayout(new GridLayout());
		((GridLayout)color.getLayout()).marginWidth = 0;
		((GridLayout)color.getLayout()).marginHeight = 0;
		color.setBackground(color.getDisplay().getSystemColor(SWT.COLOR_WHITE));
		
		listViewer = new CheckBoxDropDown(color);
		listViewer.setEnabled(canEdit);
		listViewer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)listViewer.getLayoutData()).widthHint = 200;
		
		FontData fd = (listViewer.getFont().getFontData()[0]);
		fd.setHeight(fd.getHeight() - 1);
		Font smallerFont = new Font(Display.getCurrent(), fd);
		listViewer.setFont(smallerFont);
		listViewer.addListener(SWT.Dispose, e->smallerFont.dispose());
		listViewer.setContentProvider(ArrayContentProvider.getInstance());
		listViewer.setLabelProvider(new LabelProvider(){
			public String getText(Object element){
			if (element instanceof Option){
				return ((Option) element).label;
			}
			return super.getText(element);
		}
		});
		
		listViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				
				List<Option> selection = new ArrayList<>();
				for (Object x : listViewer.getCheckObjects()) {
					selection.add((Option)x);
				}
				
				Collection<Option> lastSelection = currentOptions;
				
				currentOptions = selection;
				
				if (!(lastSelection != null && selection.size() == lastSelection.size()
						&& selection.containsAll(lastSelection))){
					queryChanged();	
				}
			}
		});
		
		listViewer.setInput(Arrays.asList(options));
		
		initDrag(parent);
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

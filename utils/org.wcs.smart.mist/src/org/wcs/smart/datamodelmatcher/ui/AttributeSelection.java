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

package org.wcs.smart.datamodelmatcher.ui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.wcs.smart.internal.ca.datamodel.xml.generate.ListNode;
import org.wcs.smart.internal.ca.datamodel.xml.generate.TreeNodeType;

public class AttributeSelection {
	public static String useTotalObservations = "<Use TOTAL_OBSERVED from MIST>";
	private Combo langSelector;
	private String[] codes;
	
	private ComboViewer attr1;
	private Composite value1;
	
	private Combo value1Combo;
	private ComboViewer value1ComboViewer;
	private Text value1Text;
    private Tree value1Tree;
	
	
	public void init(String[] codes, Combo langSelector){
		this.codes = codes;
		this.langSelector = langSelector;
	}
	
	public void CreateAttribute(Composite tab1Composite){
	      Label smartLabel2 = new Label(tab1Composite, SWT.NONE);
	      smartLabel2.setText("Attribute #1:" );
	      smartLabel2.setLayoutData(new GridData(SWT.TOP, SWT.LEFT, false, false));
	      
	      attr1 =  new ComboViewer (tab1Composite, SWT.READ_ONLY);
	      attr1.setContentProvider(ArrayContentProvider.getInstance());
	      attr1.setLabelProvider(new LabelProvider() {
	    	  @Override
	    	  public String getText(Object element) {
	    	    return ((Attribute)element).getText();
	    	  }
	    	});
	      attr1.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));

	      attr1.addSelectionChangedListener(new ISelectionChangedListener() {
			
	    	  @Override
	    	  public void selectionChanged(SelectionChangedEvent arg0) {
	  		    	String name;
	  		    	String lang;
	    		  	int langIndex = langSelector.getSelectionIndex();
	  		    	if (langIndex != -1 && langIndex < codes.length && codes != null) {
		  				lang = (String) codes[langIndex];
	  		    	}else{
	  		    		lang = "";
	  		    	}

	    			ISelection selection = attr1.getSelection();
	    	  		if (!selection.isEmpty()) {
	    	  		    IStructuredSelection structuredSelection = (IStructuredSelection) selection;      
	    	  		    Attribute selected = (Attribute) structuredSelection.getFirstElement();
	    	  		    String type = selected.getAttributeType().getType();
	    	  		    if( type.equals("NUMERIC") ){
	    	  		    	value1Text.setText(useTotalObservations);
	    	  		    	((StackLayout)value1.getLayout()).topControl = value1Text;
	    	  		    }else if( type.equals("TEXT") ){
	    	  		    	value1Text.setText("");
	    	  		    	((StackLayout)value1.getLayout()).topControl = value1Text;
	    	  		    }else if( type.equals("LIST") ){
	    	  		    	ArrayList<ListOption> options = new ArrayList<ListOption>();
	    	  		    	List<ListNode> list = selected.getAttributeType().getValues();
	    	  		    	for(ListNode i : list){
		    					name = i.getNames().get(0).getValue();
		    					for(int x=0; x < i.getNames().size(); x++){
		    						if(i.getNames().get(x).getLanguageCode().equals(lang)){
		    							name = i.getNames().get(x).getValue();
		    							break;
		    						}
		    					}
		    					options.add(new ListOption(name, i.getKey()));
		    				}
	    	  		    	value1ComboViewer.setInput(options);
	    	  		    	((StackLayout)value1.getLayout()).topControl = value1ComboViewer.getControl();
	    	  		    }else if( type.equals("TREE") ){
	    	  		    	value1Tree.removeAll();
	    	  		    	TreeItem iItem = new TreeItem (value1Tree, 0);
	    	  		    	iItem.setText("Options:");
	    	  		    	List<TreeNodeType> list = selected.getAttributeType().getTrees();
	    	  		    	addAttributesToTree(iItem, list, lang);

	    	  		    	((StackLayout)value1.getLayout()).topControl = value1Tree;
	    	  			}else if( type.equals("BOOLEAN") ){
	    	  				((StackLayout)value1.getLayout()).topControl = value1Combo;
	    	  		    }
	    	  		    value1.layout();
	    	  		}
	    	  }
	      });
	      	      
	      Label smartLabel3 = new Label(tab1Composite, SWT.NONE);
	      smartLabel3.setText("Value #1:" );
	      smartLabel3.setLayoutData(new GridData(SWT.TOP, SWT.LEFT, false, false));

	      //value1 options
	      value1 = new Composite(tab1Composite, SWT.NONE);
	      value1.setLayout(new StackLayout());
	      value1.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
	      
	      value1Combo = new Combo(value1, SWT.DROP_DOWN | SWT.SIMPLE | SWT.READ_ONLY);
	      value1Combo.setItems(new String[]{"TRUE", "FALSE"});
	      value1Combo.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, false, false));

	      value1ComboViewer =  new ComboViewer (value1, SWT.READ_ONLY);
	      value1ComboViewer.setContentProvider(ArrayContentProvider.getInstance());
	      value1ComboViewer.setLabelProvider(new LabelProvider() {
	    	  @Override
	    	  public String getText(Object element) {
	    	    return ((ListOption)element).getName();
	    	  }
	    	});
	      value1ComboViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
	      
	      
	      value1Text = new Text(value1, SWT.BORDER | SWT.SINGLE);
	      GridData gridtxt1Data = new GridData(SWT.LEFT, SWT.TOP, false, false);
	      gridtxt1Data.heightHint = 20;
	      value1Text.setLayoutData(gridtxt1Data);
	      value1Text.setText(useTotalObservations);
	      
	      value1Tree = new Tree(value1, SWT.BORDER | SWT.SINGLE);
	      TreeItem vItem = new TreeItem (value1Tree, 0);
	      vItem.setText("<Select an Attribute to populate possible values>");
	      
	      GridData gridtree1Data = new GridData(SWT.FILL, SWT.FILL, true, true);
	      gridtree1Data.heightHint = 100;
	      value1Tree.setLayoutData(gridtree1Data);
	      
	      ((StackLayout)value1.getLayout()).topControl = value1Combo;

	}
	
	public ComboViewer getAttrComboViwer(){
		return attr1;
	}
	
	public Combo getValueCombo(){
		return value1Combo;
	}
	public ComboViewer getValueComboViewer(){ 
		return value1ComboViewer;
	}
	public Text getValueText(){
		return value1Text;
	}
    public Tree getValueTree(){
    	return value1Tree;
    }
    
	protected void addAttributesToTree(TreeItem tree, List<TreeNodeType> list, String languageCode) {
		int i=0;
		  while(i < list.size()){
			  String name = list.get(i).getNames().get(0).getValue();
			  //String key = cat.get(i).getKey();
			  TreeItem iItem = new TreeItem (tree, 0);
			  for(int x=0; x < list.get(i).getNames().size(); x++){
				  if(list.get(i).getNames().get(x).getLanguageCode().equals(languageCode)){
					  name = list.get(i).getNames().get(x).getValue();
					  break;
				  }
			  }
			  iItem.setText(name);
			  iItem.setData(list.get(i));
			  addAttributesToTree(iItem, list.get(i).getChildrens(), languageCode);
			  i++;
		  }
	}

	
	public void clearValues(){
		value1Tree.removeAll();
		value1Text.setText("");
		//value1Combo.clearSelection();//doesn't seem to work for some reason...
		value1Combo.removeAll();
		value1Combo.setItems(new String[]{"TRUE", "FALSE"});
		
		value1ComboViewer.setInput(null);
	}

	public void update(String attrKey, boolean b, String text, String listKey, String treeKey) {
		//set the attribute
		for(int x=0; attr1.getElementAt(x) != null; x++){
				Attribute o = (Attribute)attr1.getElementAt(x);
				if(o.getKey().equals(attrKey) ){
					attr1.setSelection(new StructuredSelection(o) );
				}
		}
		
		//set the boolean dropdown
		if(b){
			value1Combo.select(value1Combo.indexOf("TRUE"));
		}else{
			value1Combo.select(value1Combo.indexOf("FALSE"));
		}

		//set the text
		if(text != null){
			value1Text.setText(text);
		}
		
		//set the list
		if(listKey != null && !listKey.equals("")){
			for(int x=0; value1ComboViewer.getElementAt(x) != null; x++){
				ListOption o = (ListOption) value1ComboViewer.getElementAt(x);
				if(o.getKey().equals(listKey)){
					value1ComboViewer.setSelection(new StructuredSelection(o));
				}
			}
		}
		
		//set the tree
		if(treeKey != null && !treeKey.equals("")){
			updateTree(treeKey, value1Tree.getItem(0),"");
		}
		
	}
	
	private boolean updateTree(String catKey, TreeItem ti, String prefix) {
		for(int x=0; x < ti.getItemCount(); x++){
			TreeNodeType c = (TreeNodeType)ti.getItem(x).getData();
			if(c != null){ 
				if( (prefix + c.getKey()).equals(catKey)){
					value1Tree.setSelection(ti.getItem(x));
					return true;
				}else{
					if(updateTree(catKey, ti.getItem(x), prefix + c.getKey() + ".")) return true;
				}
			}
		}
		return false;
	}

}

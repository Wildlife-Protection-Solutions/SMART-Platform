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
package org.wcs.smart.er.query.ui.panels.item;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.graphics.Image;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.dataentry.DataentryPlugIn;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.query.ui.panels.item.configmodel.ConfigurableModelTreeNode;
import org.wcs.smart.query.common.ui.itempanel.DataModelTreeNode;
import org.wcs.smart.query.common.ui.itempanel.DataModelTreeNode.Type;
import org.wcs.smart.query.common.ui.itempanel.IItemTreeNode;
import org.wcs.smart.query.common.ui.itempanel.WrappedTreeNode;

/**
 * Survey filters data model tree node that supports configurable model
 * as well as full data model.
 * 
 * @author Emily
 *
 */
public class SurveyDataModelTreeNode implements IItemTreeNode{

	public static final String KEY = "datamodel"; //$NON-NLS-1$
	
	private String name;
	
	private SurveyDesign currentDesign;
	private DataModelTreeNode dataModelNode;
	
	private ConfigurableModelTreeNode configModelNode;
	
			
	private enum TreeNode{
		DATA_MODEL("Full Data Model", SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DATA_MODEL_ICON)),
		CONFIG_MODEL("Configurable Model", DataentryPlugIn.getDefault().getImageRegistry().get(DataentryPlugIn.CONFIG_MODEL_ICON));
		
		String guiName;
		Image icon;
		
		TreeNode(String name, Image icon){
			guiName = name;
			this.icon = icon;
		}
	}
	
	/**
	 * type of node
	 * @param type
	 */
	public SurveyDataModelTreeNode(Type type){
		dataModelNode = new DataModelTreeNode(type);
		configModelNode = new ConfigurableModelTreeNode(type);
		this.name = "Data Model";
		
	}
	
	@Override
	public String getName() {
		return name;
	}

	@Override
	public ITreeContentProvider getContentProvider() {
		return new ITreeContentProvider() {
			
			@Override
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
				if (newInput == null) return;
				
				Object[] values = (Object[])newInput;
				currentDesign = (SurveyDesign) values[0];
				dataModelNode.getContentProvider().inputChanged(viewer, oldInput, values[1]);
				configModelNode.getContentProvider().inputChanged(viewer, oldInput, values[2]);
			}
			
			@Override
			public void dispose() {
			}
			
			@Override
			public boolean hasChildren(Object element) {
				if (element instanceof TreeNode){
					return true;
				}else if (element instanceof WrappedTreeNode){
					return ((WrappedTreeNode)element).getParent().getContentProvider().hasChildren(((WrappedTreeNode) element).getItem());
				}
				return false;
			}
			
			@Override
			public Object getParent(Object element) {
				if (element instanceof TreeNode){
					return null;
				}else if (element instanceof WrappedTreeNode){
					WrappedTreeNode welement = (WrappedTreeNode) element;
					return welement.getParent().getContentProvider().getParent(welement.getItem());
				}
				return null;
			}
			
			@Override
			public Object[] getElements(Object inputElement) {
				if (currentDesign == null || currentDesign.getConfigurableModel() == null){
					Object[] x = dataModelNode.getContentProvider().getElements(inputElement);
					Object[] y = new Object[x.length];
					for (int i = 0; i < x.length; i ++){
						y[i] = new WrappedTreeNode(dataModelNode, x[i]);
					}
					return y;
				}else{
					return TreeNode.values();
				}
			}
			
			@Override
			public Object[] getChildren(Object parentElement) {
				
				if (parentElement == TreeNode.DATA_MODEL){
					Object[] x = dataModelNode.getContentProvider().getElements(parentElement);
					Object[] y = new Object[x.length];
					for (int i = 0; i < x.length; i ++){
						y[i] = new WrappedTreeNode(dataModelNode, x[i]);
					}
					return y;
				}else if (parentElement == TreeNode.CONFIG_MODEL){
					//config model root
					Object[] x = configModelNode.getContentProvider().getElements(parentElement);
					Object[] y = new Object[x.length];
					for (int i = 0; i < x.length; i ++){
						y[i] = new WrappedTreeNode(configModelNode, x[i]);
					}
					return y;
				}else if (parentElement instanceof WrappedTreeNode){
					WrappedTreeNode element = (WrappedTreeNode) parentElement;
					
					Object[] x = element.getParent().getContentProvider().getChildren(element.getItem());
					Object[] y = new Object[x.length];
					for (int i = 0; i < x.length; i ++){
						y[i] = new WrappedTreeNode(element.getParent(), x[i]);
					}
					return y;
				}
				return null;
			}
		};
	}

	@Override
	public ILabelProvider getLabelProvider() {
		return new LabelProvider(){
			@Override
			public String getText(Object element){
				if (element instanceof TreeNode){
					return ((TreeNode) element).guiName;
				}else if (element instanceof WrappedTreeNode){
					WrappedTreeNode we = (WrappedTreeNode)element;
					return we.getParent().getLabelProvider().getText(we.getItem());
				}
				return super.getText(element);
			}
			
			@Override
			public Image getImage(Object element){
				if (element instanceof TreeNode){
					return ((TreeNode) element).icon;
				}else if (element instanceof WrappedTreeNode){
					WrappedTreeNode we = (WrappedTreeNode)element;
					return we.getParent().getLabelProvider().getImage(we.getItem());
				}
				return super.getImage(element);
			}
		};
		
	}
	@Override
	public String getKey() {
		return KEY;
	}
	@Override
	public Image getImage() {
		return SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DATA_MODEL_ICON);
	}


}

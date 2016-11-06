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
package org.wcs.smart.query.common.ui.itempanel;

import java.text.Collator;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.e4.tools.compat.parts.DIViewPart;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.DataModel;
import org.wcs.smart.query.QueryDataModelManager;
import org.wcs.smart.query.QueryFilterConfigManager;
import org.wcs.smart.query.QueryFilterConfigManager.IConfigurationChangeListener;
import org.wcs.smart.query.common.model.QueryFilterConfiguration;
import org.wcs.smart.query.internal.DataModelManagerUtil;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.ui.itempanel.QueryItemView;
import org.wcs.smart.ui.properties.DataModelContentProvider;
import org.wcs.smart.ui.properties.DataModelLabelProvider;
/**
 * Content provider for providing data model item for query filters.
 * 
 * @author Emily
 *
 */
public class FiltersDataModelContentProvider implements ITreeContentProvider{

	//data model 
	private DataModel dataModel = null;
	private DataModelContentProvider provider;
	
	/**
	 * Data model children items
	 */
	enum DataModelItem{
		CATEGORIES(Messages.FiltersDataModelContentProvider_CategoriesLabel, SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.CATEGORY_ICON)),
		ATTRIBUTES(Messages.FiltersDataModelContentProvider_AttributesLabel, SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ATTRIBUTE_NUMBER_ICON));
		
		String guiName;
		Image image;
		DataModelItem(String guiName, Image image){
			this.guiName = guiName;
			this.image = image;
		}
	}

	private IConfigurationChangeListener queryConfChangeListener = new IConfigurationChangeListener() {
		
		@Override
		public void configurationChanged(QueryFilterConfiguration config) {
			provider.dispose();
			boolean showInactive = config.isShowInactiveItems();
			provider = new DataModelContentProvider(false, !showInactive, true);
			
			IWorkbenchPage[] pages = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getPages();
			for (IWorkbenchPage page : pages) {
				IViewPart view = page.findView(QueryItemView.ID);
				if (view instanceof DIViewPart) {
					@SuppressWarnings("unchecked")
					QueryItemView qView = ((DIViewPart<QueryItemView>)view).getComponent();
					qView.refresh();
				}
			}
		}
	};
	
	/**
	 * Creates a new content provider 
	 */
	public FiltersDataModelContentProvider(){
		QueryFilterConfigManager.getInstance().addChangeListener(queryConfChangeListener);
		boolean showInactive = QueryFilterConfigManager.getInstance().getCurrentConfig().isShowInactiveItems();
		provider = new DataModelContentProvider(false, !showInactive, true);
	}

	/**
	 * @see org.eclipse.jface.viewers.IContentProvider#dispose()
	 */
	@Override
	public void dispose() {
		QueryFilterConfigManager.getInstance().removeChangeListener(queryConfChangeListener);
		provider.dispose();
	}

	
	/**
	 * 
	 * @param newInput must be a map that contains the keys
	 * QueryFilterContentProvider.ROOT_NODES - array of RootNodeType which should appear as 
	 * the root nodes in the tree
	 * RootNodeType.DATA_MODEL_FILTERS whose value is the current data model
	 * and 
	 * RootNodeType.PATROL_FILTERS whose value is the patrol filter options
	 * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
	 */
	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		if (newInput == null){
			provider.inputChanged(viewer, oldInput, null);
		}else if (newInput instanceof String){
			dataModel = null;
		}else{
			this.dataModel = (DataModel)newInput;
			provider.inputChanged(viewer, oldInput, this.dataModel);
		}
	}

	/**
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getElements(java.lang.Object)
	 */
	@Override
	public Object[] getElements(Object inputElement) {
		if (dataModel == null ){
			return new String[]{Messages.FiltersDataModelContentProvider_LoadingLabel};
		}
		return DataModelItem.values();
	}

	/**
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
	 */
	@Override
	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof DataModelItem){
			if (parentElement == DataModelItem.CATEGORIES){
					return provider.getChildren(provider.getElements(null)[0]);	
			}else if (parentElement == DataModelItem.ATTRIBUTES){
				boolean showInactive = QueryFilterConfigManager.getInstance().getCurrentConfig().isShowInactiveItems();
				List<Attribute> atts = QueryDataModelManager.getInstance().getAttributes(this.dataModel, !showInactive);
				
				Collections.sort(atts, new Comparator<Attribute>() {
					@Override
					public int compare(Attribute o1, Attribute o2) {
						return Collator.getInstance().compare(o1.getName(),o2.getName());
					}
				});
				return atts.toArray(new Attribute[atts.size()]);
				
			}
			return new Object[]{};
		}else{
			//assume data model
			return provider.getChildren(parentElement);
			
		}
	}


	/**
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getParent(java.lang.Object)
	 */
	@Override
	public Object getParent(Object element) {
		if (element instanceof DataModelItem){
			return null;
		}else{
			//assume data model
			return provider.getParent(element);	
		}
	}

	/**
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(java.lang.Object)
	 */
	@Override
	public boolean hasChildren(Object element) {
		if (element instanceof DataModelItem){
			return true;
		}else{
			//assume data model
			return provider.hasChildren(element);
			
		}
	}

	
	private LabelProvider lp = null;
	public LabelProvider getLabelProvider() {
		if (lp == null) {
			lp = new FiltersDataModelLabelProvider();
		}
		return lp;
	}
	
	/**
	 * Label Provider for DataModel filter.
	 * 
	 * @author elitvin
	 * @since 4.1.0
	 */
	private class FiltersDataModelLabelProvider extends LabelProvider implements IColorProvider {

		private DataModelLabelProvider dmLabelProvider = new DataModelLabelProvider();

		@Override
		public String getText(Object element) {
			if (element instanceof DataModelItem) {
				return ((DataModelItem) element).guiName;
			} else {
				return dmLabelProvider.getText(element);
			}
		}
		@Override
		public Image getImage(Object element){
			if (element instanceof DataModelItem) {
				return ((DataModelItem) element).image;
			} else {
				return dmLabelProvider.getImage(element);
			}
		}
		@Override
		public Color getForeground(Object element) {
			if (element instanceof Attribute) {
				Attribute a = (Attribute) element;
				return DataModelManagerUtil.isActive(a, dataModel) ? DataModelLabelProvider.BLACK : DataModelLabelProvider.GRAY;
			}
			return dmLabelProvider.getForeground(element);
		}
		@Override
		public Color getBackground(Object element) {
			return dmLabelProvider.getBackground(element);
		}
		
	}
}
package org.wcs.smart.query.ui.querytable.column;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.wcs.smart.query.model.QueryResultItem;

public class AttributeTableColumn implements QueryTableColumn {

	private String name;
	private ColumnLabelProvider provider = null;
	private String key;
	
	public AttributeTableColumn(String name, String key){
		this.name = name;
		this.key = key;
	}
	
	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public ColumnLabelProvider getLabelProvider() {
		if (provider == null){
			provider = getAttributeLabelProvider(this.key);
		}
		return provider;
	}
	

	private static ColumnLabelProvider getAttributeLabelProvider(final String attKey){
		
		ColumnLabelProvider provider = new ColumnLabelProvider(){
			/* 
			 * @see org.eclipse.jface.viewers.ILabelProvider#getText(java.lang.Object)
			 */
			public String getText(Object element) {
				if (element instanceof QueryResultItem){
					return ((QueryResultItem)element).getAttributeValue(attKey);
				}
				return element == null ? "" : element.toString();//$NON-NLS-1$
			}
		};
		
		return provider;
	}

}

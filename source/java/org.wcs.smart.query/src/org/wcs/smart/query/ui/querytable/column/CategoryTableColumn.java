package org.wcs.smart.query.ui.querytable.column;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.wcs.smart.query.model.QueryResultItem;

public class CategoryTableColumn implements QueryTableColumn{

	
	private String name;
	private ColumnLabelProvider provider = null;
	private int level;
	
	public CategoryTableColumn(String name, int level){
		this.name = name;
		this.level = level;
	}
	
	public String getName(){
		return this.name;
	}
	
	public ColumnLabelProvider getLabelProvider(){
		if (provider == null){
			provider = getCategoryLabelProvider(this.level);
			
		}
		return provider;
	}
	
	private static ColumnLabelProvider getCategoryLabelProvider(final int cat){
		
		ColumnLabelProvider provider = new ColumnLabelProvider(){
			/* 
			 * @see org.eclipse.jface.viewers.ILabelProvider#getText(java.lang.Object)
			 */
			public String getText(Object element) {
				if (element instanceof QueryResultItem){
					String[] items = ((QueryResultItem)element).getCategories();
					if (items == null){
						return "";
					}
					if (cat < items.length){
						return items[cat];
					}else{
						return "";
					}
				}
				return element == null ? "" : element.toString();//$NON-NLS-1$
			}
		};
		
		return provider;
	}
}

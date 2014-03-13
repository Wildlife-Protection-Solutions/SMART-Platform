package org.wcs.smart.entity.query.parser.internal;

import java.text.MessageFormat;

import org.hibernate.Session;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.entity.query.internal.Messages;
import org.wcs.smart.entity.query.ui.definition.EntityDropItemFactory;
import org.wcs.smart.query.QueryDataModelManager;
import org.wcs.smart.query.model.summary.CategoryValueItem;
import org.wcs.smart.query.ui.model.DropItem;
import org.wcs.smart.query.ui.model.impl.ErrorDropItem;

public class ObservationCategoryValueItem extends CategoryValueItem {

	/**
	 * Creates a new category value item of the form
	 * "category:sum:<hkey>"
	 * 
	 * @param key
	 * @return
	 */
	public static ObservationCategoryValueItem createItem(String key){
		return new ObservationCategoryValueItem(key);
	}
	
	/**
	 * Creates a new category value item.
	 * 
	 * @param key category value key
	 */
	public ObservationCategoryValueItem(String key){
		super(key);
	}
	
	@Override
	public DropItem asDropItem(Session session) throws Exception {
		try{
			if (categoryHkey == null){
				DropItem di = EntityDropItemFactory.INSTANCE.createCategoryValueDropItem(null);
				di.initializeData(getDropItemInitializeData());
				return di;
			}
			Category category = QueryDataModelManager.getInstance().getCategory(session, categoryHkey);
			if (category == null){
				throw new Exception(MessageFormat.format(Messages.ObservationCategoryValueItem_CategoryKeyNotFound, new Object[]{categoryHkey}));
			}
			category.getFullCategoryName();		//cache this
			DropItem di = EntityDropItemFactory.INSTANCE.createCategoryValueDropItem(category);
			di.initializeData(getDropItemInitializeData());
			return di;
		} catch (Exception ex) {
			return new ErrorDropItem(ex.getMessage());
		}
	}

}

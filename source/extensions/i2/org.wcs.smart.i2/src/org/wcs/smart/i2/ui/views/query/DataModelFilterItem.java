package org.wcs.smart.i2.ui.views.query;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.hibernate.Session;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.CategoryAttribute;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.i2.query.Operator;
import org.wcs.smart.i2.ui.views.query.dropitem.DropItem;
import org.wcs.smart.i2.ui.views.query.dropitem.TextBoxDropItem;
import org.wcs.smart.i2.ui.views.query.dropitem.TextDropItem;

public class DataModelFilterItem extends DeferredFilterItem{

	private Object LOCK = new Object();
	
	private String categoryKey = null;
	private UUID categoryUuid = null;
	private String attributeKey = null;
	private boolean hasKids = false;
	private Attribute.AttributeType type;
	
	public DataModelFilterItem(Category category){
		super(category.getName());
		this.categoryKey = category.getKeyId();
		categoryUuid = category.getUuid();
		hasKids = true;//!category.getChildren().isEmpty();
	}
	
	public DataModelFilterItem(Attribute attribute){
		super(attribute.getName());
		this.attributeKey = attribute.getKeyId();
		this.type = attribute.getType();
		hasKids = false;
	}
	
	public DataModelFilterItem(CategoryAttribute attribute){
		super(attribute.getAttribute().getName());
		this.attributeKey = attribute.getAttribute().getKeyId();
		this.categoryKey = attribute.getCategory().getKeyId();
		this.type = attribute.getAttribute().getType();
		hasKids = false;
	}
	
	public Attribute.AttributeType getType(){
		return this.type;
	}
	public String getAttributeKey(){
		return this.attributeKey;
	}
	@Override
	public List<FilterItem> getChildren() {
		if (kids == null ){
			synchronized (LOCK) {
				if (kids == null){
					if (!hasKids){
						kids = new ArrayList<FilterItem>();
					}else{
						System.out.println("loading kids:" + getName());
						Session s = HibernateManager.openSession();
						try{
							Category c = (Category)s.get(Category.class, categoryUuid);
							ArrayList<FilterItem> temp = new ArrayList<>();
							if (c != null){
								for (Category kid : c.getChildren()){
									temp.add(new DataModelFilterItem(kid));
								}
								List<CategoryAttribute> cas = new ArrayList<>();
								c.getAllCategoryAttribute(cas, null);
								cas.sort((a,b)->Collator.getInstance().compare(a.getAttribute().getName(), b.getAttribute().getName()));
								for (CategoryAttribute ca : cas){
									temp.add(new DataModelFilterItem(ca));
								}
							}
							kids = temp;
						}finally{
							s.close();
						}
					}
				}
			}
			
		}
		if (kids == null) return null;
		return Collections.unmodifiableList(kids);
	} 
	
	@Override
	public boolean hasChildren(){
		if (attributeKey != null) return false;
		return super.hasChildren();
	}
	
	
	@Override
	public DropItem[] asDropItem() {
		if (attributeKey == null){
			//category only
			String queryKey = "dm_category:" + categoryKey;
			return new DropItem[]{new TextDropItem(getName(), queryKey)};
		}
		
		//category and attribute
		StringBuilder partKey =new StringBuilder();
		partKey.append("dm_attribute:");
		partKey.append(type.typeKey);
		partKey.append(":");
		if (categoryKey != null){
			partKey.append(categoryKey);
			partKey.append(":");
		}else{
			partKey.append(":");
		}
		partKey.append(attributeKey);
		
		switch(type){
		case BOOLEAN:
			return new DropItem[]{new TextDropItem(getName(), partKey.toString())};
		case DATE:
			break;
		case LIST:
			break;
		case NUMERIC:
			return new DropItem[]{new TextBoxDropItem(getName(), partKey.toString(), TextBoxDropItem.InputType.NUMERIC)};
		case TEXT:
			return new DropItem[]{new TextBoxDropItem(getName(), partKey.toString(), TextBoxDropItem.InputType.TEXT)};
		case TREE:
			break;
		default:
			break;
			
		}
		return null;
	}
}

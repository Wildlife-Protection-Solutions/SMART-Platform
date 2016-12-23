package org.wcs.smart.i2.ui.views.query;

import java.text.Collator;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.hibernate.Session;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.CategoryAttribute;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.ui.views.query.dropitem.AttributeTreeDropItem;
import org.wcs.smart.i2.ui.views.query.dropitem.DateDropItem;
import org.wcs.smart.i2.ui.views.query.dropitem.DropItem;
import org.wcs.smart.i2.ui.views.query.dropitem.OptionDropItem;
import org.wcs.smart.i2.ui.views.query.dropitem.TextBoxDropItem;
import org.wcs.smart.i2.ui.views.query.dropitem.TextDropItem;

public class DataModelFilterItem extends DeferredFilterItem{

	private Object LOCK = new Object();
	
	private UUID categoryUuid = null;
	private UUID attributeUuid = null;
	
	private Attribute.AttributeType type;

	private String dropItemName;
	private String queryKey;

	public DataModelFilterItem(Category category){
		super(category.getName());
		this.categoryUuid = category.getUuid();
		queryKey = "dm_category:" + category.getKeyId();
		dropItemName = category.getFullCategoryName();
	}
	
	public DataModelFilterItem(Attribute attribute){
		super(attribute.getName());
		this.attributeUuid = attribute.getUuid();
		this.type = attribute.getType();
		queryKey = "dm_attribute:" + attribute.getType().typeKey + "::" + attribute.getKeyId();
		dropItemName = attribute.getName();
	}
	
	public DataModelFilterItem(CategoryAttribute attribute){
		super(attribute.getAttribute().getName());
		
		this.categoryUuid = attribute.getCategory().getUuid();
		this.attributeUuid = attribute.getAttribute().getUuid();
		this.type = attribute.getAttribute().getType();
		
		queryKey = "dm_attribute:" + attribute.getAttribute().getType().typeKey + ":" + attribute.getCategory().getKeyId() + ":" + attribute.getAttribute().getKeyId();
		dropItemName = MessageFormat.format("{0} ({1})", attribute.getAttribute().getName(), attribute.getCategory().getFullCategoryName());
	}
	
//	private void processAttributeItems(Attribute a){
//		if (a.getType() == AttributeType.LIST){
//			int cnt = 0;
//			attributeListLabels = new String[a.getAttributeList().size()];
//			attributeListKeys = new String[a.getAttributeList().size()];
//			for (AttributeListItem i : a.getActiveListItems()){
//				attributeListLabels[cnt] = i.getName();
//				attributeListKeys[cnt++] = i.getKeyId();
//			}
//		}
//	}
	public Attribute.AttributeType getType(){
		return this.type;
	}

	@Override
	public List<FilterItem> getChildren() {
		if (kids == null ){
			synchronized (LOCK) {
				if (kids == null){
					if (attributeUuid != null){
						//not kids; this is an attribute
						kids = new ArrayList<FilterItem>();
					}else{
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
		if (attributeUuid != null) return false;
		return super.hasChildren();
	}
	
	
	@Override
	public DropItem[] asDropItem() {
		if (attributeUuid == null){
			//not attribute; this is definities a category
			return new DropItem[]{new TextDropItem(dropItemName, queryKey)};
		}
		
		
		switch(type){
		case BOOLEAN:
			return new DropItem[]{new TextDropItem(dropItemName, queryKey)};
		case DATE:
			return new DropItem[]{new DateDropItem(dropItemName, queryKey)};
		case LIST:
			final List<String> labels = new ArrayList<String>();
			final List<String> keys = new ArrayList<String>();
			labels.add("<ANY>");
			keys.add("any");
			Job j = new Job("creating attribute drop item"){

				@Override
				protected IStatus run(IProgressMonitor monitor) {
					Session s = HibernateManager.openSession();
					try{
						Attribute a = (Attribute) s.get(Attribute.class, attributeUuid);
						if (a.getAttributeList() != null){
							for (AttributeListItem i : a.getAttributeList()){
								labels.add(i.getName());
								keys.add(i.getKeyId());
							}
						}
					}finally{
						s.close();
					}
					
					return Status.OK_STATUS;
				}
			};
			j.schedule();
			try {
				j.join();
			} catch (InterruptedException e) {
				Intelligence2PlugIn.displayLog("Error loading attribute list items", e);
			}
			
			return new DropItem[]{new OptionDropItem(dropItemName, queryKey, labels.toArray(new String[labels.size()]), keys.toArray(new String[keys.size()]))};
			
		case NUMERIC:
			return new DropItem[]{new TextBoxDropItem(dropItemName, queryKey, TextBoxDropItem.InputType.NUMERIC)};
		case TEXT:
			return new DropItem[]{new TextBoxDropItem(dropItemName, queryKey, TextBoxDropItem.InputType.TEXT)};
		case TREE:
			return new DropItem[]{new AttributeTreeDropItem(dropItemName, queryKey, attributeUuid)};
		default:
			break;
			
		}
		return null;
	}
}

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
package org.wcs.smart.i2.ui.views.query;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.text.Collator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Objects;

import javax.imageio.ImageIO;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.locationtech.udig.ui.graphics.AWTSWTImageUtils;
import org.wcs.smart.SmartContext;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.IIntelligenceLabelProvider;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.InternalQueryManager;
import org.wcs.smart.i2.ProfilesManager;
import org.wcs.smart.i2.internal.IntelligenceLabelProviderImpl;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.IntelAttribute;
import org.wcs.smart.i2.model.IntelAttribute.AttributeType;
import org.wcs.smart.i2.model.IntelEntityType;
import org.wcs.smart.i2.model.IntelEntityTypeAttribute;
import org.wcs.smart.i2.model.IntelRecordSource;
import org.wcs.smart.i2.model.IntelRecordSourceAttribute;
import org.wcs.smart.i2.query.Operator;
import org.wcs.smart.i2.query.observation.filter.SystemAttributeFilter;
import org.wcs.smart.i2.query.observation.filter.ValuePart;
import org.wcs.smart.i2.ui.AttributeLabelProvider;
import org.wcs.smart.i2.ui.Resources;
import org.wcs.smart.i2.ui.views.QueryView;
import org.wcs.smart.i2.ui.views.query.dropitem.AttributeGroupByDropItem;
import org.wcs.smart.i2.ui.views.query.dropitem.ConservationAreaGroupByDropItem;
import org.wcs.smart.i2.ui.views.query.dropitem.DropItem;
import org.wcs.smart.i2.ui.views.query.dropitem.EntityTypeGroupByDropItem;
import org.wcs.smart.i2.ui.views.query.dropitem.RecordSourceGroupByDropItem;
import org.wcs.smart.i2.ui.views.query.dropitem.RecordStatusGroupByDropItem;
import org.wcs.smart.i2.ui.views.query.dropitem.SystemAttributeDateGroupByDropItem;
import org.wcs.smart.i2.ui.views.query.dropitem.TextOperatorDropItem;
import org.wcs.smart.i2.ui.views.query.dropitem.ValueDropItem;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Content provider for the entity summary query types. 
 * 
 * If the input provided is not null, then the class
 * will spawn a job to load the elements and returning loading...
 * until the job is finished loading the element.
 * 
 * @author Emily
 *
 */
public class EntitySummaryContentProvider implements ITreeContentProvider{
	
	/* 
	 * tree root nodes
	 */
	public enum RootNode{
		GROUP_BY_OPTION (Messages.EntitySummaryContentProvider_GroupByTreeNode),
		VALUE_OPTION (Messages.EntitySummaryContentProvider_ValuesTreeNode),
		FILTER_OPTION(Messages.EntitySummaryContentProvider_FiltersTreeNode);
		
		String guiName;
		
		RootNode(String name){
			this.guiName = name;
		}
	}
	
	/*
	 * sub nodes
	 */
	private enum SubRootNode{
		ENTITY_TYPE_ITEM(Messages.EntitySummaryContentProvider_EntityTypesTreeNode),
		ATTRIBUTE_ITEM(Messages.EntitySummaryContentProvider_AttributeTreeNode),
		OPERATORS(Messages.EntitySummaryContentProvider_OperatorsNode),
		CA(Messages.EntitySummaryContentProvider_ConservationAreaGroupByOp);
		
		String guiName;
		
		SubRootNode(String name){
			this.guiName = name;
		}
	}
	
	/*
	 * not node
	 */
	private enum OperatorNode{
		NOT(Messages.EntitySummaryContentProvider_NotNode),
		BRACKETS("( )"); //$NON-NLS-1$
		
		String guiName;
		
		OperatorNode(String name){
			this.guiName = name;
		}
	}

	private Viewer viewer;
	
	private List<IntelEntityType> types;
	private HashMap<String, List<IntelEntityTypeAttribute>> typeAttributes;
	
	private List<IntelRecordSource> records;
	private HashMap<String, List<IntelRecordSourceAttribute>> recordAttributes;
	
	public enum Type {ENTITY, RECORD};
	private Type type;
	
	public EntitySummaryContentProvider(Type type) {
		this.type = type;
	}
	
	
	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		this.viewer = viewer;
		types = null;
		records = null;
		if (newInput != null) {
			loadDataJob.schedule();	
		}
	}
	
	@Override
	public Object[] getElements(Object inputElement) {
		if (types != null || records != null) {
			return new Object[] {
				new TreeNode(RootNode.GROUP_BY_OPTION, RootNode.GROUP_BY_OPTION, RootNode.GROUP_BY_OPTION.guiName),
				new TreeNode(RootNode.VALUE_OPTION, RootNode.VALUE_OPTION, RootNode.VALUE_OPTION.guiName),
				new TreeNode(RootNode.FILTER_OPTION, RootNode.FILTER_OPTION, RootNode.FILTER_OPTION.guiName)};
		}
		return new Object[] {DialogConstants.LOADING_TEXT};
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof TreeNode) {
			List<?> items = ((TreeNode) parentElement).getChildren();
			if (items == null) return null;
			return items.toArray();
		}
		return null;
	}

	@Override
	public Object getParent(Object element) {
		if (element instanceof TreeNode) {
			return ((TreeNode) element).getParent();
		}
		return null;
	}

	@Override
	public boolean hasChildren(Object element) {
		if (element instanceof TreeNode) {
			TreeNode n = (TreeNode)element;
			if (n.item instanceof RootNode) return true;
			if (n.item instanceof SubRootNode) return true;
			if (n.item instanceof IntelEntityType) return true;
			if (n.item instanceof IntelRecordSource) return true;
		}
		return false;
	}

	private void setDataRecord(HashMap<IntelRecordSource, List<IntelRecordSourceAttribute>> types) {
		records = new ArrayList<>();
		recordAttributes = new HashMap<>();
		records.addAll(types.keySet());
		for (Entry<IntelRecordSource, List<IntelRecordSourceAttribute>> items : types.entrySet()) {
			recordAttributes.put(items.getKey().getKeyId(), items.getValue());
		}

		Display.getDefault().syncExec(()->{
			Object label = viewer.getControl().getData(QueryView.REFRESHLABEL_KEY);
			if (label != null && label instanceof Control){
				((Control)label).dispose();
				viewer.getControl().setData(QueryView.REFRESHLABEL_KEY, null);
			}
			
			viewer.refresh();
			((TreeViewer)viewer).expandToLevel(2);	
			viewer.getControl().setEnabled(true);
			
		});
		
	}
	private void setData(HashMap<IntelEntityType, List<IntelEntityTypeAttribute>> entityTypes) {
		types = new ArrayList<>();
		typeAttributes = new HashMap<>();
		types.addAll(entityTypes.keySet());
		for (Entry<IntelEntityType, List<IntelEntityTypeAttribute>> items : entityTypes.entrySet()) {
			typeAttributes.put(items.getKey().getKeyId(), items.getValue());
		}

		Display.getDefault().syncExec(()->{
			Object label = viewer.getControl().getData(QueryView.REFRESHLABEL_KEY);
			if (label != null && label instanceof Control){
				((Control)label).dispose();
				viewer.getControl().setData(QueryView.REFRESHLABEL_KEY, null);
			}
			
			viewer.refresh();
			((TreeViewer)viewer).expandToLevel(2);	
			viewer.getControl().setEnabled(true);
			
		});
		
	}

	
	public class TreeNode extends BasicTreeFilterItem {
		
		private Object item;
		private RootNode source;
		
		public TreeNode(RootNode source, Object item, String guiName) {
			super(guiName);
			this.item = item;
			this.source = source;
		}
		
		public Object getItem() { return this.item; }
		public Object getSource() { return this.source; }
		
		
		public boolean equals(Object other) {
			if (this == other) return true;
			if (other == null) return false;
			if (other.getClass() != TreeNode.class) return false;
			return Objects.equals(item, ((TreeNode)other).item) && Objects.equals(source, ((TreeNode)other).source);
		}
		
		public int hashCode() {
			return Objects.hash(item, source);
		}
		
		@Override
		public ImageDescriptor getImage(){
			if (item instanceof IntelEntityTypeAttribute) {
				return AttributeLabelProvider.getImageDescriptor(((IntelEntityTypeAttribute) item).getAttribute().getType());
			}else if (item instanceof IntelAttribute) {
				return AttributeLabelProvider.getImageDescriptor(((IntelAttribute) item).getType());
			}else if (item instanceof IntelEntityType) {
				return ImageDescriptor.createFromImage( Resources.INSTANCE.getImage( (IntelEntityType) item) );
			}else if (item == SubRootNode.CA) {
				return SmartPlugIn.getDefault().getImageRegistry().getDescriptor(SmartPlugIn.DATA_MODEL_ICON);
			}else if (item == SubRootNode.ENTITY_TYPE_ITEM) {
				return Intelligence2PlugIn.getDefault().getImageRegistry().getDescriptor(Intelligence2PlugIn.ICON_ENTITY);
			}else if (item == SubRootNode.OPERATORS) {
				return Intelligence2PlugIn.getDefault().getImageRegistry().getDescriptor(Intelligence2PlugIn.ICON_OPERATOR);
			}else if (item == SubRootNode.ATTRIBUTE_ITEM) {
				if (source == RootNode.GROUP_BY_OPTION) {
					return SmartPlugIn.getDefault().getImageRegistry().getDescriptor(SmartPlugIn.ATTRIBUTE_LIST_ICON);	
				}
				return SmartPlugIn.getDefault().getImageRegistry().getDescriptor(SmartPlugIn.ATTRIBUTE_NUMBER_ICON);
			}else if (item == RootNode.GROUP_BY_OPTION) {
				return Intelligence2PlugIn.getDefault().getImageRegistry().getDescriptor(Intelligence2PlugIn.ICON_GROUP_BY);
			}else if (item == RootNode.VALUE_OPTION) {
				return Intelligence2PlugIn.getDefault().getImageRegistry().getDescriptor(Intelligence2PlugIn.ICON_VALUES);
			}else if (item == RootNode.FILTER_OPTION) {
				return Intelligence2PlugIn.getDefault().getImageRegistry().getDescriptor(Intelligence2PlugIn.ICON_FILTERS);
			}
			return image;
		}

		@Override
		public FilterTreeItem getParent() {
			if (item instanceof RootNode ) return null;
			if (item instanceof SubRootNode ) return new TreeNode(source, source, null);
			if (item instanceof IntelEntityType ) return new TreeNode(source, SubRootNode.ENTITY_TYPE_ITEM, null);
			if (item instanceof IntelEntityTypeAttribute ) return new TreeNode(source, ((IntelEntityTypeAttribute) item).getEntityType(), null);
			return null;
		}

		@Override
		public List<FilterTreeItem> getChildren() {
			if (item == RootNode.FILTER_OPTION || item == RootNode.GROUP_BY_OPTION) {
				List<FilterTreeItem> items = new ArrayList<>();
				if (type == Type.ENTITY) {
					items.add(new TreeNode((RootNode)item, SubRootNode.ENTITY_TYPE_ITEM, SubRootNode.ENTITY_TYPE_ITEM.guiName));
					items.add(new TreeNode((RootNode)item, SubRootNode.ATTRIBUTE_ITEM, SubRootNode.ATTRIBUTE_ITEM.guiName));
				}else if (type == Type.RECORD ) {
					TreeNode tn = new TreeNode(source, SystemAttributeFilter.SystemAttribute.RECORD_SOURCE, IntelligenceLabelProviderImpl.getName(SystemAttributeFilter.SystemAttribute.RECORD_SOURCE));
					tn.setImageDescriptor(Intelligence2PlugIn.getDefault().getImageRegistry().getDescriptor(Intelligence2PlugIn.ICON_RECORD));
					items.add(tn);
					
					tn = new TreeNode(source, SystemAttributeFilter.SystemAttribute.RECORD_STATUS, IntelligenceLabelProviderImpl.getName(SystemAttributeFilter.SystemAttribute.RECORD_STATUS));
					tn.setImageDescriptor(Intelligence2PlugIn.getDefault().getImageRegistry().getDescriptor(Intelligence2PlugIn.ICON_SRC_NEW));
					items.add(tn);
					
					tn = new TreeNode(source, SystemAttributeFilter.SystemAttribute.RECORD_DATE_CREATED, IntelligenceLabelProviderImpl.getName(SystemAttributeFilter.SystemAttribute.RECORD_DATE_CREATED));
					tn.setImageDescriptor(Intelligence2PlugIn.getDefault().getImageRegistry().getDescriptor(Intelligence2PlugIn.ICON_SYSTEM_DATEATTRIBUTE));
					items.add(tn);
					
					tn = new TreeNode(source, SystemAttributeFilter.SystemAttribute.RECORD_DATE_MODIFIED, IntelligenceLabelProviderImpl.getName(SystemAttributeFilter.SystemAttribute.RECORD_DATE_MODIFIED));
					tn.setImageDescriptor(Intelligence2PlugIn.getDefault().getImageRegistry().getDescriptor(Intelligence2PlugIn.ICON_SYSTEM_DATEATTRIBUTE));
					items.add(tn);
					
					if (item == RootNode.GROUP_BY_OPTION) {
						tn = new TreeNode(source, SystemAttributeFilter.SystemAttribute.RECORD_DATE, IntelligenceLabelProviderImpl.getName(SystemAttributeFilter.SystemAttribute.RECORD_DATE));
						tn.setImageDescriptor(Intelligence2PlugIn.getDefault().getImageRegistry().getDescriptor(Intelligence2PlugIn.ICON_SYSTEM_DATEATTRIBUTE));
						items.add(tn);
					}
					
					for(IntelRecordSource s : records) {
						tn = new TreeNode(source, s, s.getName());
						tn.setImageDescriptor(ImageDescriptor.createFromImage(Resources.INSTANCE.getImage(s)));
						items.add(tn);
					}

				}
				
				
				if (item == RootNode.FILTER_OPTION) {
					items.add(new TreeNode((RootNode)item, SubRootNode.OPERATORS, SubRootNode.OPERATORS.guiName));
				}
				if (item == RootNode.GROUP_BY_OPTION && SmartDB.isMultipleAnalysis()) {
					items.add(new TreeNode((RootNode)item, SubRootNode.CA, SubRootNode.CA.guiName));
				}
				return items;
			}
			if (item instanceof IntelRecordSource) {
				List<FilterTreeItem> filters = new ArrayList<>();
				if (source == RootNode.GROUP_BY_OPTION) {
					for (IntelRecordSourceAttribute atts : recordAttributes.get(((IntelRecordSource) item).getKeyId())) {
						if (atts.getAttribute() != null && isGroupByAttribute(atts.getAttribute().getType())) {
							TreeNode tn = new TreeNode(source, atts,  IIntelligenceLabelProvider.getName(atts));		
							tn.setImageDescriptor(ImageDescriptor.createFromImage(AttributeLabelProvider.getImage(atts.getAttribute().getType())));
							filters.add(tn);
						}
					}	
				}else {
					for (IntelRecordSourceAttribute atts : recordAttributes.get(((IntelRecordSource) item).getKeyId())) {
						
						TreeNode tn = new TreeNode(source, new AttributeTreeFilterItem(atts), IIntelligenceLabelProvider.getName(atts));
						if (atts.getAttribute() != null) {
							tn.setImageDescriptor(ImageDescriptor.createFromImage(AttributeLabelProvider.getImage(atts.getAttribute().getType())));
						}else {
							tn.setImageDescriptor(Intelligence2PlugIn.getDefault().getImageRegistry().getDescriptor(Intelligence2PlugIn.ICON_ENTITY));
						}
						filters.add(tn);
					}
				}
				return filters;
			}
			if (item == RootNode.VALUE_OPTION) {
				List<FilterTreeItem> kids = new ArrayList<>();
				ValuePart.ValueOption op = null;
				if (type == Type.ENTITY) {
					op = ValuePart.ValueOption.NUMBER_ENTITIES;
				}else if (type == Type.RECORD) {
					op = ValuePart.ValueOption.NUMBER_RECORDS;
				}
				String name = SmartContext.INSTANCE.getClass(IIntelligenceLabelProvider.class).getLabel(op, Locale.getDefault());
				kids.add(new TreeNode(RootNode.VALUE_OPTION, op, name));
				
				return kids;
			}
			
			if (item == SubRootNode.CA) {
				return null;
			}
			if (item == SubRootNode.ENTITY_TYPE_ITEM) {
				List<FilterTreeItem> nodes = new ArrayList<>();
				types.stream().sorted((a,b)-> Collator.getInstance().compare(a.getName(), b.getName())).forEach(n->nodes.add(new TreeNode(source, n, n.getName())));
				
				return nodes;
			}
			
			if (item == SubRootNode.OPERATORS) {
				List<FilterTreeItem> items = new ArrayList<>();
				items.add(new TreeNode(RootNode.FILTER_OPTION, OperatorNode.NOT,  OperatorNode.NOT.guiName));
				items.add(new TreeNode(RootNode.FILTER_OPTION, OperatorNode.BRACKETS,  OperatorNode.BRACKETS.guiName));
				return items;
			}
			
			
			if (item == SubRootNode.ATTRIBUTE_ITEM) {
				HashSet<String> keys = new HashSet<>();
				List<FilterTreeItem> nodes = new ArrayList<>();
				if (type == Type.ENTITY) {
					for (List<IntelEntityTypeAttribute> atts : typeAttributes.values()) {
						for (IntelEntityTypeAttribute a : atts) {
							if (keys.contains(a.getAttribute().getKeyId())) continue;
							keys.add(a.getAttribute().getKeyId());
							if (source == RootNode.FILTER_OPTION || (
									source == RootNode.GROUP_BY_OPTION && isGroupByAttribute(a.getAttribute().getType()))) {
								nodes.add(new TreeNode(source, a.getAttribute(), a.getAttribute().getName()));
							}
						}
					}
					
					TreeNode tn = new TreeNode(source, SystemAttributeFilter.SystemAttribute.ENTITY_DATE_CREATED, IntelligenceLabelProviderImpl.getName(SystemAttributeFilter.SystemAttribute.ENTITY_DATE_CREATED));
					tn.setImageDescriptor(Intelligence2PlugIn.getDefault().getImageRegistry().getDescriptor(Intelligence2PlugIn.ICON_SYSTEM_DATEATTRIBUTE));
					nodes.add(tn);
					
					tn = new TreeNode(source, SystemAttributeFilter.SystemAttribute.ENTITY_DATE_MODIFIED, IntelligenceLabelProviderImpl.getName(SystemAttributeFilter.SystemAttribute.ENTITY_DATE_MODIFIED));
					tn.setImageDescriptor(Intelligence2PlugIn.getDefault().getImageRegistry().getDescriptor(Intelligence2PlugIn.ICON_SYSTEM_DATEATTRIBUTE));
					nodes.add(tn);
					
//				}else if (type == Type.RECORD) {
//					for (List<IntelRecordSourceAttribute> atts : recordAttributes.values()) {
//						for (IntelRecordSourceAttribute a : atts) {
//							if (a.getAttribute() == null) continue;
//							if (keys.contains(a.getAttribute().getKeyId())) continue;
//							keys.add(a.getAttribute().getKeyId());
//							if (source == RootNode.FILTER_OPTION || (
//									source == RootNode.GROUP_BY_OPTION && isGroupByAttribute(a.getAttribute()))) {
//								nodes.add(new TreeNode(source, a.getAttribute(), a.getAttribute().getName()));
//							}
//						}
//					}
//					
//					TreeNode tn = new TreeNode(source, SystemAttributeFilter.SystemAttribute.RECORD_DATE_CREATED, IntelligenceLabelProviderImpl.getName(SystemAttributeFilter.SystemAttribute.RECORD_DATE_CREATED));
//					tn.setImageDescriptor(Intelligence2PlugIn.getDefault().getImageRegistry().getDescriptor(Intelligence2PlugIn.ICON_SYSTEM_DATEATTRIBUTE));
//					nodes.add(tn);
//					
//					tn = new TreeNode(source, SystemAttributeFilter.SystemAttribute.RECORD_DATE_MODIFIED, IntelligenceLabelProviderImpl.getName(SystemAttributeFilter.SystemAttribute.RECORD_DATE_MODIFIED));
//					tn.setImageDescriptor(Intelligence2PlugIn.getDefault().getImageRegistry().getDescriptor(Intelligence2PlugIn.ICON_SYSTEM_DATEATTRIBUTE));
//					nodes.add(tn);
//					
//					tn = new TreeNode(source, SystemAttributeFilter.SystemAttribute.RECORD_DATE, IntelligenceLabelProviderImpl.getName(SystemAttributeFilter.SystemAttribute.RECORD_DATE_MODIFIED));
//					tn.setImageDescriptor(Intelligence2PlugIn.getDefault().getImageRegistry().getDescriptor(Intelligence2PlugIn.ICON_SYSTEM_DATEATTRIBUTE));
//					nodes.add(tn);
				}
					
				return nodes;
			}
		
			if (item instanceof IntelEntityType) {
				IntelEntityType entityType = (IntelEntityType)item;
					
				if (source == RootNode.GROUP_BY_OPTION) {
					List<FilterTreeItem> kids = new ArrayList<>();
					for (IntelEntityTypeAttribute a : typeAttributes.get(entityType.getKeyId())) {
						if ( isGroupByAttribute(a.getAttribute().getType())) {
							kids.add(new TreeNode(RootNode.GROUP_BY_OPTION, a, a.getAttribute().getName()));
						}
					}
					return kids;
				}
					
				if (source == RootNode.FILTER_OPTION) {
					List<FilterTreeItem> kids = new ArrayList<>();
					for (IntelEntityTypeAttribute a : typeAttributes.get(entityType.getKeyId())) {
						kids.add(new TreeNode(RootNode.FILTER_OPTION, a, a.getAttribute().getName()));
					}
					return kids;
				}
				
			}
			return null;
		}

		@Override
		public DropItem[] asDropItem() {
			if (source == RootNode.GROUP_BY_OPTION) {
				if (item instanceof IntelEntityType) {
					return new DropItem[] {
							new EntityTypeGroupByDropItem((IntelEntityType) item)
					};
				}
				if (item instanceof IntelEntityTypeAttribute) {
					if (isGroupByAttribute(((IntelEntityTypeAttribute) item).getAttribute().getType())) {
						return new DropItem[] {new AttributeGroupByDropItem((IntelEntityTypeAttribute) item)};
					}
				}
				if (item instanceof IntelAttribute) {
					if (isGroupByAttribute(((IntelAttribute) item).getType())) {
						return new DropItem[] {new AttributeGroupByDropItem((IntelAttribute)item)};
					}
				}
				if (item instanceof SystemAttributeFilter.SystemAttribute) {
					if (item == SystemAttributeFilter.SystemAttribute.RECORD_SOURCE) {
						return new DropItem[] { new RecordSourceGroupByDropItem() };
					}else if (item == SystemAttributeFilter.SystemAttribute.RECORD_STATUS) {
						return new DropItem[] { new RecordStatusGroupByDropItem() };
					}
					return new DropItem[] {new SystemAttributeDateGroupByDropItem((SystemAttributeFilter.SystemAttribute)item)};
				}
				if (item instanceof IntelRecordSourceAttribute) {
					IntelRecordSourceAttribute fi = (IntelRecordSourceAttribute)item;
					if (isGroupByAttribute(fi.getAttribute().getType())) {
						return new DropItem[] {new AttributeGroupByDropItem(fi)};
					}
				}
			}
			
			if (source == RootNode.VALUE_OPTION && item instanceof ValuePart.ValueOption) {
				return new DropItem[] { new ValueDropItem((ValuePart.ValueOption) item) };
			}
			if (item == SubRootNode.CA) {
				return new DropItem[] { new ConservationAreaGroupByDropItem() };
			}
			if (item == OperatorNode.NOT) {
				return new DropItem[] { new TextOperatorDropItem(Operator.NOT) };
			}
			if (item == OperatorNode.BRACKETS) {
				return new DropItem[] { new TextOperatorDropItem(Operator.BRACKET_OPEN), 
									new TextOperatorDropItem(Operator.BRACKET_CLOSE) };
			}
			if (source == RootNode.FILTER_OPTION) {
				if (item instanceof IntelEntityType) {
					return (new EntityTreeFilterItem((IntelEntityType)item)).asDropItem();
				}else if (item instanceof IntelAttribute) {
					return (new AttributeTreeFilterItem( ((IntelAttribute)item), type==Type.ENTITY, type==Type.RECORD)).asDropItem();
				}else if (item instanceof IntelEntityTypeAttribute) {
					return (new AttributeTreeFilterItem( ((IntelEntityTypeAttribute)item))).asDropItem();
				}else if (item instanceof SystemAttributeFilter.SystemAttribute) {
					return (new SystemAttributeFilterItem((SystemAttributeFilter.SystemAttribute)item)).asDropItem();
				}else if (item instanceof AttributeTreeFilterItem) {
					return ((AttributeTreeFilterItem)item).asDropItem();
				}
			}
			
			return null;
		}
		
		private boolean isGroupByAttribute(IntelAttribute.AttributeType a) {
			return a.equals(AttributeType.DATE) ||
					a.equals(AttributeType.EMPLOYEE) ||
					a.equals(AttributeType.POSITION) ||
					a.equals(AttributeType.LIST);
		}
	}
	
	
	private Job loadDataJob = new Job("loading entity summary query filter tree") { //$NON-NLS-1$

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			if (type == Type.ENTITY) {
				HashMap<IntelEntityType, List<IntelEntityTypeAttribute>> attributes = new HashMap<>();
				try(Session session = HibernateManager.openSession()){
					List<IntelEntityType> entityTypes = InternalQueryManager.INSTANCE.getQueryItemProvider().getEntityTypes(ProfilesManager.INSTANCE.getActiveProfileKeys(), session);
					for (IntelEntityType type : entityTypes) {
						List<IntelEntityTypeAttribute> thisattributes = new ArrayList<>(InternalQueryManager.INSTANCE.getQueryItemProvider().getEntityTypeAttributes(type, session));
						thisattributes.forEach(e->e.getAttribute().getName());
						attributes.put(type, thisattributes);
					}
				}
				setData(attributes);
			}else if (type == Type.RECORD) {
				HashMap<IntelRecordSource, List<IntelRecordSourceAttribute>> attributes = new HashMap<>();
				
				try(Session session = HibernateManager.openSession()){
					List<IntelRecordSource> sources = InternalQueryManager.INSTANCE.getQueryItemProvider().getRecordSources(ProfilesManager.INSTANCE.getActiveProfileKeys(), session);
					for (IntelRecordSource type : sources) {
						List<IntelRecordSourceAttribute> thisattributes = new ArrayList<>(InternalQueryManager.INSTANCE.getQueryItemProvider().getRecordSourceAttributes(type, session));
						for (IntelRecordSourceAttribute  a : thisattributes) {
							if (a.getAttribute() != null) {
								a.getAttribute().getName();
								//TODO: could be entity type
							}
						}
						attributes.put(type, thisattributes);
					}
				}
				setDataRecord(attributes);
			}
			
			return Status.OK_STATUS;
		}
		
	};
}


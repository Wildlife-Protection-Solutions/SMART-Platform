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
package org.wcs.smart.asset.query.model;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.hibernate.Session;
import org.wcs.smart.asset.query.internal.Messages;
import org.wcs.smart.asset.query.parser.internal.filter.AssetFilter;
import org.wcs.smart.asset.query.parser.internal.summary.AssetAttributeValueItem;
import org.wcs.smart.asset.query.parser.internal.summary.AssetCategoryValueItem;
import org.wcs.smart.asset.query.parser.internal.summary.AssetGroupBy;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.NamedItem;
import org.wcs.smart.ca.NamedKeyItem;
import org.wcs.smart.ca.datamodel.DataModel;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.IDataModelManager;
import org.wcs.smart.query.model.filter.IFilter;
import org.wcs.smart.query.model.filter.IFilterVisitor;
import org.wcs.smart.query.model.filter.IGroupByVisitor;
import org.wcs.smart.query.model.filter.IValueVisitor;
import org.wcs.smart.query.model.filter.QueryDefinitionValidator;
import org.wcs.smart.query.model.summary.CategoryValueItem;
import org.wcs.smart.query.model.summary.IGroupBy;
import org.wcs.smart.query.model.summary.IValueItem;
import org.wcs.smart.query.xml.model.UuidItemType;
import org.wcs.smart.util.UuidUtils;

/**
 * Tools for asset query validation.
 * 
 * @author Emily
 *
 */
public class AssetQueryValidator extends QueryDefinitionValidator {

	private static final String COULDNOTRESOLVE_ERRMSG = Messages.AssetGroupBy_CouldNotResolveFilter;
	
	private String langCode;
	private HashMap<String, UuidItemType> uuidLookup;
	private ConservationArea importCa;

	/**
	 * @param langCode the language value of the query 
	 * @param uuidLookup a uuid lookup map that looks up uuid values
	 * @param session database session
	 * @param queryDmManager
	 * @param ConservationArea 
	 * 
	 */
	public AssetQueryValidator(String langCode, HashMap<String, UuidItemType> uuidLookup, 
			Session session, IDataModelManager queryDmManager, ConservationArea ca ){
		super(session, queryDmManager, ca);
		
		this.importCa = ca;
		this.langCode = langCode;
		this.uuidLookup = uuidLookup;
	}

	
	/**
	 * Validates a filter item against the database.
	 * 
	 * @param filter the filter to validate
	 * 
	 * @throws Exception if filter cannot be validated
	 */
	@Override
	public List<String> validate(IFilter filter) throws Exception{
		List<String> warnings = super.validate(filter);
		
		FilterValidatorVisitor vv = new FilterValidatorVisitor();
		filter.accept(vv);
		if (vv.ex != null){
			throw vv.ex;
		}
		warnings.addAll(vv.warnings);
		return warnings;
	}
		
	
	/**
	 * Validates a group by item.
	 * @param item
	 * @return
	 * @throws Exception
	 */
	public List<String> validate(IGroupBy item) throws Exception{
		List<String> warnings = super.validate(item);
		
		GroupByValidatorVisitor vv = new GroupByValidatorVisitor();
		item.visit(vv);
		if (vv.ex != null){
			throw vv.ex;
		}
		warnings.addAll(vv.warnings);
		return warnings;
	}
	
	
	/**
	 * Validates value items
	 */
	@Override
	public List<String> validate(IValueItem item) throws Exception{
		List<String> warnings = super.validate(item);
		
		ValueVisitor vv = new ValueVisitor();
		item.accept(vv);
		if (vv.ex != null){
			throw vv.ex;
		}
		return warnings;
	}
	
	
	private boolean validateObject(Object x){
		//TODO: implement me and fix code below
//		if (x instanceof Station){
//			if (((Station)x).getConservationArea().equals(importCa)){
//				return true;
//			}
//		}else if (x instanceof Team){
//			if (((Team)x).getConservationArea().equals(importCa)){
//				return true;
//			}
//		}else if (x instanceof PatrolMandate){
//			if (((PatrolMandate)x).getConservationArea().equals(importCa)){
//				return true;
//			}
//		}else if (x instanceof PatrolTransportType){
//			if (((PatrolTransportType)x).getConservationArea().equals(importCa)){
//				return true;
//			}
//		}else if (x instanceof Employee){
//			if (((Employee)x).getConservationArea().equals(importCa)){
//				return true;
//			}
//		}else if (x instanceof ConservationArea){
//			return true;
//		}else if (x instanceof Rank){
//			if (((Rank)x).getAgency().getConservationArea().equals(importCa)){
//				return true;
//			}
//		}else if (x instanceof Agency){
//			if (((Agency)x).getConservationArea().equals(importCa)){
//				return true;
//			}
//		}
		return false;
	}
	
	private String getCaField(AssetFilterOption op){
		//TODO: implement me
		String field = ".conservationArea"; //$NON-NLS-1$
//		if (op == AssetQueryOption.RANK){
//			field = ".agency.conservationArea"; //$NON-NLS-1$
//		}
		return field;
	}
	
	class FilterValidatorVisitor implements IFilterVisitor{

		private Exception ex;
		private ArrayList<String> warnings = new ArrayList<String>();
		
		@Override
		public void visit(IFilter filter) {
			if (ex != null) return;
			try{
				if (filter instanceof AssetFilter){
					AssetFilterOption op = ((AssetFilter) filter).getAssetOption();
					if (op.getType() == AssetQueryOptionType.UUID){
						UUID uuid = ((AssetFilter)filter).getValue();
						Object x = op.getObject(session, uuid);
						if (validateObject(x)){
							//object existing in db; do not need to look it up
							return;
						}
						
						UuidItemType item = uuidLookup.get(  ((AssetFilter)filter).getValue()  );
						if (item == null){
							throw new Exception(
								MessageFormat.format(
								Messages.FilterValidator_AssetFilterError, new Object[]{ filter.asString()}));
						}
						if (NamedKeyItem.class.isAssignableFrom(op.getSourceClass())){
							//try to match the key first
							if (item.getValue() == null || item.getValue().size() == 0){
								throw new Exception(MessageFormat.format(
										Messages.AssetQueryValidator_CouldNotMatchFilter,
										new Object[]{filter.asString()}));
							}
							NamedKeyItem it = findKeyValue(item.getValue().get(0), op.getSourceClass().getSimpleName(), getCaField(op));
							if (it != null){
								((AssetFilter)filter).setValue(it.getUuid());
								return;
							}
							
						}
						if (NamedItem.class.isAssignableFrom(op.getSourceClass())){
							//try to match names
							if (item.getValue() == null || item.getValue().size() == 0){
								throw new Exception(MessageFormat.format(
										Messages.AssetQueryValidator_CouldNotMatchFilter,
										new Object[]{filter.asString()}));
							}
							NamedItem it = findValue(langCode, item.getValue().get(0), op.getSourceClass().getSimpleName(), warnings, getCaField(op));							
							if (it == null){
								throw new Exception(MessageFormat.format(
									Messages.FilterValidator_AssetFilter_ValueMatchingError,
									new Object[]{filter.asString(), op.getSourceClass().getSimpleName(), item.getValue().get(0)}));
							}else{
								warnings.add(MessageFormat.format(Messages.FilterValidator_AssetFilter_UnqiueIdMatchingError,
										new Object[]{op.getGuiName(Locale.getDefault()), item.getValue().get(0)}));
								//update uuid
								((AssetFilter)filter).setValue(it.getUuid());
							}
						}else if (Employee.class.isAssignableFrom(op.getSourceClass())){
							//lookup employee
							Employee e = findEmployee(item.getId(), item.getValue().get(0), item.getValue().get(1), warnings);
							if (e != null){
								((AssetFilter)filter).setValue(e.getUuid());
							}else{
								throw new Exception(
									MessageFormat.format(Messages.FilterValidator_AssetFilter_EmployeeError,
										new Object[]{filter.asString(), 
										item.getValue().get(0) + " "+ item.getValue().get(1) + " [" + item.getId() + "] "}));	 //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
							}	
						}else{
							throw new Exception(
								MessageFormat.format(
								Messages.FilterValidator_AssetFilterErrorB, new Object[]{ filter.asString()}));
						}
										
					}
				}
			}catch(Exception ex){
				this.ex = ex;
			}
		}
	}
		
	class GroupByValidatorVisitor implements IGroupByVisitor{

		private Exception ex;
		private ArrayList<String> warnings = new ArrayList<String>();
		
		@Override
		public void visit(IGroupBy filter) {
			if (ex != null)
				return;
			try {
				if (filter instanceof AssetGroupBy) {
					AssetGroupBy groupBy = (AssetGroupBy) filter;
					if (groupBy.getItems() == null){
						//nothing to validate
						return;
					}
					AssetFilterOption op = groupBy.getOption();
					if (op.getType() == AssetQueryOptionType.UUID){
						for (int i = 0; i < groupBy.getItems().length; i ++){
							String key = groupBy.getItems()[i];
							UUID uuid = UuidUtils.stringToUuid(key);
							Object x = op.getObject(session, uuid);
							if (validateObject(x)){
								//object existing in db; do not need to look it up
								return;
							}
							if (NamedKeyItem.class.isAssignableFrom(op.getSourceClass())){
								UuidItemType item = uuidLookup.get(key);
								if (item == null || item.getValue().size() == 0) {
									throw new Exception(MessageFormat.format(
										COULDNOTRESOLVE_ERRMSG, new Object[]{groupBy.asString()}));
								}
								NamedKeyItem it = findKeyValue(item.getValue().get(0), op.getSourceClass().getSimpleName(), getCaField(op));
								if (it != null){
									groupBy.getItems()[i] = UuidUtils.uuidToString(it.getUuid());
									continue;
								}
								
							}
							// this item does not exist in the database for the current conservation area
							if (NamedItem.class.isAssignableFrom(op.getSourceClass())) {
								UuidItemType item = uuidLookup.get(key);
								if (item == null) {
									throw new Exception(MessageFormat.format(
										COULDNOTRESOLVE_ERRMSG, new Object[]{groupBy.asString()}));
								}
								NamedItem it = findValue(langCode, item.getValue().get(0), op.getSourceClass().getSimpleName(), warnings, getCaField(op));
								if (it == null) {
									throw new Exception(
										MessageFormat.format(
												Messages.AssetGroupBy_Error_NoMatchingValue,
												new Object[]{groupBy.asString(), op.getSourceClass().getSimpleName(),item.getValue().get(0) }));
								} else {
									warnings.add(
										MessageFormat.format(
												Messages.AssetGroupBy_Error_NotUniqueId,
												new Object[]{op.getGuiName(Locale.getDefault()), item.getValue().get(0) }));	
									// update uuid
									groupBy.getItems()[i] = UuidUtils.uuidToString(it.getUuid());
								}
							} else if (Employee.class.isAssignableFrom(op.getSourceClass())) {
								UuidItemType item = uuidLookup.get(key);
								if (item == null) {
									throw new Exception(MessageFormat.format(
										COULDNOTRESOLVE_ERRMSG, new Object[]{groupBy.asString()}));
								}
								// lookup employee
								Employee e = findEmployee(item.getId(),item.getValue().get(0), item.getValue().get(1), warnings);
								if (e != null) {
									groupBy.getItems()[i] = UuidUtils.uuidToString(e.getUuid());
								} else {
									throw new Exception(
										MessageFormat.format(
										Messages.AssetGroupBy_Error_NoEmployee,
										new Object[]{groupBy.asString(),item.getValue().get(0) + " " + item.getValue().get(1) + " [" + item.getId() + "] "})); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
								}
							} else {
								throw new Exception(MessageFormat.format(
									COULDNOTRESOLVE_ERRMSG, new Object[]{groupBy.asString()}));
							}
							
						}
					} else if (op.getType() == AssetQueryOptionType.KEY){
						Collection<ConservationArea> toSearch = Collections.singleton(importCa);
						if (importCa.getIsCcaa()){
							toSearch = SmartDB.getConservationAreaConfiguration().getConservationAreas();
						}
						for (String key : groupBy.getItems()){
							//look for key in database
							CriteriaBuilder cb = session.getCriteriaBuilder();
							CriteriaQuery<Long> c = cb.createQuery(Long.class);
							Root<?> from = c.from(op.getSourceClass());
							c.select(cb.count(from));
							c.where(cb.and(
									from.get("conservationArea").in(toSearch), //$NON-NLS-1$
									cb.equal(from.get("keyId"), key) //$NON-NLS-1$
									));
							Long cnt = session.createQuery(c).uniqueResult();
								if (cnt == 0){
									throw new Exception(MessageFormat.format(Messages.AssetGroupBy_KeyNotFoundError, new Object[]{op.getGuiName(Locale.getDefault()), key}));
								}
							}
						}
					}
			} catch (Exception ex) {
				this.ex = ex;
			}
		}
	}
	
	
	class ValueVisitor implements IValueVisitor{
		private Exception ex;
		
		@Override
		public void visit(IValueItem item) {
			if (ex != null) return ;
			try{
				if (item instanceof AssetAttributeValueItem){
					AssetAttributeValueItem it = (AssetAttributeValueItem)item;
					validateAttribute(it.getAttributeKey());
					if (it.getCategoryKey() != null){
						validateCategory(it.getCategoryKey());
					}
					if (DataModel.getAggregation(it.getAggregationKey()) == null){
						throw new Exception(MessageFormat.format(Messages.AttributeValueItem_AggNoSupported, new Object[]{ it.getAggregationKey() }));
					}
					
				}else if (item instanceof AssetCategoryValueItem){
					CategoryValueItem i = (CategoryValueItem)item;
					if (i.getCategoryHKey() == null){
						//this is okay; we assume all categories
					}else{
						validateCategory(((CategoryValueItem) item).getCategoryHKey());
					}
				}
			}catch (Exception ex){
				this.ex = ex;
			}
		}
	};
}

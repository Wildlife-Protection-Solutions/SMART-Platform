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
package org.wcs.smart.patrol.query.parser;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.NamedItem;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.patrol.query.internal.Messages;
import org.wcs.smart.patrol.query.parser.PatrolQueryOptions.PatrolQueryOption;
import org.wcs.smart.patrol.query.parser.PatrolQueryOptions.PatrolQueryOptionType;
import org.wcs.smart.patrol.query.parser.internal.filter.PatrolFilter;
import org.wcs.smart.patrol.query.parser.internal.summary.PatrolGroupBy;
import org.wcs.smart.query.model.filter.IFilter;
import org.wcs.smart.query.model.filter.IFilterVisitor;
import org.wcs.smart.query.model.filter.IGroupByVisitor;
import org.wcs.smart.query.model.filter.IValueVisitor;
import org.wcs.smart.query.model.filter.QueryDefinitionValidator;
import org.wcs.smart.query.model.summary.AttributeValueItem;
import org.wcs.smart.query.model.summary.CategoryValueItem;
import org.wcs.smart.query.model.summary.IGroupBy;
import org.wcs.smart.query.model.summary.IValueItem;
import org.wcs.smart.query.xml.model.UuidItemType;
import org.wcs.smart.util.SmartUtils;

/**
 * Tools for patrol query validation.
 * 
 * @author Emily
 *
 */
public class PatrolQueryValidator extends QueryDefinitionValidator {

	private static final String COULDNOTRESOLVE_ERRMSG = Messages.PatrolGroupBy_CouldNotResolveFilter;
	
	private String langCode;
	private HashMap<String, UuidItemType> uuidLookup;
	

	/**
	 * @param langCode the language value of the query 
	 * @param uuidLookup a uuid lookup map that looks up uuid values
	 * @param session database session
	 * 
	 */
	public PatrolQueryValidator(String langCode, HashMap<String, UuidItemType> uuidLookup, Session session ){
		super(session);
		
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
		super.validate(item);
		
		GroupByValidatorVisitor vv = new GroupByValidatorVisitor();
		item.visit(vv);
		if (vv.ex != null){
			throw vv.ex;
		}
		return Collections.emptyList();
	}
	
	
	/**
	 * Validates value items
	 */
	@Override
	public List<String> validate(IValueItem item) throws Exception{
		super.validate(item);
		
		ValueVisitor vv = new ValueVisitor();
		item.accept(vv);
		if (vv.ex != null){
			throw vv.ex;
		}
		return Collections.emptyList();
	}
	
	
	
	
	class FilterValidatorVisitor implements IFilterVisitor{

		private Exception ex;
		private ArrayList<String> warnings = new ArrayList<String>();
		
		@Override
		public void visit(IFilter filter) {
			if (ex != null) return;
			try{
				if (filter instanceof PatrolFilter){
					PatrolQueryOption op = ((PatrolFilter) filter).getPatrolOption();
					if (op.getType() == PatrolQueryOptions.PatrolQueryOptionType.UUID){
						byte[] uuid = SmartUtils.decodeHex( ((PatrolFilter)filter).getValue() );
						if (op.getObject(session, uuid)  != null){
							//object exists in db
							return;
						}else{
							UuidItemType item = uuidLookup.get(  ((PatrolFilter)filter).getValue()  );
							if (item == null){
								throw new Exception(
									MessageFormat.format(
									Messages.FilterValidator_PatrolFilterError, new Object[]{ filter.asString()}));
							}
							if (NamedItem.class.isAssignableFrom(op.getSourceClass())){
								if (item.getValue() == null || item.getValue().size() == 0){
									throw new Exception(MessageFormat.format(
											Messages.PatrolQueryValidator_CouldNotMatchFilter,
											new Object[]{filter.asString()}));
								}
								NamedItem it = findValue(langCode, item.getValue().get(0), op.getSourceClass().getSimpleName(), warnings);							
								if (it == null){
									throw new Exception(MessageFormat.format(
										Messages.FilterValidator_PatrolFilter_ValueMatchingError,
										new Object[]{filter.asString(), op.getSourceClass().getSimpleName(), item.getValue().get(0)}));
								}else{
									warnings.add(MessageFormat.format(Messages.FilterValidator_PatrolFilter_UnqiueIdMatchingError,
											new Object[]{op.getGuiName(), item.getValue().get(0)}));
									//update uuid
									((PatrolFilter)filter).setValue(SmartUtils.encodeHex(it.getUuid()));
								}
							}else if (Employee.class.isAssignableFrom(op.getSourceClass())){
								//lookup employee
								Employee e = findEmployee(item.getId(), item.getValue().get(0), item.getValue().get(1), warnings);
								if (e != null){
									((PatrolFilter)filter).setValue(SmartUtils.encodeHex(e.getUuid()));
								}else{
									throw new Exception(
										MessageFormat.format(Messages.FilterValidator_PatrolFilter_EmployeeError,
											new Object[]{filter.asString(), 
											item.getValue().get(0) + " "+ item.getValue().get(1) + " [" + item.getId() + "] "}));	 //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
								}	
							}else{
								throw new Exception(
									MessageFormat.format(
									Messages.FilterValidator_PatrolFilterErrorB, new Object[]{ filter.asString()}));
							}
						}					
					}
				}
			}catch(Exception ex){
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
				if (item instanceof AttributeValueItem){
					AttributeValueItem it = (AttributeValueItem)item;
					validateAttribute(it.getAttributeKey());
					if (it.getCategoryKey() != null){
						validateCategory(it.getCategoryKey());
					}
					if (it.getAggregation() == null){
						throw new Exception(MessageFormat.format(Messages.AttributeValueItem_AggNoSupported, new Object[]{ it.getAggregationKey() }));
					}
					
				}else if (item instanceof CategoryValueItem){
					validateCategory(((CategoryValueItem) item).getCategoryHKey());
				}
			}catch (Exception ex){
				this.ex = ex;
			}
		}
	};
	
	class GroupByValidatorVisitor implements IGroupByVisitor{

		private Exception ex;
		private ArrayList<String> warnings = new ArrayList<String>();
		
		@Override
		public void visit(IGroupBy filter) {
			if (ex != null)
				return;
			try {
				if (filter instanceof PatrolGroupBy) {
					PatrolGroupBy groupBy = (PatrolGroupBy) filter;
					if (groupBy.getItems() == null){
						//nothing to validate
						return;
					}
					if (groupBy.getOption().getType() == PatrolQueryOptionType.UUID){
						for (int i = 0; i < groupBy.getItems().length; i ++){
							String key = groupBy.getItems()[i];
							byte[] uuid = SmartUtils.decodeHex(key);
							if (groupBy.getOption().getObject(session, uuid) == null) {
								// this item does not exist in the database
								if (NamedItem.class.isAssignableFrom(groupBy.getOption().getSourceClass())) {
									UuidItemType item = uuidLookup.get(key);
									if (item == null) {
										throw new Exception(MessageFormat.format(
											COULDNOTRESOLVE_ERRMSG, new Object[]{groupBy.asString()}));
									}
									NamedItem it = findValue(langCode, item.getValue().get(0), groupBy.getOption().getSourceClass().getSimpleName(), warnings);
									if (it == null) {
										throw new Exception(
											MessageFormat.format(
													Messages.PatrolGroupBy_Error_NoMatchingValue,
													new Object[]{groupBy.asString(), groupBy.getOption().getSourceClass().getSimpleName(),item.getValue().get(0) }));
									} else {
										warnings.add(
											MessageFormat.format(
													Messages.PatrolGroupBy_Error_NotUniqueId,
													new Object[]{groupBy.getOption().getGuiName(), item.getValue().get(0) }));	
										// update uuid
										groupBy.getItems()[i] = SmartUtils.encodeHex(it.getUuid());
									}
								} else if (Employee.class.isAssignableFrom(groupBy.getOption().getSourceClass())) {
									UuidItemType item = uuidLookup.get(key);
									if (item == null) {
										throw new Exception(MessageFormat.format(
											COULDNOTRESOLVE_ERRMSG, new Object[]{groupBy.asString()}));
									}
									// lookup employee
									Employee e = findEmployee(item.getId(),item.getValue().get(0), item.getValue().get(1), warnings);
									if (e != null) {
										groupBy.getItems()[i] = SmartUtils.encodeHex(e.getUuid());
									} else {
										throw new Exception(
											MessageFormat.format(
											Messages.PatrolGroupBy_Error_NoEmployee,
											new Object[]{groupBy.asString(),item.getValue().get(0) + " " + item.getValue().get(1) + " [" + item.getId() + "] "})); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
									}
								} else {
									throw new Exception(MessageFormat.format(
										COULDNOTRESOLVE_ERRMSG, new Object[]{groupBy.asString()}));
								}
							}
						}
					} else if (groupBy.getOption().getType() == PatrolQueryOptionType.KEY){
						for (String key : groupBy.getItems()){
							//look for key in database
							Long cnt = (Long) session.createCriteria(groupBy.getOption().getSourceClass())
									.add(Restrictions.eq("keyId", key)) //$NON-NLS-1$
									.add(Restrictions.in("conservationArea", SmartDB.getConservationAreaConfiguration().getConservationAreas())) //$NON-NLS-1$
									.setProjection(Projections.rowCount()).list().get(0); 
								if (cnt == 0){
									throw new Exception(MessageFormat.format(Messages.PatrolGroupBy_KeyNotFoundError, new Object[]{groupBy.getOption().getGuiName(), key}));
								}
							}
						}
					}
			} catch (Exception ex) {
				this.ex = ex;
			}
		}
	}
}

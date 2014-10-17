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
package org.wcs.smart.er.query.importexport;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.hibernate.Session;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.NamedKeyItem;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.er.query.internal.Messages;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.er.model.MissionAttribute;
import org.wcs.smart.er.model.MissionAttributeListItem;
import org.wcs.smart.er.model.SamplingUnit;
import org.wcs.smart.er.model.SamplingUnitAttribute;
import org.wcs.smart.er.model.SamplingUnitAttributeListItem;
import org.wcs.smart.er.model.Survey;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.query.filter.MissionFilter;
import org.wcs.smart.er.query.filter.MissionMemberFilter;
import org.wcs.smart.er.query.filter.MissionPropertyFilter;
import org.wcs.smart.er.query.filter.SamplingUnitAttributeFilter;
import org.wcs.smart.er.query.filter.SamplingUnitFilter;
import org.wcs.smart.er.query.filter.SurveyDesignFilter;
import org.wcs.smart.er.query.filter.SurveyFilter;
import org.wcs.smart.er.query.filter.summary.MissionAttributeGroupBy;
import org.wcs.smart.er.query.filter.summary.MissionIdGroupBy;
import org.wcs.smart.er.query.filter.summary.SamplingUnitAttributeGroupBy;
import org.wcs.smart.er.query.filter.summary.SamplingUnitGroupBy;
import org.wcs.smart.er.query.filter.summary.SurveyIdGroupBy;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.model.filter.AttributeFilter;
import org.wcs.smart.query.model.filter.IFilter;
import org.wcs.smart.query.model.filter.IFilterVisitor;
import org.wcs.smart.query.model.filter.IGroupByVisitor;
import org.wcs.smart.query.model.filter.IValueVisitor;
import org.wcs.smart.query.model.filter.QueryDefinitionValidator;
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
public class SurveyQueryValidator extends QueryDefinitionValidator {

	private static final String COULDNOTRESOLVE_ERRMSG = Messages.SurveyQueryValidator_FilterError;
	
	private HashMap<String, UuidItemType> uuidLookup;
	

	/**
	 *  
	 * @param uuidLookup a uuid lookup map that looks up uuid values
	 * @param session database session
	 * 
	 */
	public SurveyQueryValidator(HashMap<String, UuidItemType> uuidLookup, Session session ){
		super(session);
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
	
	
	
	
	class FilterValidatorVisitor implements IFilterVisitor{

		private Exception ex;
		private ArrayList<String> warnings = new ArrayList<String>();
		
		@Override
		public void visit(IFilter filter) {
			if (ex != null) return;
			try{
				if (filter instanceof MissionMemberFilter){
					byte[] uuid = ((MissionMemberFilter)filter).getUuid();
					Object x = session.load(Employee.class, uuid);
					if (x != null 
							&& x instanceof Employee
							&& ((Employee)x).getConservationArea().equals(SmartDB.getCurrentConservationArea())){
						return;
					}
					UuidItemType item = uuidLookup.get(  SmartUtils.encodeHex(((MissionMemberFilter)filter).getUuid())  );
					if (item == null){
						throw new Exception(
							MessageFormat.format(COULDNOTRESOLVE_ERRMSG, new Object[]{ filter.asString()}));
					}
					//lookup employee
					Employee e = findEmployee(item.getId(), item.getValue().get(0), item.getValue().get(1), warnings);
					if (e != null){
						((MissionMemberFilter)filter).setUuid(e.getUuid());
					}else{
						throw new Exception(
							MessageFormat.format(Messages.SurveyQueryValidator_EmployeeNotFound,
										new Object[]{filter.asString(), 
										item.getValue().get(0) + " "+ item.getValue().get(1) + " [" + item.getId() + "] "})); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					}	
					
				}else if (filter instanceof MissionFilter){
					if (((MissionFilter) filter).getType() == MissionFilter.Type.UUID){
						byte[] uuid = SmartUtils.decodeHex(((MissionFilter)filter).getValue());
						Object x = session.load(Mission.class, uuid);
						if (x != null 
								&& x instanceof Mission
								&& ((Mission)x).getSurvey().getSurveyDesign().getConservationArea().equals(SmartDB.getCurrentConservationArea())){
							return;
						}
						throw new Exception(MessageFormat.format(Messages.SurveyQueryValidator_MissionNotFound, new Object[]{filter.asString()}));
					}
				}else if (filter instanceof SurveyFilter){
					if (((SurveyFilter) filter).getType() == SurveyFilter.Type.UUID){
						byte[] uuid = SmartUtils.decodeHex(((SurveyFilter)filter).getValue());
						Object x = session.load(Survey.class, uuid);
						if (x != null 
								&& x instanceof Survey
								&& ((Survey)x).getSurveyDesign().getConservationArea().equals(SmartDB.getCurrentConservationArea())){
							return;
						}
						throw new Exception(MessageFormat.format(Messages.SurveyQueryValidator_SurveyNotFound, new Object[]{filter.asString()}));
					}
				}else if (filter instanceof SamplingUnitFilter){
					SamplingUnitFilter suFilter = (SamplingUnitFilter) filter;
					if (suFilter.getUuid().equals(SamplingUnitFilter.NONE_KEY)){
						return;
					}
					Object x = session.load(SamplingUnit.class, SmartUtils.decodeHex(suFilter.getUuid()));
					if (x != null 
							&& x instanceof SamplingUnit
							&& ((SamplingUnit)x).getSurveyDesign().getConservationArea().equals(SmartDB.getCurrentConservationArea())){
						return;
					}
					UuidItemType item = uuidLookup.get(  suFilter.getUuid() );
					if (item != null){
						SamplingUnit it = findSamplingUnit(item.getId());
						if (it != null){
							suFilter.setUuid(SmartUtils.encodeHex(it.getUuid()));
						}
					}
					throw new Exception(MessageFormat.format(Messages.SurveyQueryValidator_SuNotFound,
							new Object[]{suFilter.asString()}));
				}else if (filter instanceof MissionPropertyFilter){
					MissionPropertyFilter mpFilter = (MissionPropertyFilter) filter;
					//validate mission attribute
					NamedKeyItem it = findKeyValue(mpFilter.getAttributeKey(), MissionAttribute.class.getSimpleName());
					if (it == null){
						throw new Exception(MessageFormat.format(Messages.SurveyQueryValidator_MisisonAttributeNotFound, new Object[]{mpFilter.getAttributeKey()}));
					}
					if (mpFilter.getAttributeType() == AttributeType.LIST){
						validateMissionAttributeListItem((String)mpFilter.getValue(), mpFilter.getAttributeKey());
					}
				}else if (filter instanceof SamplingUnitAttributeFilter){
					SamplingUnitAttributeFilter mpFilter = (SamplingUnitAttributeFilter) filter;
					//validate mission attribute
					NamedKeyItem it = findKeyValue(mpFilter.getSamplingUnitAttributeKey(), SamplingUnitAttribute.class.getSimpleName());
					if (it == null){
						throw new Exception(MessageFormat.format(Messages.SurveyQueryValidator_SuAttributeNotFound, new Object[]{mpFilter.getSamplingUnitAttributeKey()}));
					}
					if (mpFilter.getAttributeType() == AttributeType.LIST){
						validateSamplingUnitAttributeListItem((String)mpFilter.getValue(), mpFilter.getSamplingUnitAttributeKey());
					}
				}else if (filter instanceof SurveyDesignFilter){
					NamedKeyItem it = findKeyValue(
							((SurveyDesignFilter) filter).getKey(),
							SurveyDesign.class.getSimpleName());
					if (it == null){
						throw new Exception(MessageFormat.format(Messages.SurveyQueryValidator_SurveyDesignNotFound, new Object[]{((SurveyDesignFilter) filter).getKey()}));
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
				if (filter instanceof MissionAttributeGroupBy) {
					MissionAttributeGroupBy gb = (MissionAttributeGroupBy) filter;
					//validate mission attribute
					NamedKeyItem it = findKeyValue(gb.getAttributeKey(), MissionAttribute.class.getSimpleName());
					if (it == null){
						throw new Exception(MessageFormat.format(Messages.SurveyQueryValidator_MissionAttributeKeyNotFound, new Object[]{gb.getAttributeKey()}));
					}
					if (gb.getRawItems() != null){
						for (String item : gb.getRawItems()){
							validateMissionAttributeListItem(item, gb.getAttributeKey());
						}
					}
				
				}else if (filter instanceof SamplingUnitAttributeGroupBy){
					SamplingUnitAttributeGroupBy gb = (SamplingUnitAttributeGroupBy) filter;
					//validate mission attribute
					NamedKeyItem it = findKeyValue(gb.getAttributeKey(), SamplingUnitAttribute.class.getSimpleName());
					if (it == null){
						throw new Exception(MessageFormat.format(Messages.SurveyQueryValidator_SuAttributeKeyNotFound, new Object[]{gb.getAttributeKey()}));
					}
					if (gb.getRawItems() != null){
						for (String item : gb.getRawItems()){
							validateSamplingUnitAttributeListItem(item, gb.getAttributeKey());
						}
					}
				}else if (filter instanceof SamplingUnitGroupBy){
					SamplingUnitGroupBy gb = (SamplingUnitGroupBy) filter;
					String[] items = gb.getRawItems();
					for (int i = 0; i < items.length;i++){
						Object x = session.load(SamplingUnit.class, SmartUtils.decodeHex(items[i]));
						if (x != null 
							&& ((SamplingUnit)x).getSurveyDesign().getConservationArea().equals(SmartDB.getCurrentConservationArea())){
							continue;
						}
						UuidItemType item = uuidLookup.get(  SmartUtils.encodeHex(((MissionMemberFilter)filter).getUuid())  );
						if (item != null){
							SamplingUnit it = findSamplingUnit(item.getId());
							if (it != null){
								items[i] = SmartUtils.encodeHex(it.getUuid());
								continue;
							}
						}
						throw new Exception(MessageFormat.format(Messages.SurveyQueryValidator_SuGroupByError,
							new Object[]{gb.asString()}));
					}
				}else if (filter instanceof SurveyIdGroupBy){
				}else if (filter instanceof MissionIdGroupBy){
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
		}
	};
	
	/**
	 * Looks up a named by item by the keyid
	 * @param key the key to find
	 * @param objectType the type of object to search.  Assumes this object type has a conservationArea property
	 * 
	 * @return the matching item or null if nothing found
	 */
	public SamplingUnit findSamplingUnit(String key){
		
		String sql = "SELECT c FROM SamplingUnit c WHERE id = :keyId and c.surveyDesign.conservationArea = :ca "; //$NON-NLS-1$ 
		
		org.hibernate.Query query = session.createQuery(sql);
		query.setParameter("keyId", key); //$NON-NLS-1$
		query.setParameter("ca", SmartDB.getCurrentConservationArea()); //$NON-NLS-1$
		
		List<?> results = query.list();
		if (results.size() == 0){
			return null;
		}else if (results.size() > 1){
			return null;
		}else{
			return (SamplingUnit)results.get(0);
		}
	}
	
	/**
	 * Validates that a given attribute list item exists.  Throws an exception if not found.
	 * @param key the list item key
	 * @param attributeKey the attribute key (associated with the list item)
	 * @param session
	 * @throws Exception
	 */
	public void validateMissionAttributeListItem(String key, String attributeKey) throws Exception{
		if (key.equals(AttributeFilter.ANY_OPTION.getKey())){
			return ;
		}
		String sql = "SELECT c FROM " + MissionAttributeListItem.class.getSimpleName() + " c WHERE keyId = :keyId and c.attribute.conservationArea = :ca "; //$NON-NLS-1$ //$NON-NLS-2$
		
		org.hibernate.Query query = session.createQuery(sql);
		query.setParameter("keyId", key); //$NON-NLS-1$
		query.setParameter("ca", SmartDB.getCurrentConservationArea()); //$NON-NLS-1$
		
		List<?> results = query.list();
		if (results.size() == 0 || results.size() > 1){
			throw new Exception(MessageFormat.format(Messages.SurveyQueryValidator_MissionListItemNotFound, new Object[]{key, attributeKey}));
		}
	}
	
	/**
	 * Validates that a given attribute list item exists.  Throws an exception if not found.
	 * @param key the list item key
	 * @param attributeKey the attribute key (associated with the list item)
	 * @param session
	 * @throws Exception
	 */
	public void validateSamplingUnitAttributeListItem(String key, String attributeKey) throws Exception{
		if (key.equals(AttributeFilter.ANY_OPTION.getKey())){
			return ;
		}
		String sql = "SELECT c FROM " + SamplingUnitAttributeListItem.class.getSimpleName() + " c WHERE keyId = :keyId and c.attribute.conservationArea = :ca "; //$NON-NLS-1$ //$NON-NLS-2$
		
		org.hibernate.Query query = session.createQuery(sql);
		query.setParameter("keyId", key); //$NON-NLS-1$
		query.setParameter("ca", SmartDB.getCurrentConservationArea()); //$NON-NLS-1$
		
		List<?> results = query.list();
		if (results.size() == 0 || results.size() > 1){
			throw new Exception(MessageFormat.format(Messages.SurveyQueryValidator_SuListItemNotFound, new Object[]{key, attributeKey}));
		}
	}
}

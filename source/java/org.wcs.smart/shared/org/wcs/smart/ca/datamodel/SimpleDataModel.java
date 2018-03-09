package org.wcs.smart.ca.datamodel;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.hibernate.Session;
import org.wcs.smart.ICoreLabelProvider;
import org.wcs.smart.SmartContext;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.NamedKeyItem;
import org.wcs.smart.hibernate.QueryFactory;

public class SimpleDataModel {

	protected ConservationArea ca;	//the conservation area of the data model
	protected List<Category> categories;	//the root categories for the data model
	protected List<Attribute> attributes; // all attributes associated with datamodel
	
	public enum I18nMessages{
		NAME_INVALID,
		NAME_REQUIRED,
		KEY_TO_LONG,
		KEY_INVALID_CHARS,
		KEY_NOT_UNIQUE,
		KEY_KEYWORD,
		KEY_REQUIRED
	}
	
	public SimpleDataModel(ConservationArea ca, List<Category> rootCategories, List<Attribute> attributes) {
	
		this.ca = ca;
		this.categories = new ArrayList<Category>();
		this.categories.addAll(rootCategories);
		
		this.attributes = new ArrayList<Attribute>();
		this.attributes.addAll(attributes);
		
	}
	
	/**
	 * 
	 * @return conservation area associated with data model
	 */
	public ConservationArea getConservationArea(){
		return this.ca;
	}
	/**
	 * 
	 * @param ca conservation area associated with the data model
	 */
	public void setConservationArea(ConservationArea ca){
		this.ca = ca;
	}
	
	/**
	 * 
	 * @return list of root categories
	 */
	public List<Category> getCategories(){
		return this.categories;
	}
	
	/**
	 * 
	 * @return all attributes associated with any category
	 * in the data model.
	 */
	public List<Attribute> getAttributes(){
		return this.attributes;
	}
	
	private static String getMessage(I18nMessages key, Locale locale) {
		return SmartContext.INSTANCE.getClass(ICoreLabelProvider.class).getLabel(key, locale);
	}
	
	/**
	 * Validates a data model object name
	 * <p>Names must not be empty, less than DmObject.MAX_NAME_LENGTH characters</p>
	 * 
	 * 
	 * @param name the name to validate.
	 * @param l the language associated with the name
	 * @return <code>null</code> if the name is valid otherwise a string description of the error
	 */
	public static String validateName(String name, Language l, Locale locale){
		if (!isSimpleString(name.trim(), DmObject.MAX_NAME_LENGTH, 0)){
			return MessageFormat.format(getMessage(I18nMessages.NAME_INVALID, locale),
					new Object[]{l.getDisplayName()});

		}
		if (l.isDefault() && name.trim().length() == 0){
			return getMessage(I18nMessages.NAME_REQUIRED, locale);
		}
		
		return null;
	}
	
	/**
	 * Validates a data model object key.
	 * <p>Keys must not be empty, less than DmObject.MAX_KEY_LENGTH characters,
	 * and different from their siblings.</p>
	 * 
	 * @param key the key to validate.
	 * @param otherValues set of {@link DmObject} the key value must be different from
	 * @return <code>null</code> if the key is valid otherwise a string description of the error
	 */
	public static String validateKey(String key, Collection<? extends NamedKeyItem> otherValues, Locale locale){
		if (key == null || key.isEmpty()){
			return getMessage(I18nMessages.KEY_REQUIRED, locale);
		}
		if (key.length() > NamedKeyItem.MAX_KEY_LENGTH ){
			return MessageFormat.format(getMessage(I18nMessages.KEY_TO_LONG, locale), new Object[]{DmObject.MAX_KEY_LENGTH});
		}
		if (!key.matches(NamedKeyItem.VALID_DM_KEY_PATTERN)){
			return getMessage(I18nMessages.KEY_INVALID_CHARS, locale);
		}
		if (checkKeyExists(key, otherValues)){
			return getMessage(I18nMessages.KEY_NOT_UNIQUE, locale);
		}
		for (String keyword: NamedKeyItem.KEYWORDS){
			if (keyword.equals(key)){
				return MessageFormat.format(getMessage(I18nMessages.KEY_KEYWORD, locale), new Object[]{keyword});
			}
		}
		return null;
	}
	
	/*
	 * determines if a key exists in 
	 * a set of objects
	 */
	private static boolean checkKeyExists(String key, Collection<? extends NamedKeyItem> otherValues){
		if (otherValues == null){
			return false;
		}
		for (NamedKeyItem other : otherValues){
			if (key.equals(other.getKeyId())){
				return true;
			}
		}
		return false;
	}
	
	
	private static Boolean isSimpleString (String str, Integer maxChars, Integer minChar){
		Pattern p = Pattern.compile(  "[^^\\p{L}\\p{M}\\p{Nd}- :_&'<>,\\(\\)\\.\\#;\\/]", Pattern.CASE_INSENSITIVE); //$NON-NLS-1$
		Matcher m = p.matcher(str);
		boolean b = m.find();
		if (b || str.length() > maxChars || str.length() < minChar ){
			return false;
		}else{ 
			return true;
		}		
	}
	
	public static SimpleDataModel loadDataModel(ConservationArea ca, Session session) throws Exception{
		CriteriaBuilder cb = session.getCriteriaBuilder();
		CriteriaQuery<Category> c = cb.createQuery(Category.class);
		Root<Category> root = c.from(Category.class);
		c.where(cb.and(
				cb.equal(root.get("conservationArea"), ca), //$NON-NLS-1$
				cb.isNull(root.get("parent")) //$NON-NLS-1$
				));
		c.orderBy(cb.asc(root.get("categoryOrder"))); //$NON-NLS-1$
		List<Category> rootCategories = session.createQuery(c).getResultList();
					
		List<Attribute> attribute = QueryFactory.buildQuery(session, Attribute.class, 
			new Object[] {"conservationArea", ca}).getResultList(); //$NON-NLS-1$
		
		SimpleDataModel dm = new SimpleDataModel(ca, rootCategories, attribute);
		return dm;
	}
}

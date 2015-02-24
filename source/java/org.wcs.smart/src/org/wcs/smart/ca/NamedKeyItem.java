package org.wcs.smart.ca;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.regex.Pattern;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;

import org.wcs.smart.ca.datamodel.DmObject;
import org.wcs.smart.internal.Messages;


/**
 * An object that contains both a uuid, keyid and a 
 * set of names.
 * <p>The database table that implements this class
 * must have a keyid varchar(128) field.</p>
 * 
 * @author Emily
 *
 */
@Entity
@Inheritance(strategy= InheritanceType.TABLE_PER_CLASS)
public class NamedKeyItem extends NamedItem {
	/**
	 * Maximum length of the key identifier
	 */
	public static final int MAX_KEY_LENGTH = 128;
	
	
	private static final String INVALID_START_CHARS_KEY_PATTERN = "[^a-z]+"; //$NON-NLS-1$
	private static final String VALID_DM_KEY_PATTERN = "[a-z]{1}[a-z0-9_]*"; //$NON-NLS-1$
	/*
	 * These are keywords that cannot be used as keys; for querying purposes.
	 */
	private static final String[] KEYWORDS = new String[]{"and", "or", "not", "contains", "notcontains", "equals"}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
	
	
	private String keyId;		//key
	
	protected NamedKeyItem(){
		
	}
	
	/**
	 * Key that represents the team.  Teams that
	 * are to be considered the "same" in cross-ca analysis should
	 * have the same key.
	 *    
	 * @return
	 */
	@Column(name="keyId")
	public String getKeyId(){
		return this.keyId;
	}
	
	/**
	 * Unique key
	 * @param keyId
	 */
	public void setKeyId(String keyId){
		this.keyId = keyId;
	}

	/**
	 * Generates a key for a dm object from a name.
	 * 
	 * @param value the name provided
	 * @param otherValues list of other dm objects that the key must be different from
	 * 
	 * @return valid key
	 */
	public static String generateKey (String value, Collection<? extends NamedKeyItem> otherValues){
		String raw = value.toLowerCase().replaceAll("[^a-z0-9_]", ""); //$NON-NLS-1$ //$NON-NLS-2$
		//DM keys should not start with number or '_' character or queries will be invalid see ticket #354
		if (!raw.isEmpty() && Pattern.matches(INVALID_START_CHARS_KEY_PATTERN, raw.subSequence(0, 1))) {
			raw = raw.replaceFirst(INVALID_START_CHARS_KEY_PATTERN, ""); //$NON-NLS-1$
		}
		if (raw.isEmpty()){
			raw = "object"; //$NON-NLS-1$
		}
	
		int count = 0;
		String key = raw;
		if (raw.length() > NamedKeyItem.MAX_KEY_LENGTH){
			key = raw.substring(0, NamedKeyItem.MAX_KEY_LENGTH);
		}

		for (String keyword: KEYWORDS){
			if (keyword.equals(key)){
				key = key + "_"; //$NON-NLS-1$
				break;
			}
		}
		while(checkKeyExists(key, otherValues)){
			count ++;
			String cnt = String.valueOf(count);
			if (raw.length() + cnt.length() > DmObject.MAX_KEY_LENGTH){
				key = raw.substring(0, DmObject.MAX_KEY_LENGTH - cnt.length() ) + cnt;
			}else{
				key = raw + String.valueOf(count);
			}
			
		}
		
		return key;
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
	
	
	/**
	 * Validates a data model object key.
	 * <p>Keys must not be empty, less than DmObject.MAX_KEY_LENGTH characters,
	 * and different from their siblings.</p>
	 * 
	 * @param key the key to validate.
	 * @param otherValues set of {@link DmObject} the key value must be different from
	 * @return <code>null</code> if the key is valid otherwise a string description of the error
	 */
	public static String validateKey(String key, Collection<? extends NamedKeyItem> otherValues){
		if (key == null || key.isEmpty()){
			return Messages.DataModel_Error_Key_NotEmpty;
		}
		if (key.length() > MAX_KEY_LENGTH ){
			return MessageFormat.format(Messages.DataModel_Error_Key_ToLong, new Object[]{DmObject.MAX_KEY_LENGTH});
		}
		if (!key.matches(VALID_DM_KEY_PATTERN)){
			return Messages.DataModel_Error_Key_InvalidCharacters;
		}
		if (checkKeyExists(key, otherValues)){
			return Messages.DataModel_Error_Key_NotUnique;
		}
		for (String keyword: KEYWORDS){
			if (keyword.equals(key)){
				return MessageFormat.format(Messages.DataModel_KeywordKeyError, new Object[]{keyword});
			}
		}
		return null;
	}
}

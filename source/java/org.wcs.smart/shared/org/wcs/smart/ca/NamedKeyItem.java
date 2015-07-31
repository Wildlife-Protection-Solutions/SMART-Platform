package org.wcs.smart.ca;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;


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
	
	
	public static final String INVALID_START_CHARS_KEY_PATTERN = "[^a-z]+"; //$NON-NLS-1$
	public static final String VALID_DM_KEY_PATTERN = "[a-z]{1}[a-z0-9_]*"; //$NON-NLS-1$
	/*
	 * These are keywords that cannot be used as keys; for querying purposes.
	 */
	public static final String[] KEYWORDS = new String[]{"and", "or", "not", "contains", "notcontains", "equals"}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
	
	
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
}

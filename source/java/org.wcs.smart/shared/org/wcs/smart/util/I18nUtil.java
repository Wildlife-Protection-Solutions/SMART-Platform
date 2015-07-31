package org.wcs.smart.util;

import java.util.Locale;
import java.util.StringTokenizer;
import java.util.UUID;

/**
 * Utility class providing methods to access the Locale of the current thread and to get
 * Localised strings.
 * 
 * @author Roy Wetherall
 */
public class I18nUtil
{
    /**
     * Thread-local containing the general Locale for the current thread
     */
    private static ThreadLocal<UUID> languageLocale = new ThreadLocal<UUID>();
    private static ThreadLocal<UUID> caLocale = new ThreadLocal<UUID>();
    
  
    /**
     * Set the locale for the current thread.
     * 
     * @param locale    the locale
     */
    public static void setLocale(UUID locale)
    {
    	languageLocale.set(locale);
    }

    /**
     * Get the general local for the current thread, will revert to the default locale if none 
     * specified for this thread.
     * 
     * @return  the general locale
     */
    public static UUID getLocale()
    {
    	UUID locale = languageLocale.get(); 
        return locale;
    }
    
    /**
     * Set the locale for the current thread.
     * 
     * @param locale    the locale
     */
    public static void setCa(UUID locale)
    {
    	caLocale.set(locale);
    }

    /**
     * Get the general local for the current thread, will revert to the default locale if none 
     * specified for this thread.
     * 
     * @return  the general locale
     */
    public static UUID getCa()
    {
    	UUID locale = caLocale.get(); 
        return locale;
    }
   
	/**
	 * Converts a local string stored as lang_Country
	 * to a Locale object.
	 * 
	 * @param s
	 * @return
	 */
	public static String localeToString(Locale l)
	{
	    String key = l.getLanguage();
	    if (!l.getCountry().isEmpty()){
	    	key += "_" + l.getCountry(); //$NON-NLS-1$
	    }
	    return key.trim();
	}
	
	
	
	/**
	 * Converts a local string stored as lang_Country
	 * to a Locale object.
	 * 
	 * @param s
	 * @return
	 */
	public static Locale stringToLocale(String s)
	{
	    String l = ""; //$NON-NLS-1$
	    String c = ""; //$NON-NLS-1$
		StringTokenizer tempStringTokenizer = new StringTokenizer(s.trim(),"_"); //$NON-NLS-1$
	    if(tempStringTokenizer.hasMoreTokens())
	    	l = (String) tempStringTokenizer.nextElement();
	    
	    if(tempStringTokenizer.hasMoreTokens())
	    	c = (String) tempStringTokenizer.nextElement();
	    return new Locale(l,c);
	}
}

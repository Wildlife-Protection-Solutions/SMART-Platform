package org.wcs.smart.connect.report.udig;

import java.util.HashMap;

import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

public class UdigPreferenceStore extends ScopedPreferenceStore {

	private HashMap<String, Object> prefs = new HashMap<String,Object>();
	public UdigPreferenceStore() {
		super (DefaultScope.INSTANCE, "org.wcs.smart"); //$NON-NLS-1$
	}

	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.preference.IPreferenceStore#addPropertyChangeListener(org.eclipse.jface.util.IPropertyChangeListener)
	 */
	@Override
	public void addPropertyChangeListener(IPropertyChangeListener listener) {
	}


	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.preference.IPreferenceStore#contains(java.lang.String)
	 */
	@Override
	public boolean contains(String name) {
		return prefs.containsKey(name);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.preference.IPreferenceStore#firePropertyChangeEvent(java.lang.String,
	 *      java.lang.Object, java.lang.Object)
	 */
	@Override
	public void firePropertyChangeEvent(String name, Object oldValue,
			Object newValue) {

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.preference.IPreferenceStore#getBoolean(java.lang.String)
	 */
	@Override
	public boolean getBoolean(String name) {
		Boolean b = (Boolean) prefs.get(name);
		if (b == null)
			return getDefaultBoolean(name);
		return b;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.preference.IPreferenceStore#getDefaultBoolean(java.lang.String)
	 */
	@Override
	public boolean getDefaultBoolean(String name) {
		return BOOLEAN_DEFAULT_DEFAULT;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.preference.IPreferenceStore#getDefaultDouble(java.lang.String)
	 */
	@Override
	public double getDefaultDouble(String name) {
		return DOUBLE_DEFAULT_DEFAULT;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.preference.IPreferenceStore#getDefaultFloat(java.lang.String)
	 */
	@Override
	public float getDefaultFloat(String name) {
		return  FLOAT_DEFAULT_DEFAULT;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.preference.IPreferenceStore#getDefaultInt(java.lang.String)
	 */
	@Override
	public int getDefaultInt(String name) {
		return INT_DEFAULT_DEFAULT;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.preference.IPreferenceStore#getDefaultLong(java.lang.String)
	 */
	@Override
	public long getDefaultLong(String name) {
		return LONG_DEFAULT_DEFAULT;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.preference.IPreferenceStore#getDefaultString(java.lang.String)
	 */
	@Override
	public String getDefaultString(String name) {
		return STRING_DEFAULT_DEFAULT;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.preference.IPreferenceStore#getDouble(java.lang.String)
	 */
	@Override
	public double getDouble(String name) {
		Double b = (Double) prefs.get(name);
		if (b == null)
			return getDefaultDouble(name);
		return b;
	}


	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.preference.IPreferenceStore#getFloat(java.lang.String)
	 */
	@Override
	public float getFloat(String name) {
		Float b = (Float) prefs.get(name);
		if (b == null)
			return getDefaultFloat(name);
		return b;	
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.preference.IPreferenceStore#getInt(java.lang.String)
	 */
	@Override
	public int getInt(String name) {
		Integer b = (Integer) prefs.get(name);
		if (b == null)
			return getDefaultInt(name);
		return b;	
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.preference.IPreferenceStore#getLong(java.lang.String)
	 */
	@Override
	public long getLong(String name) {
		Long b = (Long) prefs.get(name);
		if (b == null)
			return getDefaultLong(name);
		return b;	
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.preference.IPreferenceStore#getString(java.lang.String)
	 */
	@Override
	public String getString(String name) {
		String b = (String) prefs.get(name);
		if (b == null)
			return getDefaultString(name);
		return b;	
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.preference.IPreferenceStore#isDefault(java.lang.String)
	 */
	@Override
	public boolean isDefault(String name) {
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.preference.IPreferenceStore#needsSaving()
	 */
	@Override
	public boolean needsSaving() {
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.preference.IPreferenceStore#putValue(java.lang.String,
	 *      java.lang.String)
	 */
	@Override
	public void putValue(String name, String value) {
		prefs.put(name, value);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.preference.IPreferenceStore#removePropertyChangeListener(org.eclipse.jface.util.IPropertyChangeListener)
	 */
	@Override
	public void removePropertyChangeListener(IPropertyChangeListener listener) {
		//NOT SUPPORTED
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.preference.IPreferenceStore#setDefault(java.lang.String,
	 *      double)
	 */
	@Override
	public void setDefault(String name, double value) {
		//NOT SUPPORTED
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.preference.IPreferenceStore#setDefault(java.lang.String,
	 *      float)
	 */
	@Override
	public void setDefault(String name, float value) {
		//NOT SUPPORTED
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.preference.IPreferenceStore#setDefault(java.lang.String,
	 *      int)
	 */
	@Override
	public void setDefault(String name, int value) {
		//NOT SUPPORTED
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.preference.IPreferenceStore#setDefault(java.lang.String,
	 *      long)
	 */
	@Override
	public void setDefault(String name, long value) {
		//NOT SUPPORTED
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.preference.IPreferenceStore#setDefault(java.lang.String,
	 *      java.lang.String)
	 */
	@Override
	public void setDefault(String name, String defaultObject) {
		//NOT SUPPORTED
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.preference.IPreferenceStore#setDefault(java.lang.String,
	 *      boolean)
	 */
	@Override
	public void setDefault(String name, boolean value) {
		//NOT SUPPORTED
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.preference.IPreferenceStore#setToDefault(java.lang.String)
	 */
	@Override
	public void setToDefault(String name) {
		//NOT SUPPORTED

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.preference.IPreferenceStore#setValue(java.lang.String,
	 *      double)
	 */
	@Override
	public void setValue(String name, double value) {
		//NOT SUPPORTED
		prefs.put(name, value);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.preference.IPreferenceStore#setValue(java.lang.String,
	 *      float)
	 */
	@Override
	public void setValue(String name, float value) {
		//NOT SUPPORTED
		prefs.put(name, value);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.preference.IPreferenceStore#setValue(java.lang.String,
	 *      int)
	 */
	@Override
	public void setValue(String name, int value) {
		//NOT SUPPORTED
		prefs.put(name, value);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.preference.IPreferenceStore#setValue(java.lang.String,
	 *      long)
	 */
	@Override
	public void setValue(String name, long value) {
		//NOT SUPPORTED
		prefs.put(name, value);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.preference.IPreferenceStore#setValue(java.lang.String,
	 *      java.lang.String)
	 */
	@Override
	public void setValue(String name, String value) {
		//NOT SUPPORTED
		prefs.put(name, value);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.preference.IPreferenceStore#setValue(java.lang.String,
	 *      boolean)
	 */
	@Override
	public void setValue(String name, boolean value) {
		//NOT SUPPORTED
		prefs.put(name, value);
	}


}

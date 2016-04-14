package org.wcs.smart.connect.report;

import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

public class UdigPreferenceStore extends ScopedPreferenceStore {

	public UdigPreferenceStore() {
		super (DefaultScope.INSTANCE, "org.wcs.smart");
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
		return false;
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
		return BOOLEAN_DEFAULT_DEFAULT;
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
		return DOUBLE_DEFAULT_DEFAULT;
	}


	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.preference.IPreferenceStore#getFloat(java.lang.String)
	 */
	@Override
	public float getFloat(String name) {
		return FLOAT_DEFAULT_DEFAULT;		
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.preference.IPreferenceStore#getInt(java.lang.String)
	 */
	@Override
	public int getInt(String name) {
		return INT_DEFAULT_DEFAULT;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.preference.IPreferenceStore#getLong(java.lang.String)
	 */
	@Override
	public long getLong(String name) {
		return LONG_DEFAULT_DEFAULT;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.preference.IPreferenceStore#getString(java.lang.String)
	 */
	@Override
	public String getString(String name) {
		return  STRING_DEFAULT_DEFAULT; 
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
		//NOT SUPPORTED
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
	}


}

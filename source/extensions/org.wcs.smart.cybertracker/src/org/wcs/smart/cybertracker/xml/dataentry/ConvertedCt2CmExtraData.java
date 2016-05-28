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
package org.wcs.smart.cybertracker.xml.dataentry;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;
import org.wcs.smart.ca.Language;
import org.wcs.smart.cybertracker.internal.Messages;
import org.wcs.smart.cybertracker.model.ConfigurableModelCtPropertiesProfile;
import org.wcs.smart.cybertracker.model.CyberTrackerPropertiesProfile;
import org.wcs.smart.dataentry.model.ConfigurableModel;
import org.wcs.smart.dataentry.model.xml.external.IConvertedCmExtraData;
import org.wcs.smart.dataentry.model.xml.generated.CmExtraDataLabelKeyType;
import org.wcs.smart.dataentry.model.xml.generated.CmExtraDataType;
import org.wcs.smart.dataentry.model.xml.generated.NameType;
import org.wcs.smart.hibernate.SmartDB;

/**
 * Wrapper for CyberTracker extra-data conversion (from XML) result.
 * 
 * @author elitvin
 * @since 4.0.0
 */
public class ConvertedCt2CmExtraData implements IConvertedCmExtraData {

	private List<String> warnings = new ArrayList<String>();
	
	private CyberTrackerPropertiesProfile profile;
	
	public ConvertedCt2CmExtraData(List<CmExtraDataType> extraDataList, Session session) {
		for (CmExtraDataType extraDataType : extraDataList) {
			if(Ct2CmXmlExtraDataContribution.TYPE_PROFILE.equals(extraDataType.getType())) {
				profile = fetchProfile(extraDataType, session);
				break;
			}
		}
	}

	private CyberTrackerPropertiesProfile fetchProfile(CmExtraDataType dataType, Session session) {
		if (dataType.getLabelKey().isEmpty()) {
			warnings.add(Messages.ConvertedCt2CmExtraData_InvalidKeyRecord);
			return null;
		}
		String name = null;
		Language language = SmartDB.getCurrentConservationArea().getDefaultLanguage();
		String codeWarnLabel = language.getCode();
		for (CmExtraDataLabelKeyType labelKeyType : dataType.getLabelKey()) {
			if (Ct2CmXmlExtraDataContribution.KEY_NAME.equals(labelKeyType.getKey())) {
				name = findNameInLanguage(language, labelKeyType.getLabel());
				if (name == null) {
					language = SmartDB.getCurrentLanguage();
					if (!codeWarnLabel.equals(language.getCode())) {
						codeWarnLabel += "; " + language.getCode(); //$NON-NLS-1$
					}
					name = findNameInLanguage(language, labelKeyType.getLabel());
				}
			}
		}
		if (name == null) {
			warnings.add(MessageFormat.format(Messages.ConvertedCt2CmExtraData_NameNotDetermined, codeWarnLabel));
			return null;
		}
		
		//name is known and contain valid data
		//try to fetch this profile from database
		Query query = session.createQuery("SELECT pr FROM CyberTrackerPropertiesProfile pr, Label lbl WHERE lbl.id.element.uuid = pr.uuid AND lbl.value = :name AND lbl.id.language = :language"); //$NON-NLS-1$
		query.setParameter("name", name); //$NON-NLS-1$
		query.setParameter("language", language); //$NON-NLS-1$
		@SuppressWarnings("unchecked")
		List<CyberTrackerPropertiesProfile> list = query.list();
		if (list.isEmpty()) {
			warnings.add(MessageFormat.format(Messages.ConvertedCt2CmExtraData_NameNotFound, name));
			return null;
		}
		if (list.size() > 1) {
			warnings.add(MessageFormat.format(Messages.ConvertedCt2CmExtraData_NameMultipleMatches, name));
		}
		return list.get(0);
	}

	private String findNameInLanguage(Language language, List<NameType> names) {
		String code = language.getCode();
		String name = null;
		for (NameType labelType : names) {
			if (code.equals(labelType.getLanguageCode())) {
				name = labelType.getValue();
				break;
			}
		}
		return name;
	}
	
	@Override
	public List<String> getWarnings() {
		return warnings;
	}

	@Override
	public boolean saveInTransaction(Session session, ConfigurableModel cm) {
		if (profile != null) {
			ConfigurableModelCtPropertiesProfile ctProfile = new ConfigurableModelCtPropertiesProfile();
			ctProfile.setModel(cm);
			ctProfile.setProfile(profile);
			session.saveOrUpdate(ctProfile);
		}
		return true;
	}

}

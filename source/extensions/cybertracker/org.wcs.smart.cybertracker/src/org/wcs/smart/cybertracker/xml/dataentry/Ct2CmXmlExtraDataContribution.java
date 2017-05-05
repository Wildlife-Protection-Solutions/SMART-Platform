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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.Label;
import org.wcs.smart.ca.UuidItem;
import org.wcs.smart.cybertracker.model.ConfigurableModelCtPropertiesProfile;
import org.wcs.smart.cybertracker.model.CyberTrackerPropertiesProfile;
import org.wcs.smart.dataentry.model.ConfigurableModel;
import org.wcs.smart.dataentry.model.xml.external.IConvertedCmExtraData;
import org.wcs.smart.dataentry.model.xml.external.IXmlCmExtraDataContribution;
import org.wcs.smart.dataentry.model.xml.generated.CmExtraDataLabelKeyType;
import org.wcs.smart.dataentry.model.xml.generated.CmExtraDataType;
import org.wcs.smart.dataentry.model.xml.generated.NameType;

/**
 * CyberTracker contribution for dataentry module to provide ability to 
 * Export/Import to/from XML file CyberTraker related data (for example associated profile).
 * 
 * @author elitvin
 * @since 4.0.0
 */
public class Ct2CmXmlExtraDataContribution implements IXmlCmExtraDataContribution {

	static final String TYPE_PROFILE = "ct_profile"; //$NON-NLS-1$

	static final String KEY_NAME = "name"; //$NON-NLS-1$
	
	@Override
	public List<CmExtraDataType> exportData(ConfigurableModel cm, Session session) {
		List<CmExtraDataType> result = new ArrayList<>(1);
		if (cm.getUuid() == null) {
			return result;
		}
		ConfigurableModelCtPropertiesProfile p = (ConfigurableModelCtPropertiesProfile)session
				.createCriteria(ConfigurableModelCtPropertiesProfile.class)
				.add(Restrictions.eq("id.model", cm)).uniqueResult(); //$NON-NLS-1$
		if (p != null && p.getProfile() != null) {
			CmExtraDataType data = fromProfile(p.getProfile());
			data.setType(TYPE_PROFILE);
			result.add(data);
		}
		return result;
	}

	private CmExtraDataType fromProfile(CyberTrackerPropertiesProfile profile) {
		CmExtraDataType data = new CmExtraDataType();

		CmExtraDataLabelKeyType nameKey = new CmExtraDataLabelKeyType();
		for (Label label : profile.getNames()) {
			NameType labelType = new NameType();
			labelType.setLanguageCode(label.getLanguage().getCode());
			labelType.setValue(label.getValue());
			nameKey.getLabel().add(labelType);
		}
		nameKey.setKey(KEY_NAME);
		data.getLabelKey().add(nameKey);

		return data;
	}
	
	@Override
	public IConvertedCmExtraData fromXml(List<CmExtraDataType> extraDataList, Map<String, UuidItem> dataMap, Session session) {
		return new ConvertedCt2CmExtraData(extraDataList, session);
	}

}

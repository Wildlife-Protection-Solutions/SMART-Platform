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
package org.wcs.smart.connect.cybertracker.xml.dataentry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.wcs.smart.ca.UuidItem;
import org.wcs.smart.connect.cybertracker.ConnectCtHibernateManager;
import org.wcs.smart.connect.cybertracker.model.ConnectAlert;
import org.wcs.smart.connect.cybertracker.model.ConnectCtProperties;
import org.wcs.smart.dataentry.model.ConfigurableModel;
import org.wcs.smart.dataentry.model.xml.external.IConvertedCmExtraData;
import org.wcs.smart.dataentry.model.xml.external.IXmlCmExtraDataContribution;
import org.wcs.smart.dataentry.model.xml.generated.CmExtraDataIntegerKeyType;
import org.wcs.smart.dataentry.model.xml.generated.CmExtraDataStringKeyType;
import org.wcs.smart.dataentry.model.xml.generated.CmExtraDataType;

/**
 * Connect Alerts CyberTracker contribution for dataentry module to provide ability to 
 * Export/Import to/from XML file Connect Alerts CyberTraker related data.
 * 
 * @author elitvin
 * @since 4.0.0
 */
public class ConnectCt2CmXmlExtraDataContribution implements IXmlCmExtraDataContribution {

	static final String TYPE_PROPERTIES = "connect_ct_properties"; //$NON-NLS-1$
	static final String TYPE_ALERT = "connect_ct_alert"; //$NON-NLS-1$
	
	static final String KEY_PING_FREQUENCY = "ping_frequency"; //$NON-NLS-1$
	
	static final String KEY_ALERT_ITEM = "alert_item"; //$NON-NLS-1$
	static final String KEY_ALERT_ATTRIBUTE = "attribute"; //$NON-NLS-1$
	static final String KEY_ALERT_TYPE = "type"; //$NON-NLS-1$
	static final String KEY_ALERT_LEVEL = "level"; //$NON-NLS-1$

	@Override
	public List<CmExtraDataType> exportData(ConfigurableModel cm, Session s) {
		List<CmExtraDataType> result = new ArrayList<>();
		if (cm.getUuid() == null) {
			return result;
		}
		
		ConnectCtProperties p = ConnectCtHibernateManager.getCtProperties(cm, s);
		if (p != null) {
			CmExtraDataType data = fromProperties(p);
			data.setType(TYPE_PROPERTIES);
			result.add(data);
		}
		
		List<ConnectAlert> alerts = ConnectCtHibernateManager.getConnectAlerts(cm, s, false);
		for (ConnectAlert a : alerts) {
			CmExtraDataType data = fromAlert(a);
			data.setType(TYPE_ALERT);
			result.add(data);
		}
		
		return result;
	}

	private CmExtraDataType fromProperties(ConnectCtProperties p) {
		CmExtraDataType data = new CmExtraDataType();

		CmExtraDataIntegerKeyType pingKey = new CmExtraDataIntegerKeyType();
		pingKey.setKey(KEY_PING_FREQUENCY);
		pingKey.setValue(p.getPingFrequency());
		data.getIntegerKey().add(pingKey);

		return data;
	}

	private CmExtraDataType fromAlert(ConnectAlert a) {
		CmExtraDataType data = new CmExtraDataType();

		//alert item
		CmExtraDataStringKeyType itemKey = new CmExtraDataStringKeyType();
		itemKey.setKey(KEY_ALERT_ITEM);
		itemKey.setValue(a.getAlertItem().getUuid().toString());
		data.getStringKey().add(itemKey);

		//alert cm attribute
		if (a.getAttrubute() != null) {
			CmExtraDataStringKeyType attributeKey = new CmExtraDataStringKeyType();
			attributeKey.setKey(KEY_ALERT_ATTRIBUTE);
			attributeKey.setValue(a.getAttrubute().getUuid().toString());
			data.getStringKey().add(attributeKey);
		}
		
		//alert type
		CmExtraDataStringKeyType typeKey = new CmExtraDataStringKeyType();
		typeKey.setKey(KEY_ALERT_TYPE);
		typeKey.setValue(a.getTypeInternal());
		data.getStringKey().add(typeKey);

		//alert level
		CmExtraDataIntegerKeyType levelKey = new CmExtraDataIntegerKeyType();
		levelKey.setKey(KEY_ALERT_LEVEL);
		levelKey.setValue(a.getLevel());
		data.getIntegerKey().add(levelKey);

		return data;
	}

	@Override
	public IConvertedCmExtraData fromXml(List<CmExtraDataType> extraDataList, Map<String, UuidItem> dataMap, Session session) {
		return new ConvertedConnectCt2CmExtraData(extraDataList, dataMap, session);
	}

}

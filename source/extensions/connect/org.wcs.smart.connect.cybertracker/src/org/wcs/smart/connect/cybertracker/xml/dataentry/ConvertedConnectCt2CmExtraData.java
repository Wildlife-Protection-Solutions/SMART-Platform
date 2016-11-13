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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.wcs.smart.ca.UuidItem;
import org.wcs.smart.connect.cybertracker.internal.Messages;
import org.wcs.smart.connect.cybertracker.model.ConnectAlert;
import org.wcs.smart.connect.cybertracker.model.ConnectCtProperties;
import org.wcs.smart.dataentry.model.CmAttribute;
import org.wcs.smart.dataentry.model.ConfigurableModel;
import org.wcs.smart.dataentry.model.xml.external.IConvertedCmExtraData;
import org.wcs.smart.dataentry.model.xml.generated.CmExtraDataIntegerKeyType;
import org.wcs.smart.dataentry.model.xml.generated.CmExtraDataStringKeyType;
import org.wcs.smart.dataentry.model.xml.generated.CmExtraDataType;
import org.wcs.smart.util.UuidUtils;

/**
 * Wrapper for Connect Alerts CyberTracker extra-data conversion (from XML) result.
 * 
 * @author elitvin
 * @since 4.0.0
 */
public class ConvertedConnectCt2CmExtraData implements IConvertedCmExtraData {

	private List<String> warnings = new ArrayList<>();

	private List<ConnectAlert> alerts = new ArrayList<>();
	private ConnectCtProperties properties;
	
	public ConvertedConnectCt2CmExtraData(List<CmExtraDataType> extraDataList, Map<String, UuidItem> dataMap, Session session) {
		for (CmExtraDataType extraDataType : extraDataList) {
			if(ConnectCt2CmXmlExtraDataContribution.TYPE_ALERT.equals(extraDataType.getType())) {
				ConnectAlert a = createAlert(extraDataType, dataMap);
				if (a != null) {
					alerts.add(a);
				}
			}
			if(ConnectCt2CmXmlExtraDataContribution.TYPE_PROPERTIES.equals(extraDataType.getType())) {
				properties = createProperties(extraDataType);
			}
		}
	}

	private ConnectAlert createAlert(CmExtraDataType dataType, Map<String, UuidItem> dataMap) {
		ConnectAlert a = new ConnectAlert();
		for (CmExtraDataStringKeyType strKeyType : dataType.getStringKey()) {
			if (ConnectCt2CmXmlExtraDataContribution.KEY_ALERT_TYPE.equals(strKeyType.getKey())) {
				a.setTypeInternal(strKeyType.getValue());
			} if (ConnectCt2CmXmlExtraDataContribution.KEY_ALERT_ITEM.equals(strKeyType.getKey())) {
				UuidItem item = dataMap.get(strKeyType.getValue());
				if (item != null) {
					a.setAlertItem(item);
				} else {
					warnings.add(MessageFormat.format(Messages.ConvertedConnectCt2CmExtraData_InvalidReference, strKeyType.getValue()));
					return null;
				}
			} if (ConnectCt2CmXmlExtraDataContribution.KEY_ALERT_ATTRIBUTE.equals(strKeyType.getKey())) {
				UuidItem item = dataMap.get(strKeyType.getValue());
				if (item != null) {
					a.setAttrubute((CmAttribute)item);
				} else {
					warnings.add(MessageFormat.format(Messages.ConvertedConnectCt2CmExtraData_InvalidReference, strKeyType.getValue()));
					return null;
				}
			}
		}
		for (CmExtraDataIntegerKeyType intKeyType : dataType.getIntegerKey()) {
			if (ConnectCt2CmXmlExtraDataContribution.KEY_ALERT_LEVEL.equals(intKeyType.getKey())) {
				a.setLevel(intKeyType.getValue());
			}
		}
		
		//validation
		if (a.getAlertItem() == null) {
			warnings.add(Messages.ConvertedConnectCt2CmExtraData_Missing_AlertItem);
			return null;
		}
		if (a.getLevel() == null) {
			warnings.add(Messages.ConvertedConnectCt2CmExtraData_Missing_Level);
			return null;
		}
		
		return a;
	}

	private ConnectCtProperties createProperties(CmExtraDataType dataType) {
		ConnectCtProperties p = new ConnectCtProperties();
		
		for (CmExtraDataIntegerKeyType intKeyType : dataType.getIntegerKey()) {
			if (ConnectCt2CmXmlExtraDataContribution.KEY_PING_FREQUENCY.equals(intKeyType.getKey())) {
				p.setPingFrequency(intKeyType.getValue());
			}else if (ConnectCt2CmXmlExtraDataContribution.KEY_DATAUPLOAD_FREQUENCY.equals(intKeyType.getKey())) {
				p.setDataFrequency(intKeyType.getValue());
			}
		}
		
		
		for (CmExtraDataStringKeyType strKeyType : dataType.getStringKey()) {
			if (ConnectCt2CmXmlExtraDataContribution.KEY_PING_ALERTTYPE.equals(strKeyType.getKey()) && strKeyType.getValue() != null) {
				p.setPingType(UuidUtils.stringToUuid(strKeyType.getValue()));
			}
		}
		//validation
		if (p.getPingFrequency() == null) {
			warnings.add(Messages.ConvertedConnectCt2CmExtraData_Missing_PingFrequency);
			p.setPingFrequency(ConnectCtProperties.FREQUENCY_DEFAULT_VALUE);			
		}
		if (p.getDataFrequency() == null) {
			warnings.add(Messages.ConvertedConnectCt2CmExtraData_Missing_DataFrequency);
			p.setPingFrequency(ConnectCtProperties.FREQUENCY_DEFAULT_VALUE);			
		}
		if (p.getPingFrequency() > 0 && p.getPingType() == null){
			warnings.add(Messages.ConvertedConnectCt2CmExtraData_PositionTypeNotSet);
		}
		
		return p;
	}

	@Override
	public List<String> getWarnings() {
		return warnings;
	}

	@Override
	public boolean saveInTransaction(Session session, ConfigurableModel cm) {
		if (properties != null) {
			properties.setModel(cm);
			session.saveOrUpdate(properties);
		}
		for (ConnectAlert a : alerts) {
			a.setModel(cm);
			session.saveOrUpdate(a);
		}
		return true;
	}

}

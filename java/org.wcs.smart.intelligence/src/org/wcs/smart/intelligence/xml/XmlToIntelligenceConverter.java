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
package org.wcs.smart.intelligence.xml;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Label;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.NamedItem;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.intelligence.IntelligenceHibernateManager;
import org.wcs.smart.intelligence.internal.Messages;
import org.wcs.smart.intelligence.model.Intelligence;
import org.wcs.smart.intelligence.model.IntelligenceAttachment;
import org.wcs.smart.intelligence.model.IntelligencePoint;
import org.wcs.smart.intelligence.model.IntelligenceSource;
import org.wcs.smart.intelligence.xml.model.IntelligenceType;
import org.wcs.smart.intelligence.xml.model.LabelType;
import org.wcs.smart.intelligence.xml.model.PointType;
import org.wcs.smart.util.SmartUtils;

/**
 * Class responsible for converting XML data to intelligence object.
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class XmlToIntelligenceConverter {

	private Session session;
	private ConservationArea ca;

	private Intelligence intelligence;
	private List<String> warnings = new ArrayList<String>();
	
	private File attachmentLocation = null;
	
	public void fromXml(IntelligenceType xml, Session session, ConservationArea ca, File attachLoc) throws Exception {
		this.session = session;
		this.ca = ca;
		this.attachmentLocation = attachLoc;
		
		intelligence = new Intelligence();
		intelligence.setConservationArea(ca);
		intelligence.setCreator(SmartDB.getCurrentEmployee());
		
		/* names */
		for(LabelType labelType : xml.getName()) {
			Label label = labelForElement(labelType, intelligence);
			if (label != null) {
				//validate name
				if (!SmartUtils.isSimpleString(label.getValue(),
						SmartUtils.RegExLevel.ALLOWED_CHARS_COMPLEX_REGEX,
						org.wcs.smart.ca.Label.MAX_LENGTH, 1)) {
					if (label.getLanguage().isDefault()){
						//error
						throw new Exception(MessageFormat.format(Messages.XmlToIntelligenceConverter_InvalidDefaultName, label.getValue(), org.wcs.smart.ca.Label.MAX_LENGTH, SmartUtils.RegExLevel.ALLOWED_CHARS_COMPLEX_REGEX.textDesc));
					}else{
						//just skip
						warnings.add(MessageFormat.format(Messages.XmlToIntelligenceConverter_InvalidAdditionalName, label.getValue(), label.getLanguage().getCode(), org.wcs.smart.ca.Label.MAX_LENGTH,SmartUtils.RegExLevel.ALLOWED_CHARS_COMPLEX_REGEX.textDesc));
					}
				}else{
					//valid names lets add it
					intelligence.getNames().add(label);
				}
			}
		}
		//a name for default language is required
		if (intelligence.getNames().size() == 0){
			//this should never happen; as a check is done elsewhere for this
			throw new Exception(Messages.XmlToIntelligenceConverter_NoNameFound);
		}
		if (intelligence.findNameNull(ca.getDefaultLanguage()) == null){
			//just pick any value
			intelligence.updateName(ca.getDefaultLanguage(), intelligence.getNames().iterator().next().getValue());
		}
		
		/* dates */
		if (xml.getReceivedDate() == null){
			throw new Exception(Messages.XmlToIntelligenceConverter_InvalidRecievedDate);
		}
		if (xml.getFromDate() == null){
			throw new Exception(Messages.XmlToIntelligenceConverter_InvalidFromDate);
		}
		intelligence.setReceivedDate(xml.getReceivedDate().toGregorianCalendar().getTime());
		intelligence.setFromDate(xml.getFromDate().toGregorianCalendar().getTime());
		if (xml.getToDate() != null) {
			intelligence.setToDate(xml.getToDate().toGregorianCalendar().getTime());
		}

		/* source */
		String key = xml.getSource();
		key = key != null ? key.toLowerCase() : ""; //$NON-NLS-1$
		IntelligenceSource source = IntelligenceHibernateManager.getSourceByKeyId(session, key);
		if (source == null) {
			warnings.add(MessageFormat.format(Messages.XmlToIntelligenceConverter_SourceNotFound, xml.getSource()));
		}
		intelligence.setSource(source);

		/* description */
		intelligence.setDescription(xml.getDescription());
		if (intelligence.getDescription().length() > Intelligence.MAX_DESCRIPTION_LENTH){
			warnings.add(MessageFormat.format(Messages.XmlToIntelligenceConverter_DescriptionTooLong, Intelligence.MAX_DESCRIPTION_LENTH));
			intelligence.setDescription(intelligence.getDescription().substring(0, Intelligence.MAX_DESCRIPTION_LENTH));
		}

		/* points */
		for(PointType pointType : xml.getPoints()) {
			IntelligencePoint point = new IntelligencePoint();
			point.setIntelligence(intelligence);
			point.setX(pointType.getX());
			point.setY(pointType.getY());
			intelligence.getPoints().add(point);
		}

		/* attachments */
		if (attachmentLocation != null) {
			for(String filename : xml.getAttachments()) {
				IntelligenceAttachment attach = new IntelligenceAttachment();
				File f = new File(attachmentLocation.getAbsoluteFile() + File.separator + IIntelligenceXmlDataConstants.ATTACHMENT_DIR_NAME + File.separator + filename );
				if (!f.exists()){
					warnings.add(MessageFormat.format(Messages.XmlToIntelligenceConverter_NoAttachmentFound_Warning, filename, f.getAbsolutePath()));
				} else {
					attach.setCopyFromLocation(f);
					attach.setIntelligence(intelligence);
					attach.setFilename(filename);
					intelligence.getAttachments().add(attach);
				}
			}
		}
	}

	private Label labelForElement(LabelType labelType, NamedItem element) {
		Criteria criteria = session.createCriteria(Language.class).add(Restrictions.eq("ca", ca)).add(Restrictions.ilike("code", labelType.getLanguageCode())); //$NON-NLS-1$ //$NON-NLS-2$
		
		@SuppressWarnings("unchecked")
		List<Language> results = criteria.list();
		if (results.isEmpty()) {
			warnings.add(MessageFormat.format(Messages.XmlToIntelligenceConverter_NoLangCodeFound_Warning, labelType.getLanguageCode()));
			return null;
		} else if (results.size() > 1) {
			warnings.add(MessageFormat.format(Messages.XmlToIntelligenceConverter_SeveralLangCodeFound_Warning, labelType.getLanguageCode()));
		}
		Label label = new Label();
		label.setLanguage((Language) results.get(0));
		label.setElement(intelligence);
		label.setValue(labelType.getValue());
		return label;
	}

	/**
	 * @return any warnings generated during the import process
	 */
	public List<String> getWarnings(){
		return warnings;
	}
	
	/**
	 * @return the imported intelligence
	 */
	public Intelligence getImportedIntelligence(){
		return intelligence;
	}
	
}

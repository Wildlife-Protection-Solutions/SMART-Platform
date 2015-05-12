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
package org.wcs.smart.conversion.util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.wcs.smart.conversion.model.SmartMapping;
import org.wcs.smart.er.xml.model.missions.MissionType;
import org.wcs.smart.internal.ca.datamodel.xml.generate.DataModel;
import org.wcs.smart.patrol.xml.model.PatrolType;

public class FileUtil {

	public static SmartMapping loadSmartMapping(File file) throws JAXBException, IOException {
		JAXBContext context = JAXBContext.newInstance(SmartMapping.class);
		Unmarshaller un = context.createUnmarshaller();	
		Object o = un.unmarshal(file);
		return (SmartMapping) o;
	}

//	public static void write(File file, Object obj) throws JAXBException, IOException {
//		BufferedOutputStream outXml = new BufferedOutputStream(new FileOutputStream(file));
//		try {
//			write(obj, outXml, obj.getClass());
//		} finally {
//			outXml.close();
//		}
//	}
	
	public static void write(File file, SmartMapping mapping) throws JAXBException, IOException {
		BufferedOutputStream outXml = new BufferedOutputStream(new FileOutputStream(file));
		try {
			write(mapping, outXml, SmartMapping.class);
		} finally {
			outXml.close();
		}
	}

	public static void write(File file, PatrolType patrol) throws JAXBException, IOException{
		JAXBContext context = JAXBContext.newInstance(PatrolType.class);
		Marshaller marshaller = context.createMarshaller();
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, new Boolean(true));
		
		org.wcs.smart.patrol.xml.model.ObjectFactory objFactor = new org.wcs.smart.patrol.xml.model.ObjectFactory();
		
		JAXBElement<PatrolType> element = objFactor.createPatrol(patrol);
		marshaller.marshal(element, file);
	}

	public static void write(File file, MissionType mission) throws JAXBException, IOException{
		JAXBContext context = JAXBContext.newInstance(MissionType.class);
		Marshaller marshaller = context.createMarshaller();
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, new Boolean(true));
		
		org.wcs.smart.er.xml.model.missions.ObjectFactory objFactor = new org.wcs.smart.er.xml.model.missions.ObjectFactory();
		
		JAXBElement<MissionType> element = objFactor.createMission(mission);
		marshaller.marshal(element, file);
	}
	
	public static void write(Object obj, OutputStream file, Class<?> clazz) throws JAXBException, IOException {
		JAXBContext context = JAXBContext.newInstance(obj.getClass());
		Marshaller marshaller = context.createMarshaller();
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
		marshaller.marshal(obj, file);
	}

	public static DataModel loadDataModel(File file) throws JAXBException, FileNotFoundException {
		FileInputStream is = new FileInputStream(file);

		//read file directly instead of using the XmlSmartDataModelManager
		//because that manager uses classes which require hibernate and
		//we don't have to have to include hibernate in our build
		//this.smartDataModel = XmlSmartDataModelManager.readDataModel(is);
		JAXBContext context = JAXBContext.newInstance("org.wcs.smart.internal.ca.datamodel.xml.generate");
		Unmarshaller un = context.createUnmarshaller();	
		Object o = un.unmarshal(is);
		return (DataModel) o;
	}
	
}

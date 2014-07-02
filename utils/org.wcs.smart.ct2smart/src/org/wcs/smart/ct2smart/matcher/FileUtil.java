package org.wcs.smart.ct2smart.matcher;

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

import org.wcs.smart.ct2smart.matcher.model.Ct2Smart;
import org.wcs.smart.internal.ca.datamodel.xml.generate.DataModel;
import org.wcs.smart.patrol.xml.model.ObjectFactory;
import org.wcs.smart.patrol.xml.model.PatrolType;

public class FileUtil {

	public static Ct2Smart loadCt2Smart(File file) throws JAXBException, IOException {
		JAXBContext context = JAXBContext.newInstance(Ct2Smart.class);
		Unmarshaller un = context.createUnmarshaller();	
		Object o = un.unmarshal(file);
		return (Ct2Smart) o;
	}

//	public static void write(File file, Object obj) throws JAXBException, IOException {
//		BufferedOutputStream outXml = new BufferedOutputStream(new FileOutputStream(file));
//		try {
//			write(obj, outXml, obj.getClass());
//		} finally {
//			outXml.close();
//		}
//	}
	
	public static void write(File file, Ct2Smart ct2Smart) throws JAXBException, IOException {
		BufferedOutputStream outXml = new BufferedOutputStream(new FileOutputStream(file));
		try {
			write(ct2Smart, outXml, Ct2Smart.class);
		} finally {
			outXml.close();
		}
	}

	public static void write(File file, PatrolType patrol) throws JAXBException, IOException{
		JAXBContext context = JAXBContext.newInstance(PatrolType.class);
		Marshaller marshaller = context.createMarshaller();
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, new Boolean(true));
		
		ObjectFactory objFactor = new ObjectFactory();
		
		JAXBElement<PatrolType> element = objFactor.createPatrol(patrol);
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

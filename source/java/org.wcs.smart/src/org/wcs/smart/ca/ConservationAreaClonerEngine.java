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
package org.wcs.smart.ca;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.hibernate.Interceptor;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.id.UUIDGenerationStrategy;
import org.hibernate.id.UUIDGenerator;
import org.hibernate.id.uuid.StandardRandomStrategy;
import org.hibernate.type.UUIDBinaryType;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.hibernate.SmartHibernateManager;
import org.wcs.smart.internal.Messages;

/**
 * Engine for manging the creation of a conservation area using another conservation
 * area as a template.
 * 
 * @author Emily
 *
 */
public class ConservationAreaClonerEngine {

	private static final String ALL_KEY = "org.wcs.smart.*"; //$NON-NLS-1$

	public static final String EXTENSION_ID = "org.wcs.smart.ca.templateCloner"; //$NON-NLS-1$
	
	private ConservationArea templateCa;
	private ConservationArea newCa;
	
	private HashMap<UuidItem, UuidItem> templateToNewObjectMap;
	
	private Session session;
	private UUIDGenerator uuidGenerator = UUIDGenerator.buildSessionFactoryUniqueIdentifierGenerator();
	
	/**
	 * Creates a new cloner engine
	 * @param templateCa template conservation area
	 * @param newCa new conservation area
	 */
	public ConservationAreaClonerEngine(ConservationArea templateCa, ConservationArea newCa){
		this.templateCa = templateCa;
		this.newCa = newCa;
		templateToNewObjectMap = new HashMap<UuidItem, UuidItem>();
		createLanguageMap();
		
		Properties prop = new Properties();
		prop.put(UUIDGenerator.UUID_GEN_STRATEGY,
				StandardRandomStrategy.INSTANCE);
		prop.put(UUIDGenerator.UUID_GEN_STRATEGY_CLASS,
				UUIDGenerationStrategy.class.getName());
		uuidGenerator.configure(new UUIDBinaryType(), prop, null);
	}
	
	/*
	 * Maps template conservation area languages to new conservation area languages
	 */
	private void createLanguageMap(){
		for (Language l : templateCa.getLanguages()){
			for (Language l2 : newCa.getLanguages()){
				if (l.isSame(l2)){
					templateToNewObjectMap.put(l, l2);
					break;
				}
			}
		}
	}
	
	/**
	 * 
	 * @return the current hibernate session
	 */
	public Session getSession(){
		return this.session;
	}
	
	/**
	 * Copies names from one object to another.
	 * 
	 * @param copyFrom object to copy names from
	 * @param copyTo object to copy names to
	 */
	public void copyLabels(NamedItem copyFrom, NamedItem copyTo){
		for (Label l : copyFrom.getNames()){
			Label clone = new Label();
			Language lang = (Language) templateToNewObjectMap.get(l.getLanguage());
			if (lang != null){
				clone.setLanguage(lang);
				clone.setValue(l.getValue());
				clone.setElement(copyTo);
				copyTo.getNames().add(clone);
			}
		}
		
	}
	
	/**
	 * Copies descriptions from a NamedDescriptionItem object another object.
	 *  
	 * @param copyFrom item to copy descriptions from
	 * @param copyTo item to copy descriptions to
	 */
	public void copyDescriptions(NamedDescriptionItem copyFrom, NamedDescriptionItem copyTo){
		UUID uuid = (UUID) uuidGenerator.generate((SessionImplementor) session, copyTo);
		copyTo.setDescUuid(uuid);
		for (DescriptionLabel l : copyFrom.getDescriptions(session)){
			Language lang = (Language) templateToNewObjectMap.get(l.getLanguage());
			if (lang != null){
				DescriptionLabel clone = new DescriptionLabel();	
				clone.getId().setElement(uuid);
				clone.setLanguage(lang);
				clone.setValue(l.getValue());
				clone.setElement(uuid);
				getSession().save(clone);
				getSession().saveOrUpdate(copyTo);
				copyTo.getDescriptions(session).add(clone);
			}
		}
		
	}
	
	/**
	 * Copies descriptions from a NamedDescriptionKeyItem object another object.
	 *  
	 * @param copyFrom item to copy descriptions from
	 * @param copyTo item to copy descriptions to
	 */
	public void copyDescriptions(NamedDescriptionKeyItem copyFrom, NamedDescriptionKeyItem copyTo){
		UUID uuid = (UUID) uuidGenerator.generate((SessionImplementor) session, copyTo);
		copyTo.setDescUuid(uuid);
		for (DescriptionLabel l : copyFrom.getDescriptions(session)){
			Language lang = (Language) templateToNewObjectMap.get(l.getLanguage());
			if (lang != null){
				DescriptionLabel clone = new DescriptionLabel();	
				clone.getId().setElement(uuid);
				clone.setLanguage(lang);
				clone.setValue(l.getValue());
				clone.setElement(uuid);
				getSession().save(clone);
				getSession().saveOrUpdate(copyTo);
				copyTo.getDescriptions(session).add(clone);
			}
		}
		
	}
	
	/**
	 * 
	 * @return the conservation area to use a template
	 */
	public ConservationArea getTemplateCa(){
		return this.templateCa;
	}
	
	/**
	 * 
	 * @return the newly created conservation area
	 */
	public ConservationArea getNewCa(){
		return this.newCa;
	}
	
	/**
	 * Maintains a map of template conservation area objects to new conservation area objects.
	 * <p>
	 * This is used when uuid's need to be updated - for example a Patrol Team reference a Patrol Mandate.  After
	 * mandates are cloned, this map can contain a link from the old mandate to the new mandate that can be
	 * used when cloning the teams.
	 * 
	 * @param templateItem
	 * @param newItem
	 */
	public void addConservationItemMapping(UuidItem templateItem, UuidItem newItem){
		templateToNewObjectMap.put(templateItem,  newItem);
	}
	
	/**
	 * Searches the object map for a given uuid element.  If it cannot be found
	 * null is returned.
	 * 
	 * @param templateUuid
	 * @return 
	 * @throws Exception
	 */
	public UuidItem getNewConservationItem(UUID templateUuid) throws Exception{
		for (UuidItem key : templateToNewObjectMap.keySet()){
			if (key.getUuid().equals(templateUuid)){
				return templateToNewObjectMap.get(key);
			}
		}
		return null;
	}
	
	/**
	 * Searches the object map for a given object.  If not found then null is
	 * returned.
	 * 
	 * @param templateItem
	 * @return
	 * @throws Exception
	 */
	public UuidItem getNewConservationItem(UuidItem templateItem) {
		return  templateToNewObjectMap.get(templateItem);
	}
	

	
	/**
	 * Creates a new conservation area in the database, copying all the existing information
	 * from the template conservation area.
	 * 
	 * @param monitor
	 * @throws Exception
	 */
	public void processTemplate(IProgressMonitor monitor) throws Exception{
		List<Cloner> cloners = getCloners();
		
		monitor.beginTask(Messages.ConservationAreaClonerEngine_Progress_CopyingCa, cloners.size() * 10);
		
		List<Cloner> allCloners = new ArrayList<Cloner>();
		allCloners.addAll(cloners);
		
		List<Interceptor> interceptors = new ArrayList<>();
		for (Cloner c : allCloners) {
			interceptors.addAll(c.cloner.getInterceptors());
		}
		Interceptor interceptor = null;
		if (!interceptors.isEmpty()) {
			if (interceptors.size() == 1) {
				interceptor = interceptors.get(0);
			} else {
				//NOTE: MultiInterceptor implementation is based on reflection. This may have negative impact on performance.
				//Alternative approach is to implement similar logic without reflection.
				IMultiInterceptor mi = MultiInterceptor.createInstance();
				mi.addAll(interceptors);
				interceptor = mi;
			}
		}
		
		SmartHibernateManager.setUserName(SmartDB.DbUser.ADMIN.getUserName(), SmartDB.DbUser.ADMIN.getPassword());
		session = HibernateManager.openSession(interceptor);
		Transaction t = session.beginTransaction();
		try{
			session.save(newCa);
			for(Employee e : newCa.getEmployees()){
				HibernateManager.generateEmployeeId(e, getSession());
				session.save(e);
			}
			session.flush();
			
			List<Cloner> runAtEnd = new ArrayList<Cloner>();
			for(Cloner c : cloners){
				for (String s : c.requirements){
					if (s.equalsIgnoreCase(ALL_KEY)){
						runAtEnd.add(c);
						break;
					}
				}
			}
			cloners.removeAll(runAtEnd);
			
			Set<String> processed = new HashSet<String>();
			int cnt = 0;
			while(cloners.size() > 0){
				Cloner c = cloners.remove(0);
				boolean canProcess = true;
				
				for(String s : c.requirements){
					//if all cloners which match string s
					List<Cloner> matches = new ArrayList<Cloner>();
					for (Cloner tc : allCloners){
						if (tc.id.matches(s)){
							matches.add(tc);
						}
					}
					for (Cloner tc : matches){
						if (!processed.contains(tc.id)){
							canProcess = false;
							break;
						}
					}
					if (!canProcess){
						break;
					}
				}
				
				
				if (!canProcess){
					cloners.add(c);
					cnt++;
				}else{
					cnt = 0;
					c.cloner.cloneTemplateData(this, new SubProgressMonitor(monitor, 10));
					processed.add(c.id);
				}
				if (cnt > cloners.size()){
					throw new Exception(Messages.ConservationAreaClonerEngine_Error_CircularDependency);
				}
			}
			
			//run the ones at end
			while(runAtEnd.size() > 0){
				Cloner c = runAtEnd.remove(0);
				boolean canProcess = true;
				
				for(String s : c.requirements){
					//if all cloners which match string s
					if (s.equals(ALL_KEY)) continue;
					List<Cloner> matches = new ArrayList<Cloner>();
					for (Cloner tc : allCloners){
						if (tc.id.matches(s)){
							matches.add(tc);
						}
					}
					for (Cloner tc : matches){
						if (!processed.contains(tc.id)){
							canProcess = false;
							break;
						}
					}
					if (!canProcess){
						break;
					}
				}
				
				
				if (!canProcess){
					runAtEnd.add(c);
					cnt++;
				}else{
					cnt = 0;
					c.cloner.cloneTemplateData(this, new SubProgressMonitor(monitor, 10));
					processed.add(c.id);
				}
				if (cnt > runAtEnd.size()){
					throw new Exception(Messages.ConservationAreaClonerEngine_Error_CircularDependency);
				}
			}
			
			
			
			t.commit();
		}catch(Exception ex){
			
			t.rollback();
			
			//remove anything copied to the filestore
			File f = new File(newCa.getFileDataStoreLocation());
			if (f.exists()){
				try{
					FileUtils.forceDelete(f);
				}catch(Exception ex2){
					SmartPlugIn.displayLog( MessageFormat.format(Messages.ConservationAreaClonerEngine_ErrorCleanUpRequired, new Object[]{f.toString()}), ex2);
				}
			}
			throw ex;
		}finally{
			session.close();
			SmartHibernateManager.setUserName(SmartDB.DbUser.LOGIN.getUserName(), SmartDB.DbUser.LOGIN.getPassword());
			monitor.done();
		}	
	}
	
	
	
	/**
	 * Loads the cloner information from the extension registry.
	 * @return
	 * @throws CoreException
	 */
	private List<Cloner> getCloners() throws CoreException{
		List<Cloner> cloners = new ArrayList<Cloner>();
		IConfigurationElement[] elements = Platform.getExtensionRegistry().getConfigurationElementsFor(EXTENSION_ID);
		for (IConfigurationElement element : elements){
			String id = element.getAttribute("id"); //$NON-NLS-1$
			IConservationAreaTemplateCloner templateCloner = (IConservationAreaTemplateCloner) element.createExecutableExtension("class"); //$NON-NLS-1$
			ArrayList<String> requires = new ArrayList<String>();
			for (IConfigurationElement kid : element.getChildren("require")){ //$NON-NLS-1$
				requires.add(kid.getAttribute("id")); //$NON-NLS-1$
			}
			Cloner cloner = new Cloner(id, templateCloner, requires);
			cloners.add(cloner);
		}
		return cloners;
	}
	
	/*
	 * simple class for tracking cloner extension information
	 */
	private class Cloner{
		private String id;
		private IConservationAreaTemplateCloner cloner;
		private List<String> requirements;
		
		public Cloner(String id, IConservationAreaTemplateCloner cloner, List<String> requirements){
			this.id = id;
			this.cloner = cloner;
			this.requirements = requirements;
		}
		
	
	}
}

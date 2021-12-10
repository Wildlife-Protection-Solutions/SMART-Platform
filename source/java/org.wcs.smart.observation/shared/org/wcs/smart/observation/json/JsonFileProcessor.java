/*
 * Copyright (C) 2021 Wildlife Conservation Society
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
package org.wcs.smart.observation.json;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.Session;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.wcs.smart.SmartContext;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.observation.IObservationLabelProvider;

/**
 * Creates a new file processor.  One file should be processed at a time.
 * 
 * @author Emily
 *
 */
public class JsonFileProcessor {

	private static final String JSON_FEATURES = "features"; //$NON-NLS-1$
	private static final String GEOJSON_FC = "FeatureCollection"; //$NON-NLS-1$
	private static final String GEOJSON_FEATURE = "Feature"; //$NON-NLS-1$
	private static final String JSON_TYPE = "type"; //$NON-NLS-1$
	

	public enum Messages{
		INVALID_JSON,
		MISSING_TYPE,
		INVALID_TYPE,
		PROCESSOR_NOTFOUND,
		MISSING_PROPERTIES,
		MISSING_DATATYPE,
		MISSING_FEATURETYPE;
		
		public String getMessage(Locale l) {
			return SmartContext.INSTANCE.getClass(IObservationLabelProvider.class).getLabel(this, l);
		}
	}
	
	private Set<IJsonFeatureProcessor> processors;
	private ConservationArea ca;
	private Locale locale;
	
	/**
	 * Creates a new processor for processing data files.  Once clients are
	 * finished with this processor (and results are committed to the database)
	 * they must call dispose to release any temporary
	 * resources created while processing the data.
	 * 
	 * @param ca
	 * @param processors
	 * @return
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 * @throws NoSuchMethodException
	 * @throws SecurityException
	 */
	public static JsonFileProcessor create(ConservationArea ca, Set<Class<? extends IJsonFeatureProcessor>> processors, Locale locale) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		
		JsonFileProcessor p = new JsonFileProcessor(ca);
		p.locale = locale;
		for (Class<? extends IJsonFeatureProcessor> item : processors) {
			p.processors.add(item.getDeclaredConstructor().newInstance());
		}
		return p;
	}
	
	private JsonFileProcessor(ConservationArea ca) {
		this.ca = ca;
		this.processors = new HashSet<>();
	}
	
	public Collection<IJsonFeatureProcessor> getProcessors(){
		return this.processors;
	}
	
	/**
	 * Processes the JSON represented by the given file.
	 * 
	 * @param p
	 * @param session
	 * @throws Exception
	 */
	public void processData(Path p, Session session) throws Exception {
		try(InputStream is = Files.newInputStream(p)){
			processData(is, session);
		}
	}
	
	/**
	 * Processes the json represented by the input stream
	 * 
	 * @param is
	 * @param session
	 * @throws Exception
	 */
	public void processData(InputStream is, Session session) throws Exception {
		// parse file
		JSONParser json = new JSONParser();
		Object root = null;
		try (InputStreamReader reader = new InputStreamReader(is)) {
			root = json.parse(reader);
		}

		// validate GeoJSON
		if (!(root instanceof JSONObject)) {
			throw new Exception(Messages.INVALID_JSON.getMessage(locale));
		}
		JSONObject r = (JSONObject) root;
		if (!r.containsKey(JSON_TYPE)) {
			throw new Exception(Messages.MISSING_TYPE.getMessage(locale));
		}

		// process features one at a time
		String type = r.get(JSON_TYPE).toString();
		if (type.equals(GEOJSON_FEATURE)) {
			processFeature(r, session);
		} else if (type.equals(GEOJSON_FC)) {
			JSONArray all = (JSONArray) r.get(JSON_FEATURES);
			for (int i = 0; i < all.size(); i++) {
				processFeature((JSONObject) all.get(i), session);
			}
		} else {
			throw new Exception(
					MessageFormat.format(Messages.INVALID_TYPE.getMessage(locale),
							type, GEOJSON_FEATURE, GEOJSON_FC));
		}
	}
	
	/**
	 * disposes of any temporary resources created while processing dataset
	 */
	public void dispose() {
		processors.forEach(e->e.dispose());
	}
	
	/**
	 * 
	 * @return set of messages created during the load process
	 */
	public List<String> getMessages(){
		List<String> messages = processors.stream()
				.map(e->e.getMessage(locale))
				.filter(e->e != null).collect(Collectors.toList());
		return messages;
	}
	
	/**
	 * 
	 * @return set of warnings generated during the load process
	 */
	public List<String> getWarnings(){
		List<String> all = new ArrayList<>();
		processors.forEach(e->all.addAll(e.getWarnings()));
		return all;
	}
	
	/**
	 * processes and individual GeoJSON feature
	 * 
	 * @param feature
	 * @param session
	 * @throws Exception
	 */
	protected void processFeature(JSONObject feature, Session session) throws Exception{
		if (!feature.containsKey(IJsonFeatureProcessor.JSON_PROPERTIES)) 
			throw new Exception(Messages.MISSING_PROPERTIES.getMessage(locale));		

		JSONObject props = (JSONObject) feature.get(IJsonFeatureProcessor.JSON_PROPERTIES);
		if (!props.containsKey(IJsonFeatureProcessor.JSON_SMARTDATATYPE)) 
			throw new Exception(MessageFormat.format(Messages.MISSING_DATATYPE.getMessage(locale), IJsonFeatureProcessor.JSON_SMARTDATATYPE));		
		if (!props.containsKey(IJsonFeatureProcessor.JSON_SMARTFEATURETYPE))
			throw new Exception(MessageFormat.format(Messages.MISSING_FEATURETYPE.getMessage(locale), IJsonFeatureProcessor.JSON_SMARTFEATURETYPE));
		
		String smartDataType = props.get(IJsonFeatureProcessor.JSON_SMARTDATATYPE).toString();
		processFeature(smartDataType, feature, session);
	}
	
	protected void processFeature(String dataType, JSONObject feature, Session session) throws Exception{
		IJsonFeatureProcessor p = findProcessor(dataType);
		if (p == null)  throw new Exception(MessageFormat.format(Messages.PROCESSOR_NOTFOUND.getMessage(locale), dataType));
		p.processFeature(feature, ca, session, locale);
	}
	
	/**
	 * Find json processor for feature type or null if not found
	 * @param dataType
	 * @return
	 * @throws Exception
	 */
	protected IJsonFeatureProcessor findProcessor(String dataType) throws Exception {
		for (IJsonFeatureProcessor p : processors) {
			if (p.canProcess(dataType)) return p;
		}
		return null;
	}
	
}

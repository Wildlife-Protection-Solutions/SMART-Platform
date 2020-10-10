/*
 * Copyright (C) 2020 Wildlife Conservation Society
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
package org.wcs.smart.udig.legend.style;

import static org.locationtech.udig.style.advanced.utils.Utilities.getFormat;

import java.awt.Color;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.Collator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.NameImpl;
import org.geotools.styling.ExternalGraphic;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.Fill;
import org.geotools.styling.Graphic;
import org.geotools.styling.Mark;
import org.geotools.styling.PointSymbolizer;
import org.geotools.styling.Rule;
import org.geotools.styling.Stroke;
import org.geotools.styling.Style;
import org.geotools.styling.StyleBuilder;
import org.geotools.styling.Symbolizer;
import org.geotools.util.factory.GeoTools;
import org.hibernate.Session;
import org.locationtech.udig.ui.graphics.SLDs;
import org.locationtech.udig.ui.palette.ColourScheme;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.style.SemanticType;
import org.wcs.smart.ca.Label;
import org.wcs.smart.ca.NamedItem;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.CcaaDataModel;
import org.wcs.smart.ca.datamodel.CcaaDataModelDesktop;
import org.wcs.smart.ca.icon.Icon;
import org.wcs.smart.ca.icon.IconFile;
import org.wcs.smart.ca.icon.IconSet;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.internal.Messages;

/**
 * Generates styles based on a feature attribute matching
 * a data model element using the icons supplied with the
 * data model element where possible.
 * 
 * @author Emily
 *
 */
public class DataModelStyleGenerator {

	private static StyleBuilder styleBuilder = new StyleBuilder(CommonFactoryFinder.getStyleFactory(GeoTools.getDefaultHints()));
	private static FilterFactory2 filterFactory = CommonFactoryFinder.getFilterFactory2();

	private SimpleFeatureSource fsource;
	private IconSet iSet;
	private boolean includeAll;
	private int iconSize;
	
	private ColourScheme colorPalette;
	private int colorIndex = 0;
	
	private CcaaDataModel ccaaModel;
	
	/**
	 * 
	 * @param fsource the feature source
	 * @param iset the icon set to use
	 * @param includeAll true if all data model elements are to be included in the
	 * style, false to only include items in the dataset 
	 * @param iconSize the icon size
	 * @param colorscheme the color scheme to use
	 */
	public DataModelStyleGenerator(SimpleFeatureSource fsource, IconSet iset, 
			boolean includeAll, int iconSize,
			ColourScheme colorscheme) {
		this.iSet = iset;
		this.includeAll = includeAll;
		this.iconSize = iconSize;
		this.fsource = fsource;
		
		colorPalette = colorscheme;
		ccaaModel = CcaaDataModelDesktop.getInstance();
	}
	
	public Style generateThemesCategory(AttributeDescriptor fattribute, int level, Session session) throws IOException {
//get all values for a given category
		List<Category> categories = null;
		if (SmartDB.isMultipleAnalysis()) {
			categories = ccaaModel.getCategories(session, level);
		}else {
			String query = "FROM Category WHERE conservationArea = :ca and smart.hkeyLength(hkey) = :length"; //$NON-NLS-1$
			categories = session.createQuery(query, Category.class)
				.setParameter("ca", SmartDB.getCurrentConservationArea()) //$NON-NLS-1$
				.setParameter("length", level) //$NON-NLS-1$
				.list();
		}
		
		Set<String> values = getAttributeValues(fsource, fattribute);
		boolean hasnone = values.contains(null);
		values.remove(null);
		
		HashMap<String, Category> map = new HashMap<>();
		for (String value : values) {
			for (Category category : categories) {
				if (category.getName().equalsIgnoreCase(value)) {
					map.put(value,  category);
					break;
				}
			}
		}
		
		List<String> sortedValues = new ArrayList<>(values);
		
		if (includeAll) {
			for (Category category : categories) {
				if (!map.values().contains(category)) {
					sortedValues.add(category.getName());
					map.put(category.getName(), category);
				}
			}
		}
		
		sortedValues.sort((a,b)->Collator.getInstance().compare(a, b));
		
		List<Rule> rules = new ArrayList<>();
		
		for (String s : sortedValues) {
			Category category  = map.get(s);
			
			PointSymbolizer ps = null;
			if (category == null || category.getIcon() == null) {
				ps = buildPointSymbol();
			}else {
				ps = buildPointSymbol(category.getIcon(), session);
			}
			
			Filter filterExp;
			if (category == null) {					
				filterExp = filterFactory.equals(filterFactory.property(fattribute.getName()), filterFactory.literal(s));
			}else {
				filterExp = generateFilter(fattribute, category);
			}
			Rule r = createRule(s, ps, filterExp);
			rules.add(r);
		}
		
		if (hasnone) {
			Rule r = createRule(Messages.DataModelStyleGenerator_NoValueLabel, buildNoDataPointSymbol(), createEmptyFilter(fattribute));
			rules.add(r);
		}
		return createStyle(rules);
	}
	
	public Style generateThemesAttribute(AttributeDescriptor fattribute, Attribute dmAttribute, Session session) throws IOException {
		if(dmAttribute.getType() == Attribute.AttributeType.LIST) {
			return generateThemesAttributeList(fsource, fattribute, dmAttribute, session);
		}else if (dmAttribute.getType() == Attribute.AttributeType.TREE) {
			return generateThemesAttributeTree(fsource, fattribute, dmAttribute, session);
		}
		return null;
	}
	
	private Style generateThemesAttributeList(SimpleFeatureSource fsource, AttributeDescriptor fattribute, Attribute dmAttribute, Session session) throws IOException {
		List<AttributeListItem> items = new ArrayList<>();
		if (SmartDB.isMultipleAnalysis()) {
			items = ccaaModel.getAttributeListItems(dmAttribute, session);
		}else {
			dmAttribute = session.get(Attribute.class, dmAttribute.getUuid());
			items = dmAttribute.getAttributeList();
		}
		Set<String> values = getAttributeValues(fsource, fattribute);
		
		boolean hasnone = values.contains(null);
		values.remove(null);
		
		HashMap<String, AttributeListItem> map = new HashMap<>();
		for (String value : values) {
			for (AttributeListItem ali : items) {
				if (ali.getName().equalsIgnoreCase(value)) {
					map.put(value,  ali);
					break;
				}
			}
		}
		
		List<String> sortedValues = new ArrayList<>(values);
		if (includeAll) {
			for (AttributeListItem ali : items) {
				if (!map.values().contains(ali)) {
					sortedValues.add(ali.getName());
					map.put(ali.getName(), ali);
				}
			}
		}
		
		sortedValues.sort((a,b)->Collator.getInstance().compare(a, b));
		
		List<Rule> rules = new ArrayList<>();
		
		for (String s : sortedValues) {
			AttributeListItem li  = map.get(s);
			
			PointSymbolizer ps = null;
			if (li == null || li.getIcon() == null) {
				ps = buildPointSymbol();	
			}else {
				ps = buildPointSymbol(li.getIcon(), session);
			}

			Filter filterExp;
			if (li == null) {					
				filterExp = filterFactory.equals(filterFactory.property(fattribute.getName()), filterFactory.literal(s));
			}else {
				filterExp = generateFilter(fattribute, li);
			}
			Rule r = createRule(s, ps, filterExp);
			rules.add(r);
		}
			
		if (hasnone) {
			Rule r = createRule(Messages.DataModelStyleGenerator_NoValueLabel, buildNoDataPointSymbol(), createEmptyFilter(fattribute));
			rules.add(r);
		}
			
		return createStyle(rules);		
	}
	
	private Style generateThemesAttributeTree(SimpleFeatureSource fsource, AttributeDescriptor fattribute, Attribute dmAttribute, Session session) throws IOException {
		List<AttributeTreeNode> toVisit = new ArrayList<>();
		if (SmartDB.isMultipleAnalysis()) {
			toVisit.addAll(ccaaModel.getAttributeTreeNodes(dmAttribute, session));
		}else {
			dmAttribute = session.get(Attribute.class, dmAttribute.getUuid());
			toVisit.addAll(dmAttribute.getActiveTreeNodes());
		}
		
		Set<String> values = getAttributeValues(fsource, fattribute);
		boolean hasnone = values.contains(null);
		values.remove(null);
		
		HashMap<String, AttributeTreeNode> map = new HashMap<>();
		
		
		while(!toVisit.isEmpty()) {
			AttributeTreeNode n = toVisit.remove(0);
			if (n.getActiveChildren() != null) toVisit.addAll(n.getActiveChildren());
			
			for(String s : values) {
				if (s.equalsIgnoreCase(n.getName())) {
					map.put(s, n);
					break;
				}
			}
		}
		
		List<String> sortedValues = new ArrayList<>(values);
		if (includeAll) {
			if (SmartDB.isMultipleAnalysis()) {
				toVisit.addAll(ccaaModel.getAttributeTreeNodes(dmAttribute, session));	
			}else {
				toVisit.addAll(dmAttribute.getActiveTreeNodes());
			}
			
			while(!toVisit.isEmpty()) {
				AttributeTreeNode n = toVisit.remove(0);
				if (n.getActiveChildren() != null) toVisit.addAll(n.getActiveChildren());
				
				if (!map.values().contains(n)) {
					sortedValues.add(n.getName());
					map.put(n.getName(), n);
				}
			}
		}
		
		sortedValues.sort((a,b)->Collator.getInstance().compare(a, b));
	
		List<Rule> rules = new ArrayList<>();
		
		for (String s : sortedValues) {
			AttributeTreeNode node = map.get(s);
			
			PointSymbolizer ps = null;
			if (node == null || node.getIcon() == null) {
				ps = buildPointSymbol();	
			}else {
				ps = buildPointSymbol(node.getIcon(), session);
			}
			
			Filter filterExp;
			if (node == null) {					
				filterExp = filterFactory.equals(filterFactory.property(fattribute.getName()), filterFactory.literal(s));
			}else {
				filterExp = generateFilter(fattribute, node);
			}
			Rule r = createRule(s, ps, filterExp);
			rules.add(r);
		}
			
		if (hasnone) {
			Rule r = createRule(Messages.DataModelStyleGenerator_NoValueLabel, buildNoDataPointSymbol(), createEmptyFilter(fattribute));
			rules.add(r);
		}
			
		return createStyle(rules);
	}
	
	private Style createStyle(List<Rule> rules) {
		FeatureTypeStyle fts = styleBuilder.createFeatureTypeStyle(fsource.getSchema().getTypeName(), rules.toArray(new Rule[rules.size()]));
		Style style = styleBuilder.createStyle();
		
		fts.featureTypeNames().clear();
        fts.featureTypeNames().add(new NameImpl(SLDs.GENERIC_FEATURE_TYPENAME));
		
        fts.semanticTypeIdentifiers().clear();
        fts.semanticTypeIdentifiers().add(new SemanticType("generic:geometry")); //$NON-NLS-1$
        fts.semanticTypeIdentifiers().add(new SemanticType("simple")); //$NON-NLS-1$
		
		style.featureTypeStyles().add(fts);
		return style;
	}
	
	/*
	 * create a rule
	 */
	private Rule createRule(String title, Symbolizer sym, Filter filter) {
		Rule r = styleBuilder.createRule(sym);
		r.setName(generateRuleName(title));
		r.getDescription().setTitle(title);
		r.setFilter(filter);
		return r;
	}
	
	/*
	 * convert a display title into a rule name
	 */
	private String generateRuleName(String part) {
		String rname = part.replaceAll("[^\\p{L}\\p{N}0-9]", ""); //$NON-NLS-1$ //$NON-NLS-2$
		if (rname.isEmpty()) {
			rname = "rule"; //$NON-NLS-1$
		}
		return rname;
	}

	/*
	 * Get all attribute values from the feature source
	 */
	private Set<String> getAttributeValues(SimpleFeatureSource fsource, AttributeDescriptor fattribute) throws IOException{
		Set<String> values = new HashSet<>();
		try(SimpleFeatureIterator fi = fsource.getFeatures().features()){
			while(fi.hasNext()) {
				SimpleFeature sf = fi.next();
				Object x = sf.getAttribute(fattribute.getName());
				if (x != null && !x.toString().isBlank()) {
					values.add(x.toString());
				}else {
					values.add(null);
				}
			}
		}
		
		return values;
	}
	
	/*
	 * Build a point symbolizer from the icon
	 */ 
	private PointSymbolizer buildPointSymbol(Icon icon, Session session) throws MalformedURLException {
		icon = session.get(Icon.class, icon.getUuid());
		
		IconFile selectedFile = icon.getIconFile(iSet);
		String fname = selectedFile.getFilename();

		try {
			selectedFile.computeFileLocation(session);
		}catch (Exception ex) {}
		if (selectedFile.getUuid() != null && !selectedFile.isSystemIcon()) {
			fname = "file:" + selectedFile.getAttachmentFile().toString(); //$NON-NLS-1$
		}else if (selectedFile.getUuid() == null && !selectedFile.isSystemIcon()) {
			fname = "file:" + fname; //$NON-NLS-1$
		}
		
		PointSymbolizer ps = styleBuilder.createPointSymbolizer();
		
		URL url = new URL(fname);
		ExternalGraphic graphic = styleBuilder.createExternalGraphic(url, getFormat(url.toString()));
		ps.setGraphic( styleBuilder.createGraphic(graphic, null, null) );
		ps.getGraphic().setSize(styleBuilder.literalExpression(iconSize));
		
		return ps;
		
	}
	
	/*
	 * Build a circle point symbolizer
	 */
	private PointSymbolizer buildPointSymbol() {
		
		Color c = colorPalette.getColour(colorIndex);
		colorIndex++;
		if (colorIndex >= colorPalette.getSizePalette()) colorIndex = 0;
		
		
		Stroke starstroke = styleBuilder.createStroke(c.darker(), 1);
		Fill starfill = styleBuilder.createFill(c);
		Mark starmark = styleBuilder.createMark(styleBuilder.literalExpression("circle"), starfill, starstroke); //$NON-NLS-1$
		Graphic starg = styleBuilder.createGraphic(null,  starmark,  null);
		starg.setSize(styleBuilder.literalExpression(8));
        
		return styleBuilder.createPointSymbolizer(starg);
	}
	
	/*
	 * Build a circle point symbolizer
	 */
	private PointSymbolizer buildNoDataPointSymbol() {
		
		Color fill = new Color(200, 200, 200);
		Color outline = new Color(150, 150, 150);
		
		Stroke starstroke = styleBuilder.createStroke(outline, 1);
		Fill starfill = styleBuilder.createFill(fill);
		Mark starmark = styleBuilder.createMark(styleBuilder.literalExpression("circle"), starfill, starstroke); //$NON-NLS-1$
		Graphic starg = styleBuilder.createGraphic(null,  starmark,  null);
		starg.setSize(styleBuilder.literalExpression(8));
        
		return styleBuilder.createPointSymbolizer(starg);
	}
	
	/*
	 * Creates a filter that matches all labels associated
	 * with the named item
	 */
	private Filter generateFilter(AttributeDescriptor fattribute, NamedItem item) {
		List<Filter> items = new ArrayList<>();
		
		items.add( filterFactory.equals(
				filterFactory.property(fattribute.getName()), 
				filterFactory.literal(item.getName())) );
		
		for (Label l : item.getNames()) {
			if (l.getValue().equals(item.getName())) continue;
			items.add( filterFactory.equals(
					filterFactory.property(fattribute.getName()), 
					filterFactory.literal(l.getValue())) );
		}
		
		if (items.size() == 1) return items.get(0);
		
		return filterFactory.or(items);
	}
	
	private Filter createEmptyFilter(AttributeDescriptor fattribute) {
		Filter f1 = filterFactory.isNull(filterFactory.property(fattribute.getName()));
		Filter f2 = filterFactory.equals(filterFactory.property(fattribute.getName()), filterFactory.literal("")); //$NON-NLS-1$
		return filterFactory.or(f1, f2);

	}
}

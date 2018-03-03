package org.wcs.smart.i2.model;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.wcs.smart.i2.query.observation.filter.ParsedObservationQuery;
import org.wcs.smart.i2.query.observation.filter.SumQueryDefinition;
import org.wcs.smart.i2.query.observation.filter.IQueryFilter.FilterType;
import org.wcs.smart.i2.query.observation.parser.ParseException;
import org.wcs.smart.i2.query.observation.parser.Parser;

@Entity
@Table(name="smart.i_entity_summary_query")
public class IntelEntitySummaryQuery extends AbstractIntelQuery {
	
	public static final String KEY = "I2_ENTITY_SUMM_QUERY"; //$NON-NLS-1$

	@Override
	@Transient
	public String getKeyId() {
		return KEY;
	}
	
	
	@Transient
	public static SumQueryDefinition parseQuery(String queryString) throws Exception{
		System.out.println(queryString);
		if (queryString.isEmpty()) throw new ParseException("At least one value must be supplied.");
		try(InputStream is = new ByteArrayInputStream(queryString.getBytes())){
			Parser parser = new Parser(is);
			return parser.ParseSummary();
		}catch (Throwable ex) {
			ex.printStackTrace();
			throw new Exception(ex);
		}
	}
}

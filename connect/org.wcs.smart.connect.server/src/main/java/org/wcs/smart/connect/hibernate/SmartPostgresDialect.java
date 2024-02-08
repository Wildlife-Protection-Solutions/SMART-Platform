/*
 * Copyright (C) 2015 Wildlife Conservation Society
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
package org.wcs.smart.connect.hibernate;

import java.sql.Types;

import org.hibernate.boot.model.TypeContributions;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.descriptor.jdbc.BinaryJdbcType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;

/**
 * Extension of the postgreSQL dialog to change the way BLOB types are
 * registered to work with the @Lob annotation requirements for apache derby
 * byte[] fields.
 * 
 * @author Emily
 *
 */
public class SmartPostgresDialect extends PostgreSQLDialect {

	public SmartPostgresDialect() {
		super(DatabaseVersion.make(14));
	}

    /*
     * We use @Lob annotation for byte[] to make apache derby happy (apache derby doesn't have
     * bytea type only blob).  So
     * we need to map this to the bytea type in postgresql, otherwise it is mapped
     * to the oid type which doesn't work for us.
    */

	@Override
	public void contributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.contributeTypes(typeContributions, serviceRegistry);
		final JdbcTypeRegistry jdbcTypeRegistry = typeContributions.getTypeConfiguration().getJdbcTypeRegistry();
		jdbcTypeRegistry.addDescriptor(Types.BLOB, BinaryJdbcType.INSTANCE);

	}

	@Override
	protected String columnType(int sqlTypeCode) {
		if (sqlTypeCode == java.sql.Types.BLOB)
			return "bytea"; //$NON-NLS-1$
		return super.columnType(sqlTypeCode);
	}

	@Override
	protected String castType(int sqlTypeCode) {
		if (sqlTypeCode == java.sql.Types.BLOB)
			return "bytea"; //$NON-NLS-1$
		return super.castType(sqlTypeCode);
	}

}

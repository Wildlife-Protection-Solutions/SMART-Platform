package org.wcs.smart.connect.model;

import java.lang.reflect.InvocationTargetException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
import org.wcs.smart.connect.model.Alert.AlertStatusEnum;
 
/**
 * 
 * @author lb@octagen.at
 *
 */
public class AlertStatusType extends GenericEnumType<String, AlertStatusEnum> {
 
	public AlertStatusType() throws NoSuchMethodException,
			InvocationTargetException, IllegalAccessException {
		super(AlertStatusEnum.class, AlertStatusEnum.values(), "getValue", Types.OTHER);
	}
 
	@Override
	public Object nullSafeGet(ResultSet rs, String[] names,
			SessionImplementor session, Object owner)
			throws HibernateException, SQLException {
		return nullSafeGet(rs, names, owner);
	}
 
	@Override
	public void nullSafeSet(PreparedStatement st, Object value, int index,
			SessionImplementor session) throws HibernateException, SQLException {
		nullSafeSet(st, value, index);
	}
 
}
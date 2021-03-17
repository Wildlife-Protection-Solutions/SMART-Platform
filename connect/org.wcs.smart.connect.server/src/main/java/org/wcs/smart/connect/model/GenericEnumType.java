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
package org.wcs.smart.connect.model;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

import org.hibernate.HibernateException;
import org.hibernate.usertype.UserType;


public abstract class GenericEnumType<T,E extends Enum<E>> implements UserType {
 
       private int sqlType;
       private Class<E> clazz = null;
       private HashMap<String, E> enumMap;
       private HashMap<E, String> valueMap;
 
       public GenericEnumType(Class<E> clazz,
                E[] enumValues, String method, int sqlType) throws
            NoSuchMethodException, InvocationTargetException,
            IllegalAccessException
       {
            this.clazz = clazz;
            enumMap = new HashMap<String, E>(enumValues.length);
            valueMap = new HashMap<E, String>(enumValues.length);
            Method m = clazz.getMethod(method);
 
            for (E e: enumValues) {
             
                @SuppressWarnings("unchecked")
                T value = (T)m.invoke(e);
                 
                enumMap.put(value.toString(), e);
                valueMap.put(e, value.toString());
           }
           this.sqlType = sqlType;
       }
 
       public Object assemble(Serializable cached, Object owner)
       throws HibernateException {
             return cached;
       }
 
       public Object deepCopy(Object obj) throws
       HibernateException
       {
             return obj;
       }
 
       public Serializable disassemble(Object obj) throws
       HibernateException
       {
            return (Serializable)obj;
       }
 
       public boolean equals(Object obj1, Object obj2) throws
       HibernateException
       {
             if (obj1 == obj2) {
                   return true;
             }
 
             if (obj1 == null || obj2 == null) {
                   return false;
             }
             return obj1.equals(obj2);
       }
 
       public int hashCode(Object obj) throws HibernateException
       {
             return obj.hashCode();
       }
 
       public boolean isMutable()
       {
             return false;
       }
 
       public Object nullSafeGet(ResultSet rs, String[] names, Object owner)
       throws HibernateException, SQLException
       {
             String value = rs.getString(names[0]);
             if (!rs.wasNull()) {
                   return enumMap.get(value);
             }
             return null;
       }
 
       public void nullSafeSet(PreparedStatement ps, Object obj, int index)
       throws HibernateException, SQLException
       {
               if (obj == null) {
                     ps.setNull(index, sqlType);
               } else {
                     ps.setObject(index, valueMap.get(obj), sqlType);
               }
       }
 
       public Object replace(Object original, Object target, Object owner)
       throws HibernateException
       {
               return original;
       }
 
       public Class<E> returnedClass() {
               return clazz;
       }
 
       public int[] sqlTypes()
       {
               return new int[] {sqlType};
       }
}
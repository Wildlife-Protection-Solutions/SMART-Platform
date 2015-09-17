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
package org.wcs.smart.query.common.engine;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.wcs.smart.util.UuidUtils;

/**
 * <p>This class allows for the use of named parameters in sql queries.</p>
 * <p>
 * It was adapted from code found here:
 * http://www.javaworld.com/article/2077706/core-java/named-parameters-for-preparedstatement.html
 * </p>
 * 
 * @author Emily
 *
 */
public class NamedPreparedStatement {
    
	/** The statement this object is wrapping. */
    protected final PreparedStatement statement;

    /** Maps parameter names to arrays of ints which are the parameter indices. */
    private final Map<String, List<Integer>> indexMap;


    /**
     * Creates a NamedParameterStatement.  Wraps a call to
     * c.{@link Connection#prepareStatement(java.lang.String) prepareStatement}.
     * @param connection the database connection
     * @param query      the parameterized query
     * @throws SQLException if the statement could not be created
     */
    public NamedPreparedStatement(Connection connection, String query) throws SQLException {
        indexMap=new HashMap<String, List<Integer>>();
        String parsedQuery=parse(query);
        statement=connection.prepareStatement(parsedQuery);
    }


    /**
     * Parses a query with named parameters.  The parameter-index mappings are 
     * put into the map, and the parsed query is returned.  
     * This  method is non-private so JUnit code can test it.
     * @param query    query to parse
     * @param paramMap map to hold parameter-index mappings
     * @return the parsed query
     */
    public final String parse(String query) {
        // I was originally using regular expressions, but they didn't work well for ignoring
        // parameter-like strings inside quotes.
        int length=query.length();
        StringBuffer parsedQuery=new StringBuffer(length);
        boolean inSingleQuote=false;
        boolean inDoubleQuote=false;
        int index=1;

        for(int i=0;i<length;i++) {
            char c=query.charAt(i);
            if(inSingleQuote) {
                if(c=='\'') {
                    inSingleQuote=false;
                }
            } else if(inDoubleQuote) {
                if(c=='"') {
                    inDoubleQuote=false;
                }
            } else {
                if(c=='\'') {
                    inSingleQuote=true;
                } else if(c=='"') {
                    inDoubleQuote=true;
                } else if(c==':' && i+1<length &&
                        Character.isJavaIdentifierStart(query.charAt(i+1))) {
                    int j=i+2;
                    while(j<length && Character.isJavaIdentifierPart(query.charAt(j))) {
                        j++;
                    }
                    String name=query.substring(i+1,j);
                    c='?'; // replace the parameter with a question mark
                    i+=name.length(); // skip past the end if the parameter

                    List<Integer> indexList= indexMap.get(":" + name); //$NON-NLS-1$
                    if(indexList==null) {
                        indexList=new ArrayList<Integer>();
                        indexMap.put(":" + name, indexList); //$NON-NLS-1$
                    }
                    indexList.add(new Integer(index));

                    index++;
                }
            }
            parsedQuery.append(c);
        }
        return parsedQuery.toString();
    }


    /**
     * Returns the indexes for a parameter.
     * @param name parameter name including the : at the beginning of the name
     * @return parameter indexes
     * @throws IllegalArgumentException if the parameter does not exist
     */
    protected Collection<Integer> getIndexes(String name) {
    	List<Integer> indexes = indexMap.get(name);
        if(indexes==null) {
            throw new IllegalArgumentException("Parameter not found: " + name); //$NON-NLS-1$
        }
        return indexes;
    }


    /**
     * Sets a parameter.
     * @param name  parameter name
     * @param value parameter value
     * @throws SQLException if an error occurred
     * @throws IllegalArgumentException if the parameter does not exist
     * @see PreparedStatement#setObject(int, java.lang.Object)
     */
    public void setObject(String name, Object value) throws SQLException {
        for(int i: getIndexes(name)) {
        	if (value instanceof UUID){
        		statement.setBytes(i, UuidUtils.uuidToByte((UUID)value));
        	}else{
        		statement.setObject(i, value);
        	}
        }
    }


//    public void setBytes(String name, byte[] value) throws SQLException{
//    	for (int i : getIndexes(name)){
//    		statement.setBytes(i, value);
//    	}
//    }
   
    /**
     * Returns the underlying statement.
     * @return the statement
     */
    public PreparedStatement getStatement() {
        return statement;
    }


    /**
     * Executes the statement.
     * @return true if the first result is a {@link ResultSet}
     * @throws SQLException if an error occurred
     * @see PreparedStatement#execute()
     */
    public boolean execute() throws SQLException {
        return statement.execute();
    }


    /**
     * Executes the statement, which must be a query.
     * @return the query results
     * @throws SQLException if an error occurred
     * @see PreparedStatement#executeQuery()
     */
    public ResultSet executeQuery() throws SQLException {
        return statement.executeQuery();
    }


    /**
     * Executes the statement, which must be an SQL INSERT, UPDATE or DELETE 
statement;
     * or an SQL statement that returns nothing, such as a DDL statement.
     * @return number of rows affected
     * @throws SQLException if an error occurred
     * @see PreparedStatement#executeUpdate()
     */
    public int executeUpdate() throws SQLException {
        return statement.executeUpdate();
    }


    /**
     * Closes the statement.
     * @throws SQLException if an error occurred
     * @see Statement#close()
     */
    public void close() throws SQLException {
        statement.close();
    }

}

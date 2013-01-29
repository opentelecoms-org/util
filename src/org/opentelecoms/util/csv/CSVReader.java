/*
 *  Utility class for reading CSV files
 *  
 *  Copyright 2012 Daniel Pocock <daniel@pocock.com.au>
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.opentelecoms.util.csv;

import java.io.*;
import java.lang.reflect.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Logger;

/**
 * Utility class for reading CSV data from input streams and dynamically
 * creating an object instance for each row.
 */
public class CSVReader {
	
	public static final String DEFAULT_DATE_FORMAT = "dd/MM/yyyy";
	
	Logger logger = Logger.getLogger(getClass().getCanonicalName());

  Class targetClass;
  Class[] pList;
  Constructor[] pConstructor;
  Constructor constructor;
  DateFormat dateFormat;

  /**
   * Instantiates a CSVReader.  Each CSV record will instantiate an
   * instance of targetClass, using the targetClass constructor that
   * is specified by paramList.
   *
   * There must be as many fields in each CSV record as there are
   * fields in paramList.  
   *
   * @param targetClass the class to instantiate for each row.
   * @param paramList the specification for the constructor  to use when
   *                  instantiating targetClass
   */
  public CSVReader(Class targetClass, Class[] paramList, String dateFormat) throws Exception {
    this.targetClass = targetClass;
    this.pList = paramList;
    pConstructor = new Constructor[pList.length];
    for(int i = 0; i < pList.length; i++)
    	pConstructor[i] = pList[i].getConstructor(String.class); 
    constructor = targetClass.getConstructor(paramList);
    this.dateFormat = new SimpleDateFormat(dateFormat);
  }

  public CSVReader(Class targetClass, Class[] paramList) throws Exception {
	  this(targetClass, paramList, DEFAULT_DATE_FORMAT);
  }

  /**
   * Read a single record from in, and return an instance of the
   * targetClass for this CSVReader.
   *
   * @param in a BufferedReader that is reading CSV format text
   */
  public Object read(BufferedReader in) throws Exception {
	  int i = 0;
    try {
      String inputLine = in.readLine();
      if(inputLine == null)
        return null;
      // System.err.println("Read: " + inputLine);
      Object[] args = new Object[pList.length];
      StreamTokenizer st = new StreamTokenizer(new StringReader(inputLine));
      st.resetSyntax();
      st.wordChars(0, 255);
      st.ordinaryChar(',');
      st.quoteChar('"');
      args[0] = getDefaultValue(pList[i]);
      while(i < pList.length) {
        st.nextToken();
        if(st.ttype == StreamTokenizer.TT_WORD || st.ttype == '"') {
	  //System.err.println("Token: " + st.sval);
          if(pList[i] == String.class) {
            if(args[i] != null) {
              args[i] = args[i] + st.sval;
            } else {
              args[i] = st.sval;
            }
          } else if(pList[i] == java.lang.Integer.TYPE) {
            args[i] = Integer.valueOf(st.sval);
          } else if(pList[i] == java.lang.Long.TYPE) {
            args[i] = Long.valueOf(st.sval);
          } else if(pList[i] == java.util.Date.class) {
	    args[i] = dateFormat.parse(st.sval);
	  } else if(pList[i] == java.math.BigDecimal.class) {
	    args[i] = new BigDecimal(st.sval);
          } else {
            args[i] = pConstructor[i].newInstance(st.sval);
          }
	} else if(st.ttype == ',') {
          i++;
          if(i < pList.length)
	    args[i] = getDefaultValue(pList[i]);
        } else if(st.ttype == StreamTokenizer.TT_EOF) {
          if(i == (pList.length - 1))
            i++;
          else
            return null;
	} else {
	  logger.severe("Column: " + i + ", unknown type: " + st.ttype);
	}
      }
      return constructor.newInstance(args);
    } catch (Exception e) {
    	logger.severe("exception while parsing column " + (i+1) + ":" + e.getMessage());
      throw e;
    }
  }

  Object getDefaultValue(Class c) {
    if(c == String.class)
      return "";
    else if(c == Integer.TYPE) 
      return new Integer("0");
    else if(c == Long.TYPE)
      return new Long("0");

    return null;
  }

  /**
   * Read all lines from in, and return a Vector containing instances
   * of targetClass, one instance per CSV record.
   */
  public List readAll(BufferedReader in) throws Exception {
	  int row = 1;
    ArrayList r = new ArrayList();
    try {
      Object o = read(in);
      while(o != null) {
	r.add(o);
	row++;
	o = read(in);
      }
    } catch (Exception e) {
    	logger.severe("exception while reading row " + row + ":" + e.getMessage());
      throw e;
    }
    return r;
  }

}



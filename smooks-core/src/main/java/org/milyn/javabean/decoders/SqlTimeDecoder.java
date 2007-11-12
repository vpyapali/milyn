/*
	Milyn - Copyright (C) 2006

	This library is free software; you can redistribute it and/or
	modify it under the terms of the GNU Lesser General Public
	License (version 2.1) as published by the Free Software
	Foundation.

	This library is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.

	See the GNU Lesser General Public License for more details:
	http://www.gnu.org/licenses/lgpl.txt
*/
package org.milyn.javabean.decoders;

import org.milyn.javabean.DataDecoder;
import org.milyn.javabean.DataDecodeException;
import org.milyn.javabean.DecodeType;
import org.milyn.cdr.SmooksResourceConfiguration;
import org.milyn.cdr.SmooksConfigurationException;

import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.Date;
import java.sql.Time;

/**
 * {@link java.sql.Time} data decoder.
 * <p/>
 * Extends {@link org.milyn.javabean.decoders.DateDecoder} and returns
 * a java.sql.Time instance.
 * <p/>
 *
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
@DecodeType(Time.class)
public class SqlTimeDecoder extends DateDecoder {

    public static final String FORMAT_CONFIG_KEY = "format";
    public static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";

    private String format = DEFAULT_DATE_FORMAT;
    private SimpleDateFormat decoder = new SimpleDateFormat(DEFAULT_DATE_FORMAT);

    public Object decode(String data) throws DataDecodeException {
    	Date date = (Date)super.decode(data);
        return new Time(date.getTime());
    }
}
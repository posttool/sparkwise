/*******************************************************************************
 * Copyright 2012 Tomorrow Partners LLC
 * 
 * This file is part of SparkwiseServer.
 * 
 * SparkwiseServer is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 * 
 * Sparkwise is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with SparkwiseServer.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package se.sparkwi.web.util;

import java.util.ArrayList;
import java.util.List;

import se.sparkwi.web.module.DashboardModule;

import com.pagesociety.persistence.Entity;
import com.pagesociety.util.OBJECT;
import com.pagesociety.web.exception.WebApplicationException;

public class CommonUtil
{

	/*******************************
	 *
	 *  UTILITY
	 *
	 */

	public static boolean empty(String name)
	{
		if (name == null)
			return true;
		if (name.trim().length()==0)
			return true;
		return false;
	}

	public static boolean empty(List<?> c)
	{
		if (c == null)
			return true;
		if (c.isEmpty())
			return true;
		return false;
	}

	public static List<Integer> to_list(int[] a)
	{
		List<Integer> list = new ArrayList<Integer>(a.length);
		for (int n : a)
		  list.add(n);
		return list;
	}

	public static void set_dict_attr(Entity e, String field_name, String dict_key, String val) throws WebApplicationException
	{
		String s = (String) e.getAttribute(field_name);
		OBJECT o = null;
		if(s == null)
			o = new OBJECT();
		else
			o = OBJECT.decode(s);
		o.put(dict_key, val);
		e.setAttribute(field_name, OBJECT.encode(o));
	}
	
	public static void remove_dict_attr(Entity e, String field_name, String dict_key) throws WebApplicationException
	{
		String s = (String) e.getAttribute(field_name);
		OBJECT o = null;
		if(s == null)
			o = new OBJECT();
		else
			o = OBJECT.decode(s);
		o.remove(dict_key);
	}

	public static Object get_dict_attr(Entity e, String field_name, String dict_key) throws WebApplicationException
	{
		String s = (String) e.getAttribute(field_name);
		if(s == null)
			return null;
		OBJECT o = OBJECT.decode(s);
		return o.get(dict_key);
	}


	public static boolean is_correlation(Entity wi)
	{
		int type = (Integer)wi.getAttribute(DashboardModule.WIDGETINSTANCE_FIELD_TYPE);
		return type == DashboardModule.WIDGETINSTANCE_TYPE_CORRELATION;
	}

}

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
package se.sparkwi.web.widget;

import java.util.Date;

import se.sparkwi.web.module.WidgetDefinition;
import se.sparkwi.web.module.auth.ConnectionApi;
import se.sparkwi.web.util.DateUtil;

import com.pagesociety.util.ARRAY;
import com.pagesociety.util.OBJECT;
import com.pagesociety.web.exception.WebApplicationException;

public class Facebook_GetUserAgeGroup extends WidgetDefinition
{

	@Override
	public String get_required_connection()
	{
		return ConnectionApi.CONNECTION_TYPE_FACEBOOK;
	}

	@Override
	protected OBJECT[] get_service_selector_info()
	{
		return new OBJECT[] { new OBJECT(SERVICE_SELECTOR_NAME, "fan page", SERVICE_SELECTOR_TYPE, SELECTOR_TYPE_SELECT) };
	}

	@Override
	protected int[] get_return_types()
	{
		return new int[] { RETURN_TYPE_DICT };
	}

	@Override
	protected OBJECT get_selector_data(ConnectionApi api, OBJECT selector_values, String name) throws WebApplicationException
	{
		OBJECT response = api.call("/me/accounts");
		ARRAY page_list = response.A("data");
		ARRAY page_names = new ARRAY();
		ARRAY page_ids = new ARRAY();

		for (int i = 0; i < page_list.size(); i++)
		{
			OBJECT p = (OBJECT) page_list.get(i);
			if (p.containsKey("name") && p.containsKey("category") && !p.S("category").equals("Application"))
			{
				page_names.add(p.S("name"));
				page_ids.add(p.S("id"));
			}
		}
		return new OBJECT("selector_name", name, "display_values", page_names, "values", page_ids);
	}

	@Override
	protected OBJECT get_data(ConnectionApi api, OBJECT selector_values, OBJECT props) throws WebApplicationException
	{
		OBJECT response = api.call("/" + selector_values.get("fan page") + "/insights/page_fans_gender_age/lifetime");
		OBJECT latest_value = FacebookUtils.get_valid_fb_insights_value(response, "F.18-24");
		String date_s = latest_value.S("end_time");
		Date data_date = DateUtil.parseOrNull(date_s);
		OBJECT val = new OBJECT("65+",0,"55-64",0,"45-54",0,"35-44",0,"25-34",0,"18-24",0,"13-17",0);
		try
		{
			OBJECT fbval = latest_value.O("value");
			for (String s : fbval.keySet())
			{
				String k = s.substring(2);
				int c = val.I(k);
				c += (Integer)fbval.get(s);
				val.put(k, c);
			}

		}
		catch (ClassCastException e)
		{
			throw new WebApplicationException("This fan page has no data. Facebook only collects insights data for pages with 30+ fans.");
		}
		return new OBJECT("data_date", data_date, "dict", val);

	}
/*
M.65+=3
M.35-44=35
M.45-54=16
M.55-64=2
M.18-24=2
M.13-17=1
M.25-34=14

 */
}

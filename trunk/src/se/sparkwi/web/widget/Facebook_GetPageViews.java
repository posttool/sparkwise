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

public class Facebook_GetPageViews extends WidgetDefinition
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
		return RETURN_TYPES_NUMBER;
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

		OBJECT response = api.call("/" + selector_values.get("fan page") + "/insights/page_views/day");
		OBJECT latest_value = FacebookUtils.get_valid_fb_insights_value(response);
		String date_s = latest_value.S("end_time");
		Date data_date = DateUtil.parseOrNull(date_s);
		return new OBJECT("data_date", data_date, "value", latest_value.I("value"));
	}

}

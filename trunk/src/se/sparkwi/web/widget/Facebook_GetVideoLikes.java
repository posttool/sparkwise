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

import se.sparkwi.web.module.WidgetDefinition;
import se.sparkwi.web.module.auth.ConnectionApi;

import com.pagesociety.util.ARRAY;
import com.pagesociety.util.OBJECT;
import com.pagesociety.web.exception.WebApplicationException;

public class Facebook_GetVideoLikes extends WidgetDefinition
{

	@Override
	public String get_required_connection()
	{
		return ConnectionApi.CONNECTION_TYPE_FACEBOOK;
	}

	@Override
	protected OBJECT[] get_service_selector_info()
	{
		return new OBJECT[] { new OBJECT(SERVICE_SELECTOR_NAME, "page", SERVICE_SELECTOR_TYPE, SELECTOR_TYPE_SELECT),
				new OBJECT(SERVICE_SELECTOR_NAME, "video", SERVICE_SELECTOR_TYPE, SELECTOR_TYPE_SELECT) };
	}

	@Override
	protected int[] get_return_types()
	{
		return RETURN_TYPES_NUMBER;
	}

	@Override
	protected OBJECT get_selector_data(ConnectionApi api, OBJECT selector_values, String name) throws WebApplicationException
	{

		if (name.equals("page"))
		{
			OBJECT response = api.call("/me/accounts");
			ARRAY page_list = response.A("data");
			ARRAY page_names = new ARRAY();
			ARRAY page_ids = new ARRAY();

			page_names.add(api.profile().get("username"));
			page_ids.add(api.profile().get("uid"));
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
		if (name.equals("video") && selector_values.get("page") == null)
		{
			return new OBJECT("selector_name", name, "display_values", new ARRAY(""), "values", new ARRAY("_waiting"));
		}

		if (name.equals("video"))
		{
			OBJECT response = api.call("/" + selector_values.get("page") + "/videos");
			ARRAY video_list = response.A("data");
			ARRAY video_names = new ARRAY();
			ARRAY video_ids = new ARRAY();

			for (int i = 0; i < video_list.size(); i++)
			{
				OBJECT p = (OBJECT) video_list.get(i);
				video_names.add(p.S("description"));
				video_ids.add(p.S("id"));

			}
			return new OBJECT("selector_name", name, "display_values", video_names, "values", video_ids);
		}
		return null;
	}

	@Override
	protected OBJECT get_data(ConnectionApi api, OBJECT selector_values, OBJECT props) throws WebApplicationException
	{

		OBJECT likes = api.call("/" + selector_values.get("video") + "/likes", new OBJECT("limit", "0"));

		return new OBJECT("value", likes.A("data").size());

	}

}

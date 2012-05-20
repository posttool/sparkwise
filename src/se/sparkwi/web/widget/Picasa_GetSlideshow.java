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

import se.sparkwi.web.module.DashboardModule;
import se.sparkwi.web.module.WidgetDefinition;
import se.sparkwi.web.module.auth.ConnectionApi;

import com.pagesociety.util.ARRAY;
import com.pagesociety.util.OBJECT;
import com.pagesociety.web.exception.WebApplicationException;

public class Picasa_GetSlideshow extends WidgetDefinition
{

	@Override
	public String get_required_connection()
	{
		return ConnectionApi.CONNECTION_TYPE_PICASA;
	}

	@Override
	protected OBJECT[] get_service_selector_info()
	{
		return new OBJECT[] { new OBJECT(SERVICE_SELECTOR_NAME, "album", SERVICE_SELECTOR_TYPE, SELECTOR_TYPE_SELECT) };
	}

	@Override
	protected int[] get_return_types()
	{
		return RETURN_TYPES_SLIDES;
	}

	@Override
	public boolean isLiveDataWidget()
	{
		return true;
	}

	@Override
	protected OBJECT get_selector_data(ConnectionApi api, OBJECT selector_values, String name) throws WebApplicationException
	{
		OBJECT response = api.call("/user/default");
		OBJECT feed = response.O("feed");
		ARRAY entries = feed.A("entry");
		if (entries == null)
			return selector_empty(name);
		ARRAY account_names = new ARRAY();
		ARRAY account_ids = new ARRAY();
		for (int i = 0; i < entries.size(); i++)
		{
			OBJECT entry = entries.O(i);
			String id = entry.S("id.$t");
			account_ids.add(id.substring(id.lastIndexOf('/') + 1));
			account_names.add(entry.S("title.$t"));
		}
		return new OBJECT("selector_name", name, "display_values", account_names, "values", account_ids);
	}

	@Override
	protected OBJECT get_data(ConnectionApi api, OBJECT selector_values, OBJECT props) throws WebApplicationException
	{
		String aid = selector_values.S("album");
		OBJECT response = api.call("/user/default/albumid/" + aid);
		OBJECT feed = response.O("feed");
		ARRAY entries = feed.A("entry");
		Date now = new Date();
		if (entries == null)
			return new OBJECT("data_date", now, "value", null);
		ARRAY values = new ARRAY();
		for (int i = 0; i < entries.size(); i++)
		{
			OBJECT entry = entries.O(i);
			int width = entry.I("gphoto$width.$t");
			int height = entry.I("gphoto$height.$t");
			String title = entry.S("title.$t");
			String license = entry.S("gphoto$license.$t");
			String url = entry.S("content.src");
			values.add(new OBJECT(DashboardModule.WIDGETDATA_FIELD_DATA, new OBJECT("data_date", now, "value", url, "width", width,
					"height", height, "title", title, "license", license), DashboardModule.WIDGETDATA_FIELD_DATE_UTC, now));
		}
		// looks different because its a live data widget
		return new OBJECT("values", values);

	}

}

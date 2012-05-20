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

public class Flickr_GetSlideshow extends WidgetDefinition
{

	@Override
	public String get_required_connection()
	{
		return ConnectionApi.CONNECTION_TYPE_FLICKR;
	}

	@Override
	protected OBJECT[] get_service_selector_info()
	{
		return new OBJECT[] { 
				new OBJECT(SERVICE_SELECTOR_NAME, "album", SERVICE_SELECTOR_TYPE, SELECTOR_TYPE_SELECT) };
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
	protected OBJECT get_selector_data(ConnectionApi api,
			OBJECT selector_values, String name) throws WebApplicationException
	{
		OBJECT response = api.call("", new OBJECT("method","flickr.photosets.getList"));
		ARRAY photosets = response.A("photosets.photoset");
		if (photosets==null)
			return selector_empty(name);
		ARRAY account_names = new ARRAY();
		ARRAY account_ids = new ARRAY();
		for (int i=0; i<photosets.size(); i++)
		{
			OBJECT entry = photosets.O(i);
			int photos = entry.I("photos");
			if (photos==0)
				continue;
			account_ids.add(entry.S("id"));
			account_names.add(entry.S("title._content"));
		}
		return new OBJECT("selector_name", name, "display_values", account_names, "values", account_ids);
	}
	

	

	@Override
	protected OBJECT get_data(ConnectionApi api, OBJECT selector_values,
			OBJECT props) throws WebApplicationException
	{
		String aid = selector_values.S("album"); 
		OBJECT response = api.call("", new OBJECT("method","flickr.photosets.getPhotos","photoset_id", aid, "extras", "o_dims,url_m"));
		ARRAY entries = response.A("photoset.photo");
		Date now = new Date();
		if (entries==null)
			return new OBJECT("data_date", now, "value", null);
		ARRAY values = new ARRAY();
		for (int i=0; i<entries.size(); i++)
		{
			OBJECT entry = entries.O(i);
			int width = entry.I("width_m");
			int height = entry.I("height_m");
			String title = entry.S("title");
			String url = entry.S("url_m");
			values.add(new OBJECT(DashboardModule.WIDGETDATA_FIELD_DATA, new OBJECT("data_date", now, "value", url, "width", width, "height", height, "title", title),
					DashboardModule.WIDGETDATA_FIELD_DATE_UTC, now));
		}
		//looks different because its a live data widget
		return new OBJECT("values",values);

	}

}

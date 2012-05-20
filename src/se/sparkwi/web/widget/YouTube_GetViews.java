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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import com.pagesociety.util.OBJECT;
import com.pagesociety.util.ARRAY;
import com.pagesociety.web.exception.WebApplicationException;

import se.sparkwi.web.module.DashboardModule;
import se.sparkwi.web.module.WidgetDefinition;
import se.sparkwi.web.module.WidgetException;
import se.sparkwi.web.module.auth.ConnectionApi;



public class YouTube_GetViews extends WidgetDefinition
{


	@Override
	public String get_required_connection()
	{
		return ConnectionApi.CONNECTION_TYPE_YOUTUBE;
	}
	
	@Override
	protected OBJECT[] get_service_selector_info()
	{
		return new OBJECT[]{new OBJECT(SERVICE_SELECTOR_NAME,"video",
		           					   SERVICE_SELECTOR_TYPE,SELECTOR_TYPE_SELECT)
							};
	}

	@Override
	protected int[] get_return_types()
	{
		return RETURN_TYPES_NUMBER;
	}

	@Override
	protected OBJECT get_selector_data(ConnectionApi api, OBJECT selector_values,String name) throws WebApplicationException
	{
		OBJECT response 		= api.call("/users/"+api.profile().get("username")+"/uploads");
		ARRAY video_list	    = response.A("feed.entry");
		ARRAY video_names 		= new ARRAY();
		ARRAY video_ids 		= new ARRAY();

		if (video_list!=null)
			for(int i = 0;i < video_list.size();i++)
			{
				OBJECT p = (OBJECT)video_list.get(i);
				String[] id_parts = p.S("id.$t").split(":");
				video_names.add(p.S("title.$t"));
				video_ids.add(id_parts[id_parts.length-1]);
	
			}

		return new OBJECT("selector_name", name, "display_values", video_names, "values", video_ids);

	}

	@Override
	protected OBJECT get_data(ConnectionApi api, OBJECT selector_values, OBJECT props) throws WebApplicationException
	{
		OBJECT response = api.call("/videos/"+selector_values.get("video"));
		return new OBJECT("value",response.I("entry.yt$statistics.viewCount"));
	}

}

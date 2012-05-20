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



public class Vimeo_GetViews extends WidgetDefinition
{


	@Override
	public String get_required_connection()
	{
		return ConnectionApi.CONNECTION_TYPE_VIMEO;
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
		//dict = api.call('vimeo.videos.getAll',{'user_id':api.profile['uid'],'page':'1','per_page':'50','sort':'newest'})
		OBJECT response 	= api.call("vimeo.videos.getAll",new OBJECT("user_id",api.profile().S("uid"),
																		"page","1",
																		"per_page","100",
																		"sort","newest"));
		ARRAY video_list	    = response.A("videos.video");
		ARRAY video_names 		= new ARRAY();
		ARRAY video_ids 		= new ARRAY();



		for(int i = 0;i < video_list.size();i++)
		{
			OBJECT p = (OBJECT)video_list.get(i);
			video_names.add(p.S("title"));
			video_ids.add(p.S("id"));

		}

		return new OBJECT("selector_name", name, "display_values", video_names, "values", video_ids);

	}

	@Override
	protected OBJECT get_data(ConnectionApi api, OBJECT selector_values, OBJECT props) throws WebApplicationException
	{

		OBJECT response = api.call("vimeo.videos.getInfo",new OBJECT("video_id",selector_values.get("video")));
		return new OBJECT("value",response.I("video[0].number_of_plays"));
	}

}

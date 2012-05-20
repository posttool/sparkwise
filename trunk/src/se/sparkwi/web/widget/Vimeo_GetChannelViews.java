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
import java.util.List;
import java.util.Map;

import com.pagesociety.util.OBJECT;
import com.pagesociety.util.ARRAY;
import com.pagesociety.web.exception.WebApplicationException;

import se.sparkwi.web.module.DashboardModule;
import se.sparkwi.web.module.WidgetDefinition;
import se.sparkwi.web.module.WidgetException;
import se.sparkwi.web.module.auth.ConnectionApi;



public class Vimeo_GetChannelViews extends WidgetDefinition
{


	@Override
	public String get_required_connection()
	{
		return ConnectionApi.CONNECTION_TYPE_VIMEO;
	}
	@Override
	protected OBJECT[] get_service_selector_info()
	{
		return new OBJECT[]{new OBJECT(SERVICE_SELECTOR_NAME,"channel",
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


		OBJECT response 	= api.call("vimeo.channels.getAll",new OBJECT("user_id",api.profile().S("uid"),
																		"page","1",
																		"per_page","100",
																		"sort","newest"));


		//#vimeo has a non standard way of returning channels so we need to test for type
		//#is it a list of channels

		ARRAY channel_names 		= new ARRAY();
		ARRAY channel_ids 		    = new ARRAY();
		if(((OBJECT)response.get("channels")).containsKey("channel"))
		{
			Object cval = ((OBJECT)response.get("channels")).get("channel");
			if(cval instanceof List)
			{
				ARRAY clist = (ARRAY)cval;
				for(int i = 0;i < clist.size();i++)
				{
					OBJECT channel = (OBJECT)clist.get(i);
					channel_ids.add(channel.S("id"));
					channel_names.add(channel.S("name"));
				}
			}
			else
			{
				 OBJECT channel = (OBJECT)cval;
				 channel_ids.add(channel.get("id"));
				 channel_names.add(channel.get("name"));
			}
		}

		return new OBJECT("selector_name", name, "display_values", channel_names, "values", channel_ids);

	}

	@Override
	protected OBJECT get_data(ConnectionApi api, OBJECT selector_values, OBJECT props) throws WebApplicationException
	{

		OBJECT response = api.call("vimeo.channels.getInfo",new OBJECT("channel_id",selector_values.get("channel")));
		return new OBJECT("value",response.I("channel.total_subscribers"));
	}

}

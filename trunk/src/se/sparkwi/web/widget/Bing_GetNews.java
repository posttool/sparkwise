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
import se.sparkwi.web.module.auth.type.BingApp;
import se.sparkwi.web.util.DateUtil;

import com.pagesociety.util.ARRAY;
import com.pagesociety.util.OBJECT;
import com.pagesociety.web.exception.WebApplicationException;

public class Bing_GetNews extends WidgetDefinition
{

	@Override
	public String get_required_connection()
	{
		return ConnectionApi.CONNECTION_TYPE_RAW;
	}

	@Override
	protected int[] get_return_types()
	{
		return RETURN_TYPES_ANNOTATED_NUMBER;
	}

	@Override
	protected OBJECT[] get_service_selector_info()
	{
		return new OBJECT[] { new OBJECT(SERVICE_SELECTOR_NAME, "Search Query", SERVICE_SELECTOR_TYPE, SELECTOR_TYPE_FREE_TEXT),
				new OBJECT(SERVICE_SELECTOR_NAME, "Market", SERVICE_SELECTOR_TYPE, SELECTOR_TYPE_SELECT) };
	}

	@Override
	protected OBJECT get_selector_data(ConnectionApi api, OBJECT selector_values, String name) throws WebApplicationException
	{
		String q = selector_values.S("Search Query");
		if (q == null)
			return selector_waiting(name);
		return BingUtils.getMarketSelectorInfo(name);
	}

	@Override
	protected OBJECT get_data(ConnectionApi api, OBJECT selector_values, OBJECT props) throws WebApplicationException
	{
		String query = (String) selector_values.get("Search Query");
		String market = (String) selector_values.get("Market");
		OBJECT o = api.call("http://api.bing.net/json.aspx", new OBJECT("AppId", BingApp.ID, "Query", "\"" + query + "\"", "Market",
				market, "Sources", "News", "Version", "2.0", "News.Offset", "0", "News.SortBy", "Relevance"));
		OBJECT web = o.O("SearchResponse.News");
		int count = web == null ? 0 : web.I("Total");
		ARRAY annotations = new ARRAY();
		if (count != 0)
		{
			ARRAY results = web.A("Results");
			if (results != null)
				for (int i = 0; i < results.size(); i++)
				{
					OBJECT r = (OBJECT) results.get(i);
					String title = r.S("Title");
					String url = r.S("Url");
					Date date = DateUtil.parseOrNull(r.S("Date"));
					// String description = r.S("description");
					// ^too much data!
					annotations.add(new OBJECT("date", date, "title", title, "url", url));
				}
		}
		return new OBJECT("data_date", new Date(), "value", count, "annotations", annotations);
	}
}

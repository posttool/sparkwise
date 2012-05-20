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
import static se.sparkwi.web.module.WidgetDefinition.RETURN_TYPE_DICT;
import static se.sparkwi.web.module.WidgetDefinition.RETURN_TYPE_GEODATA;
import static se.sparkwi.web.module.WidgetDefinition.RETURN_TYPE_NUMBER;
import static se.sparkwi.web.module.WidgetDefinition.SELECTOR_TYPE_FREE_TEXT;
import static se.sparkwi.web.module.WidgetDefinition.SELECTOR_TYPE_SELECT;
import static se.sparkwi.web.module.WidgetDefinition.SERVICE_SELECTOR_LABEL;
import static se.sparkwi.web.module.WidgetDefinition.SERVICE_SELECTOR_NAME;
import static se.sparkwi.web.module.WidgetDefinition.SERVICE_SELECTOR_OPTIONAL;
import static se.sparkwi.web.module.WidgetDefinition.SERVICE_SELECTOR_TYPE;
import static se.sparkwi.web.util.CommonUtil.empty;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

import se.sparkwi.web.module.auth.ConnectionApi;
import se.sparkwi.web.util.DateUtil;
import se.sparkwi.web.util.UTCDateInfo;

import com.pagesociety.util.ARRAY;
import com.pagesociety.util.OBJECT;
import com.pagesociety.web.exception.WebApplicationException;

public class GoogleAnalyticsUtil
{
	public static final int[] NUMBER_GEO_RETURN_TYPES = new int[] { RETURN_TYPE_NUMBER, RETURN_TYPE_GEODATA };
	public static final int[] DICT_RETURN_TYPES = new int[]{ RETURN_TYPE_DICT };

	public static OBJECT[] STANDARD_SELECTORS = new OBJECT[] {
			new OBJECT(SERVICE_SELECTOR_NAME, "account", SERVICE_SELECTOR_TYPE, SELECTOR_TYPE_SELECT),
			new OBJECT(SERVICE_SELECTOR_NAME, "page", SERVICE_SELECTOR_TYPE, SELECTOR_TYPE_FREE_TEXT, SERVICE_SELECTOR_LABEL,"Page (optional)",SERVICE_SELECTOR_OPTIONAL, true)
		};
	
	public static OBJECT getSelectorData(ConnectionApi api, String name) throws WebApplicationException
	{
		if (name.equals("account"))
		{
			OBJECT response = api.call("/accounts/default");
			OBJECT feed = response.O("feed");
			ARRAY entries = feed.A("entry");
			ARRAY account_names = new ARRAY();
			ARRAY account_ids = new ARRAY();
			if (entries==null)
			{
				return new OBJECT("selector_name", name, "display_values", account_names, "values", account_ids);
			}
			for (int i=0; i<entries.size(); i++)
			{
				OBJECT entry = entries.O(i);
				account_ids.add(entry.S("dxp$tableId.$t"));
				account_names.add(entry.S("title.$t"));
			}
			return new OBJECT("selector_name", name, "display_values", account_names, "values", account_ids);
		}
		return null;
	}
	public static OBJECT getData(ConnectionApi api, String account, String page, String metrics) throws WebApplicationException
	{
		return getData(api,account,page,metrics,true);
	}
	public static OBJECT getData(ConnectionApi api, String account, String page, String metrics, boolean get_geo_data) throws WebApplicationException
	{
		UTCDateInfo dd = DateUtil.getUTCDateInfo(new Date());
		Date yesterday = dd.utc_date_yesterday;
		SimpleDateFormat d = new SimpleDateFormat("yyyy-MM-dd");
		String start_date = d.format(yesterday);
		String end_date = start_date;

		OBJECT query_params = new OBJECT(
				"ids", account, 
				"dimensions", "ga:longitude,ga:latitude,ga:city",
				"metrics", metrics, 
				"start-date", start_date, 
				"end-date", end_date);
		if (!empty(page))
		{
			if (page.indexOf("http://")!=-1)
			{
				throw new WebApplicationException("Use a relative URL. This is the part of the URL after your domain, starting with the slash.");
			}
			if (!page.startsWith("/"))
				page = "/" + page;
			query_params.put("filters", "ga:pagePath=~^" + page + ".*");
		}

		OBJECT response = api.call("/data", query_params);
		OBJECT feed = response.O("feed");
		if (feed == null)
			throw new WebApplicationException("Google Analytics no 'feed' in results!!!");

		String title = feed.S("dxp$dataSource[0].dxp$tableName.$t");
		double value = feed.F("dxp$aggregates.dxp$metric[0].value");
		if (!get_geo_data)
			return new OBJECT("data_date", yesterday, "value", value, "title", title);
			
		ARRAY geo_values = new ARRAY();
		if (feed.containsKey("entry"))
		{
			ARRAY entries = feed.A("entry");
			for (int i = 0; i < entries.size(); i++)
			{
				OBJECT e = (OBJECT) entries.get(i);
				OBJECT geo_coord = new OBJECT(
						"longitude", e.S("dxp$dimension[0].value"), 
						"latitude", e.S("dxp$dimension[1].value"),
						"city", e.S("dxp$dimension[2].value"), 
						"scale", e.S("dxp$metric[0].value"));
				geo_values.add(geo_coord);
			}
		}

		return new OBJECT("data_date", yesterday, "value", value, "geo_values", geo_values, "title", title);
	}
	
	public static OBJECT getDictData(ConnectionApi api, String account, String page, String dimension, String metric) throws WebApplicationException
	{
		UTCDateInfo dd = DateUtil.getUTCDateInfo(new Date());
		Date yesterday = dd.utc_date_yesterday;
		SimpleDateFormat d = new SimpleDateFormat("yyyy-MM-dd");
		String start_date = d.format(yesterday);
		String end_date = start_date;

		OBJECT query_params = new OBJECT(
				"ids", account,
				"dimensions", dimension,
				"metrics",metric,
				"sort","-"+metric,
				"start-date", start_date,
				"end-date", end_date,
				"max-results","20");
		if (!empty(page))
			query_params.put("filter", "//ga:pagePath=~^" + page + ".*");

		OBJECT response = api.call("/data", query_params);
		OBJECT feed = response.O("feed");
		if (feed == null)
			throw new WebApplicationException("Google Analytics no 'feed' in results!!!");

		OBJECT dict = new OBJECT();
		if (feed.containsKey("entry"))
		{
			ARRAY entries = feed.A("entry");
			for (int i = 0; i < entries.size(); i++)
			{
				OBJECT entry = (OBJECT) entries.get(i);
				String name = entry.S("dxp$dimension[0].value");
				dict.put(name, entry.I("dxp$metric[0].value"));
			}
		}
		return new OBJECT("data_date", yesterday, "dict", dict);
	}
}

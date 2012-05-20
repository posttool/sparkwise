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

import se.sparkwi.web.module.auth.ConnectionApi;

import com.pagesociety.util.ARRAY;
import com.pagesociety.util.OBJECT;
import com.pagesociety.util.Text;
import com.pagesociety.web.exception.WebApplicationException;

public class GoogleSpreadsheetsUtil
{
	public static OBJECT getSpreadsheets(ConnectionApi api) throws WebApplicationException
	{
		OBJECT response = api.call("/spreadsheets/private/full");
		OBJECT feed = response.O("feed");
		ARRAY entries = feed.A("entry");
		if (entries == null)
			return null;
		ARRAY account_names = new ARRAY();
		ARRAY account_ids = new ARRAY();
		for (int i = 0; i < entries.size(); i++)
		{
			OBJECT entry = entries.O(i);
			account_ids.add(entry.S("id.$t").substring(51));
			account_names.add(entry.S("title.$t"));
		}
		return new OBJECT("display_values", account_names, "values", account_ids);
	}

	public static OBJECT getWorksheets(ConnectionApi api, String key) throws WebApplicationException
	{
		OBJECT response = api.call("/worksheets/" + key + "/private/full");
		OBJECT feed = response.O("feed");
		ARRAY entries = feed.A("entry");
		if (entries == null)
			return null;
		ARRAY account_names = new ARRAY();
		ARRAY account_ids = new ARRAY();
		for (int i = 0; i < entries.size(); i++)
		{
			OBJECT entry = entries.O(i);
			String wsid = entry.S("id.$t");
			int wsids = wsid.lastIndexOf("/") + 1;
			wsid = wsid.substring(wsids);
			account_ids.add(wsid);
			account_names.add(entry.S("title.$t"));
		}
		return new OBJECT("display_values", account_names, "values", account_ids);
	}

	public static OBJECT getHeaders(ConnectionApi api, String key, String worksheetId) throws WebApplicationException
	{
		OBJECT response = api.call("/cells/" + key + "/" + worksheetId + "/private/full", new OBJECT("min-row", "1", "max-row", "1"));
		OBJECT feed = response.O("feed");
		ARRAY entries = feed.A("entry");
		if (entries == null)
			return null;
		ARRAY names = new ARRAY();
		ARRAY ids = new ARRAY();
		for (int i = 0; i < entries.size(); i++)
		{
			OBJECT entry = entries.O(i);
			String id = entry.S("content.$t");
			ids.add(id);
			names.add(id);
		}
		return new OBJECT("display_values", names, "values", ids);
	}
	
	public static final String makeColumnLabelAttributeSafe(String s)
	{
		if(s==null)
			return "";
		String result = "";
		String encoded = Text.encodeURIComponent(s.toLowerCase());
		for (int i = 0; i < encoded.length(); i++)
		{
			if (encoded.charAt(i) == '%')
			{
				i += 2;
				continue;
			}
			if (encoded.charAt(i) == '+')
			{
				continue;
			}
			result += encoded.charAt(i);
		}
		return result;
	}
}

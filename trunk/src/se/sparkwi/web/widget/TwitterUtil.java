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

import se.sparkwi.web.module.auth.ConnectionApi;
import se.sparkwi.web.module.auth.type.RawAuthorizationHandler;
import se.sparkwi.web.util.DateUtil;
import se.sparkwi.web.util.UTCDateInfo;
import se.sparkwi.web.widget.feed.Entry;
import se.sparkwi.web.widget.feed.Feed;

import com.pagesociety.util.ARRAY;
import com.pagesociety.util.OBJECT;
import com.pagesociety.web.exception.WebApplicationException;

public class TwitterUtil
{
	private static final int MAX_ENTRIES_TO_COLLECT = 21;

	public static Feed search(String query) throws WebApplicationException
	{
		return search(query,null);
	}
	
	public static Feed search(String query, TwitterIter it) throws WebApplicationException
	{
		UTCDateInfo info = DateUtil.getUTCDateInfo(new Date());
		Date since = info.utc_date_yesterday;
		Date until = info.utc_date_tomorrow;
		int rows_per_page = 50;
		String since_str = DateUtil.formatDate0(since);
		String until_str = DateUtil.formatDate0(until);
		OBJECT params = new OBJECT("rpp", Integer.toString(rows_per_page), "q", query, "since", since_str, "until", until_str);
		return search(params, it);
	}

//	public static Feed search(String query, Date since, Date until, int rows_per_page, TwitterIter it) throws WebApplicationException
//	{
//		String since_str = DateUtil.formatDate0(since);
//		String until_str = DateUtil.formatDate0(until);
//		OBJECT params = new OBJECT("rpp", Integer.toString(rows_per_page), "q", query, "since", since_str, "until", until_str);
//		return search(params, it);
//	}

	public static Feed search(OBJECT params, TwitterIter it) throws WebApplicationException
	{
		ConnectionApi api = RawAuthorizationHandler.createApi(null);

		Feed f = new Feed();
		int l = 0;
		int rows_per_page = params.I("rpp");
		OBJECT response = api.call("http://search.twitter.com/search.json", params);
		ARRAY results = response.A("results");
		if (it != null)
		{
			for (int i = 0; i < results.size(); i++)
			{
				OBJECT r = (OBJECT) results.get(i);
				if (it.process(r))
				{
					l++;
					if (l<MAX_ENTRIES_TO_COLLECT)
						f.entries.add(get_entry(r));
				}
			}
		}
		else
		{
			l += results.size();
			for (int i=0; i<results.size() && i<MAX_ENTRIES_TO_COLLECT; i++)
			{
				OBJECT r = (OBJECT) results.get(i);
				f.entries.add(get_entry(r));
			}
		}
		while (results.size() == rows_per_page)
		{
			results = response.A("results");
			if (it != null)
			{
				for (int i = 1; i < results.size(); i++)
				{
					if (it.process((OBJECT) results.get(i)))
						l++;
				}
			}
			else
				l += response.A("results").size() - 1;

			String max_id = (String) ((OBJECT) results.get(results.size() - 1)).get("id_str");
			params.put("max_id", max_id);
			response = api.call("http://search.twitter.com/search.json", params);
		}
		f.total_count = l;
		return f;
	}

	private static Entry get_entry(OBJECT r) throws WebApplicationException
	{
		Entry e = new Entry();
		e.title = r.S("text");
		String date_str = r.S("created_at");
		e.date = DateUtil.parseOrNull(date_str);
		e.link = "http://twitter.com/u/status/"+r.get("id");
		return e;
	}

	public static boolean isRetweet(String username, String text)
	{
		int idx = text.toLowerCase().indexOf(username.toLowerCase());
		if (idx < 3)
			return false;
		if (text.substring(idx - 3, idx - 1).equals("RT"))
			return true;
		return false;
	}

	public static int callApi(ConnectionApi api, String method, Date since, Date until, int page_size) throws WebApplicationException
	{
		return callApi(api, method, new OBJECT(), since, until, page_size);
	}

	public static SimpleDateFormat dateFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss ZZZZZ yyyy");
	static {
		dateFormat.setLenient(false);
	}
	
	public static int callApi(ConnectionApi api, String method, OBJECT extra_params, Date since, Date until, int page_size)
			throws WebApplicationException
	{

		OBJECT params = new OBJECT("id", api.profile().get("uid"), "count", Integer.toString(page_size));
		for (String k : extra_params.keySet())
			params.put(k, extra_params.get(k));

		OBJECT response = api.call(method, params);
		ARRAY vals = response.A("values");
		int count = 0;
		String max_id = null;
		outer: while (true)
		{
			for (int i = 0; i < vals.size(); i++)
			{
				OBJECT r = (OBJECT) vals.get(i);
				System.out.println(method+" "+i+ r.get("created_at"));
				Date created_at = DateUtil.parseOrNull((String) r.get("created_at"));
				if (created_at.getTime() > since.getTime() && created_at.getTime() < until.getTime())
				{
					if (!r.get("id_str").equals(max_id))
						count++;
				}
				else if (created_at.getTime() < since.getTime())
				{
					break outer;
				}
				else
					continue;
			}
			if (vals.size() == page_size)
			{
				max_id = (String) ((OBJECT) vals.get(vals.size() - 1)).get("id_str");
				params.put("max_id", max_id);
				response = api.call(method, params);
				vals = response.A("values");
			}
			else
				break;
		}
		return count;
	}
	

}

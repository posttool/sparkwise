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

import com.pagesociety.util.ARRAY;
import com.pagesociety.util.OBJECT;
import com.pagesociety.web.exception.WebApplicationException;

public class FacebookUtils
{
	// pass through test_key if you are expecting a map back for value and want
	// to make sure the amp is there//
	// pass through null if value is terminal...see insights page views vs
	// insights fan genders
	public static OBJECT get_valid_fb_insights_value(OBJECT response) throws WebApplicationException
	{
		return get_valid_fb_insights_value(response, null);
	}

	public static OBJECT get_valid_fb_insights_value(OBJECT response, String test_key) throws WebApplicationException
	{

		ARRAY latest_values;
		ARRAY data = response.A("data");
		if (data.size() == 0)
			throw new WebApplicationException("Facebook insights data service is presently not functioning properly. It returned no data.");

		latest_values = response.A("data[0].values");
		OBJECT latest_value;
		for (int i = latest_values.size() - 1; i >= 0; i--)
		{
			latest_value = (OBJECT) latest_values.get(i);
			if (latest_value.get("value") == null)
				continue;
			try
			{
				if (latest_value.get("end_time") == null)
					continue;
				if (test_key != null)
				{
					if (((OBJECT) latest_value.get("value")).get(test_key) != null)
						return latest_value;
				}
				else
				{
					return latest_value;
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
				throw new WebApplicationException(
						"Facebook insights data service is presently not functioning properly. It returned no valid data.");
			}
		}
		throw new WebApplicationException(
				"Facebook insights data service is presently not functioning properly. It returned no valid data.");
	}
}

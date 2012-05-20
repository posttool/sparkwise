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

import se.sparkwi.web.module.WidgetDefinition;
import se.sparkwi.web.module.auth.ConnectionApi;

import com.pagesociety.util.OBJECT;
import com.pagesociety.web.exception.WebApplicationException;

public class GoogleAnalytics_GetSiteVisits extends WidgetDefinition
{
	@Override
	public String get_required_connection()
	{
		return ConnectionApi.CONNECTION_TYPE_GOOGLE_ANALYTICS;
	}

	@Override
	protected OBJECT[] get_service_selector_info()
	{
		return GoogleAnalyticsUtil.STANDARD_SELECTORS;
	}

	@Override
	protected int[] get_return_types()
	{
		return GoogleAnalyticsUtil.NUMBER_GEO_RETURN_TYPES;
	}

	@Override
	protected OBJECT get_selector_data(ConnectionApi api, OBJECT selector_values, String name) throws WebApplicationException
	{
		return GoogleAnalyticsUtil.getSelectorData(api, name);
	}

	@Override
	protected OBJECT get_data(ConnectionApi api, OBJECT selector_values, OBJECT props) throws WebApplicationException
	{
		String account = selector_values.S("account");
		String page = selector_values.S("page");
		return GoogleAnalyticsUtil.getData(api, account, page, "ga:visitors");
	}

}

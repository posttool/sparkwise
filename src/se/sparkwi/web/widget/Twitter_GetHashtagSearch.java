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
import se.sparkwi.web.util.DateUtil;
import se.sparkwi.web.util.UTCDateInfo;
import se.sparkwi.web.widget.feed.Feed;

import com.pagesociety.util.OBJECT;
import com.pagesociety.web.exception.WebApplicationException;

public class Twitter_GetHashtagSearch extends WidgetDefinition
{

	@Override
	public String get_required_connection()
	{
		return ConnectionApi.CONNECTION_TYPE_TWITTER;
	}

	@Override
	protected OBJECT[] get_service_selector_info()
	{
		return new OBJECT[] { new OBJECT(SERVICE_SELECTOR_NAME, "hashtag", SERVICE_SELECTOR_LABEL, "#hashtag", SERVICE_SELECTOR_TYPE,
				SELECTOR_TYPE_FREE_TEXT, SERVICE_SELECTOR_INPUT_BEHAVIOR, "force_prepend", SERVICE_SELECTOR_INPUT_BEHAVIOR_VAL, "#") };
	}

	@Override
	protected int[] get_return_types()
	{
		return RETURN_TYPES_ANNOTATED_NUMBER;
	}

	/* getting yesterdays data */
	@Override
	protected OBJECT get_data(ConnectionApi api, OBJECT selector_values, OBJECT props) throws WebApplicationException
	{

		String hashtag = selector_values.S("hashtag");
		if (!hashtag.startsWith("#"))
			hashtag = "#" + hashtag;

		Feed f = TwitterUtil.search(hashtag);
		UTCDateInfo info = DateUtil.getUTCDateInfo(new Date());
		return new OBJECT("data_date", info.utc_date_yesterday, "value", f.total_count, "annotations", f.getEntryAsO());
	}

}

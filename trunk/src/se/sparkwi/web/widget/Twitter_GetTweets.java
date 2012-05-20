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

import com.pagesociety.util.OBJECT;
import com.pagesociety.web.exception.WebApplicationException;

public class Twitter_GetTweets extends WidgetDefinition
{

	@Override
	public String get_required_connection()
	{
		return ConnectionApi.CONNECTION_TYPE_TWITTER;
	}

	@Override
	protected int[] get_return_types()
	{
		return RETURN_TYPES_NUMBER;
	}

	@Override
	protected OBJECT get_data(ConnectionApi api, OBJECT selector_values, OBJECT props) throws WebApplicationException
	{
		UTCDateInfo info = DateUtil.getUTCDateInfo(new Date());
		Date today = DateUtil.floorDate(info.utc_date, DateUtil.DATE_QUANTIZE_DAY, 1);
		Date yesterday = DateUtil.floorDate(info.utc_date_yesterday, DateUtil.DATE_QUANTIZE_DAY, 1);
		int count = TwitterUtil.callApi(api, "/statuses/user_timeline", new OBJECT("trim_user", "true"), yesterday, today, 20);
		return new OBJECT("data_date", yesterday, "value", count);

	}

}

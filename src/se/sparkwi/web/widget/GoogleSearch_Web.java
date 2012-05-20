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

public class GoogleSearch_Web extends WidgetDefinition
{



	@Override
	public String get_required_connection()
	{
		return ConnectionApi.CONNECTION_TYPE_RAW;
	}

	protected OBJECT[] get_service_selector_info()
	{
		return new OBJECT[]{new OBJECT(SERVICE_SELECTOR_NAME,"query",
		           					   SERVICE_SELECTOR_TYPE,SELECTOR_TYPE_FREE_TEXT)
							};
	}


	@Override
	protected int[] get_return_types()
	{
		return RETURN_TYPES_NUMBER;
	}

	@Override
	protected OBJECT get_data(ConnectionApi api, OBJECT selector_values, OBJECT props) throws WebApplicationException
	{
		OBJECT response = api.call("https://ajax.googleapis.com/ajax/services/search/web", new OBJECT(
											"q", selector_values.get("query"),
											"v", "1.0"));
		double value = 0;
		if (response.hasValue("responseData.cursor.estimatedResultCount"))
			value = response.F("responseData.cursor.estimatedResultCount");
		return new OBJECT( "value", value );
	}
	

}

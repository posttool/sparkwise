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

import se.sparkwi.web.module.DashboardModule;
import se.sparkwi.web.module.WidgetDefinition;
import se.sparkwi.web.module.auth.ConnectionApi;

import com.pagesociety.util.ARRAY;
import com.pagesociety.util.OBJECT;
import com.pagesociety.web.exception.WebApplicationException;

public class GoogleSpreadsheets_GetDict extends WidgetDefinition
{

	@Override
	public String get_required_connection()
	{
		return ConnectionApi.CONNECTION_TYPE_GOOGLE_SPREADSHEETS;
	}

	@Override
	protected OBJECT[] get_service_selector_info()
	{
		return new OBJECT[] { new OBJECT(SERVICE_SELECTOR_NAME, "spreadsheet", SERVICE_SELECTOR_TYPE, SELECTOR_TYPE_SELECT),
				new OBJECT(SERVICE_SELECTOR_NAME, "worksheet", SERVICE_SELECTOR_TYPE, SELECTOR_TYPE_SELECT),
				new OBJECT(SERVICE_SELECTOR_NAME, "label column", SERVICE_SELECTOR_TYPE, SELECTOR_TYPE_SELECT),
				new OBJECT(SERVICE_SELECTOR_NAME, "value column", SERVICE_SELECTOR_TYPE, SELECTOR_TYPE_SELECT) };
	}

	@Override
	protected int[] get_return_types()
	{
		return new int[] { RETURN_TYPE_DICT };
	}

	@Override
	public boolean isLiveDataWidget()
	{
		return true;
	}

	@Override
	protected OBJECT get_selector_data(ConnectionApi api, OBJECT selector_values, String name) throws WebApplicationException
	{
		OBJECT response = null;
		if (name.equals("spreadsheet"))
		{
			response = GoogleSpreadsheetsUtil.getSpreadsheets(api);
		}
		else if (name.equals("worksheet"))
		{
			String key = selector_values.S("spreadsheet");
			if (key == null)
				return selector_waiting(name);
			response = GoogleSpreadsheetsUtil.getWorksheets(api, key);
		}
		else if (name.equals("label column") || name.equals("value column"))
		{
			String key = selector_values.S("spreadsheet");
			String worksheetId = selector_values.S("worksheet");
			if (key == null || worksheetId == null)
				return selector_waiting(name);
			response = GoogleSpreadsheetsUtil.getHeaders(api, key, worksheetId);
		}
		if (response == null)
			return selector_empty(name);
		response.put("selector_name", name);
		return response;
	}

	@Override
	protected OBJECT get_data(ConnectionApi api, OBJECT selector_values, OBJECT props) throws WebApplicationException
	{
		String key = selector_values.S("spreadsheet");
		String worksheetId = selector_values.S("worksheet");
		String label_col_name = GoogleSpreadsheetsUtil.makeColumnLabelAttributeSafe(selector_values.S("label column"));
		String value_col_name = GoogleSpreadsheetsUtil.makeColumnLabelAttributeSafe(selector_values.S("value column"));
		OBJECT response = api.call("/list/" + key + "/" + worksheetId + "/private/full");
		OBJECT feed = response.O("feed");
		ARRAY entries = feed.A("entry");
		if (entries == null)
			return new OBJECT("data_date", new Date(), "value", null);
		OBJECT dict = new OBJECT();
		for (int i = 0; i < entries.size(); i++)
		{
			OBJECT entry = entries.O(i);
			String label = entry.S("gsx$" + label_col_name + ".$t");
			String val = entry.S("gsx$" + value_col_name + ".$t");
			dict.put(label, val);
		}
		Date date = new Date();
		ARRAY values = new ARRAY(new OBJECT(DashboardModule.WIDGETDATA_FIELD_DATA, new OBJECT("data_date", date, "dict", dict),
				DashboardModule.WIDGETDATA_FIELD_DATE_UTC, date));
		// looks different because its a live data widget
		return new OBJECT("values", values);

	}

}

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
package se.sparkwi.web.module.auth;

import com.pagesociety.util.OBJECT;
import com.pagesociety.web.exception.WebApplicationException;

public interface ConnectionApi
{

	public static final String CONNECTION_TYPE_NONE = "None";
	public static final String CONNECTION_TYPE_FACEBOOK = "Facebook";
	public static final String CONNECTION_TYPE_TWITTER = "Twitter";
	public static final String CONNECTION_TYPE_YOUTUBE = "YouTube";
	public static final String CONNECTION_TYPE_GOOGLE_ANALYTICS = "GoogleAnalytics";
	public static final String CONNECTION_TYPE_GOOGLE_SPREADSHEETS = "GoogleSpreadsheets";
	public static final String CONNECTION_TYPE_PICASA = "Picasa";
	public static final String CONNECTION_TYPE_FLICKR = "Flickr";
	public static final String CONNECTION_TYPE_VIMEO = "Vimeo";
	public static final String CONNECTION_TYPE_RAW = "Raw";

	public static final String[] CONNECTION_TYPES = new String[] {
			CONNECTION_TYPE_FACEBOOK, CONNECTION_TYPE_TWITTER,
			CONNECTION_TYPE_YOUTUBE, CONNECTION_TYPE_GOOGLE_ANALYTICS,
			CONNECTION_TYPE_GOOGLE_SPREADSHEETS, CONNECTION_TYPE_PICASA,
			CONNECTION_TYPE_FLICKR, CONNECTION_TYPE_VIMEO, CONNECTION_TYPE_RAW };

	public OBJECT profile();

	public OBJECT call(String path_or_method) throws WebApplicationException;

	public OBJECT call(String path_or_method, OBJECT params)
			throws WebApplicationException;

	public OBJECT call(String path_or_method, OBJECT params, String method,
			OBJECT headers, boolean use_endpoint_as_base)
			throws WebApplicationException;

//	public String calls(String path_or_method, OBJECT params, String method,
//			OBJECT headers) throws WebApplicationException;

}

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

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import se.sparkwi.web.module.WidgetDefinition;
import se.sparkwi.web.module.auth.ConnectionApi;

import com.pagesociety.util.OBJECT;
import com.pagesociety.web.exception.WebApplicationException;

public class Custom_XML extends WidgetDefinition
{

	@Override
	public String get_required_connection()
	{
		return ConnectionApi.CONNECTION_TYPE_RAW;
	}

	@Override
	protected int[] get_return_types()
	{
		return RETURN_TYPES_NUMBER;
	}

	@Override
	protected OBJECT[] get_service_selector_info()
	{
		return new OBJECT[] { new OBJECT(SERVICE_SELECTOR_NAME, "XML URL feed", SERVICE_SELECTOR_TYPE, SELECTOR_TYPE_FREE_TEXT),
				new OBJECT(SERVICE_SELECTOR_NAME, "Xpath", SERVICE_SELECTOR_TYPE, SELECTOR_TYPE_FREE_TEXT) };
	}

	@Override
	protected OBJECT get_data(ConnectionApi api, OBJECT selector_values, OBJECT props) throws WebApplicationException
	{
		String url = (String) selector_values.get("XML URL feed");
		String path = (String) selector_values.get("Xpath");

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder;
		try
		{
			builder = factory.newDocumentBuilder();
		}
		catch (ParserConfigurationException e)
		{
			e.printStackTrace();
			throw new WebApplicationException("Unable to find data [" + e.getMessage() + "]", e);
		}
		Document doc;
		try
		{
			doc = builder.parse(url);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw new WebApplicationException("Unable to find data [" + e.getMessage() + "]", e);
		}
		XPathFactory xPathfactory = XPathFactory.newInstance();
		XPath xpath = xPathfactory.newXPath();
		XPathExpression expr;
		NodeList result;
		try
		{
			expr = xpath.compile(path);
			result = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
		}
		catch (XPathExpressionException e)
		{
			e.printStackTrace();
			throw new WebApplicationException("Unable to find data [" + e.getCause().getMessage() + "]", e);
		}
		int value = 0;
		for (int i = 0; i < result.getLength(); i++)
		{
			String v = result.item(i).getNodeValue();
			try
			{
				double c = Double.parseDouble(v);
				value += c;
			}
			catch (Exception e)
			{
				throw new WebApplicationException("Cannot parse [" + v + "]", e);
			}
		}
		return new OBJECT("value", value);

	}
}

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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import se.sparkwi.web.module.WidgetDefinition;
import se.sparkwi.web.module.auth.ConnectionApi;
import se.sparkwi.web.util.DateUtil;
import se.sparkwi.web.util.UTCDateInfo;
import se.sparkwi.web.widget.feed.Entry;
import se.sparkwi.web.widget.feed.Feed;

import com.pagesociety.util.ARRAY;
import com.pagesociety.util.OBJECT;
import com.pagesociety.web.exception.WebApplicationException;

public class GoogleAlerts_GetFeed extends WidgetDefinition
{

	@Override
	public String get_required_connection()
	{
		return ConnectionApi.CONNECTION_TYPE_RAW;
	}

	@Override
	protected int[] get_return_types()
	{
		return RETURN_TYPES_ANNOTATED_NUMBER;
	}

	@Override
	protected OBJECT[] get_service_selector_info()
	{
		return new OBJECT[] { new OBJECT(SERVICE_SELECTOR_NAME, "URL feed", SERVICE_SELECTOR_TYPE, SELECTOR_TYPE_FREE_TEXT, 
				SERVICE_SELECTOR_LABEL, "Unique Alert URL") };
	}

	// TODO should be a way to set the title from here...
	// so that client can always reliably ask for a def|proxy's title

	@Override
	protected OBJECT get_data(ConnectionApi api, OBJECT selector_values, OBJECT props) throws WebApplicationException
	{
		UTCDateInfo info = DateUtil.getUTCDateInfo(new Date());
		Date today = DateUtil.floorDate(info.utc_date, DateUtil.DATE_QUANTIZE_DAY, 1);
		Date yesterday = DateUtil.floorDate(info.utc_date_yesterday, DateUtil.DATE_QUANTIZE_DAY, 1);
		String url = selector_values.S("URL feed");
		try
		{
			Feed f = consume_feed(url);
			int count = 0;
			ARRAY annotations = new ARRAY();
			for (Entry e : f.entries)
			{
				if (e.date.getTime() > yesterday.getTime() && e.date.getTime() < today.getTime())
				{
					count++;
					annotations.add(new OBJECT("date", e.date, "title", e.title, "url", e.link));
					// "note", e.content //<too much data! TODO save a key here and put actual text/webpage on cdn????
				}
			}
			return new OBJECT("data_date", yesterday, "value", count, "annotations", annotations, "title", f.title);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw new WebApplicationException("Cannot consume feed", e);
		}
	}

	private Feed consume_feed(String url) throws WebApplicationException, SAXException, IOException, ParserConfigurationException
	{
		if (!url.startsWith("http://www.google.com/alerts/feeds"))
			throw new WebApplicationException("Feed must start with http://www.google.com/alerts/feeds");
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document doc = builder.parse(url);
		NodeList nodes = doc.getDocumentElement().getChildNodes();
		String alert_title = "Unknown alert";
		List<Element> entries = new ArrayList<Element>();
		for (int i = 0; i < nodes.getLength(); i++)
		{
			if (nodes.item(i).getNodeType() != Node.ELEMENT_NODE)
				continue;
			Element e = (Element) nodes.item(i);
			if (e.getNodeName().equals("title"))
				alert_title = e.getTextContent();
			else if (e.getNodeName().equals("entry"))
				entries.add(e);
		}
		Feed f = new Feed();
		f.title = alert_title;
		for (int i = 0; i < entries.size(); i++)
		{
			Element el = entries.get(i);
			String date_s = el.getAttribute("gr:crawl-timestamp-msec");
			Date date = new Date();
			date.setTime(Long.parseLong(date_s));
			NodeList title_nl = el.getElementsByTagName("title");
			String title = title_nl.item(0).getTextContent();
			NodeList link_nl = el.getElementsByTagName("link");
			String link = link_nl.item(0).getAttributes().getNamedItem("href").getNodeValue();
			NodeList content_nl = el.getElementsByTagName("content");
			String content = content_nl.item(0).getTextContent();
			Entry e = new Entry();
			e.date = date;
			e.title = title;
			e.link = link;
			e.content = content;
			f.entries.add(e);
		}
		return f;
	}

	public static void main(String[] args) throws Exception
	{
		GoogleAlerts_GetFeed ga = new GoogleAlerts_GetFeed();
		Feed f = ga.consume_feed("http://www.google.com/alerts/feeds/00340119932232307135/7508863645058952739");
		System.out.println(f.title);
		for (Entry e : f.entries)
			System.out.println(" " + e.date + " " + e.title);
	}
}

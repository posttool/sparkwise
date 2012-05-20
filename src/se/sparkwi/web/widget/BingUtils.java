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

public class BingUtils
{
	public static final String[] MARKETS = new String[] 
	{
		"en-US",
		"English - United States",
		"ar-XA",
		"Arabic - Arabia",
		"bg-BG",
		"Bulgarian - Bulgaria",
		"cs-CZ",
		"Czech - Czech Republic",
		"da-DK",
		"Danish - Denmark",
		"de-AT",
		"German - Austria",
		"de-CH",
		"German - Switzerland",
		"de-DE",
		"German - Germany",
		"el-GR",
		"Greek - Greece",
		"en-AU",
		"English - Australia",
		"en-CA",
		"English - Canada",
		"en-GB",
		"English - United Kingdom",
		"en-ID",
		"English - Indonesia",
		"en-IE",
		"English - Ireland",
		"en-IN",
		"English - India",
		"en-MY",
		"English - Malaysia",
		"en-NZ",
		"English - New Zealand",
		"en-PH",
		"English - Philippines",
		"en-SG",
		"English - Singapore",
		"en-XA",
		"English - Arabia",
		"en-ZA",
		"English - South Africa",
		"es-AR",
		"Spanish - Argentina",
		"es-CL",
		"Spanish - Chile",
		"es-ES",
		"Spanish - Spain",
		"es-MX",
		"Spanish - Mexico",
		"es-US",
		"Spanish - United States",
		"es-XL",
		"Spanish - Latin America",
		"et-EE",
		"Estonian - Estonia",
		"fi-FI",
		"Finnish - Finland",
		"fr-BE",
		"French - Belgium",
		"fr-CA",
		"French - Canada",
		"fr-CH",
		"French - Switzerland",
		"fr-FR",
		"French - France",
		"he-IL",
		"Hebrew - Israel",
		"hr-HR",
		"Croatian - Croatia",
		"hu-HU",
		"Hungarian - Hungary",
		"it-IT",
		"Italian - Italy",
		"ja-JP",
		"Japanese - Japan",
		"ko-KR",
		"Korean - Korea",
		"lt-LT",
		"Lithuanian - Lithuania",
		"lv-LV",
		"Latvian - Latvia",
		"nb-NO",
		"Norwegian - Norway",
		"nl-BE",
		"Dutch - Belgium",
		"nl-NL",
		"Dutch - Netherlands",
		"pl-PL",
		"Polish - Poland",
		"pt-BR",
		"Portuguese - Brazil",
		"pt-PT",
		"Portuguese - Portugal",
		"ro-RO",
		"Romanian - Romania",
		"ru-RU",
		"Russian - Russia",
		"sk-SK",
		"Slovak - Slovak Republic",
		"sl-SL",
		"Slovenian - Slovenia",
		"sv-SE",
		"Swedish - Sweden",
		"th-TH",
		"Thai - Thailand",
		"tr-TR",
		"Turkish - Turkey",
		"uk-UA",
		"Ukrainian - Ukraine",
		"zh-CN",
		"Chinese - China",
		"zh-HK",
		"Chinese - Hong Kong SAR",
		"zh-TW",
		"Chinese - Taiwan"
	};
	
	public static OBJECT getMarketSelectorInfo(String name)
	{
		ARRAY display_values = new ARRAY();
		ARRAY values = new ARRAY();
		for (int i=0; i< MARKETS.length; i+=2)
		{
			display_values.add(MARKETS[i+1]);
			values.add(MARKETS[i]);
		}
		return new OBJECT("selector_name", name, "display_values", display_values, "values", values);
	}
}

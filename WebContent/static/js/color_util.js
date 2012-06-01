//> global Object
var ColorUtil =
{
	// requires 2 rgb & a percent to go between
	//>Object fn(Object c0, Object c1, float percent)
	between: function (c0, c1, percent)
	{
		return {
			r: (c1.r-c0.r)*percent+c0.r,
			g: (c1.g-c0.g)*percent+c0.g,
			b: (c1.b-c0.b)*percent+c0.b
		};
	},
	
	// requires the color (as an int) and a scalar
	// used mostly to move a color toward black 
	multiplyIntAsRGB: function(basecolor, s)
	{
		var rgb = ColorUtil.IntToRGB(basecolor);
		rgb.r *= s;
		rgb.g *= s;
		rgb.b *= s;
		return ColorUtil.toInt(rgb);
	},
	
	
	toInt: function (o)
	{
		return o.r<<16 | o.g<<8 | o.b;
	},
	
	
	toRgb: function (c)
	{
		var o = {};
		o.r = c >> 16 & 0xFF;
		o.g = c >> 8 & 0xFF;
		o.b = c & 0xFF;
		return o;
	},
	
	toHexString: function (c)
	{
		var s = "000000"+c.toString(16);
		return "#"+s.substr(s.length-6,6);
	},
	
	parse: function(s)
	{
		if (s.indexOf('#')==0)
			s = s.substring(1);
		return parseInt(s, 16);
	},
	
	random: function ()
	{
		return ColorUtil.HSBToRGB(Math.random()*0xff, 0xff, 0xff);
	},
	
	RGBToInt: function (r,g,b)
	{
		var hex = r<<16 | g<<8 | b;
		return hex;
	},
	
	IntToRGB: function (value)
	{
		var rgb = {}
		rgb.r = (value >> 16) & 0xFF;
		rgb.g = (value >> 8) & 0xFF;
		rgb.b = value & 0xFF;		
		return rgb;
	},
	
	RGBToHSB: function (r,g,b)
	{
		var hsb = {};
		var _max = Math.max(r,g,b);
		var _min = Math.min(r,g,b);
		
		hsb.s = (_max != 0) ? (_max - _min) / _max * 100: 0;
		hsb.b = _max / 255 * 100;
		if(hsb.s == 0){
			hsb.h = 0;
		}else{
			switch(_max)
			{
				case r:
					hsb.h = (g - b)/(_max - _min)*60 + 0;
					break;
				case g:
					hsb.h = (b - r)/(_max - _min)*60 + 120;
					break;
				case b:
					hsb.h = (r - g)/(_max - _min)*60 + 240;
					break;
			}
		}
		
		hsb.h = Math.min(360, Math.max(0, Math.round(hsb.h)));
		hsb.s = Math.min(100, Math.max(0, Math.round(hsb.s)));
		hsb.b = Math.min(100, Math.max(0, Math.round(hsb.b)));
		
		return hsb;
	},
	
	HSBToRGB: function (h,s,b)
	{
		var rgb = {};
	
		var max = (b*0.01)*255;
		var min = max*(1-(s*0.01));
		
		if(h == 360){
			h = 0;
		}
		
		if(s == 0){
			rgb.r = rgb.g = rgb.b = b*(255*0.01) ;
		}else{
			var _h = Math.floor(h / 60);
			
			switch(_h){
				case 0:
					rgb.r = max	;																																																					
					rgb.g = min+h * (max-min)/ 60;
					rgb.b = min;
					break;
				case 1:
					rgb.r = max-(h-60) * (max-min)/60;
					rgb.g = max;
					rgb.b = min;
					break;
				case 2:
					rgb.r = min ;
					rgb.g = max;
					rgb.b = min+(h-120) * (max-min)/60;
					break;
				case 3:
					rgb.r = min;
					rgb.g = max-(h-180) * (max-min)/60;
					rgb.b =max;
					break;
				case 4:
					rgb.r = min+(h-240) * (max-min)/60;
					rgb.g = min;
					rgb.b = max;
					break;
				case 5:
					rgb.r = max;
					rgb.g = min;
					rgb.b = max-(h-300) * (max-min)/60;
					break;
				case 6:
					rgb.r = max;
					rgb.g = min+h  * (max-min)/ 60;
					rgb.b = min;
					break;
			}
	
			rgb.r = Math.min(255, Math.max(0, Math.round(rgb.r)));
			rgb.g = Math.min(255, Math.max(0, Math.round(rgb.g)));
			rgb.b = Math.min(255, Math.max(0, Math.round(rgb.b)));
		}
		return rgb;
	}
}
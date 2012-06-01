se.sparkwi.widget.viz.util.percent_complete = function(self,basecolor)
	{
		self.viz_util.title_and_icon(self);

 		//utility functions for color & drawing
		var hexs = function(c){ return ColorUtil.toHexString(c); };
		var darker = function(c,f){ return ColorUtil.multiplyIntAsRGB(c,f); };
		var arc = function (center, radius, startAngle, endAngle) 
		{
			var angle = startAngle;
			var coords = toCoords(center, radius, angle);
			var path = "M " + coords[0] + " " + coords[1];
			while(angle<=endAngle) {
				coords = toCoords(center, radius, angle);
				path += " L " + coords[0] + " " + coords[1];
				angle += 1;
			}
			return path;
		};
		var toCoords = function (center, radius, angle) 
		{
			var radians = (angle/180) * Math.PI;
			var x = center[0] + Math.cos(radians) * radius;
			var y = center[1] + Math.sin(radians) * radius;
			return [x, y];
		};

		// begin: set up all vars
		if (self.data==null || self.data.values==null || self.data.values.length==0)
			return;
		var value = self.data.values[0].data.value;
		var date = self.data.values[0].date_utc;
		var goal = self.props.goal ? self.props.goal : value ;
		var percent = value/goal;
		var colors = [ hexs(basecolor), hexs(darker(basecolor, .95)) ];

		// thicknesses & widths & text offsets
		var TS = [ 62, 76, 98 ];
		var WS = [ 190, 250, 310 ];
		var TXT_YS = [ 0, -4, -6 ];
		
		// size info
		var is_one_cell_high = self.height<300,
		    is_one_cell_wide = self.width<300;

		var idx = is_one_cell_wide ? 0 :
		          is_one_cell_high ? 1 : 2;
		
		// thickness of gauge
		var t = TS[idx];
		
		// width & height of box that will contain the arc
		var w = WS[idx]-t;
		var h = w*.65;

		// left angle, right angle and the goal angle
		var la = -170;
		var ra = -10;
		var ba = la + (ra - la)*Math.min(1,percent);

		// the radius is derived from box defined by w,h
		var r = h/2 + (w*w)/(8*h);

		// circles are plotted relative to their center. cx,cy are offsets into the cell.
		var cx = self.width*.5;
		var cy = r+t*.5 + (self.height - h - 40)*.5 + self.$title.height() * .5;
		
		// draw!!
		// self.paper.rect((self.width-w)*.5,(self.height - h - 20)*.5,w,h).attr({fill:'#f00'}); /* debug rect */
		var overf = function()
		{
			var txt = '<b>'+Math.round(percent*100)+'% complete</b><br/>'+value+' of '+goal+' on '+sparkwise_local_date(date);
			graph_tooltip_show(null,txt); 
 		};
 		var outf = function()
 		{
 			graph_tooltip_hide();
 		};
 		self.paper.path(arc([cx,cy], r,	la, ra)).attr({ 'stroke-width': t+3, 'stroke': '#d9d9d9'});
 		self.paper.path(arc([cx,cy], r,	la, ra)).attr({ 'stroke-width': t, 'stroke': '#dfdfdf'}).mouseover(overf).mouseout(outf);
 		self.paper.path(arc([cx,cy], r+t*.2, la, ba)).attr({ 'stroke-width': t*.4+1,	'stroke':colors[0]}).mouseover(overf).mouseout(outf);
 		self.paper.path(arc([cx,cy], r-t*.2, la, ba)).attr({ 'stroke-width': t*.4, 'stroke': colors[1]}).mouseover(overf).mouseout(outf);
 		
 		// the text
 		var percent_text = Math.min(999,Math.round(percent * 100));
 		var dxnumber = percent >= 1 ? 9 : percent < .1 ? 3 : 7;
 		var dxpercent = percent >= 1 ? 21 : percent < .1 ? 13 :16;
 		var texty = cy+TXT_YS[idx];
 		// "94%"
 		self.paper.text(cx-dxnumber,texty,percent_text).attr({'fill': '#333', 'font-family': 'Akzidenz-GroteskStdRgCn', 'font-size': '34px', 'text-anchor': 'center'});
 		self.paper.text(cx+dxpercent,texty,'%').attr({'fill': '#333', 'font-family': 'Akzidenz-GroteskStdRgCn', 'font-size': '24px', 'text-anchor': 'center'});
 		// 0    999
 		self.paper.text(cx-r,texty+7,'0').attr({'fill': '#666', 'font-family': 'Helvetica,Arial', 'font-size': '11px', 'text-anchor': 'center'});
 		self.paper.text(cx+r,texty+7,self.util.number_to_size(self.props.goal)).attr({'fill': '#666', 'font-family': 'Helvetica,Arial', 'font-size': '11px', 'text-anchor': 'center'});

	}

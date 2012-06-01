
se.sparkwi.widget.viz.util.mapstyles = [
{
    featureType: "water",
    stylers: [
      { saturation: -97 },
      { lightness: 87 }
    ]
  },{
    featureType: "landscape.natural",
    stylers: [
      { visibility: "off" },
      { gamma: 9.01 },
      { saturation: -100 },
      { lightness: -6 }
    ]
  },{
    featureType: "road.local",
    stylers: [
      { visibility: "off" }
    ]
  },{
    featureType: "road.highway.controlled_access",
    stylers: [
      { visibility: "off" }
    ]
  },{
    featureType: "landscape.natural",
    stylers: [
      { visibility: "off" }
    ]
  },{
    featureType: "landscape",
    stylers: [
      { visibility: "off" }
    ]
  },{
    featureType: "landscape.man_made",
    stylers: [
      { visibility: "off" }
    ]
  },{
    featureType: "poi",
    stylers: [
      { visibility: "off" }
    ]
  },{
    featureType: "road",
    stylers: [
      { visibility: "off" }
    ]
  },{
    featureType: "landscape.natural",
    stylers: [
      { saturation: -93 },
      { lightness: -6 }
    ]
  },{
  }
]
se.sparkwi.widget.viz.util.map = function(self,geo_values_list,labels,colors)
	{
		self.viz_util.title_and_icon(self);
		var to = self.title_height;
		var h = self.height - to;
		
		var $mapel = $("<div></div>").css({ 'height':h+'px', 'top':to+'px' });
		self.element.append($mapel);
		
		var compute_bounds = true;
		var init_center = null;
		var init_zoom = null;
		var latlngbounds = null;
		if (self.props.map_lat != null && self.props.map_lng != null)
		{
			compute_bounds = false;
			init_center = new google.maps.LatLng(Number(self.props.map_lat), Number(self.props.map_lng));
			init_zoom = Number(self.props.map_zoom);
		}
		else
		{
			latlngbounds = new google.maps.LatLngBounds();
			for (var l=0; l<geo_values_list.length; l++)
			{
				var geo_values = geo_values_list[l];
				for (var i=0; i<geo_values.length; i++)
				{
					latlngbounds.extend(new google.maps.LatLng(geo_values[i].latitude, geo_values[i].longitude));
				}
			}
			init_center = latlngbounds.getCenter();
			init_zoom = 1;
		}
		
        var map_options = {
			center: init_center,
			zoom: init_zoom,
			scrollwheel: false,
			mapTypeId: google.maps.MapTypeId.ROADMAP,
			styles: se.sparkwi.widget.viz.util.mapstyles,
	        panControl: false,
	        zoomControl: true,
	        zoomControlOptions: {
	            style: google.maps.ZoomControlStyle.SMALL,
	            position: google.maps.ControlPosition.LEFT_BOTTOM
	        },
	        mapTypeControl: false,
	        scaleControl: false,
	        streetViewControl: false,
	        overviewMapControl: false
        };
        var map = new google.maps.Map($mapel[0], map_options);
        if (compute_bounds)
        	map.fitBounds(latlngbounds);
		
        var markerImage = new google.maps.MarkerImage("/static/image/map-marker.png", 
        	new google.maps.Size(12,12), new google.maps.Point(0,0), new google.maps.Point(6,6) );
		var markers = [];
		for (var l=0; l<geo_values_list.length; l++)
		{
			var geo_values = geo_values_list[l];
			for (var i=0; i<geo_values.length; i++)
			{
				markers.push(new google.maps.Marker({
				    position: new google.maps.LatLng(geo_values[i].latitude, geo_values[i].longitude),
				    icon: markerImage
				  }) );
			}
		}
		var markerCluster = new MarkerClusterer(map, markers, { 
			/*gridSize: 40, */
			minZoom: 0,
			maxZoom: 15,
			styles: [{
		        url: "/static/image/map-small.png",
		        height: 35,
		        width: 35,
		        anchor: [0, 0],
		        textColor: '#000',
		        textSize: 11
		      }, {
		        url: "/static/image/map-mid.png",
		        height: 42,
		        width: 42,
		        anchor: [0, 0],
		        textColor: '#000',
		        textSize: 11
		      }, {
		        url: "/static/image/map-large.png",
		        width: 52,
		        height: 52,
		        anchor: [0, 0],
		        textColor: '#000',
		        textSize: 11
		      }] 
		    });
		
		// save state
		var save_center_and_zoom = function()
		{
			if (self.save_props == null)
				return;
		    var c = map.getCenter();
		    var z = map.getZoom();
			self.save_props({map_lat:c.lat(), map_lng:c.lng(), map_zoom: z});
		}
		var tll = google.maps.event.addListener(map, 'tilesloaded', function(){
			google.maps.event.removeListener(tll);
			google.maps.event.addListener(map, 'bounds_changed', save_center_and_zoom);
		});

	}

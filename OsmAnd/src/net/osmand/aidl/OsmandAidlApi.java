package net.osmand.aidl;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.view.View;

import net.osmand.aidl.maplayer.AMapLayer;
import net.osmand.aidl.maplayer.point.AMapPoint;
import net.osmand.aidl.mapmarker.AMapMarker;
import net.osmand.aidl.mapwidget.AMapWidget;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.MapMarkersHelper;
import net.osmand.plus.MapMarkersHelper.MapMarker;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.views.AidlMapLayer;
import net.osmand.plus.views.MapInfoLayer;
import net.osmand.plus.views.OsmandMapLayer;
import net.osmand.plus.views.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.MapWidgetRegistry.MapWidgetRegInfo;
import net.osmand.plus.views.mapwidgets.TextInfoWidget;
import net.osmand.util.Algorithms;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class OsmandAidlApi {

	private static final String AIDL_REFRESH_MAP = "aidl_refresh_map";
	private static final String AIDL_OBJECT_ID = "aidl_object_id";

	private static final String AIDL_ADD_MAP_WIDGET = "aidl_add_map_widget";
	private static final String AIDL_REMOVE_MAP_WIDGET = "aidl_remove_map_widget";

	private static final String AIDL_ADD_MAP_LAYER = "aidl_add_map_layer";
	private static final String AIDL_REMOVE_MAP_LAYER = "aidl_remove_map_layer";

	private OsmandApplication app;
	private Map<String, AMapWidget> widgets = new ConcurrentHashMap<>();
	private Map<String, TextInfoWidget> widgetControls = new ConcurrentHashMap<>();
	private Map<String, AMapLayer> layers = new ConcurrentHashMap<>();
	private Map<String, OsmandMapLayer> mapLayers = new ConcurrentHashMap<>();

	private BroadcastReceiver refreshMapReceiver;
	private BroadcastReceiver addMapWidgetReceiver;
	private BroadcastReceiver removeMapWidgetReceiver;
	private BroadcastReceiver addMapLayerReceiver;
	private BroadcastReceiver removeMapLayerReceiver;

	public OsmandAidlApi(OsmandApplication app) {
		this.app = app;
	}

	public void onCreateMapActivity(final MapActivity mapActivity) {
		registerRefreshMapReceiver(mapActivity);
		registerAddMapWidgetReceiver(mapActivity);
		registerRemoveMapWidgetReceiver(mapActivity);
		registerAddMapLayerReceiver(mapActivity);
		registerRemoveMapLayerReceiver(mapActivity);
	}

	public void onDestroyMapActivity(final MapActivity mapActivity) {
		if (refreshMapReceiver != null) {
			mapActivity.unregisterReceiver(refreshMapReceiver);
		}

		if (addMapWidgetReceiver != null) {
			mapActivity.unregisterReceiver(addMapWidgetReceiver);
		}
		if (removeMapWidgetReceiver != null) {
			mapActivity.unregisterReceiver(removeMapWidgetReceiver);
		}
		widgetControls.clear();

		if (addMapLayerReceiver != null) {
			mapActivity.unregisterReceiver(addMapLayerReceiver);
		}
		if (removeMapLayerReceiver != null) {
			mapActivity.unregisterReceiver(removeMapLayerReceiver);
		}
	}

	private void registerRefreshMapReceiver(final MapActivity mapActivity) {
		refreshMapReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				mapActivity.refreshMap();
			}
		};
		mapActivity.registerReceiver(refreshMapReceiver, new IntentFilter(AIDL_REFRESH_MAP));
	}

	private int getDrawableId(String id) {
		if (Algorithms.isEmpty(id)) {
			return 0;
		} else {
			return app.getResources().getIdentifier(id, "drawable", app.getPackageName());
		}
	}

	private void registerAddMapWidgetReceiver(final MapActivity mapActivity) {
		addMapWidgetReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				String widgetId = intent.getStringExtra(AIDL_OBJECT_ID);
				if (widgetId != null) {
					AMapWidget widget = widgets.get(widgetId);
					if (widget != null) {
						MapInfoLayer layer = mapActivity.getMapLayers().getMapInfoLayer();
						if (layer != null) {
							TextInfoWidget control = createWidgetControl(mapActivity, widgetId);
							widgetControls.put(widgetId, control);
							int menuIconId = getDrawableId(widget.getMenuIconName());
							MapWidgetRegInfo widgetInfo = layer.registerSideWidget(control,
									menuIconId, widget.getMenuTitle(), "aidl_widget_" + widgetId,
									false, widget.getOrder());
							if (!mapActivity.getMapLayers().getMapWidgetRegistry().isVisible(widgetInfo.key)) {
								mapActivity.getMapLayers().getMapWidgetRegistry().setVisibility(widgetInfo, true, false);
							}
							layer.recreateControls();
						}
					}
				}
			}
		};
		mapActivity.registerReceiver(addMapWidgetReceiver, new IntentFilter(AIDL_ADD_MAP_WIDGET));
	}

	private void registerRemoveMapWidgetReceiver(final MapActivity mapActivity) {
		removeMapWidgetReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				String widgetId = intent.getStringExtra(AIDL_OBJECT_ID);
				if (widgetId != null) {
					MapInfoLayer layer = mapActivity.getMapLayers().getMapInfoLayer();
					TextInfoWidget widgetControl = widgetControls.get(widgetId);
					if (layer != null && widgetControl != null) {
						layer.removeSideWidget(widgetControl);
						widgetControls.remove(widgetId);
						layer.recreateControls();
					}
				}
			}
		};
		mapActivity.registerReceiver(removeMapWidgetReceiver, new IntentFilter(AIDL_REMOVE_MAP_WIDGET));
	}

	public void registerWidgetControls(MapActivity mapActivity) {
		for (AMapWidget widget : widgets.values()) {
			MapInfoLayer layer = mapActivity.getMapLayers().getMapInfoLayer();
			if (layer != null) {
				TextInfoWidget control = createWidgetControl(mapActivity, widget.getId());
				widgetControls.put(widget.getId(), control);
				int menuIconId = getDrawableId(widget.getMenuIconName());
				MapWidgetRegInfo widgetInfo = layer.registerSideWidget(control,
						menuIconId, widget.getMenuTitle(), "aidl_widget_" + widget.getId(),
						false, widget.getOrder());
				if (!mapActivity.getMapLayers().getMapWidgetRegistry().isVisible(widgetInfo.key)) {
					mapActivity.getMapLayers().getMapWidgetRegistry().setVisibility(widgetInfo, true, false);
				}
			}
		}
	}

	private void registerAddMapLayerReceiver(final MapActivity mapActivity) {
		addMapLayerReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				String layerId = intent.getStringExtra(AIDL_OBJECT_ID);
				if (layerId != null) {
					AMapLayer layer = layers.get(layerId);
					if (layer != null) {
						OsmandMapLayer mapLayer = mapLayers.get(layerId);
						if (mapLayer != null) {
							mapActivity.getMapView().removeLayer(mapLayer);
						}
						mapLayer = new AidlMapLayer(mapActivity, layer);
						mapActivity.getMapView().addLayer(mapLayer, layer.getZOrder());
						mapLayers.put(layerId, mapLayer);
					}
				}
			}
		};
		mapActivity.registerReceiver(addMapLayerReceiver, new IntentFilter(AIDL_ADD_MAP_LAYER));
	}

	private void registerRemoveMapLayerReceiver(final MapActivity mapActivity) {
		removeMapLayerReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				String layerId = intent.getStringExtra(AIDL_OBJECT_ID);
				if (layerId != null) {
					OsmandMapLayer mapLayer = mapLayers.remove(layerId);
					if (mapLayer != null) {
						mapActivity.getMapView().removeLayer(mapLayer);
						mapActivity.refreshMap();
					}
				}
			}
		};
		mapActivity.registerReceiver(removeMapLayerReceiver, new IntentFilter(AIDL_REMOVE_MAP_LAYER));
	}

	public void registerMapLayers(MapActivity mapActivity) {
		for (AMapLayer layer : layers.values()) {
			OsmandMapLayer mapLayer = mapLayers.get(layer.getId());
			if (mapLayer != null) {
				mapActivity.getMapView().removeLayer(mapLayer);
			}
			mapLayer = new AidlMapLayer(mapActivity, layer);
			mapActivity.getMapView().addLayer(mapLayer, layer.getZOrder());
			mapLayers.put(layer.getId(), mapLayer);
		}
	}

	private void refreshMap() {
		Intent intent = new Intent();
		intent.setAction(AIDL_REFRESH_MAP);
		app.sendBroadcast(intent);
	}

	private TextInfoWidget createWidgetControl(final MapActivity mapActivity, final String widgetId) {
		final TextInfoWidget control = new TextInfoWidget(mapActivity) {

			@Override
			public boolean updateInfo(DrawSettings drawSettings) {
				AMapWidget widget = widgets.get(widgetId);
				if (widget != null) {
					String txt = widget.getText();
					String subtxt = widget.getDescription();
					boolean night = drawSettings != null && drawSettings.isNightMode();
					int icon = night ? getDrawableId(widget.getDarkIconName()) : getDrawableId(widget.getLightIconName());
					setText(txt, subtxt);
					if (icon != 0) {
						setImageDrawable(icon);
					} else {
						setImageDrawable(null);
					}
					return true;
				} else {
					return false;
				}
			}
		};
		control.updateInfo(null);

		control.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				AMapWidget widget = widgets.get(widgetId);
				if (widget != null && widget.getIntentOnClick() != null) {
					app.startActivity(widget.getIntentOnClick());
				}
			}
		});
		return control;
	}

	boolean addMapMarker(AMapMarker marker) {
		if (marker != null) {
			PointDescription pd = new PointDescription(
					PointDescription.POINT_TYPE_MAP_MARKER, marker.getName() != null ? marker.getName() : "");
			MapMarkersHelper markersHelper = app.getMapMarkersHelper();
			markersHelper.addMapMarker(new LatLon(marker.getLatLon().getLatitude(), marker.getLatLon().getLongitude()), pd);
			refreshMap();
			return true;
		} else {
			return false;
		}
	}

	boolean removeMapMarker(AMapMarker marker) {
		if (marker != null) {
			LatLon latLon = new LatLon(marker.getLatLon().getLatitude(), marker.getLatLon().getLongitude());
			MapMarkersHelper markersHelper = app.getMapMarkersHelper();
			List<MapMarker> mapMarkers = markersHelper.getMapMarkers();
			for (MapMarker m : mapMarkers) {
				if (m.getOnlyName().equals(marker.getName()) && latLon.equals(new LatLon(m.getLatitude(), m.getLongitude()))) {
					markersHelper.removeMapMarker(m);
					refreshMap();
					return true;
				}
			}
			return false;
		} else {
			return false;
		}
	}

	boolean updateMapMarker(AMapMarker markerPrev, AMapMarker markerNew) {
		if (markerPrev != null && markerNew != null) {
			LatLon latLon = new LatLon(markerPrev.getLatLon().getLatitude(), markerPrev.getLatLon().getLongitude());
			LatLon latLonNew = new LatLon(markerNew.getLatLon().getLatitude(), markerNew.getLatLon().getLongitude());
			MapMarkersHelper markersHelper = app.getMapMarkersHelper();
			List<MapMarker> mapMarkers = markersHelper.getMapMarkers();
			for (MapMarker m : mapMarkers) {
				if (m.getOnlyName().equals(markerPrev.getName()) && latLon.equals(new LatLon(m.getLatitude(), m.getLongitude()))) {
					PointDescription pd = new PointDescription(
							PointDescription.POINT_TYPE_MAP_MARKER, markerNew.getName() != null ? markerNew.getName() : "");
					MapMarker marker = new MapMarker(m.point, pd, m.colorIndex, m.selected, m.index);
					markersHelper.moveMapMarker(marker, latLonNew);
					refreshMap();
					return true;
				}
			}
			return false;
		} else {
			return false;
		}
	}

	boolean addMapWidget(AMapWidget widget) {
		if (widget != null) {
			if (widgets.containsKey(widget.getId())) {
				updateMapWidget(widget);
			} else {
				widgets.put(widget.getId(), widget);
				Intent intent = new Intent();
				intent.setAction(AIDL_ADD_MAP_WIDGET);
				intent.putExtra(AIDL_OBJECT_ID, widget.getId());
				app.sendBroadcast(intent);
			}
			refreshMap();
			return true;
		} else {
			return false;
		}
	}

	boolean removeMapWidget(String widgetId) {
		if (!Algorithms.isEmpty(widgetId) && widgets.containsKey(widgetId)) {
			widgets.remove(widgetId);
			Intent intent = new Intent();
			intent.setAction(AIDL_REMOVE_MAP_WIDGET);
			intent.putExtra(AIDL_OBJECT_ID, widgetId);
			app.sendBroadcast(intent);
			return true;
		} else {
			return false;
		}
	}

	boolean updateMapWidget(AMapWidget widget) {
		if (widget != null && widgets.containsKey(widget.getId())) {
			widgets.put(widget.getId(), widget);
			refreshMap();
			return true;
		} else {
			return false;
		}
	}

	boolean addMapLayer(AMapLayer layer) {
		if (layer != null) {
			if (layers.containsKey(layer.getId())) {
				updateMapLayer(layer);
			} else {
				layers.put(layer.getId(), layer);
				Intent intent = new Intent();
				intent.setAction(AIDL_ADD_MAP_LAYER);
				intent.putExtra(AIDL_OBJECT_ID, layer.getId());
				app.sendBroadcast(intent);
			}
			refreshMap();
			return true;
		} else {
			return false;
		}
	}

	boolean removeMapLayer(String layerId) {
		if (!Algorithms.isEmpty(layerId) && layers.containsKey(layerId)) {
			layers.remove(layerId);
			Intent intent = new Intent();
			intent.setAction(AIDL_REMOVE_MAP_LAYER);
			intent.putExtra(AIDL_OBJECT_ID, layerId);
			app.sendBroadcast(intent);
			return true;
		} else {
			return false;
		}
	}

	boolean updateMapLayer(AMapLayer layer) {
		if (layer != null && layers.containsKey(layer.getId())) {
			layers.put(layer.getId(), layer);
			refreshMap();
			return true;
		} else {
			return false;
		}
	}

	boolean putMapPoint(String layerId, AMapPoint point) {
		if (point != null) {
			AMapLayer layer = layers.get(layerId);
			if (layer != null) {
				layer.putPoint(point);
				refreshMap();
				return true;
			}
		}
		return false;
	}

	boolean removeMapPoint(String layerId, String pointId) {
		if (pointId != null) {
			AMapLayer layer = layers.get(layerId);
			if (layer != null) {
				layer.removePoint(pointId);
				refreshMap();
				return true;
			}
		}
		return false;
	}
}

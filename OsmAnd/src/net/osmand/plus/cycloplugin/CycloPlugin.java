package net.osmand.plus.cycloplugin;

import android.app.Activity;
import android.content.Intent;
import android.provider.Settings;
import android.util.Log;
import android.view.WindowManager;

import androidx.annotation.NonNull;

import net.osmand.Location;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.routing.RouteCalculationResult;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.settings.backend.OsmandSettings;

import java.util.Arrays;
import java.util.List;
import net.osmand.plus.routing.RouteCalculationResult.NextDirectionInfo;

public class CycloPlugin extends HeadsetGesturesPlugin {

	public static final String ID = "osmand.cyclo";

	private static final String CYCLE_PLUGIN_COMPONENT = "net.osmand.cycloPlugin";


	private Float brightness=1.0F;
	private OsmandSettings settings;
	private OsmandApplication app;
	private HeadsetGesturesPlugin headset;
	private NextDirectionInfo nextDirection = new NextDirectionInfo();
	int prevZoom=20;
	boolean zooming=false;
	Boolean next_next=false;

	@Override
	public String getId() {
		return ID;
	}

	public CycloPlugin(OsmandApplication app) {
		super(app);
		settings = app.getSettings();
	}

	/*@Override
	public int getLogoResourceId() {
		return R.drawable.ic_plugin_srtm;
	}
*/


	@Override
	public String getDescription() {
		return "It provides headset navigation for bikes";
	}

	@Override
	public String getName() {
		return "Cyclo plugin";
	}

	@Override
	public boolean isMarketPlugin() {
		return true;
	}
	@Override
	public String getComponentId1() {
		return CYCLE_PLUGIN_COMPONENT;
	}
	private float computeRotation(List<Location> loc, int current,int previous)
	{
		Location currentLocation=loc.get(current);
		previous=Math.max(previous,0);
		int reference = current;
		double distance=0;
		while(distance <20.F &&reference>=previous)
		{
			distance=currentLocation.distanceTo(loc.get(reference));
			reference--;
		}

		Location referenceLocation=loc.get(reference+1);
		return -referenceLocation.bearingTo(currentLocation);
	}
	private void toggleBrightness(MapActivity mapActivity)
	{
		WindowManager.LayoutParams layout = mapActivity.getWindow().getAttributes();
		if(layout.screenBrightness!=0.01F) {
			brightness = layout.screenBrightness;
			layout.screenBrightness=0.01F;
			mapActivity.getLockerHelper().lock();
		}
		else
			layout.screenBrightness =brightness;
		mapActivity.getWindow().setAttributes(layout);
	}
	@Override
	public void  headsetButton(MapActivity mapActivity,int clicks){
			if(clicks==0) {
				next_next=false;
				mapActivity.getMapViewTrackingUtilities().setMapLinkedToLocation(true);

				toggleBrightness(mapActivity);
				if(zooming)
				{
					next_next=false;
					mapActivity.getMapView().setMapPosition(OsmandSettings.BOTTOM_CONSTANT );
					//mapActivity.getMapView().setIntZoom(prevZoom);
					mapActivity.getMapViewTrackingUtilities().backToLocationImpl(prevZoom,true);
					zooming=false;

				}

			}
		RoutingHelper routingHelper = app.getRoutingHelper();

		if(clicks==1)
			{

				if(zooming)
				{
					next_next=false;
					mapActivity.getMapView().setMapPosition(OsmandSettings.BOTTOM_CONSTANT );
					//mapActivity.getMapView().setIntZoom(prevZoom);
					mapActivity.getMapViewTrackingUtilities().backToLocationImpl(prevZoom,true);
					zooming=false;

				}
				else {
					toggleBrightness(mapActivity);
				}


			}
			if(clicks==2)
			{

				mapActivity.dismissCardDialog();

				try {
					mapActivity.getMapViewTrackingUtilities().setMapLinkedToLocation(false);

					mapActivity.getMapView().setMapPosition(OsmandSettings.CENTER_CONSTANT);
					mapActivity.refreshMap();
					if (!next_next) {
						NextDirectionInfo r = routingHelper.getNextRouteDirectionInfo(nextDirection, false);
						List<Location> loc = routingHelper.getCurrentCalculatedRoute();
						Location l = loc.get(r.directionInfo.routePointOffset);
						prevZoom = mapActivity.getMapView().getZoom();
						zooming = true;
						mapActivity.getMapView().setIntZoom(20);
						mapActivity.getMapView().setLatLon(l.getLatitude(), l.getLongitude());
						mapActivity.getMapView().setRotate(computeRotation(loc, r.directionInfo.routePointOffset, 0), true);

						nextDirection = r;
						next_next = true;


						Log.d("Route", r.toString());
					} else {

						NextDirectionInfo r2 = routingHelper.getNextRouteDirectionInfoAfter(nextDirection, new NextDirectionInfo(), false);
						List<Location> loc = routingHelper.getCurrentCalculatedRoute();
						//Location lPrev=loc.get( nextDirection.directionInfo.routePointOffset);

						Location l = loc.get(r2.directionInfo.routePointOffset);

						mapActivity.getMapView().setLatLon(l.getLatitude(), l.getLongitude());
						mapActivity.getMapView().setRotate(computeRotation(loc, r2.directionInfo.routePointOffset, nextDirection.directionInfo.routePointOffset), true);
						next_next = true;
						nextDirection = r2;
						Log.d("Route", r2.toString());
					}
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}

			}
			if(clicks==3) {
				int curZoom=mapActivity.getMapView().getZoom();
				if(!zooming)
				{
					prevZoom=curZoom;
				}
				mapActivity.getMapView().setIntZoom(curZoom+1);

			}
	}


	@Override
	public void mapActivityCreate(MapActivity activity) {
		activity.startService(new Intent(activity, PlayerService.class));

	}
	@Override
	public void mapActivityResume(MapActivity activity) {
		activity.startService(new Intent(activity, PlayerService.class));
		WindowManager.LayoutParams layout = activity.getWindow().getAttributes();

		layout.screenBrightness =brightness;
		activity.getWindow().setAttributes(layout);
	}
	private Boolean screenOff=false;
	@Override
	public void mapActivityScreenOff(MapActivity activity) {
		screenOff=true;
	}

	@Override
	public boolean init(@NonNull final OsmandApplication _app, Activity activity) {
		app=_app;
		OsmandSettings settings = app.getSettings();
		app.startService(new Intent(app, PlayerService.class));

		Boolean canwrite=Settings.System.canWrite(app);

		if(!canwrite) {
		/*	try {
				Intent i = new Intent(
						Settings.ACTION_MANAGE_WRITE_SETTINGS,
						Uri.parse("package:" + app.getApplicationContext().getPackageName())
				);
				i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				app.startActivity(i);
			} catch (Exception e) {
				e.printStackTrace();
			}
*/

		}

		/*
		CommonPreference<String> pref = settings.getCustomRenderProperty("contourLines");
		if (pref.get().equals("")) {
			for (ApplicationMode m : ApplicationMode.allPossibleValues()) {
				if (pref.getModeValue(m).equals("")) {
					pref.setModeValue(m, "13");
				}
			}
		}*/
		return true;
	}



	@Override
	public void disable(OsmandApplication app) {
	}


}

package net.osmand.plus.cycloplugin;

import android.app.Activity;
import android.content.Intent;
import android.provider.Settings;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.Location;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.quickaction.QuickActionType;
import net.osmand.plus.quickaction.QuickActionsWidget;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.settings.backend.OsmandSettings;

import java.util.ArrayList;
import java.util.List;
import net.osmand.plus.routing.RouteCalculationResult.NextDirectionInfo;
import net.osmand.plus.views.OsmandMapTileView;

public class CycloPlugin extends HeadsetGesturesPlugin {

	public static final String ID = "osmand.cyclo";

	private static final String CYCLE_PLUGIN_COMPONENT = "net.osmand.cycloPlugin";


	private Float brightness=1.0F;
	private OsmandSettings settings;
	private OsmandApplication app;
	private HeadsetGesturesPlugin headset;
	private NextDirectionInfo nextDirection = new NextDirectionInfo();
	private QuickActionsWidget widget=null;
	private boolean inited=false;
	@Nullable
	private HeadsetUI headsetUI=null;
	int prevZoom=20;
	CycloUIWidget valueTV=null;
	boolean zooming=false;
	Boolean next_next=false;
	CycloPluginLayer cycleLayer;
	View cycle_view;
	private float distance=0.0f;
	@Override
	public String getId() {
		return ID;
	}

	public CycloPlugin(OsmandApplication _app) {
		super(_app);
		settings = _app.getSettings();
		app=_app;
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

/*	@Override
	public boolean isMarketPlugin() {
		return true;
	}
*/
@Override
	public String getComponentId1() {
		return CYCLE_PLUGIN_COMPONENT;
	}


	@Override
	public void registerLayers(@NonNull MapActivity activity) {
		//cycleLayer = new CycloPluginLayer(activity, this);
	}
	@Override
	public void updateLayers(OsmandMapTileView mapView, MapActivity activity) {
		/*if (cycleLayer == null) {
			registerLayers(activity);
		}
		cycleLayer.refreshLayer();*/
	}
	@Override
	protected List<QuickActionType> getQuickActionTypes() {
		List<QuickActionType> quickActionTypes = new ArrayList<>();
		quickActionTypes.add(NextNavigationAction.TYPE);
		quickActionTypes.add(ZoomInAction.TYPE);
		quickActionTypes.add(ZoomOutAction.TYPE);

		return quickActionTypes;
	}
	@Override
	public void  headsetButton(final MapActivity mapActivity, final int clicks){
		if(!inited)
			return;
		headsetUI.headsetButton(mapActivity,clicks);
		/*if(clicks==-1) {


				next_next=false;
				mapActivity.getMapViewTrackingUtilities().setMapLinkedToLocation(true);

				lightDown(mapActivity);
				if(zooming)
				{
					next_next=false;
					mapActivity.getMapView().setMapPosition(OsmandSettings.BOTTOM_CONSTANT );
					//mapActivity.getMapView().setIntZoom(prevZoom);
					mapActivity.getMapViewTrackingUtilities().backToLocationImpl(prevZoom,true);
					zooming=false;

				}
				mapActivity.getLockerHelper().lock();
				mapActivity.lock();


			}
		RoutingHelper routingHelper = app.getRoutingHelper();

		if(clicks==1) {

			if(app.getRendererRegistry().getCurrentSelectedRenderer().getName()!="HighContrastLowDetails")
			{
				RenderingRulesStorage loaded = app.getRendererRegistry().getRenderer("HighContrastLowDetails");
			if (loaded != null) {
				OsmandMapTileView view = mapActivity.getMapView();
				view.getSettings().RENDERER.set("HighContrastLowDetails");
				app.getRendererRegistry().setCurrentSelectedRender(loaded);
				ConfigureMapMenu.refreshMapComplete(mapActivity);
				mapActivity.getDashboard().refreshContent(true);
				}
			}

				if(valueTV==null) {
					//widget = new QuickActionsWidget(mapActivity);
					//widget.setVisibility(View.VISIBLE);
					valueTV = new CycloUIWidget(mapActivity);
					List<QuickAction> l= new ArrayList<QuickAction>();
					l.add(new MarkerAction());
					l.add(new MarkerAction());
					valueTV.setActions(l);
					//valueTV.setId(5);

					DrawerLayout fl = (DrawerLayout) mapActivity.findViewById(R.id.drawer_layout);
					//FrameLayout fl = (FrameLayout) mapActivity.findViewById(R.id.MapHudButtonsOverlay);
					cycle_view = mapActivity.getLayoutInflater().inflate(R.layout.cycletouring_frame, null);
					View myView=cycle_view.findViewById(R.id.center_layout);

					FrameLayout.LayoutParams params=new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT, Gravity.BOTTOM);
					valueTV.setLayoutParams(params);
					fl.addView(cycle_view);
					myView.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {

							Toast.makeText(mapActivity,"clicked 2",Toast.LENGTH_SHORT).show();
							cycle_view.setVisibility(View.GONE);
							mapActivity.findViewById(R.id.MapHudButtonsOverlay).setVisibility(View.VISIBLE);

						}
					});
					myView.setClickable(false);
					myView.setEnabled(false);
					mapActivity.findViewById(R.id.MapHudButtonsOverlay).setVisibility(View.INVISIBLE);

				}
				valueTV.setVisibility(View.VISIBLE);



				if(zooming)
				{
					next_next=false;
					//getMapView().setMapPosition(OsmandSettings.BOTTOM_CONSTANT );
					//mapActivity.getMapView().setIntZoom(prevZoom);
					cycle_view.setVisibility(View.INVISIBLE);

					mapActivity.getMapViewTrackingUtilities().backToLocationImpl(prevZoom,true);
					zooming=false;

				}
				else {
					lightUp(mapActivity);
				}


			}
			if(clicks==2&&routingHelper!=null)
			{


				try {
					mapActivity.getMapViewTrackingUtilities().setMapLinkedToLocation(false);

					mapActivity.getMapView().setMapPosition(OsmandSettings.CENTER_CONSTANT);
					mapActivity.refreshMap();
					if (!next_next) {
						NextDirectionInfo r = routingHelper.getNextRouteDirectionInfo(nextDirection, false);
						distance=r.distanceTo;
						List<Location> loc = routingHelper.getCurrentCalculatedRoute();
						Location l = loc.get(r.directionInfo.routePointOffset);
						prevZoom = mapActivity.getMapView().getZoom();
						zooming = true;
						Location lEnd=findEndAction(r,routingHelper);
						mapActivity.getMapView().setLatLon(l.getLatitude(), l.getLongitude());
						mapActivity.getMapView().setRotate(computeRotation(loc, r.directionInfo.routePointOffset, 0), true);
						mapActivity.getMapView().setIntZoom(20);

						RotatedTileBox tb = mapActivity.getMapView().getCurrentRotatedTileBox().copy();
						tb.setLatLonCenter(l.getLatitude(), l.getLongitude());

						while (tb.containsLatLon(lEnd.getLatitude(), lEnd.getLongitude())==false) {
							tb.setZoom(tb.getZoom() -1);
						}
						mapActivity.getMapView().setIntZoom(tb.getZoom());

						nextDirection = r;
						next_next = true;


						Log.d("Route", r.toString());
					} else {

						NextDirectionInfo r2 = routingHelper.getNextRouteDirectionInfoAfter(nextDirection, new NextDirectionInfo(), false);
						List<Location> loc = routingHelper.getCurrentCalculatedRoute();
						//Location lPrev=loc.get( nextDirection.directionInfo.routePointOffset);
						distance+=r2.distanceTo;

						Location l = loc.get(r2.directionInfo.routePointOffset);

						mapActivity.getMapView().setLatLon(l.getLatitude(), l.getLongitude());
						mapActivity.getMapView().setRotate(computeRotation(loc, r2.directionInfo.routePointOffset, nextDirection.directionInfo.routePointOffset), true);
						next_next = true;
						nextDirection = r2;
						mapActivity.getMapView().setIntZoom(20);
						RotatedTileBox tb = mapActivity.getMapView().getCurrentRotatedTileBox().copy();
						tb.setLatLonCenter(l.getLatitude(), l.getLongitude());


						Location lEnd=findEndAction(r2,routingHelper);
						while (tb.containsLatLon(lEnd.getLatitude(), lEnd.getLongitude())==false) {
							tb.setZoom(tb.getZoom() -1);
						}
						mapActivity.getMapView().setIntZoom(tb.getZoom());

						Log.d("Route", r2.toString());
					}
					TextView textView =  (TextView) cycle_view.findViewById(R.id.bottom_text);
					if(distance >1000)
					{
						textView.setText(String.format("%.1fkm", distance/1000));
					}
					else
					{
						textView.setText(String.format("%.0fm", distance));
					}
					cycle_view.setVisibility(View.VISIBLE);

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

			}*/
	}
	@Nullable
	private RoutingHelper routingHelper(){
		return app.getRoutingHelper();
	}
	private Location location(int i){

		return routingHelper().getCurrentCalculatedRoute().get(i);

	}
/*	public void fitRectOnMap(QuadRect rect,MapActivity mapActivity,NextDirectionInfo dir) {
		Location l=location()
		left = p.getLongitude();
		right = p.getLongitude();
		top = p.getLatitude();
		bottom = p.getLatitude();
		} else {
		left = Math.min(left, p.getLongitude());
		right = Math.max(right, p.getLongitude());
		top = Math.max(top, p.getLatitude());
		bottom = Math.min(bottom, p.getLatitude());

			RotatedTileBox tb = mapActivity.getMapView().getCurrentRotatedTileBox().copy();
			int tileBoxWidthPx = 0;
			int tileBoxHeightPx = 0;

			tileBoxWidthPx = tb.getPixWidth();

			tileBoxHeightPx = tb.getPixHeight();

				mapActivity.getMapView().fitRectToMap(rect.left, rect.right, rect.top, rect.bottom,
						tileBoxWidthPx, tileBoxHeightPx, 40);


	}*/
	@Override
	public void mapActivityPause(MapActivity activity) {
		if(!inited)
			return;
		activity.startService(new Intent(activity, PlayerService.class));
		headsetUI.resetState(activity);

	}

	@Override
	public void mapActivityDestroy(MapActivity activity) {
		if(!inited)
			return;
		activity.stopService(new Intent(activity, PlayerService.class));

	}

	@Override
	public void mapActivityResume(MapActivity activity) {
		if(!inited)
			return;
		activity.stopService(new Intent(activity, PlayerService.class));
		WindowManager.LayoutParams layout = activity.getWindow().getAttributes();

		layout.screenBrightness =brightness;
		if(headsetUI!=null)
			headsetUI.lightUp(activity);

		activity.getWindow().setAttributes(layout);
	}
	private Boolean screenOff=false;
	@Override
	public void mapActivityScreenOff(MapActivity activity) {
		if(!inited)
			return;
		screenOff=true;
		activity.startService(new Intent(activity, PlayerService.class));
		headsetUI.resetState(activity);

	}

	@Override
	public boolean init(@NonNull final OsmandApplication _app, Activity activity) {
		app=_app;
		OsmandSettings settings = app.getSettings();
		//app.startService(new Intent(app, PlayerService.class));
		headsetUI= new HeadsetUI(_app);

		inited=true;
		return true;
	}



	@Override
	public void disable(OsmandApplication app) {
	}


}

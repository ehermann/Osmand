package net.osmand.plus.cycloplugin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;

import net.osmand.Location;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.dialogs.ConfigureMapMenu;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;
import net.osmand.plus.routing.RouteCalculationResult;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.render.RenderingRulesStorage;

import java.util.ArrayList;
import java.util.List;

public class NextNavigationAction extends CycleTourQuickAction {
    private transient RouteCalculationResult.NextDirectionInfo nextDirection=null;
    private transient View view=null;
    State state=new State();
    double distance=0.0;
    public static final QuickActionType TYPE = new QuickActionType(2,
            "navigation.next", NextNavigationAction.class).nameRes(R.string.map_widget_next_turn).iconRes(R.drawable.ic_action_next_turn).
            category(QuickActionType.NAVIGATION). nonEditable();


    public NextNavigationAction() {
        super(TYPE);
    }
    public NextNavigationAction(QuickAction quickAction)
    {

            super(quickAction);
    }
    RouteCalculationResult.NextDirectionInfo getAction(int i, RoutingHelper routingHelper, RouteCalculationResult.NextDirectionInfo rPrev)
    {

        while(i<rPrev.directionInfo.routePointOffset){
            RouteCalculationResult.NextDirectionInfo rNext = routingHelper.getNextRouteDirectionInfoAfter(rPrev, new RouteCalculationResult.NextDirectionInfo(), false);
            rPrev=rNext;
        }
        if(rPrev.directionInfo.routePointOffset==i)
            return rPrev;
        else
            return null;
    }
    private Location calculateProjection(double part, Location lp, Location l) {
        Location p = new Location(l);
        part=part/lp.distanceTo(l);
        p.setLatitude(lp.getLatitude() + part * (l.getLatitude() - lp.getLatitude()));
        p.setLongitude(lp.getLongitude() + part * (l.getLongitude() - lp.getLongitude()));
        return p;
    }

    private List<Location> findEndAction(RouteCalculationResult.NextDirectionInfo rPrev, RoutingHelper routingHelper,int zoom){
        double DISTANCE_ACTION = 35;
        if(zoom >= 17) {
            DISTANCE_ACTION = 15;
        } else if (zoom == 15) {
            DISTANCE_ACTION = 70;
        } else if (zoom < 15) {
            DISTANCE_ACTION = 110;
        }        List<Location> loc = routingHelper.getCurrentCalculatedRoute();
        //TODO: folow locs, if distance > ACTION, project remaining,
        int i=rPrev.directionInfo.routePointOffset;
        double distance=0;
        ArrayList<Location> res= new ArrayList<Location>();

        res.add(0,new Location(loc.get(i)));;
        res.add(1,new Location(loc.get(i)));

        for(; i< loc.size();i++)
        {
            RouteCalculationResult.NextDirectionInfo action=getAction(i,routingHelper,rPrev);
            if(action!=null) {
                rPrev = action;
                distance=0;
            }
            double dist=loc.get(rPrev.directionInfo.routePointOffset).distanceTo(loc.get(i));
            distance+=dist;
            if(distance>DISTANCE_ACTION) {
                distance-=dist;
                break;
            }
            else{
                distance=0;
            }
            updateBounds(res,loc.get(i));


        }
        Location proj=calculateProjection(DISTANCE_ACTION-distance, loc.get(i-1),loc.get(i));
        updateBounds(res,proj);
        return res;

    }
    private void updateBounds(ArrayList<Location> res,Location l)
    {
        Location min=res.get(0);
        Location max=res.get(1);

        if(l.getLatitude()<min.getLatitude()){
            min.setLatitude(l.getLatitude());
        }
        if(l.getLongitude()<min.getLongitude()){
            min.setLongitude(l.getLongitude());
        }
        if(l.getLatitude()>max.getLatitude()){
            max.setLatitude(l.getLatitude());
        }
        if(l.getLongitude()>max.getLongitude()){
            max.setLongitude(l.getLongitude());
        }
    }
    private float computeRotation(List<Location> loc, int current,int previous)
    {
        Location currentLocation=loc.get(current);;



        previous=Math.max(previous,0);
        int reference = current;
        double dist=0;
        while(dist <20.F &&reference>=previous)
        {
            dist=currentLocation.distanceTo(loc.get(reference));
            reference--;
        }

        Location referenceLocation=loc.get(reference+1);
        return -referenceLocation.bearingTo(currentLocation);
    }
    public OsmandApplication getApp(MapActivity mapActivity) {
        return ((OsmandApplication) mapActivity.getApplicationContext());
    }
class State{
        int prevZoom;
        int mapPosition;
        boolean mapLinkedToLocation;
        int hudVisibility;
        boolean followingMode;
        OsmandSettings.DayNightMode dayNightMode;
    RenderingRulesStorage mapStyle;
        public void save(MapActivity mapActivity){
            mapPosition=mapActivity.getMapView().getMapPosition();

            prevZoom = mapActivity.getMapView().getZoom();
            mapLinkedToLocation= mapActivity.getMapViewTrackingUtilities().isMapLinkedToLocation();
            mapStyle=getApp(mapActivity).getRendererRegistry().getCurrentSelectedRenderer();
            hudVisibility=mapActivity.findViewById(R.id.MapHudButtonsOverlayTop).getVisibility();
            followingMode=getApp(mapActivity).getRoutingHelper().isFollowingMode();
            dayNightMode=mapActivity.getMyApplication().getSettings().DAYNIGHT_MODE.get();

        }
        public void restore(MapActivity mapActivity){
            mapActivity.getMapView().setIntZoom(prevZoom);
            mapActivity.getMapView().setMapPosition(mapPosition);
            mapActivity.getMapViewTrackingUtilities().setMapLinkedToLocation(mapLinkedToLocation);
            getApp(mapActivity).getRendererRegistry().setCurrentSelectedRender(mapStyle);
            ConfigureMapMenu.refreshMapComplete(mapActivity);
            mapActivity.getDashboard().refreshContent(true);
            mapActivity.findViewById(R.id.MapHudButtonsOverlayTop).setVisibility(hudVisibility);
            getApp(mapActivity).getRoutingHelper().setFollowingMode(followingMode);
            mapActivity.getMyApplication().getSettings().DAYNIGHT_MODE.set(dayNightMode);

        }
}
    @Override
    void destroy(MapActivity mapActivity) {
        if(nextDirection!=null)
            state.restore(mapActivity);
        nextDirection=null;
        distance=0;
    }
    @Nullable
    RouteCalculationResult.NextDirectionInfo getNextDirectionInfo(MapActivity mapActivity) {
            RoutingHelper routingHelper = ((OsmandApplication) mapActivity.getApplicationContext()).getRoutingHelper();
            RouteCalculationResult.NextDirectionInfo r;
            if(routingHelper==null)
                return null;

            if (nextDirection == null) {
                r = routingHelper.getNextRouteDirectionInfo(new RouteCalculationResult.NextDirectionInfo(), false);
                distance = r.distanceTo;

            }
            else{
                r = routingHelper.getNextRouteDirectionInfoAfter(nextDirection, new RouteCalculationResult.NextDirectionInfo(), false);

            }
            return r;
    }
    private void setupStyle(MapActivity mapActivity)
    {
        OsmandApplication app=getApp(mapActivity);
        if(app.getRendererRegistry().getCurrentSelectedRenderer().getName()!="HighContrastLowDetails")
        {
            RenderingRulesStorage loaded = app.getRendererRegistry().getRenderer("HighContrastLowDetails");
            if (loaded != null) {
                OsmandMapTileView view = mapActivity.getMapView();
               // view.getSettings().RENDERER.set("HighContrastLowDetails");
                app.getRendererRegistry().setCurrentSelectedRender(loaded);
              //  ConfigureMapMenu.refreshMapComplete(mapActivity);
                //mapActivity.getDashboard().refreshContent(true);
            }
        }
      //  mapActivity.findViewById(R.id.MapHudButtonsOverlayTop).setVisibility(View.INVISIBLE);
        getApp(mapActivity).getRoutingHelper().setFollowingMode(false);
        mapActivity.getMyApplication().getSettings().DAYNIGHT_MODE.set(OsmandSettings.DayNightMode.NIGHT);


    }
    private boolean locationsVisible(MapActivity mapActivity, RotatedTileBox tb,List<Location> l)
    {

        View topView =  view.findViewById(R.id.top_container);
        int[] ls = new int[2];
        topView.getLocationOnScreen(ls);
        int dy=ls[1]+topView.getHeight();
        for (Location li : l) {

            double tx = tb.getPixXFromLatLon(li.getLatitude(), li.getLongitude());
            double ty = tb.getPixYFromLatLon(li.getLatitude(), li.getLongitude())+dy;
            boolean res= tx >= 0 && tx <= tb.getPixWidth() && ty >= 0 && ty <= tb.getPixHeight();
            if(!res)
                return false;
        }
        return true;
    }

    @Override
    public void execute(MapActivity mapActivity) {
        setupStyle(mapActivity);
        RoutingHelper routingHelper = ((OsmandApplication) mapActivity.getApplicationContext()).getRoutingHelper();
        if(routingHelper==null)
        {
            state.restore(mapActivity);
            return;
        }
        try {

            mapActivity.getMapViewTrackingUtilities().setMapLinkedToLocation(false);

            mapActivity.getMapView().setMapPosition(OsmandSettings.CENTER_CONSTANT);
            mapActivity.refreshMap();
            RouteCalculationResult.NextDirectionInfo r=getNextDirectionInfo(mapActivity);
            if (r!=null) {
                distance=r.distanceTo;
                List<Location> loc = routingHelper.getCurrentCalculatedRoute();
                Location l = loc.get(r.directionInfo.routePointOffset);
                List<Location> lEnd=findEndAction(r,routingHelper,20);
                mapActivity.getMapView().setLatLon(l.getLatitude(), l.getLongitude());
                mapActivity.getMapView().setRotate(computeRotation(loc, r.directionInfo.routePointOffset, 0), true);
                mapActivity.getMapView().setIntZoom(20);

                RotatedTileBox tb = mapActivity.getMapView().getCurrentRotatedTileBox().copy();

                while (locationsVisible(mapActivity,tb,lEnd)==false) {
                    tb.setZoom(tb.getZoom() -1);
                    lEnd=findEndAction(r,routingHelper,tb.getZoom() );
                }
                mapActivity.getMapView().setIntZoom(tb.getZoom());

                nextDirection = r;


              //  Log.d("Route", r.toString());
            }
            TextView textView =  (TextView) view.findViewById(R.id.bottom_text);
            if(distance >1000)
            {
                textView.setText(String.format("%.1fkm", distance/1000));
            }
            else
            {
                textView.setText(String.format("%.0fm", distance));
            }
            textView =  (TextView) view.findViewById(R.id.top_text);
            textView.setText(r.directionInfo.getStreetName());


        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

    }

    @Override
    public void drawUI(ViewGroup parent, MapActivity activity) {
        RoutingHelper routingHelper = ((OsmandApplication) activity.getApplicationContext()).getRoutingHelper();
        if(routingHelper==null)
            return;
        state.save(activity);
      //  if (view==null)
            view = LayoutInflater.from(parent.getContext())
        .inflate(R.layout.cycletouring_frame, null);

        parent.addView(view);
    }
}

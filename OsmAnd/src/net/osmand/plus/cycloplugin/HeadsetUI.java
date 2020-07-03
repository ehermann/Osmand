package net.osmand.plus.cycloplugin;

import android.content.Intent;
import android.content.res.Resources;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.drawerlayout.widget.DrawerLayout;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionRegistry;
import net.osmand.plus.routing.RoutingHelper;

import java.util.List;

public class HeadsetUI extends FrameLayout {
    private boolean inAction = false;
    private OsmandApplication app;
    private boolean init = false;
    private int currentAction = 0;
    private boolean awake = true;

    public HeadsetUI(OsmandApplication _app) {
        super(_app);
        app = _app;
    }


    public void init(MapActivity mapActivity) {
        if (init)
            return;
        //TODO: avoid setting layout every time
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.BOTTOM);
        this.setLayoutParams(params);

        DrawerLayout fl = (DrawerLayout) mapActivity.findViewById(R.id.drawer_layout);
        fl.addView(this);

        init = true;

    }
    public void resetState(MapActivity mapActivity) {
        if (!inAction)
            return;
        QuickAction qa = getQuickAction(currentAction);

        if (inAction&&qa instanceof CycleTourQuickAction) {
            CycleTourQuickAction ctqa = (CycleTourQuickAction) qa;
            ctqa.destroy(mapActivity);
            inAction = false;
        }
        removeAllViews();
        final android.view.ViewParent parent = this.getParent();

        if (parent instanceof android.view.ViewManager) {
            final android.view.ViewManager viewManager = (android.view.ViewManager) parent;

            viewManager.removeView(this);
        }
        inAction = false;
        init=false;

    }
    private void longPress(MapActivity mapActivity) {
       resetState(mapActivity);
    }

    public void lightUp(MapActivity mapActivity) {
        WindowManager.LayoutParams layout = mapActivity.getWindow().getAttributes();
        toastAction(mapActivity,currentAction);

        layout.screenBrightness = 1.0F;
        RoutingHelper routingHelper = ((OsmandApplication) mapActivity.getApplicationContext()).getRoutingHelper();
        awake = true;

        if (routingHelper != null)
            routingHelper.setPauseNavigation(false);
        mapActivity.getWindow().setAttributes(layout);

    }

    public void lightDown(MapActivity mapActivity) {
        WindowManager.LayoutParams layout = mapActivity.getWindow().getAttributes();

        //brightness = layout.screenBrightness;
        layout.screenBrightness = 0.01F;
        mapActivity.getLockerHelper().lock();
        mapActivity.startService(new Intent(mapActivity, PlayerService.class));

        mapActivity.getWindow().setAttributes(layout);
        awake = false;
    }

    private void holding(MapActivity mapActivity) {
        RoutingHelper routingHelper = ((OsmandApplication) mapActivity.getApplicationContext()).getRoutingHelper();

        lightDown(mapActivity);
    }

    public void toastAction(MapActivity mapActivity, int action)
    {
    QuickAction qa = getQuickAction(action);

       if(qa!=null) {
            Toast.makeText(mapActivity, qa.getName(mapActivity), Toast.LENGTH_LONG).show();

            return;
        }
    /*Toast toast= Toast.makeText(mapActivity,           qa.getName(mapActivity) , Toast.LENGTH_LONG);
     RelativeLayout toastLayout = (RelativeLayout) toast.getView();
     TextView toastTV = (TextView) toastLayout.getChildAt(0);
     toastTV.setTextSize(30);
     toast.show();
     */
    // ImageView view = new ImageView(mapActivity);
    Toast toast = new Toast(mapActivity);
            toast.setDuration(Toast.LENGTH_SHORT);
    //view.setImageResource(qa.getIconRes());
    LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(Resources.getSystem().getDisplayMetrics().widthPixels, Resources.getSystem().getDisplayMetrics().heightPixels);
    View view = LayoutInflater.from(mapActivity)
            .inflate(R.layout.cycletour_toast, null);
    TextView textView = (TextView) view.findViewById(R.id.bottom_toast_text);
            textView.setText(qa.getName(mapActivity));
    ImageView imageView = (ImageView) view.findViewById(R.id.top_toast_image);
            imageView.setImageResource(qa.getIconRes());
    FrameLayout fl = new FrameLayout(mapActivity);
            view.setLayoutParams(layoutParams);

            fl.setLayoutParams(layoutParams);
            fl.addView(view);
            toast.setView(fl);
            toast.show();
}
    private void click(MapActivity mapActivity)
    {
        if(!awake)
        {
            lightUp(mapActivity);
            return;
        }
     //   if(!inAction)
        {
            toastAction(mapActivity,currentAction+1);

            resetState(mapActivity);
            currentAction=currentAction+1;


            return;
        }
       /* QuickAction qa=getQuickAction(currentAction);

        if(qa instanceof CycleTourQuickAction) {
            CycleTourQuickAction ctqa = (CycleTourQuickAction) qa;
            ctqa.singleClick(mapActivity);
        }
*/

    }
    private QuickAction getQuickAction(int i){
        QuickActionRegistry actionRegistry = app.getQuickActionRegistry();
        List<QuickAction>  actions = actionRegistry.getQuickActions();
        if(actions.size()==0)
        {
            return new NextNavigationAction();
        }
        i=i%actions.size();
        QuickAction qa=actions.get(i);
        return qa;
    }

    private void doubleClick(MapActivity mapActivity)
    {
        init(mapActivity);

        QuickAction qa=getQuickAction(currentAction);
        if(qa==null){
            currentAction=0;
            return;
        }
        if(inAction==false)
            qa.drawUI(this,mapActivity);
        inAction=true;
        qa.execute(mapActivity);
    }
    private void multiClick(MapActivity mapActivity, int d)
    {
        QuickActionRegistry actionRegistry = app.getQuickActionRegistry();
        List<QuickAction>  actions = actionRegistry.getQuickActions();
        int i=d-2;
        if(i<actions.size())
        {
            actions.get(i).execute(mapActivity);
        }
    }

    public void  headsetButton(final MapActivity mapActivity, final int clicks) {
        switch (clicks) {
            case -1:
                holding(mapActivity);
                break;
            case 0:
                longPress(mapActivity);
                break;
            case 1:

                click(mapActivity);

                break;
            case 2:
                doubleClick(mapActivity);
                break;
            default:
                multiClick(mapActivity, clicks);
                break;
        }
    }
   /* public void  headsetButton(final MapActivity mapActivity, final int clicks){
        if(clicks==-1) {


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
                    RouteCalculationResult.NextDirectionInfo r = routingHelper.getNextRouteDirectionInfo(nextDirection, false);
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

                    RouteCalculationResult.NextDirectionInfo r2 = routingHelper.getNextRouteDirectionInfoAfter(nextDirection, new RouteCalculationResult.NextDirectionInfo(), false);
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

        }
    }*/
}

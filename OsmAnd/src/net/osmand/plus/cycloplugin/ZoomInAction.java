package net.osmand.plus.cycloplugin;

import android.view.ViewGroup;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;

public class ZoomInAction extends CycleTourQuickAction {
    public static final QuickActionType TYPE = new QuickActionType(120,
            "zoom.in", ZoomInAction.class).nameRes(R.string.zoomIn).iconRes(R.drawable.ic_zoomin_action).
            category(QuickActionType.NAVIGATION).nonEditable();

    private int originalZoom;
    public ZoomInAction() {
        super(TYPE);
    }

    public ZoomInAction(QuickAction quickAction) {

        super(quickAction);
    }

    @Override
    void destroy(MapActivity mapActivity) {
        mapActivity.getMapView().setIntZoom(originalZoom);

    }
    @Override
   public void execute(MapActivity mapActivity) {
        int z = mapActivity.getMapView().getZoom()+1;
        if(z>20)
            z=7;
        mapActivity.getMapView().setIntZoom(z);
    }


    @Override
    public void drawUI(ViewGroup parent, MapActivity mapActivity) {

        originalZoom= mapActivity.getMapView().getZoom();
    }
}

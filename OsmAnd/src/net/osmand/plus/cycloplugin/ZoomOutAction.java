package net.osmand.plus.cycloplugin;

import android.view.ViewGroup;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;

public class ZoomOutAction extends CycleTourQuickAction {
    public static final QuickActionType TYPE = new QuickActionType(121,
            "zoom.out", ZoomOutAction.class).nameRes(R.string.zoomOut).iconRes(R.drawable.ic_zoomout_action).
            category(QuickActionType.NAVIGATION).nonEditable();

    private int originalZoom;
    public ZoomOutAction() {
        super(TYPE);
    }

    public ZoomOutAction(QuickAction quickAction) {

        super(quickAction);
    }

    @Override
    void destroy(MapActivity mapActivity) {
        mapActivity.getMapView().setIntZoom(originalZoom);

    }
    @Override
    public void execute(MapActivity mapActivity) {
        int z = mapActivity.getMapView().getZoom()-1;
        if(z<7)
            z=20;
        mapActivity.getMapView().setIntZoom(z);
    }



    @Override
    public void drawUI(ViewGroup parent, MapActivity mapActivity) {

        originalZoom= mapActivity.getMapView().getZoom();
    }
}

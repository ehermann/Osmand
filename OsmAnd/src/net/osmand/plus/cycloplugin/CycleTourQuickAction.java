package net.osmand.plus.cycloplugin;

import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;

public class CycleTourQuickAction extends QuickAction {

    public CycleTourQuickAction(QuickActionType type) {
        super(type);
    }
    public CycleTourQuickAction(QuickAction quickAction)
    {

        super(quickAction);
    }
    void doubleClick(MapActivity mapActivity){
        execute(mapActivity);
    }
    void singleClick(MapActivity mapActivity){}
    void destroy(MapActivity mapActivity){}
}

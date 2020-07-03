package net.osmand.plus.cycloplugin;

import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;

public class CyclePluginQuickAction extends QuickAction {
    public CyclePluginQuickAction(QuickAction quickAction)
    {

        super(quickAction);
    }
    public CyclePluginQuickAction(QuickActionType type) {
        super(type);
    }
    public void destroy(){}
}

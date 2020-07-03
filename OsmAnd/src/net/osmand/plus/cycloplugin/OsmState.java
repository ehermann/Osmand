package net.osmand.plus.cycloplugin;

import android.view.WindowManager;

import androidx.annotation.Nullable;

import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.views.OsmandMapLayer;

public class OsmState {
    @Nullable
    WindowManager.LayoutParams layout;
    public  class LayerVisibility{
        OsmandMapLayer layer;

    }
    public void  saveScreen(MapActivity mapActivity)
    {
        layout= mapActivity.getWindow().getAttributes();
    }
    public void  restoreScreen(MapActivity mapActivity)
    {
        mapActivity.getWindow().setAttributes(layout);
    }
  public void save(){

  }
  public void restore()
  {

  }
}

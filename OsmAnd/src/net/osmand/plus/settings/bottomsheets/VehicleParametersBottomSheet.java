package net.osmand.plus.settings.bottomsheets;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.mapcontextmenu.other.HorizontalSelectionAdapter;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.fragments.ApplyQueryType;
import net.osmand.plus.settings.fragments.OnConfirmPreferenceChange;
import net.osmand.plus.settings.preferences.SizePreference;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.util.Arrays;

public class VehicleParametersBottomSheet extends BasePreferenceBottomSheet {

	private static final Log LOG = PlatformUtil.getLog(VehicleParametersBottomSheet.class);
	public static final String TAG = VehicleParametersBottomSheet.class.getSimpleName();
	private String selectedItem;
	private float currentValue;

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		OsmandApplication app = getMyApplication();
		if (app == null) {
			return;
		}
		items.add(createBottomSheetItem(app));
	}

	private BaseBottomSheetItem createBottomSheetItem(OsmandApplication app) {
		final SizePreference preference = (SizePreference) getPreference();
		View mainView = UiUtilities.getMaterialInflater(app, nightMode)
				.inflate(R.layout.bottom_sheet_item_edit_with_recyclerview, null);
		String key = preference.getKey();
		TextView title = mainView.findViewById(R.id.title);
		title.setText(preference.getTitle().toString());
		String parameterName = key.substring(key.lastIndexOf("_") + 1);
		VehicleSizeAssets vehicleSizeAssets = VehicleSizeAssets.getAssets(parameterName);
		if (vehicleSizeAssets != null) {
			ImageView imageView = mainView.findViewById(R.id.image_view);
			imageView.setImageDrawable(ContextCompat.getDrawable(app,
					!nightMode ? vehicleSizeAssets.getDayIconId() : vehicleSizeAssets.getNightIconId()));
			TextView description = mainView.findViewById(R.id.description);
			description.setText(app.getString(vehicleSizeAssets.getDescriptionRes()));
		}
		final HorizontalSelectionAdapter adapter = new HorizontalSelectionAdapter(app, nightMode);
		final TextView metric = mainView.findViewById(R.id.metric);
		metric.setText(app.getString(preference.getAssets().getMetricRes()));
		final TextView text = mainView.findViewById(R.id.text_edit);
		try {
			currentValue = Float.parseFloat(preference.getValue());
		} catch (NumberFormatException e) {
			currentValue = 0.0f;
		}
		selectedItem = preference.getEntryFromValue(preference.getValue());

		String currentValueStr = currentValue == 0.0f ? "" : String.valueOf(currentValue + 0.01f);
		text.setText(currentValueStr);
		text.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}

			@Override
			public void afterTextChanged(Editable s) {
				if (!Algorithms.isEmpty(s)) {
					try {
						currentValue = Float.parseFloat(s.toString()) - 0.01f;
					} catch (NumberFormatException e) {
						currentValue = 0.0f;
					}
				} else {
					currentValue = 0.0f;
				}
				selectedItem = preference.getEntryFromValue(String.valueOf(currentValue));
				adapter.setSelectedItem(selectedItem);
			}
		});

		adapter.setItems(Arrays.asList(preference.getEntries()));
		adapter.setListener(new HorizontalSelectionAdapter.HorizontalSelectionAdapterListener() {
			@Override
			public void onItemSelected(String item) {
				selectedItem = item;
				currentValue = preference.getValueFromEntries(selectedItem);
				String currentValueStr = currentValue == 0.0f ? "" : String.valueOf(currentValue + 0.01f);
				text.setText(currentValueStr);
				adapter.notifyDataSetChanged();
			}
		});

		RecyclerView recyclerView = mainView.findViewById(R.id.recycler_view);
		recyclerView.setAdapter(adapter);
		adapter.setSelectedItem(selectedItem);
		return new BaseBottomSheetItem.Builder()
				.setCustomView(mainView)
				.create();
	}

	@Override
	protected int getRightBottomButtonTextId() {
		return R.string.shared_string_apply;
	}

	@Override
	protected void onRightBottomButtonClick() {
		Fragment target = getTargetFragment();
		if (target instanceof OnConfirmPreferenceChange) {

			((OnConfirmPreferenceChange) target).onConfirmPreferenceChange(
					getPreference().getKey(), String.valueOf(currentValue), ApplyQueryType.SNACK_BAR);
		}
		dismiss();
	}

	public static void showInstance(@NonNull FragmentManager fm, String key, Fragment target,
	                                boolean usedOnMap, @Nullable ApplicationMode appMode) {
		try {
			if (!fm.isStateSaved()) {
				Bundle args = new Bundle();
				args.putString(PREFERENCE_ID, key);
				VehicleParametersBottomSheet fragment = new VehicleParametersBottomSheet();
				fragment.setArguments(args);
				fragment.setUsedOnMap(usedOnMap);
				fragment.setAppMode(appMode);
				fragment.setTargetFragment(target, 0);
				fragment.show(fm, TAG);
			}
		} catch (RuntimeException e) {
			LOG.error("showInstance", e);
		}
	}
}

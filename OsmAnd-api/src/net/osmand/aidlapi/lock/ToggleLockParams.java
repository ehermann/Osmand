package net.osmand.aidlapi.lock;

import android.os.Parcel;

import net.osmand.aidlapi.AidlParams;

public class ToggleLockParams extends AidlParams {

	public ToggleLockParams() {

	}

	public ToggleLockParams(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<ToggleLockParams> CREATOR = new Creator<ToggleLockParams>() {
		@Override
		public ToggleLockParams createFromParcel(Parcel in) {
			return new ToggleLockParams(in);
		}

		@Override
		public ToggleLockParams[] newArray(int size) {
			return new ToggleLockParams[size];
		}
	};
}
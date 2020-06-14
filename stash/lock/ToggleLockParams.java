package net.osmand.aidl.lock;

import android.os.Parcel;
import android.os.Parcelable;

public class ToggleLockParams implements Parcelable {

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

	@Override
	public void writeToParcel(Parcel out, int flags) {
	}

	private void readFromParcel(Parcel in) {
	}

	@Override
	public int describeContents() {
		return 0;
	}
}

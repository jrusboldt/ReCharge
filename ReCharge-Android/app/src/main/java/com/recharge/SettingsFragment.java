package com.recharge;

import android.os.Bundle;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.SeekBarPreference;

public class SettingsFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);

        // Set the minimum value for the seekbar slider
        SeekBarPreference seekBar = (SeekBarPreference) findPreference("seekBar_Radius");
        seekBar.setMin(1);
    }
}

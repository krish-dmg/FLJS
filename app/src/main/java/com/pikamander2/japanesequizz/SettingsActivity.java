package com.pikamander2.japanesequizz;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.pikamander2.japanesequizz.features.NotificationScheduler;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Settings");
        }

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings_container, new GeneralPreferenceFragment())
                    .commit();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        return true;
    }

    public static class GeneralPreferenceFragment extends PreferenceFragmentCompat
            implements SharedPreferences.OnSharedPreferenceChangeListener {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.pref_general, rootKey);

            bindPreferenceSummaryToValue(findPreference("choices_list"));
            bindPreferenceSummaryToValue(findPreference("count_list"));
            bindPreferenceSummaryToValue(findPreference("theme_mode"));
            bindPreferenceSummaryToValue(findPreference("reminder_frequency"));
            bindPreferenceSummaryToValue(findPreference("notification_time"));

            Preference testPref = findPreference("send_test_notification");
            if (testPref != null) {
                testPref.setOnPreferenceClickListener(p -> {
                    if (getContext() != null) {
                        NotificationScheduler.sendNotification(getContext());
                        Toast.makeText(getContext(), "Test notification sent", Toast.LENGTH_SHORT).show();
                    }
                    return true;
                });
            }
        }

        @Override
        public void onResume() {
            super.onResume();
            SharedPreferences sp = getPreferenceManager().getSharedPreferences();
            if (sp != null) {
                sp.registerOnSharedPreferenceChangeListener(this);
            }
        }

        @Override
        public void onPause() {
            super.onPause();
            SharedPreferences sp = getPreferenceManager().getSharedPreferences();
            if (sp != null) {
                sp.unregisterOnSharedPreferenceChangeListener(this);
            }
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if ("study_reminders_enabled".equals(key) ||
                "reminder_frequency".equals(key) ||
                "notification_time".equals(key)) {
                if (getContext() != null) {
                    NotificationScheduler.scheduleReminders(getContext());
                }
            }
        }

        private void bindPreferenceSummaryToValue(Preference preference) {
            if (preference == null) return;

            preference.setOnPreferenceChangeListener((pref, value) -> {
                String stringValue = value.toString();

                if (pref instanceof ListPreference) {
                    ListPreference listPreference = (ListPreference) pref;
                    int index = listPreference.findIndexOfValue(stringValue);
                    pref.setSummary(index >= 0 ? listPreference.getEntries()[index] : null);
                } else {
                    pref.setSummary(stringValue);
                }
                return true;
            });

            // Trigger immediately with current value
            String currentValue = PreferenceManager
                    .getDefaultSharedPreferences(preference.getContext())
                    .getString(preference.getKey(), "");

            if (preference instanceof ListPreference) {
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(currentValue);
                preference.setSummary(index >= 0 ? listPreference.getEntries()[index] : null);
            } else {
                preference.setSummary(currentValue);
            }
        }
    }
}

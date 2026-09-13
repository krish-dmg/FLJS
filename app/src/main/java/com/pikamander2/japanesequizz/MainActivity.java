package com.pikamander2.japanesequizz;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import android.widget.Button;
import android.graphics.Color;


import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;

import com.google.android.material.card.MaterialCardView;
import com.pikamander2.japanesequizz.features.NotificationScheduler;

public class MainActivity extends AppCompatActivity {

    public static final String EXTRA_QUIZ_ID = "com.pikamander2.japanesequizz.MODE_ID";
    public static final String PREF_DARK_MODE = "dark_mode";

    private TextView textViewBestScore;
    private TextView textViewTotalAnswered;

    private final ActivityResultLauncher<String> requestNotificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    NotificationScheduler.scheduleReminders(this);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply dark/light mode before setContentView
        applyThemeFromPrefs();

        super.onCreate(savedInstanceState);
        
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (!prefs.contains("user_name")) {
            startActivity(new Intent(this, com.pikamander2.japanesequizz.features.OnboardingActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("");
        }

        textViewBestScore    = findViewById(R.id.textViewBestScore);
        textViewTotalAnswered = findViewById(R.id.textViewTotalAnswered);

        findViewById(R.id.textCredits).setOnClickListener(v -> {
            android.net.Uri uri = android.net.Uri.parse("https://github.com/otzua");
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(intent);
        });

        setupListeners();

        NotificationScheduler.initChannel(this);
        checkAndRequestNotificationPermission();

        // Staggered fade-in animation for cards — Apple-like entrance
        if (savedInstanceState == null) {
            animateEntrance();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStats();
        updateJlptCountdown();
        NotificationScheduler.scheduleReminders(this);
    }

    private void applyThemeFromPrefs() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String theme = prefs.getString("theme_mode", "system");
        if ("dark".equals(theme)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else if ("light".equals(theme)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }
    }

    /** Staggered fade-up entrance animation for all cards */
    private void animateEntrance() {
        int[] viewIds = {
            R.id.textTitleJapanese,
            R.id.textTitleCharacters,
            R.id.textSubtitle,
            R.id.cardJlptCountdown,
            R.id.layoutStats,
            R.id.btnFlashcards,
            R.id.btnGrammar,
            R.id.btnMockTest,
            R.id.btnStats,
            R.id.cardHiragana,
            R.id.cardKatakana,
            R.id.cardMixture,
            R.id.cardReadingHiragana,
            R.id.cardReadingKatakana,
            R.id.cardReadingMixed,
            R.id.cardReadingKanji,
            R.id.cardHiraganaChart,
            R.id.cardKatakanaChart
        };

        for (int i = 0; i < viewIds.length; i++) {
            View v = findViewById(viewIds[i]);
            if (v == null) continue;
            v.setAlpha(0f);
            final int delay = i * 40; // 40ms stagger between each card
            v.postDelayed(() -> {
                Animation anim = AnimationUtils.loadAnimation(this, R.anim.fade_in_up);
                anim.setFillAfter(true);
                v.startAnimation(anim);
                v.setAlpha(1f);
            }, delay);
        }
    }

    
    private void updateJlptCountdown() {
        TextView textDays = findViewById(R.id.textJlptCountdownDays);
        TextView textLabel = findViewById(R.id.textJlptCountdownLabel);
        if (textDays == null || textLabel == null) return;
        
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String level = prefs.getString("jlpt_level", "N5");
        String name = prefs.getString("user_name", "");
        String firstName = name.trim().isEmpty() ? "" : name.trim().split("\\s+")[0];
        
        textLabel.setText((firstName.isEmpty() ? "" : firstName + ", ") + "until next JLPT " + level);
        
        Calendar now = Calendar.getInstance();
        
        // Find next July or Dec
        Calendar july = Calendar.getInstance();
        july.set(Calendar.MONTH, Calendar.JULY);
        july.set(Calendar.DAY_OF_MONTH, 1);
        while (july.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
            july.add(Calendar.DAY_OF_MONTH, 1);
        }
        if (july.before(now)) {
            july.add(Calendar.YEAR, 1);
            july.set(Calendar.MONTH, Calendar.JULY);
            july.set(Calendar.DAY_OF_MONTH, 1);
            while (july.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
                july.add(Calendar.DAY_OF_MONTH, 1);
            }
        }
        
        Calendar dec = Calendar.getInstance();
        dec.set(Calendar.MONTH, Calendar.DECEMBER);
        dec.set(Calendar.DAY_OF_MONTH, 1);
        while (dec.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
            dec.add(Calendar.DAY_OF_MONTH, 1);
        }
        if (dec.before(now)) {
            dec.add(Calendar.YEAR, 1);
            dec.set(Calendar.MONTH, Calendar.DECEMBER);
            dec.set(Calendar.DAY_OF_MONTH, 1);
            while (dec.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
                dec.add(Calendar.DAY_OF_MONTH, 1);
            }
        }
        
        Calendar nextExam = july.before(dec) ? july : dec;
        long diff = nextExam.getTimeInMillis() - now.getTimeInMillis();
        long days = TimeUnit.MILLISECONDS.toDays(diff);
        
        textDays.setText(days + " Days");
        if (days <= 30) {
            textDays.setTextColor(Color.RED);
            findViewById(R.id.cardJlptCountdown).setBackgroundColor(Color.parseColor("#ffcccc"));
        } else {
            textDays.setTextColor(getResources().getColor(R.color.colorOnBackground));
        }
    }

    private void checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    private void updateStats() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        int bestStreak    = prefs.getInt("best_streak", 0);
        int totalAnswered = prefs.getInt("total_answered", 0);
        textViewBestScore.setText(String.valueOf(bestStreak));
        textViewTotalAnswered.setText(String.valueOf(totalAnswered));
    }

    private void setupListeners() {

        View btnFlashcards = findViewById(R.id.btnFlashcards);
        if (btnFlashcards != null) btnFlashcards.setOnClickListener(v -> startActivity(new Intent(this, com.pikamander2.japanesequizz.features.FlashcardActivity.class)));
        
        View btnGrammar = findViewById(R.id.btnGrammar);
        if (btnGrammar != null) btnGrammar.setOnClickListener(v -> startActivity(new Intent(this, com.pikamander2.japanesequizz.features.GrammarActivity.class)));
        
        View btnMockTest = findViewById(R.id.btnMockTest);
        if (btnMockTest != null) btnMockTest.setOnClickListener(v -> startActivity(new Intent(this, com.pikamander2.japanesequizz.features.MockTestActivity.class)));
        
        View btnStats = findViewById(R.id.btnStats);
        if (btnStats != null) btnStats.setOnClickListener(v -> startActivity(new Intent(this, com.pikamander2.japanesequizz.features.StatsActivity.class)));

        MaterialCardView cardHiragana = findViewById(R.id.cardHiragana);
        MaterialCardView cardKatakana = findViewById(R.id.cardKatakana);
        MaterialCardView cardMixture  = findViewById(R.id.cardMixture);

        cardHiragana.setOnClickListener(v -> animateAndLaunchQuiz(v, 1));
        cardKatakana.setOnClickListener(v -> animateAndLaunchQuiz(v, 2));
        cardMixture.setOnClickListener(v  -> animateAndLaunchQuiz(v, 4));

        MaterialCardView cardReadingHiragana = findViewById(R.id.cardReadingHiragana);
        MaterialCardView cardReadingKatakana = findViewById(R.id.cardReadingKatakana);
        MaterialCardView cardReadingMixed    = findViewById(R.id.cardReadingMixed);
        MaterialCardView cardReadingKanji    = findViewById(R.id.cardReadingKanji);

        findViewById(R.id.cardHiraganaChart).setOnClickListener(v ->
                launchChart(v, ChartActivity.TYPE_HIRAGANA));
        findViewById(R.id.cardKatakanaChart).setOnClickListener(v ->
                launchChart(v, ChartActivity.TYPE_KATAKANA));

        cardReadingHiragana.setOnClickListener(v ->
                animateAndLaunchReading(v, ReadingPassage.Script.HIRAGANA.ordinal()));
        cardReadingKatakana.setOnClickListener(v ->
                animateAndLaunchReading(v, ReadingPassage.Script.KATAKANA.ordinal()));
        cardReadingMixed.setOnClickListener(v ->
                animateAndLaunchReading(v, ReadingPassage.Script.MIXED.ordinal()));
        cardReadingKanji.setOnClickListener(v ->
                animateAndLaunchReading(v, ReadingPassage.Script.KANJI.ordinal()));
    }

    private void animateAndLaunchQuiz(View view, int quizID) {
        view.startAnimation(AnimationUtils.loadAnimation(this, R.anim.card_press));
        view.postDelayed(() -> {
            Intent intent = new Intent(this, QuizActivity.class);
            intent.putExtra(EXTRA_QUIZ_ID, quizID);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        }, 120);
    }

    private void animateAndLaunchReading(View view, int scriptOrdinal) {
        view.startAnimation(AnimationUtils.loadAnimation(this, R.anim.card_press));
        view.postDelayed(() -> {
            Intent intent = new Intent(this, ReadingActivity.class);
            intent.putExtra(ReadingActivity.EXTRA_SCRIPT, scriptOrdinal);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        }, 120);
    }

    private void launchChart(View view, int chartType) {
        view.startAnimation(AnimationUtils.loadAnimation(this, R.anim.card_press));
        view.postDelayed(() -> {
            Intent intent = new Intent(this, ChartActivity.class);
            intent.putExtra(ChartActivity.EXTRA_CHART_TYPE, chartType);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        }, 120);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            return true;
        }
        // Dark mode toggle from menu
        if (item.getItemId() == R.id.action_dark_mode) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            boolean currentlyDark = prefs.getBoolean(PREF_DARK_MODE, false);
            prefs.edit().putBoolean(PREF_DARK_MODE, !currentlyDark).apply();
            AppCompatDelegate.setDefaultNightMode(
                    !currentlyDark ? AppCompatDelegate.MODE_NIGHT_YES
                                   : AppCompatDelegate.MODE_NIGHT_NO);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}

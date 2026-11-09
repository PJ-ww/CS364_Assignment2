package com.example.assignment2;

import android.Manifest;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.View;
import androidx.appcompat.app.AlertDialog;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.annotation.NonNull;

import java.util.Locale;

public class MainActivity extends AppCompatActivity
        implements SensorEventListener {

    // ==============================
    // SENSOR
    // ==============================

    private SensorManager sensorManager;

    private Sensor stepCounterSensor;
    private Sensor stepDetectorSensor;
    private Sensor accelerometerSensor;

    private boolean isSensorRegistered = false;

    // ==============================
    // STEP & SHAKE DATA
    // ==============================

    private int currentSteps = 0;
    private int currentShakes = 0;
    private long mLastShakeTime = 0;

    // ค่าเริ่มต้นจาก Step Counter
    private float initialStepCount = 0;

    private boolean hasInitialStepCount = false;

    // ==============================
    // SESSION TIMER
    // ==============================

    private long sessionStartTime;
    private long sessionStartedAt;
    private float latestStepCount;
    private boolean hasLatestStepCount;
    private SessionStore sessionStore;
    private boolean hasSavedSummary;
    private SessionStore.Summary summary = new SessionStore.Summary(0, 0, 0, 0, 0);
    private TextView tvTotalSteps, tvTotalShakes, tvHistoryEmpty;
    private TextView tvStepRecord, tvShakeRecord;

    /** Person 4: override and return the SQLite implementation. Calls run on the UI thread. */
    protected SessionStore createSessionStore() {
        return new DBHelper(this);
    }


    private final Handler handler = new Handler(Looper.getMainLooper());

    // ==============================
    // UI
    // ==============================

    private TextView tvSteps;
    private TextView tvShakes;
    private TextView tvStepsPerMinute;
    private TextView tvShakePerMinute;
    private TextView tvDuration;
    private TextView tvSessionTimer;
    private TextView tvSensorStatus;

    // ==============================
    // TIMER UPDATE
    // ==============================

    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {

            updateSessionDisplay();

            handler.postDelayed(this, 1000);
        }
    };

    // ==============================
    // ON CREATE
    // ==============================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);

        setContentView(R.layout.activity_main);

        // เชื่อม UI
        initializeViews();
        sessionStore = createSessionStore();
        findViewById(R.id.btnStart).setOnClickListener(v -> saveSession());
        findViewById(R.id.btnResetSession).setOnClickListener(v ->
                new AlertDialog.Builder(this)
                        .setTitle(R.string.reset_session)
                        .setMessage(R.string.reset_confirmation)
                        .setNegativeButton(R.string.cancel, null)
                        .setPositiveButton(R.string.confirm, (dialog, which) -> resetSession())
                        .show());

        // เชื่อม SensorManager
        sensorManager = (SensorManager)
                getSystemService(SENSOR_SERVICE);

        // ตรวจสอบ Sensor ที่โทรศัพท์มี
        stepCounterSensor = sensorManager.getDefaultSensor(
                Sensor.TYPE_STEP_COUNTER
        );

        stepDetectorSensor = sensorManager.getDefaultSensor(
                Sensor.TYPE_STEP_DETECTOR
        );

        accelerometerSensor = sensorManager.getDefaultSensor(
                Sensor.TYPE_ACCELEROMETER
        ); // Accelerometer Sensor

        // กู้คืนข้อมูลเมื่อหน้าจอถูกสร้างใหม่
        if (savedInstanceState != null) {

            sessionStartedAt = savedInstanceState.getLong("sessionStartedAt", System.currentTimeMillis());
            latestStepCount = savedInstanceState.getFloat("latestStepCount");
            hasLatestStepCount = savedInstanceState.getBoolean("hasLatestStepCount");
            currentSteps = savedInstanceState.getInt("steps");
            currentShakes = savedInstanceState.getInt("shakes", 0);

            initialStepCount = savedInstanceState.getFloat("baseline");

            hasInitialStepCount =
                    savedInstanceState.getBoolean("hasBaseline");

            sessionStartTime =
                    savedInstanceState.getLong("sessionStartTime");

        } else {

            sessionStartTime = SystemClock.elapsedRealtime();
            sessionStartedAt = System.currentTimeMillis();
        }

        // ตั้งค่า Edge-to-Edge
        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.main),
                (view, insets) -> {

                    Insets systemBars = insets.getInsets(
                            WindowInsetsCompat.Type.systemBars()
                    );

                    view.setPadding(
                            systemBars.left,
                            systemBars.top,
                            systemBars.right,
                            systemBars.bottom
                    );

                    return insets;
                }
        );

        // แสดงค่าเริ่มต้น
        updateSessionDisplay();

        // เริ่มจับเวลา
        handler.post(timerRunnable);

        // ขอ Permission
        checkActivityPermission();
    }

    // ==============================
    // INITIALIZE UI
    // ==============================

    private void initializeViews() {

        tvTotalSteps = findViewById(R.id.tvTotalSteps);
        tvTotalShakes = findViewById(R.id.tvTotalShakes);
        tvHistoryEmpty = findViewById(R.id.tvHistoryEmpty);
        tvStepRecord = findViewById(R.id.tvStepRecord);
        tvShakeRecord = findViewById(R.id.tvShakeRecord);
        tvSteps = findViewById(R.id.tvSteps);
        tvShakes = findViewById(R.id.tvShakes);

        tvStepsPerMinute =
                findViewById(R.id.tvStepsPerMinute);

        tvShakePerMinute =
                findViewById(R.id.tvShakePerMinute);

        tvDuration =
                findViewById(R.id.tvDuration);

        tvSessionTimer =
                findViewById(R.id.tvSessionTimer);

        tvSensorStatus =
                findViewById(R.id.tvSensorStatus);
    }

    // ==============================
    // CHECK PERMISSION
    // ==============================

    private void checkActivityPermission() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACTIVITY_RECOGNITION
            ) != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(
                        new String[]{
                                Manifest.permission.ACTIVITY_RECOGNITION
                        },
                        100
                );

            } else {

                registerStepSensor();
                registerShakeSensor();
            }

        } else {

            registerStepSensor();
            registerShakeSensor();
        }
    }

    // ==============================
    // REGISTER STEP & SHAKE SENSOR
    // ==============================

    private void registerStepSensor() {

        if (isSensorRegistered) {
            return;
        }

        // เลือก Step Counter ก่อน
        if (stepCounterSensor != null) {

            isSensorRegistered = sensorManager.registerListener(
                    this,
                    stepCounterSensor,
                    SensorManager.SENSOR_DELAY_NORMAL
            );

            if (isSensorRegistered) {

                tvSensorStatus.setText(
                        getString(R.string.step_counter_ready)
                );

                registerShakeSensor();
                return;
            }
        }

        // ถ้าไม่มี Step Counter ใช้ Step Detector แทน
        if (stepDetectorSensor != null) {

            hasInitialStepCount = false;

            isSensorRegistered = sensorManager.registerListener(
                    this,
                    stepDetectorSensor,
                    SensorManager.SENSOR_DELAY_NORMAL
            );

            if (isSensorRegistered) {

                tvSensorStatus.setText(
                        getString(R.string.step_detector_ready)
                );

                registerShakeSensor();
                return;
            }
        }

        tvSensorStatus.setText(
                getString(R.string.sensor_not_available)
        );

        Toast.makeText(
                this,
                "ไม่พบ Step Sensor ในโทรศัพท์",
                Toast.LENGTH_LONG
        ).show();

        registerShakeSensor();
    }

    private void registerShakeSensor() {
        if (accelerometerSensor != null) {
            sensorManager.registerListener(
                    this,
                    accelerometerSensor,
                    SensorManager.SENSOR_DELAY_UI
            );
        }
    }

    // ==============================
    // SENSOR EVENT
    // ==============================

    @Override
    public void onSensorChanged(SensorEvent event) {

        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {

            // อ่านจำนวนก้าวสะสมจากโทรศัพท์
            float totalSteps = event.values[0];
            latestStepCount = totalSteps;
            hasLatestStepCount = true;

            // กำหนดค่าเริ่มต้นครั้งแรก
            if (!hasInitialStepCount) {

                initialStepCount = totalSteps;

                hasInitialStepCount = true;

            } else {

                // คำนวณจำนวนก้าวของ Session ปัจจุบัน
                if (totalSteps >= initialStepCount) {

                    currentSteps = Math.round(
                            totalSteps - initialStepCount
                    );

                } else {

                    // กรณีโทรศัพท์รีสตาร์ต
                    initialStepCount = totalSteps;

                    currentSteps = 0;
                }
            }

        } else if (event.sensor.getType()
                == Sensor.TYPE_STEP_DETECTOR) {

            // Step Detector แจ้งเมื่อเดินครบหนึ่งก้าว
            currentSteps++;

        } else if (event.sensor.getType()
                == Sensor.TYPE_ACCELEROMETER) {

            // ตรวจจับการเขย่าด้วย Accelerometer
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            float gX = x / SensorManager.GRAVITY_EARTH;
            float gY = y / SensorManager.GRAVITY_EARTH;
            float gZ = z / SensorManager.GRAVITY_EARTH;

            // คำนวณแรง G-force รวม
            float gForce = (float) Math.sqrt(gX * gX + gY * gY + gZ * gZ);

            if (gForce > 2.2f) { // Threshold สำหรับการเขย่า
                long now = System.currentTimeMillis();
                if (mLastShakeTime + 500 > now) {
                    return; // ป้องกันการนับซ้ำถี่เกินไป
                }
                mLastShakeTime = now;
                currentShakes++;
            }
        }

        // อัปเดตหน้าจอ
        updateSessionDisplay();
    }

    // ==============================
    // UPDATE UI
    // ==============================

    private void updateSessionDisplay() {

        if (tvSteps == null) {
            return;
        }

        tvStepRecord.setVisibility(hasSavedSummary && currentSteps > summary.maxSteps ? View.VISIBLE : View.GONE);
        tvShakeRecord.setVisibility(hasSavedSummary && currentShakes > summary.maxShakes ? View.VISIBLE : View.GONE);
        findViewById(R.id.btnStart).setEnabled(currentSteps > 0 || currentShakes > 0);
        // แสดงจำนวนก้าว
        tvSteps.setText(
                String.valueOf(currentSteps)
        );

        // แสดงจำนวนครั้งการเขย่า
        if (tvShakes != null) {
            tvShakes.setText(
                    String.valueOf(currentShakes)
            );
        }

        // คำนวณเวลาที่ผ่านไป
        long elapsedMillis =
                SystemClock.elapsedRealtime() - sessionStartTime;

        long elapsedSeconds = elapsedMillis / 1000;

        // แปลงเวลาเป็น MM:SS
        String duration = formatTime(elapsedSeconds);

        tvDuration.setText(duration);

        tvSessionTimer.setText(duration);

        // คำนวณ Steps per Minute
        double stepsPerMinute = 0.0;

        if (elapsedSeconds > 0) {

            stepsPerMinute =
                    (currentSteps * 60.0) / elapsedSeconds;
        }

        tvStepsPerMinute.setText(
                String.format(
                        Locale.US,
                        "%.1f",
                        stepsPerMinute
                )
        );

        // คำนวณ Shakes per Minute
        double shakesPerMinute = 0.0;

        if (elapsedSeconds > 0) {

            shakesPerMinute =
                    (currentShakes * 60.0) / elapsedSeconds;
        }

        if (tvShakePerMinute != null) {
            tvShakePerMinute.setText(
                    String.format(
                            Locale.US,
                            "%.1f",
                            shakesPerMinute
                    )
            );
        }
    }

    private void saveSession() {
        if (currentSteps == 0 && currentShakes == 0) return;
        if (sessionStore == null) {
            Toast.makeText(this, R.string.storage_not_connected, Toast.LENGTH_LONG).show();
            return;
        }
        long durationMillis = Math.max(1, SystemClock.elapsedRealtime() - sessionStartTime);
        SessionStore.Session session = new SessionStore.Session(sessionStartedAt,
                currentSteps, currentShakes, durationMillis);
        try {
            sessionStore.insert(session);
        } catch (Exception error) {
            Toast.makeText(this, R.string.save_failed, Toast.LENGTH_LONG).show();
            return; // Keep the current round available for retry.
        }
        resetSession();
        refreshSavedSummary();
        Toast.makeText(this, R.string.save_success, Toast.LENGTH_SHORT).show();
    }

    private void resetSession() {
        currentSteps = 0;
        currentShakes = 0;
        mLastShakeTime = 0;
        // Rebase to the last absolute reading so the next event retains new steps.
        initialStepCount = latestStepCount;
        hasInitialStepCount = hasLatestStepCount;
        sessionStartTime = SystemClock.elapsedRealtime();
        sessionStartedAt = System.currentTimeMillis();
        updateSessionDisplay();
    }

    /** Also call after person 5 deletes an entry, once SQLite recalculation succeeds. */
    public void refreshSavedSummary() {
        if (sessionStore == null) {
            tvHistoryEmpty.setText(R.string.storage_not_connected);
            return;
        }
        try {
            summary = sessionStore.readSummary();
            hasSavedSummary = true;
            tvTotalSteps.setText(getString(R.string.total_steps_value, summary.totalSteps));
            tvTotalShakes.setText(getString(R.string.total_shakes_value, summary.totalShakes));
            tvHistoryEmpty.setText(summary.count == 0 ? getString(R.string.history_empty)
                    : getString(R.string.history_count, summary.count));
            updateSessionDisplay();
        } catch (Exception error) {
            tvHistoryEmpty.setText(R.string.summary_failed);
        }
    }

    // ==============================
    // FORMAT TIME
    // ==============================

    private String formatTime(long seconds) {

        long minutes = seconds / 60;

        long remainingSeconds = seconds % 60;

        return String.format(
                Locale.US,
                "%02d:%02d",
                minutes,
                remainingSeconds
        );
    }

    // ==============================
    // SENSOR ACCURACY
    // ==============================

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

        // ไม่จำเป็นต้องประมวลผลเพิ่มเติม
    }

    // ==============================
    // PERMISSION RESULT
    // ==============================

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {

        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
        );

        if (requestCode == 100) {

            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                registerStepSensor();
                registerShakeSensor();

            } else {

                tvSensorStatus.setText(
                        getString(R.string.sensor_permission_denied)
                );

                Toast.makeText(
                        this,
                        "กรุณาอนุญาต Activity Recognition เพื่อใช้งาน Step Sensor",
                        Toast.LENGTH_LONG
                ).show();

                registerShakeSensor();
            }
        }
    }

    // ==============================
    // ON RESUME
    // ==============================

    @Override
    protected void onResume() {

        super.onResume();
        refreshSavedSummary();

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
                || ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACTIVITY_RECOGNITION
        ) == PackageManager.PERMISSION_GRANTED) {

            registerStepSensor();
        }
        registerShakeSensor();
    }

    // ==============================
    // ON PAUSE
    // ==============================

    @Override
    protected void onPause() {

        super.onPause();

        if (sensorManager != null) {

            sensorManager.unregisterListener(this);

            isSensorRegistered = false;
        }
    }

    // ==============================
    // SAVE INSTANCE STATE
    // ==============================

    @Override
    protected void onSaveInstanceState(Bundle outState) {

        outState.putLong("sessionStartedAt", sessionStartedAt);
        outState.putFloat("latestStepCount", latestStepCount);
        outState.putBoolean("hasLatestStepCount", hasLatestStepCount);
        outState.putInt("steps", currentSteps);
        outState.putInt("shakes", currentShakes);

        outState.putFloat("baseline", initialStepCount);

        outState.putBoolean(
                "hasBaseline",
                hasInitialStepCount
        );

        outState.putLong(
                "sessionStartTime",
                sessionStartTime
        );

        super.onSaveInstanceState(outState);
    }

    // ==============================
    // ON DESTROY
    // ==============================

    @Override
    protected void onDestroy() {

        handler.removeCallbacks(timerRunnable);

        if (sensorManager != null) {

            sensorManager.unregisterListener(this);
        }

        super.onDestroy();
    }
}
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

    private boolean isSensorRegistered = false;

    // ==============================
    // STEP DATA
    // ==============================

    private int currentSteps = 0;

    // ค่าเริ่มต้นจาก Step Counter
    private float initialStepCount = 0;

    private boolean hasInitialStepCount = false;

    // ==============================
    // SESSION TIMER
    // ==============================

    private long sessionStartTime;

    private final Handler handler = new Handler(Looper.getMainLooper());

    // ==============================
    // UI
    // ==============================

    private TextView tvSteps;
    private TextView tvStepsPerMinute;
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

        // กู้คืนข้อมูลเมื่อหน้าจอถูกสร้างใหม่
        if (savedInstanceState != null) {

            currentSteps = savedInstanceState.getInt("steps");

            initialStepCount = savedInstanceState.getFloat("baseline");

            hasInitialStepCount =
                    savedInstanceState.getBoolean("hasBaseline");

            sessionStartTime =
                    savedInstanceState.getLong("sessionStartTime");

        } else {

            sessionStartTime = SystemClock.elapsedRealtime();
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

        tvSteps = findViewById(R.id.tvSteps);

        tvStepsPerMinute =
                findViewById(R.id.tvStepsPerMinute);

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
            }

        } else {

            registerStepSensor();
        }
    }

    // ==============================
    // REGISTER STEP SENSOR
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
    }

    // ==============================
    // SENSOR EVENT
    // ==============================

    @Override
    public void onSensorChanged(SensorEvent event) {

        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {

            // อ่านจำนวนก้าวสะสมจากโทรศัพท์
            float totalSteps = event.values[0];

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

        // แสดงจำนวนก้าว
        tvSteps.setText(
                String.valueOf(currentSteps)
        );

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

            } else {

                tvSensorStatus.setText(
                        getString(R.string.sensor_permission_denied)
                );

                Toast.makeText(
                        this,
                        "กรุณาอนุญาต Activity Recognition เพื่อใช้งาน Step Sensor",
                        Toast.LENGTH_LONG
                ).show();
            }
        }
    }

    // ==============================
    // ON RESUME
    // ==============================

    @Override
    protected void onResume() {

        super.onResume();

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
                || ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACTIVITY_RECOGNITION
        ) == PackageManager.PERMISSION_GRANTED) {

            registerStepSensor();
        }
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

        outState.putInt("steps", currentSteps);

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
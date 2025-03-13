package com.example.feedbackapp;

import static android.os.Build.*;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_QUERY_ALL_PACKAGES = 1;
    private static final String SERVER_URL = "http://10.192.40.115:5000/api/submit_bugreport";

    private ImageView appIcon;
    private TextView appName;
    private Spinner problemTypeSpinner;
    private EditText descriptionEditText;
    private TextView selectedTimeTextView;
    private String selectedAppPackage;
    private String selectedTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        appIcon = findViewById(R.id.app_icon);
        appName = findViewById(R.id.app_name);
        problemTypeSpinner = findViewById(R.id.problem_type_spinner);
        descriptionEditText = findViewById(R.id.description_edittext);
        selectedTimeTextView = findViewById(R.id.selected_time_textview);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.problem_types, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        problemTypeSpinner.setAdapter(adapter);

        findViewById(R.id.select_app_button).setOnClickListener(v -> {
            checkAndRequestPermission();
            new AppSelectionDialog().show(getSupportFragmentManager(), "AppSelectionDialog");
        });

        findViewById(R.id.select_time_button).setOnClickListener(v -> showDateTimePicker());

        findViewById(R.id.submit_button).setOnClickListener(v -> submitReport());

        Button captureBugReportButton = findViewById(R.id.capture_bug_report_button);
        if (captureBugReportButton != null) {
            captureBugReportButton.setOnClickListener(v -> captureAndUploadBugReport());
        } else {
            Toast.makeText(this, "捕获Bug Report按钮未找到", Toast.LENGTH_SHORT).show();
        }
    }

    private void checkAndRequestPermission() {
        if (VERSION.SDK_INT >= VERSION_CODES.R) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.QUERY_ALL_PACKAGES)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.QUERY_ALL_PACKAGES},
                        REQUEST_QUERY_ALL_PACKAGES);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_QUERY_ALL_PACKAGES) {
            if (grantResults.length == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "未授予查询所有包的权限，应用选择功能受限", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void showDateTimePicker() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    calendar.set(year, month, dayOfMonth);
                    TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                            (view1, hourOfDay, minute) -> {
                                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                                calendar.set(Calendar.MINUTE, minute);
                                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                                selectedTime = sdf.format(calendar.getTime());
                                selectedTimeTextView.setText(selectedTime);
                            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true);
                    timePickerDialog.show();
                }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show();
    }

    private void submitReport() {
        String selectedProblemType = problemTypeSpinner.getSelectedItem().toString();
        String description = descriptionEditText.getText().toString().trim();

        if (selectedAppPackage == null || selectedAppPackage.isEmpty()) {
            Toast.makeText(this, "请选择应用", Toast.LENGTH_SHORT).show();
            return;
        }
        if (description.isEmpty()) {
            Toast.makeText(this, "请输入问题描述", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedTime == null || selectedTime.isEmpty()) {
            Toast.makeText(this, "请选择问题时间", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            getPackageManager().getApplicationInfo(selectedAppPackage, 0);
        } catch (PackageManager.NameNotFoundException e) {
            Toast.makeText(this, "所选应用已卸载，请重新选择", Toast.LENGTH_SHORT).show();
            return;
        }

        int androidVersion = VERSION.SDK_INT;

        JSONObject json = new JSONObject();
        try {
            json.put("app_package", selectedAppPackage);
            json.put("problem_type", selectedProblemType);
            json.put("description", description);
            json.put("time", selectedTime);
            json.put("device_model", MODEL);
            json.put("android_version", androidVersion);
            Log.d("SubmitReport", "Generated JSON: " + json.toString());
        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(this, "JSON创建失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return;
        }

        new SubmitReportTask().execute(json.toString());
    }

    private void captureAndUploadBugReport() {
        Toast.makeText(this, "需要DUMP权限，请通过ADB运行 'adb shell pm grant com.example.feedbackapp android.permission.DUMP' 授权", Toast.LENGTH_LONG).show();
        new Thread(() -> {
            try {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.DUMP) != PackageManager.PERMISSION_GRANTED) {
                    runOnUiThread(() -> Toast.makeText(this, "权限未授权，功能受限", Toast.LENGTH_LONG).show());
                    return;
                }

                Process process = Runtime.getRuntime().exec("dumpstate");
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                StringBuilder bugReport = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    bugReport.append(line).append("\n");
                }
                reader.close();
                process.waitFor();

                String fileName = "bugreport_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(System.currentTimeMillis()) + ".txt";
                File bugReportFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), fileName);
                FileUtils.writeStringToFile(bugReportFile, bugReport.toString());

                uploadBugReportToServer(bugReportFile);
            } catch (IOException | InterruptedException e) {
                runOnUiThread(() -> Toast.makeText(this, "获取Bug Report失败: " + e.getMessage(), Toast.LENGTH_LONG).show());
                e.printStackTrace();
            }
        }).start();
    }

    private void uploadBugReportToServer(File bugReportFile) {
        new Thread(() -> {
            try {
                URL url = new URL(SERVER_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/octet-stream");
                conn.setDoOutput(true);

                byte[] fileContent = FileUtils.readFileToByteArray(bugReportFile);
                OutputStream os = conn.getOutputStream();
                os.write(fileContent);
                os.flush();
                os.close();

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    runOnUiThread(() -> Toast.makeText(this, "Bug Report上传成功", Toast.LENGTH_LONG).show());
                    bugReportFile.delete();
                } else {
                    runOnUiThread(() -> Toast.makeText(this, "上传失败，HTTP错误码: " + responseCode, Toast.LENGTH_LONG).show());
                }
            } catch (IOException e) {
                runOnUiThread(() -> Toast.makeText(this, "上传失败: " + e.getMessage(), Toast.LENGTH_LONG).show());
                e.printStackTrace();
            }
        }).start();
    }

    public void onAppSelected(ApplicationInfo appInfo) {
        PackageManager pm = getPackageManager();
        selectedAppPackage = appInfo.packageName;
        appName.setText(pm.getApplicationLabel(appInfo));
        appIcon.setImageDrawable(pm.getApplicationIcon(appInfo));
    }

    private class SubmitReportTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            String jsonString = params[0];
            try {
                URL url = new URL(SERVER_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setDoOutput(true);
                conn.setDoInput(true);
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                OutputStream os = conn.getOutputStream();
                if (jsonString != null && !jsonString.isEmpty()) {
                    byte[] jsonBytes = jsonString.getBytes("UTF-8");
                    Log.d("SubmitReportTask", "Writing bytes: " + new String(jsonBytes));
                    os.write(jsonBytes);
                    os.flush();
                } else {
                    return "错误：JSON字符串为空";
                }
                os.close();

                int responseCode = conn.getResponseCode();
                Log.d("SubmitReportTask", "Response code: " + responseCode);
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    return "提交成功";
                } else {
                    return "提交失败，HTTP错误码: " + responseCode;
                }
            } catch (IOException e) {
                e.printStackTrace();
                Log.e("SubmitReportTask", "写入失败: " + e.getMessage());
                return "网络连接失败: " + e.getMessage();
            }
        }

        @Override
        protected void onPostExecute(String result) {
            Toast.makeText(MainActivity.this, result, Toast.LENGTH_LONG).show();
        }
    }
}

// 静态工具类，用于文件操作
class FileUtils {
    public static void writeStringToFile(File file, String content) throws IOException {
        try (java.io.FileWriter writer = new java.io.FileWriter(file)) {
            writer.write(content);
        }
    }

    public static byte[] readFileToByteArray(File file) throws IOException {
        try (java.io.FileInputStream fis = new java.io.FileInputStream(file)) {
            byte[] buffer = new byte[(int) file.length()];
            fis.read(buffer);
            return buffer;
        }
    }
}
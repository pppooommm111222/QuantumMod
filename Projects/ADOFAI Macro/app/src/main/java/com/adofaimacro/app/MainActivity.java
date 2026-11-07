package com.adofaimacro.app;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity {
	
	private TextView tvStatus;
	private Button btnSelectFile, btnEnableService, btnStartMacro;
	private Button btnNew, btnImport, btnExport, btnTestShizuku;
	
	private static final int REQUEST_CODE_PICK_FILE = 1;
	private static final int REQUEST_CODE_IMPORT_MACRO = 2;
	private static final int REQUEST_CODE_EXPORT_MACRO = 3;
	
	private List<Double> parsedHitTimes = null;
	private List<Double> macroHitTimes = null;
	
	private static final int DELAY_MS = 6000;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		tvStatus = findViewById(R.id.tvStatus);
		btnSelectFile = findViewById(R.id.btnSelectFile);
		btnEnableService = findViewById(R.id.btnEnableService);
		btnStartMacro = findViewById(R.id.btnStartMacro);
		btnNew = findViewById(R.id.btnNew);
		btnImport = findViewById(R.id.btnImport);
		btnExport = findViewById(R.id.btnExport);
		btnTestShizuku = findViewById(R.id.btnTestShizuku);
		
		btnSelectFile.setOnClickListener(v -> {
			Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
			intent.setType("*/*");
			intent.addCategory(Intent.CATEGORY_OPENABLE);
			startActivityForResult(intent, REQUEST_CODE_PICK_FILE);
		});
		
		btnEnableService.setOnClickListener(v -> {
			Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
			startActivity(intent);
			Toast.makeText(this, "请找到 ADOFAI Macro 并开启", Toast.LENGTH_LONG).show();
		});
		
		btnStartMacro.setOnClickListener(v -> {
			List<Double> times = (macroHitTimes != null && !macroHitTimes.isEmpty())
			? macroHitTimes : parsedHitTimes;
			if (times == null || times.isEmpty()) {
				Toast.makeText(this, "请先选择谱面或导入宏文件！", Toast.LENGTH_SHORT).show();
				return;
			}
			if (AutoClickService.getInstance() == null) {
				Toast.makeText(this, "请先开启无障碍服务！", Toast.LENGTH_LONG).show();
				return;
			}
			
			Toast.makeText(this, "将在 " + (DELAY_MS / 1000) + " 秒后开始，请立刻切回游戏并手动点开始！", Toast.LENGTH_LONG).show();
			
			new Handler(Looper.getMainLooper()).postDelayed(() -> {
				AutoClickService service = AutoClickService.getInstance();
				if (service == null) return;
				float x = getResources().getDisplayMetrics().widthPixels / 2f;
				float y = getResources().getDisplayMetrics().heightPixels / 2f;
				service.setUserOffset(0);
				service.startMacro(times, x, y);
			}, DELAY_MS);
			
			moveTaskToBack(true);
		});
		
		btnNew.setOnClickListener(v -> {
			Toast.makeText(this, "新建宏功能待实现", Toast.LENGTH_SHORT).show();
		});
		
		btnImport.setOnClickListener(v -> {
			Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
			intent.setType("*/*");
			intent.addCategory(Intent.CATEGORY_OPENABLE);
			startActivityForResult(intent, REQUEST_CODE_IMPORT_MACRO);
		});
		
		btnExport.setOnClickListener(v -> {
			List<Double> times = (parsedHitTimes != null && !parsedHitTimes.isEmpty())
			? parsedHitTimes : macroHitTimes;
			if (times == null || times.isEmpty()) {
				Toast.makeText(this, "还没有可导出的数据！", Toast.LENGTH_SHORT).show();
				return;
			}
			Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
			intent.addCategory(Intent.CATEGORY_OPENABLE);
			intent.setType("*/*");
			intent.putExtra(Intent.EXTRA_TITLE, "macro.macro");
			startActivityForResult(intent, REQUEST_CODE_EXPORT_MACRO);
		});
		
		btnTestShizuku.setOnClickListener(v -> testShizuku());
	}
	
	private void testShizuku() {
		try {
			Class<?> serviceManagerClass = Class.forName("android.os.ServiceManager");
			Method getServiceMethod = serviceManagerClass.getMethod("getService", String.class);
			Object binder = getServiceMethod.invoke(null, "shizuku");
			
			if (binder == null) {
				Toast.makeText(this, "Shizuku 服务未找到（Binder 为空）", Toast.LENGTH_LONG).show();
				} else {
				Toast.makeText(this, "Shizuku 服务已找到！Binder: " + binder.getClass().getName(), Toast.LENGTH_LONG).show();
			}
			} catch (Exception e) {
			Toast.makeText(this, "测试异常: " + e.getClass().getSimpleName() + " - " + e.getMessage(), Toast.LENGTH_LONG).show();
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		if (requestCode == REQUEST_CODE_PICK_FILE && resultCode == RESULT_OK) {
			if (data != null && data.getData() != null) {
				Uri fileUri = data.getData();
				tvStatus.setText("正在解析...");
				new Thread(() -> {
					try {
						InputStream is = getContentResolver().openInputStream(fileUri);
						BufferedReader reader = new BufferedReader(new InputStreamReader(is));
						StringBuilder sb = new StringBuilder();
						String line;
						while ((line = reader.readLine()) != null) sb.append(line);
						reader.close();
						is.close();
						String content = sb.toString();
						
						List<Double> angles = parseAngleData(content);
						if (angles.isEmpty()) {
							runOnUiThread(() -> tvStatus.setText("未找到 angleData"));
							return;
						}
						
						double bpm = parseDoubleField(content, "bpm", 120.0);
						double offset = parseDoubleField(content, "offset", 0.0);
						
						List<Double> hitTimes = new ArrayList<>();
						double currentTime = offset;
						for (double angle : angles) {
							currentTime += (1000.0 * angle) / (3.0 * bpm);
							hitTimes.add(currentTime);
						}
						parsedHitTimes = hitTimes;
						macroHitTimes = null;
						
						runOnUiThread(() -> tvStatus.setText("解析成功！\nBPM: " + bpm + "\n音符数: " + hitTimes.size()));
						
						} catch (Exception e) {
						runOnUiThread(() -> tvStatus.setText("解析失败: " + e.getMessage()));
					}
				}).start();
			}
		}
		
		if (requestCode == REQUEST_CODE_IMPORT_MACRO && resultCode == RESULT_OK) {
			if (data != null && data.getData() != null) {
				Uri fileUri = data.getData();
				new Thread(() -> {
					try {
						InputStream is = getContentResolver().openInputStream(fileUri);
						BufferedReader reader = new BufferedReader(new InputStreamReader(is));
						List<Double> hitTimes = new ArrayList<>();
						String line;
						while ((line = reader.readLine()) != null) {
							line = line.trim();
							if (line.isEmpty()) continue;
							String[] parts = line.split("\\|");
							if (parts.length >= 2) {
								try {
									double ms = Double.parseDouble(parts[1].trim());
									hitTimes.add(ms);
								} catch (Exception ignored) {}
							}
						}
						reader.close();
						is.close();
						
						macroHitTimes = hitTimes;
						parsedHitTimes = null;
						
						runOnUiThread(() -> tvStatus.setText("宏导入成功！\n音符数: " + hitTimes.size()));
						
						} catch (Exception e) {
						runOnUiThread(() -> tvStatus.setText("导入失败: " + e.getMessage()));
					}
				}).start();
			}
		}
		
		if (requestCode == REQUEST_CODE_EXPORT_MACRO && resultCode == RESULT_OK) {
			if (data != null && data.getData() != null) {
				Uri fileUri = data.getData();
				List<Double> times = (parsedHitTimes != null && !parsedHitTimes.isEmpty())
				? parsedHitTimes : macroHitTimes;
				new Thread(() -> {
					try {
						OutputStream os = getContentResolver().openOutputStream(fileUri);
						StringBuilder sb = new StringBuilder();
						double prev = 0;
						for (Double t : times) {
							double interval = t - prev;
							prev = t;
							sb.append("120 | ").append(interval).append("\n");
						}
						os.write(sb.toString().getBytes());
						os.close();
						
						runOnUiThread(() -> Toast.makeText(this, "导出成功！", Toast.LENGTH_SHORT).show());
						} catch (Exception e) {
						runOnUiThread(() -> Toast.makeText(this, "导出失败: " + e.getMessage(), Toast.LENGTH_LONG).show());
					}
				}).start();
			}
		}
	}
	
	private List<Double> parseAngleData(String content) {
		List<Double> angles = new ArrayList<>();
		Pattern p = Pattern.compile("\"angleData\"\\s*:\\s*\\[([^\\]]+)\\]");
		Matcher m = p.matcher(content);
		if (m.find()) {
			String[] parts = m.group(1).split(",");
			for (String s : parts) {
				s = s.trim();
				if (!s.isEmpty()) {
					try { angles.add(Double.parseDouble(s)); } catch (Exception ignored) {}
				}
			}
		}
		return angles;
	}
	
	private double parseDoubleField(String content, String key, double def) {
		Pattern p = Pattern.compile("\"" + key + "\"\\s*:\\s*([0-9.\\-]+)");
		Matcher m = p.matcher(content);
		if (m.find()) {
			try { return Double.parseDouble(m.group(1)); } catch (Exception ignored) {}
		}
		return def;
	}
}
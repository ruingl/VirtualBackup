package com.virtualbackup;

import java.io.File;
import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements OnClickListener {
	public static final String TAG = "VirtualBackup";
	
	private String dir;
	private TextView status;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		dir = Environment.getExternalStorageDirectory() + "/VirtualBackup";
		
		TextView t = findViewById(R.id.info);
		t.setText(getResources().getString(R.string.backup_dir) + ": " + dir + "\n\n" + getFilesDir());
		
		status = findViewById(R.id.status);
		
		Button backup = findViewById(R.id.backup);
		backup.setOnClickListener(this);
		
		Button restore = findViewById(R.id.restore);
		restore.setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		final boolean restore = v.getId() == R.id.restore;
		
		final String files = getFilesDir().getAbsolutePath();
		final String own = getPackageName();
		
		int pos = files.indexOf(own);
		String root = files.substring(0, pos);
		
		ArrayList<String> apps = new ArrayList<String>();
		final ArrayList<String> pkgs = new ArrayList<String>();
		PackageManager pm = getPackageManager();
		
		File[] dirs = new File(root).listFiles();
		if (dirs != null) {
			for (File dir: dirs) {
				if (!dir.isDirectory()) continue;
				String pkg = dir.getName();
				try {
					ApplicationInfo ai = pm.getApplicationInfo(pkg, 0);
					String label = ai.loadLabel(pm).toString();
					pkgs.add(pkg);
					apps.add(label + "\n" + pkg);
				} catch (NameNotFoundException e) {
					// ignore
				}
			}
		}
		
		final String[] items = apps.toArray(new String[apps.size()]);
		new AlertDialog.Builder(this)
			.setTitle(R.string.select_app)
			.setItems(items, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					String pkg = pkgs.get(which);
					
					File backup = new File(dir + "/" + pkg);
					File data = new File(files.replace(own, pkg)).getParentFile();
					
					final String[] cmd = {"cp", "-R", "-f", "from", "to"};
					final int from = 3;
					final int to = 4;
					if (restore) {
						cmd[from] = backup.getAbsolutePath() + "/.";
						cmd[to] = data.getAbsolutePath();
					} else {
						cmd[from] = data.getAbsolutePath() + "/.";
						cmd[to] = backup.getAbsolutePath();
					}
					
					status.setText(cmd[from] + "\n\n-->\n\n" + cmd[to]);
					
					new Thread(new Runnable() {
						@Override
						public void run() {
							String descr;
							try {
								new File(cmd[to]).mkdirs();
								Process proc = Runtime.getRuntime().exec(cmd);
								descr = "end: " + proc.waitFor();
							} catch (Exception e) {
								Log.w(TAG, "Failed copy", e);
								descr = e.toString();
							}
							final String ret = descr;
							runOnUiThread(new Runnable() {
								@Override
								public void run() {
									status.setText(cmd[from] + "\n\n-->\n\n" + cmd[to] + "\n\n" + ret);
									Toast.makeText(MainActivity.this, R.string.ended, Toast.LENGTH_LONG).show();
								}
							});
						}
					}).start();
				}
			}).create().show();
	}

}

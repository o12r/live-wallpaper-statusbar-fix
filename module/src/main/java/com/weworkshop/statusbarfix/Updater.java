package com.weworkshop.statusbarfix;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * In-app update from this project's GitHub releases: reads {@code releases/latest}, compares its
 * tag ({@code vX.Y.Z}) with the installed versionName, downloads the release's APK asset and
 * hands it to {@link PackageInstaller} (the system then asks the user to confirm).
 */
final class Updater {
    static final String ACTION_INSTALL_STATUS = Settings.PACKAGE + ".INSTALL_STATUS";
    private static final String LATEST =
            "https://api.github.com/repos/o12r/live-wallpaper-statusbar-fix/releases/latest";

    private final Activity activity;
    private final Handler main = new Handler(Looper.getMainLooper());

    Updater(Activity activity) {
        this.activity = activity;
    }

    /** Checks for a newer release; [manual] also reports "up to date" and errors. */
    void check(boolean manual) {
        new Thread(() -> {
            try {
                JSONObject release = new JSONObject(new String(get(LATEST), "UTF-8"));
                String latest = release.getString("tag_name").replaceFirst("^v", "");
                String apkUrl = null;
                JSONArray assets = release.optJSONArray("assets");
                for (int i = 0; assets != null && i < assets.length(); i++) {
                    JSONObject a = assets.getJSONObject(i);
                    if (a.getString("name").endsWith(".apk")) apkUrl = a.getString("browser_download_url");
                }
                String current = installedVersion();
                String notes = release.optString("body", "");
                String url = apkUrl;
                main.post(() -> {
                    if (activity.isFinishing()) return;
                    if (url != null && compare(latest, current) > 0) {
                        offer(latest, notes, url);
                    } else if (manual) {
                        Toast.makeText(activity, activity.getString(R.string.update_none, current), Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                if (manual) main.post(() -> Toast.makeText(activity,
                        activity.getString(R.string.update_failed, e.getMessage()), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void offer(String version, String notes, String url) {
        new AlertDialog.Builder(activity)
                .setTitle(activity.getString(R.string.update_available, version))
                .setMessage(notes)
                .setPositiveButton(R.string.update_install, (d, w) -> download(url))
                .setNegativeButton(R.string.update_later, null)
                .show();
    }

    @SuppressWarnings("deprecation")
    private void download(String url) {
        ProgressDialog progress = new ProgressDialog(activity);
        progress.setMessage(activity.getString(R.string.update_downloading));
        progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progress.setCancelable(false);
        progress.show();
        new Thread(() -> {
            try {
                PackageInstaller installer = activity.getPackageManager().getPackageInstaller();
                PackageInstaller.SessionParams params =
                        new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);
                params.setAppPackageName(activity.getPackageName());
                int id = installer.createSession(params);
                try (PackageInstaller.Session session = installer.openSession(id)) {
                    HttpURLConnection c = open(url);
                    long total = c.getContentLengthLong();
                    try (InputStream in = c.getInputStream();
                         OutputStream out = session.openWrite("update.apk", 0, total)) {
                        byte[] buf = new byte[64 * 1024];
                        long done = 0;
                        int n;
                        while ((n = in.read(buf)) > 0) {
                            out.write(buf, 0, n);
                            done += n;
                            if (total > 0) {
                                int pct = (int) (done * 100 / total);
                                main.post(() -> progress.setProgress(pct));
                            }
                        }
                        session.fsync(out);
                    }
                    Intent status = new Intent(activity, SettingsActivity.class).setAction(ACTION_INSTALL_STATUS);
                    PendingIntent pi = PendingIntent.getActivity(activity, 0, status,
                            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
                    session.commit(pi.getIntentSender());
                }
                main.post(progress::dismiss);
            } catch (Exception e) {
                main.post(() -> {
                    progress.dismiss();
                    Toast.makeText(activity, activity.getString(R.string.update_failed, e.getMessage()), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    /** Called by {@link SettingsActivity} with the installer's status intent. */
    static void onInstallStatus(Activity activity, Intent intent) {
        int status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE);
        if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
            Intent confirm = intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent.class);
            if (confirm != null) activity.startActivity(confirm);
        } else if (status != PackageInstaller.STATUS_SUCCESS) {
            String msg = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE);
            Toast.makeText(activity, activity.getString(R.string.update_failed, msg), Toast.LENGTH_LONG).show();
        }
    }

    private String installedVersion() throws Exception {
        PackageInfo info = activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0);
        return info.versionName;
    }

    /** Compares dotted numeric versions ("1.3.10" > "1.3.2"). */
    static int compare(String a, String b) {
        String[] x = a.split("\\."), y = b.split("\\.");
        for (int i = 0; i < Math.max(x.length, y.length); i++) {
            int p = i < x.length ? parse(x[i]) : 0, q = i < y.length ? parse(y[i]) : 0;
            if (p != q) return Integer.compare(p, q);
        }
        return 0;
    }

    private static int parse(String s) {
        try {
            return Integer.parseInt(s.replaceAll("\\D.*", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static HttpURLConnection open(String url) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(15000);
        c.setReadTimeout(30000);
        c.setInstanceFollowRedirects(true);
        c.setRequestProperty("Accept", "application/vnd.github+json");
        if (c.getResponseCode() / 100 != 2) throw new Exception("HTTP " + c.getResponseCode());
        return c;
    }

    private static byte[] get(String url) throws Exception {
        HttpURLConnection c = open(url);
        try (InputStream in = c.getInputStream()) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            return out.toByteArray();
        }
    }
}

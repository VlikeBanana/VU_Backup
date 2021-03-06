
package com.aokp.backup.backup;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.net.Uri;
import android.provider.Settings;
import android.util.Log;
import com.aokp.backup.R;
import com.aokp.backup.util.SVal;
import com.aokp.backup.util.Tools;
import eu.chainfire.libsuperuser.Shell;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class JBBackup extends Backup {

    public static final int CAT_GENERAL_UI = 0;
    public static final int CAT_NAVIGATION_BAR = 1;
    public static final int CAT_LOCKSCREEN_OPTS = 2;
    public static final int CAT_WEATHER = 3;
    public static final int CAT_LED_OPTIONS = 4;
    public static final int CAT_SOUND = 5;
    public static final int CAT_SB_TOGGLES = 6;
    public static final int CAT_SB_CLOCK = 7;
    public static final int CAT_SB_BATTERY = 8;
    public static final int CAT_SB_SIGNAL = 9;
    public static final int NUM_CATS = 10;

    String rcUser = null;

    public JBBackup(Context c, String name) {
        super(c, name);
    }

    public JBBackup(Context c, File zip) throws IOException {
        super(c, zip);
    }

    @Override
    public int getNumCats() {
        return NUM_CATS;
    }

    @Override
    public List<String> getSuCommands() {
        return null;
    }

    @Override
    public String[] getSettingsCategory(int categoryIndex) {
        Resources res = mContext.getResources();
        switch (categoryIndex) {
            case CAT_GENERAL_UI:
                return res.getStringArray(R.array.jbcat_general_ui);
            case CAT_NAVIGATION_BAR:
                return res.getStringArray(R.array.jbcat_navigation_bar);
            case CAT_LED_OPTIONS:
                return res.getStringArray(R.array.jbcat_led);
            case CAT_LOCKSCREEN_OPTS:
                return res.getStringArray(R.array.jbcat_lockscreen);
            case CAT_SB_BATTERY:
                return res.getStringArray(R.array.jbcat_statusbar_battery);
            case CAT_SB_CLOCK:
                return res.getStringArray(R.array.jbcat_statusbar_clock);
            case CAT_SB_TOGGLES:
                return res.getStringArray(R.array.jbcat_statusbar_toggles);
            case CAT_WEATHER:
                return res.getStringArray(R.array.jbcat_weather);
            case CAT_SOUND:
                return res.getStringArray(R.array.jbcat_sound);
            case CAT_SB_SIGNAL:
                return res.getStringArray(R.array.jbcat_statusbar_signal);
            default:
                return null;
        }
    }

    @Override
    public boolean handleBackupSpecialCase(String setting) {
        if (rcUser == null) {
            Tools.getRomControlPid();
        }
        String outDir = Tools.getBackupDirectory(mContext).getAbsolutePath();

        boolean found = false;

        if (setting.equals("disable_boot_animation")) {
            if (!new File("/system/media/bootanimation.zip").exists()) {
            }

            found = true;
        } else if (setting.equals("disable_boot_audio")) {
            if (!new File("/system/media/boot_audio.mp3").exists()) {
                mSpecialCaseKeys.add(new SVal(setting, "1"));
            }

            found = true;

        } else if (setting.equals("disable_bug_mailer")) {
            if (!new File("/system/bin/bugmailer.sh").exists()) {
                mSpecialCaseKeys.add(new SVal(setting, "1"));
            }

            found = true;
        } else if (setting.equals("navigation_bar_icons")) {
            ContentResolver resolver = mContext.getContentResolver();
            for (int i = 0; i < 7; i++) {
                String iconSetting = "navigation_custom_app_icon_" + i;
                String iconValue = Settings.System.getString(resolver, iconSetting);
                File iconFile = new File("/data/data/com.aokp.romcontrol/files/navbar_icon_" + i + ".png");
                if (iconValue != null && iconFile.exists()) {
                    // add the value to the file so we know to restore the default icon or something
                    mSpecialCaseKeys.add(new SVal(iconSetting, iconValue));
                    if (iconValue.length() > 0) {
                        // copy if there's an actual image there
                        String cmd = "cp '" + iconFile.getAbsolutePath() + "' '" + outDir + "'";
                        Shell.SU.run(cmd);
                    }
                }
            }

            found = true;

        } else if (setting.equals("lockscreen_wallpaper")) {
            File wallpaper = new File("/data/data/com.aokp.romcontrol/files/lockscreen_wallpaper.jpg");
            if (wallpaper.exists())
                Shell.SU.run("cp '" + wallpaper.getAbsolutePath() + "' '"
                        + outDir + "'");
            found = true;

        } else if (setting.equals("notification_wallpaper")) {
            File wallpaper = new File("/data/data/com.aokp.romcontrol/files/notification_wallpaper.jpg");
            if (wallpaper.exists())
                Shell.SU.run("cp '" + wallpaper + "' '"
                        + outDir + "'");
            found = true;

        } else if (setting.equals("lockscreen_icons")) {
            ContentResolver resolver = mContext.getContentResolver();
            for (int i = 0; i < 8; i++) {
                String set = "lockscreen_custom_app_icon_" + i;
                String val = Settings.System.getString(resolver, set);
                File iconFile = new File("/data/data/com.aokp.romcontrol/files/lockscreen_icon_" + i + ".png");
                if (val != null) {
                    mSpecialCaseKeys.add(new SVal(set, val));
                    File f = new File(Uri.parse(val).getPath());
                    if (f.exists()) {
                        Shell.SU.run("cp '" + iconFile + "' '" + outDir + "'");
                    }
                }
            }
            found = true;

        } else if (setting.equals("rc_prefs")) {
            final String[] xmlFiles = {
                    "WeatherServicePreferences.xml",
                    "_has_set_default_values.xml",
                    "aokp_weather.xml",
                    "com.aokp.romcontrol_preferences.xml",
                    "vibrations.xml"
            };
            for (String xmlName : xmlFiles) {
                File xml = new File("/data/data/com.aokp.romcontrol/shared_prefs/" + xmlName);
                if (xml.exists()) {
                    List<String> result = Shell.SU.run("cp '" + xml.getAbsolutePath() + "' '" + outDir + xml.getName() + "'");
                    if (result != null) {
//                        Log.e(TAG, "run success");
                    } else {
                        Log.e(TAG, "error backing up: " + xmlName);
                    }
                }
            }
            found = true;
        }
        if (found) {
            mSpecialCaseKeys.add(new SVal(setting, "1"));
        }
        return found;
    }

    public boolean handleRestoreSpecialCase(SVal sval) {
        if (rcUser == null) {
            Tools.getRomControlPid();
        }
        String setting = sval.getKey();
        String value = sval.getValue();

        if (setting.equals("disable_boot_animation") && value.equals("1")) {
            if (new File("/system/media/bootanimation.zip").exists()) {
                Shell.SU.run("mv /system/media/bootanimation.zip /system/media/bootanimation.unicorn");
            }

        } else if (setting.equals("disable_boot_audio") && value.equals("1")) {
            if (new File("/system/media/boot_audio.mp3").exists()) {
                Shell.SU.run("mv /system/media/boot_audio.mp3 /system/media/boot_audio.unicorn");
            }

        } else if (setting.equals("disable_bug_mailer") && value.equals("1")) {
            if (new File("/system/bin/bugmailer.sh").exists()) {
                Shell.SU.run("mv /system/bin/bugmailer.sh /system/bin/bugmailer.sh.unicorn");
            }

        } else if (setting.equals("navigation_bar_icons")) {
            File outDir = Tools.getTempRestoreDirectory(mContext, false);
            for (int i = 0; i < 7; i++) {
//                String settingName = "navigation_custom_app_icon_" + i;
                String iconName = "navbar_icon_" + i + ".png";
                File source = new File(outDir, iconName);
                File target = new File(rcFilesDir, iconName);
                if (!source.exists())
                    continue;

                // delete the current icon since we're restoring some
//                if (settingsFromFile.containsKey(settingName)) {
                Shell.SU.run("cp " + source.getAbsolutePath() + " " + target.getAbsolutePath());
                Tools.chmodAndOwn(target, "0660", rcUser);
//                    restoreSetting(settingsFromFile.get(settingName));
//                } else {
                Shell.SU.run("rm " + target.getAbsolutePath());
//                    restoreSetting(settingName, "", false);
//                }

            }
        } else if (setting.equals("lockscreen_wallpaper")) {
            File outDir = Tools.getTempRestoreDirectory(mContext, false);

            File source = new File(outDir, "lockscreen_wallpaper.jpg");
            File target = new File(rcFilesDir, "lockscreen_wallpaper.jpg");
            if (target.exists())
                Shell.SU.run("rm " + target.getAbsolutePath());
            if (source.exists()) {
                Shell.SU.run("cp " + source.getAbsolutePath() + " "
                        + target.getAbsolutePath());
                Tools.chmodAndOwn(target, "0660", rcUser);
            }

            return true;
        } else if (setting.equals("notification_wallpaper")) {
            File outDir = Tools.getTempRestoreDirectory(mContext, false);

            File source = new File(outDir, "notification_wallpaper.jpg");
            File target = new File(rcFilesDir, "notification_wallpaper.jpg");
            if (target.exists())
                Shell.SU.run("rm " + target.getAbsolutePath());
            if (source.exists()) {
                Shell.SU.run("cp " + source.getAbsolutePath() + " "
                        + target.getAbsolutePath());
                Tools.chmodAndOwn(target, "0660", rcUser);
            }

            return true;
        } else if (setting.equals("lockscreen_icons")) {
            // not used anymore
            if (true)
                return true;
            File outDir = Tools.getTempRestoreDirectory(mContext, false);

            for (int i = 0; i < 8; i++) {
                String settingName = "lockscreen_custom_app_icon_" + i;
                File iconToRestore = new File(outDir, settingName);

                Shell.SU.run("rm " + rcFilesDir.getAbsolutePath() + settingName + ".*");
//                if (settingsFromFile.containsKey(settingName)) {
                Shell.SU.run("cp " + iconToRestore.getAbsolutePath() + ".* "
                        + rcFilesDir.getAbsolutePath());
                Tools.chmodAndOwn(iconToRestore, "0660", rcUser);
//                    restoreSetting(settingsFromFile.get(settingName));
//                } else {
//                    restoreSetting(settingName, "", false);
//                }

            }
            return true;
        } else if (setting.equals("rc_prefs")) {
            File outDir = Tools.getTempRestoreDirectory(mContext, false);

            String[] xmlFiles = {
                    "WeatherServicePreferences.xml", "_has_set_default_values.xml",
                    "aokp_weather.xml", "vibrations.xml"
            };
            if (rcUser != null && !rcUser.isEmpty()) {
                for (String xmlName : xmlFiles) {
                    File xml = new File(rcPrefsDir, xmlName);
                    if (xml.exists()) {
                        // remove previous
                        Shell.SU.run("rm " + xml.getAbsolutePath());
                        // copy backed up file
                        Shell.SU.run("cp " + outDir + "/" + xml.getName() + " "
                                + xml.getAbsolutePath());
                        Tools.chmodAndOwn(xml, "0660", rcUser);
                    }
                }
            } else {
                Log.e(TAG, "Error getting RC user");
            }
            return true;
        }

        return false;
    }

    public boolean okayToRestore() {
        boolean result = false;
        int minimumGooVersion = 20;
        if (minimumGooVersion == -1) {
            return false;
        }
        final int maximumGooVersion = 26;

        try {
            int currentVersion = Tools.getAOKPGooVersion();
            if (currentVersion == -1) {
                result = false;
            }
            if ("aokp".equals(Tools.getInstance().getProp("ro.goo.rom"))) {
                result = true;
            }

            if (currentVersion <= maximumGooVersion && currentVersion >= minimumGooVersion)
                result = true;
        } catch (Exception e) {
        }
        return result;
    }
}

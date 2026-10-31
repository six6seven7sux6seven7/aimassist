package com.aimassist;

import net.fabricmc.loader.api.FabricLoader;
import java.io.*;
import java.nio.file.Path;
import java.util.Properties;

public class AimConfig {
    public static boolean enabled = false;
    public static double speed = 8.0;
    public static double maxSpeed = 220;
    public static double range = 6.0;
    public static double fov = 90;
    public static double zone = 0.35;
    public static double height = 0.70;
    public static double offset = 0.45;
    public static boolean playersOnly = true;
    public static boolean onlyWhenAttack = false;

    private static Path file() { return FabricLoader.getInstance().getConfigDir().resolve("aimassist.properties"); }

    public static void save() {
        Properties p = new Properties();
        p.setProperty("speed", "" + speed); p.setProperty("maxSpeed", "" + maxSpeed);
        p.setProperty("range", "" + range); p.setProperty("fov", "" + fov);
        p.setProperty("zone", "" + zone); p.setProperty("offset", "" + offset);
        p.setProperty("height", "" + height);
        p.setProperty("playersOnly", "" + playersOnly); p.setProperty("onlyWhenAttack", "" + onlyWhenAttack);
        try (OutputStream o = new FileOutputStream(file().toFile())) { p.store(o, "Aim Assist"); } catch (IOException ignored) {}
    }

    public static void load() {
        File f = file().toFile();
        if (!f.exists()) return;
        Properties p = new Properties();
        try (InputStream i = new FileInputStream(f)) {
            p.load(i);
            speed = Double.parseDouble(p.getProperty("speed", "" + speed));
            maxSpeed = Double.parseDouble(p.getProperty("maxSpeed", "" + maxSpeed));
            range = Double.parseDouble(p.getProperty("range", "" + range));
            fov = Double.parseDouble(p.getProperty("fov", "" + fov));
            zone = Double.parseDouble(p.getProperty("zone", "" + zone));
            offset = Double.parseDouble(p.getProperty("offset", "" + offset));
            height = Double.parseDouble(p.getProperty("height", "" + height));
            playersOnly = Boolean.parseBoolean(p.getProperty("playersOnly", "" + playersOnly));
            onlyWhenAttack = Boolean.parseBoolean(p.getProperty("onlyWhenAttack", "" + onlyWhenAttack));
        } catch (Exception ignored) {}
    }
}

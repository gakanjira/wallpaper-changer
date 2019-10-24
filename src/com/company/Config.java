package com.company;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.Random;

public class Config {
    public String appPath = "";
    public String configPath = "";
    public JSONObject config = new JSONObject();

    public Config(String appPath) {
        this.appPath = appPath;
        this.configPath = appPath + "config.json";
    }

    public void save() {
        File tmp = new File(configPath);
        if(!tmp.exists()) {
            config.put("appPath", appPath);
            JSONArray ignoredWallpapers = new JSONArray();
            JSONArray wallpapers = new JSONArray();
            config.put("ignoredWallpapers", ignoredWallpapers);
            config.put("wallpapers", wallpapers);
            saveToFile();
        } else {
            try {
                String configString = new String(Files.readAllBytes(Paths.get(configPath)), StandardCharsets.UTF_8);
                config = new JSONObject(configString);
            } catch(Exception e) {
                System.out.println(e);
            }
        }
    }

    public void saveToFile() {
        try {
            Files.write(Paths.get(configPath), config.toString().getBytes());
        } catch (IOException e) {
            System.out.println(e);
        }
    }

    public void addIgnoredWallpaper(String name) {
        Main.dislikeItem.setEnabled(false);
        JSONArray ignoredWallpapers = config.getJSONArray("ignoredWallpapers");
        JSONArray wallpapers = config.getJSONArray("wallpapers");

        try {
            Files.deleteIfExists(Paths.get(Main.imagesPath + name + ".jpg"));
        }
        catch(NoSuchFileException e) {
            System.out.println("No such file/directory exists");
        }
        catch(DirectoryNotEmptyException e) {
            System.out.println("Directory is not empty.");
        }
        catch(IOException e) {
            System.out.println("Invalid permissions.");
        }

        for(int i = 0; i < wallpapers.length(); i++) {
            if(wallpapers.getString(i).equals(name)) {
                wallpapers.remove(i);
                break;
            }
        }

        ignoredWallpapers.put(name);
        saveToFile();
        Main.dislikeItem.setEnabled(true);
    }

    public void addWallpaperToConfig(String name) {
        JSONArray wallpapers = config.getJSONArray("wallpapers");
        JSONArray ignoredWallpapers = config.getJSONArray("ignoredWallpapers");

        Boolean f = false;
        for(int i = 0; i < wallpapers.length(); i++) {
            if(wallpapers.getString(i).equals(name)) {
                f = true;
                break;
            }
        }

        Boolean f2 = false;
        for(int i = 0; i < ignoredWallpapers.length(); i++) {
            if(ignoredWallpapers.getString(i).equals(name)) {
                f2 = true;
                break;
            }
        }

        if(!f && !f2 && !name.equals("") && !name.equals("..\\default")) {
            wallpapers.put(name);
            saveToFile();
        }
    }

    public String getRandomWallpaperName(Boolean jpg) {
        JSONArray wallpapers = null;

        try {
            wallpapers = config.getJSONArray("wallpapers");
            if(wallpapers.length() == 0) {
                return jpg ? "..\\default.jpg" : "..\\default";
            } else {
                Random r = new Random();
                String randomWallpaperName = wallpapers.getString(r.nextInt(wallpapers.length()));
                while (Main.currentWallpaperTitle == randomWallpaperName) {
                    randomWallpaperName = wallpapers.getString(r.nextInt(wallpapers.length()));
                }

                return jpg ? randomWallpaperName + ".jpg" : randomWallpaperName;
            }
        } catch(Exception e) {}

        return jpg ? "..\\default.jpg" : "..\\default";
    }

    public Boolean checkIfCurrentWallpaperIsIgnored() {
        JSONArray ignoredWallpapers = config.getJSONArray("ignoredWallpapers");

        Boolean f = false;
        for(int i = 0; i < ignoredWallpapers.length(); i++) {
            if(ignoredWallpapers.getString(i).equals(Main.currentWallpaperTitle)) {
                f = true;
                break;
            }
        }

       return f;
    }
}

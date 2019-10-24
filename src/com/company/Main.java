package com.company;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.URL;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.WinDef.UINT_PTR;
import com.sun.jna.win32.*;
import org.json.JSONArray;
import org.json.JSONObject;

public class Main {
    public static String currentWallpaperTitle = "";
    public static URL currentWallpaperUrl = null;
    public static String userName = System.getProperty("user.name");
    public static String appPath = "C:\\Users\\" + userName +"\\wallpapers\\";
    public static String imagesPath = appPath + "img\\";
    public static String currentWallpaperPath = "";
    public static Config config = new Config(appPath);
    public static MenuItem dislikeItem = new MenuItem("Dislike");

    public static void main(String[] args) {
        // init
        config.save();
        getTodaysWallpaper();

        try {
            downloadImage(currentWallpaperUrl, currentWallpaperPath);
            config.addWallpaperToConfig(currentWallpaperTitle);
        } catch(IOException e) {
            System.out.println(e);
        }

        String path = currentWallpaperPath;

        SPI.INSTANCE.SystemParametersInfo(
                new UINT_PTR(SPI.SPI_SETDESKWALLPAPER),
                new UINT_PTR(0),
                path,
                new UINT_PTR(SPI.SPIF_UPDATEINIFILE | SPI.SPIF_SENDWININICHANGE));

        addTray();
        startSlideShow();
    }

    public interface SPI extends StdCallLibrary {
        long SPI_SETDESKWALLPAPER = 20;
        long SPIF_UPDATEINIFILE = 0x01;
        long SPIF_SENDWININICHANGE = 0x02;

        @SuppressWarnings("deprecation")
        SPI INSTANCE = (SPI) Native.loadLibrary("user32", SPI.class, new HashMap<String, Object>() {
            {
                put(OPTION_TYPE_MAPPER, W32APITypeMapper.UNICODE);
                put(OPTION_FUNCTION_MAPPER, W32APIFunctionMapper.UNICODE);
            }
        });

        boolean SystemParametersInfo(
                UINT_PTR uiAction,
                UINT_PTR uiParam,
                String pvParam,
                UINT_PTR fWinIni
        );
    }

    public static void getTodaysWallpaper() {
        try {
            JSONObject json = JsonReader.readJsonFromUrl("https://www.nationalgeographic.com/photography/photo-of-the-day/_jcr_content/.gallery.json");
            JSONArray allWallpapers = json.getJSONArray("items");
            JSONObject todaysWallpaper = allWallpapers.getJSONObject(0);
            currentWallpaperUrl = new URL(todaysWallpaper.getString("originalUrl"));
            currentWallpaperTitle = todaysWallpaper.getString("title");
            if(config.checkIfCurrentWallpaperIsIgnored()) {
                currentWallpaperTitle = config.getRandomWallpaperName(false);
            }
            currentWallpaperPath = imagesPath + currentWallpaperTitle + ".jpg";
        } catch (IOException e) {
            System.out.println(e);
        }
    }

    public static void downloadImage(URL imageUrl, String destinationFile) throws IOException {
        if(!config.checkIfCurrentWallpaperIsIgnored()) {
            File tmp = new File(destinationFile);
            if (!tmp.exists()) {
                try {
                    InputStream is = imageUrl.openStream();
                    OutputStream os = new FileOutputStream(destinationFile);

                    byte[] b = new byte[2048];
                    int length;

                    while ((length = is.read(b)) != -1) {
                        os.write(b, 0, length);
                    }

                    is.close();
                    os.close();
                } catch(Exception e) {
                    System.out.println(e);
                }
            }
        }
    }

    public static void addTray() {
        final TrayIcon trayIcon;

        if (SystemTray.isSupported()) {

            SystemTray tray = SystemTray.getSystemTray();
            Image image = Toolkit.getDefaultToolkit().getImage(appPath + "icon.png");

            ActionListener exitListener = new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    System.out.println("Exiting...");
                    System.exit(0);
                }
            };

            ActionListener dislikeListener = new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    config.addIgnoredWallpaper(currentWallpaperTitle);
                    SPI.INSTANCE.SystemParametersInfo(
                            new UINT_PTR(SPI.SPI_SETDESKWALLPAPER),
                            new UINT_PTR(0),
                            imagesPath + config.getRandomWallpaperName(true),
                            new UINT_PTR(SPI.SPIF_UPDATEINIFILE | SPI.SPIF_SENDWININICHANGE));
                }
            };

            ActionListener randomListener = new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    setRandomWallpaper();
                }
            };

            PopupMenu popup = new PopupMenu();
            MenuItem defaultItem = new MenuItem("Exit");
            MenuItem randomItem = new MenuItem("Random");
            defaultItem.addActionListener(exitListener);
            dislikeItem.addActionListener(dislikeListener);
            randomItem.addActionListener(randomListener);
            popup.add(defaultItem);
            popup.add(dislikeItem);
            popup.add(randomItem);

            trayIcon = new TrayIcon(image, "Wallpaper Changer", popup);

            ActionListener actionListener = new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    trayIcon.displayMessage("Action Event",
                            "An Action Event Has Been Performed!",
                            TrayIcon.MessageType.INFO);
                }
            };

            trayIcon.setImageAutoSize(true);
            trayIcon.addActionListener(actionListener);

            try {
                tray.add(trayIcon);
            } catch (AWTException e) {
                System.err.println("TrayIcon could not be added.");
            }

        } else {

            //  System Tray is not supported

        }
    }

    public static void startSlideShow() {
        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run(){
                setRandomWallpaper();
            }
        },0,5000);
    }

    public static void setRandomWallpaper() {
        String wp = config.getRandomWallpaperName(false);
        currentWallpaperTitle = wp;

        SPI.INSTANCE.SystemParametersInfo(
                new UINT_PTR(SPI.SPI_SETDESKWALLPAPER),
                new UINT_PTR(0),
                imagesPath + wp + ".jpg",
                new UINT_PTR(SPI.SPIF_UPDATEINIFILE | SPI.SPIF_SENDWININICHANGE));
    }
}
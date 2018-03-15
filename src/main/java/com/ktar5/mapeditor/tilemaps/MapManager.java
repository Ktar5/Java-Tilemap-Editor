package com.ktar5.mapeditor.tilemaps;

import com.ktar5.mapeditor.Main;
import com.ktar5.mapeditor.gui.dialogs.GenericAlert;
import com.ktar5.mapeditor.gui.dialogs.CreateBaseTilemap;
import com.ktar5.mapeditor.gui.dialogs.LoadDialog;
import com.ktar5.mapeditor.tilemaps.whole.WholeTilemap;
import com.ktar5.mapeditor.util.StringUtil;
import lombok.Getter;
import org.json.JSONObject;
import org.pmw.tinylog.Configurator;
import org.pmw.tinylog.Level;
import org.pmw.tinylog.Logger;
import org.pmw.tinylog.writers.ConsoleWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class MapManager {
    public static MapManager instance;
    private File tempDir;
    private HashMap<UUID, BaseTilemap> openMaps;
    @Getter
    private UUID currentLevel = null;

    public MapManager(File dir) {
        instance = this;

        openMaps = new HashMap<>();

        //Initialize tinylog
        Configurator.defaultConfig()
                .writer(new ConsoleWriter())
                .level(Level.DEBUG)
                .addWriter(new org.pmw.tinylog.writers.FileWriter("log.txt"))
                .formatPattern("{date:mm:ss:SSS} {class_name}.{method}() [{level}]: {message}")
                .activate();

        //Create the save and temp directories
        this.tempDir = dir;
        if (!tempDir.exists() || !tempDir.isDirectory()) {
            tempDir.mkdir();
        }

        //Save maps every 5 minutes
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                saveTempAllMaps();
            }
        }, 5 * 60 * 1000, 5 * 60 * 1000);
    }

    public static MapManager get() {
        if (instance == null) {
            throw new RuntimeException("Please initialize tile manager first.");
        }
        return instance;
    }

    public void remove(UUID uuid) {
        if (this.openMaps.containsKey(uuid)) {
            Logger.debug("Removed tilemap: " + getMap(uuid).getMapName());
            openMaps.remove(uuid);
        }
    }

    public BaseTilemap getMap(UUID id) {
        if (!openMaps.containsKey(id)) {
            throw new RuntimeException("BaseTilemap with id: " + id + " already exists");
        }
        return openMaps.get(id);
    }

    public BaseTilemap createMap() {
        CreateBaseTilemap createDialog = CreateBaseTilemap.create();
        if (createDialog == null) {
            new GenericAlert("Something went wrong during the process of creating the map, please try again.");
            return null;
        }

        File file = createDialog.getFile();
        for (BaseTilemap baseTilemap1 : openMaps.values()) {
            if (baseTilemap1.getSaveFile().getAbsolutePath().equals(file.getAbsolutePath())) {
                new GenericAlert("BaseTilemap with path " + file.getAbsolutePath() + " already loaded.\n" +
                        "Please close tab for " + file.getName() + " then try creating new map again.");
                return null;
            }
        }

        BaseTilemap baseTilemap = new WholeTilemap(createDialog.getFile(), createDialog.getWidth(),
                createDialog.getHeight(), createDialog.getTilesize());
        openMaps.put(baseTilemap.getId(), baseTilemap);
        Main.root.getCenterView().getEditorViewPane().createTab(baseTilemap.getId());
        return baseTilemap;
    }

    public BaseTilemap loadMap() {
        File loaderFile = LoadDialog.create("Load a tilemap", "Json Tilemap File", "*.json");
        if (loaderFile == null) {
            Logger.info("Tried to load map, cancelled or failed");
            return null;
        } else if (!loaderFile.exists()) {
            new GenericAlert("The selected file: " + loaderFile.getPath() + " does not exist. Try again.");
            return null;
        }

        Logger.info("Beginning to load map from file: " + loaderFile.getPath());

        String data = StringUtil.readFileAsString(loaderFile);
        if (data == null || data.isEmpty()) {
            Logger.error("Data from file: " + loaderFile.getPath() + " is either null or empty.");
            return null;
        }
        BaseTilemap baseTilemap = new WholeTilemap(loaderFile, new JSONObject(data));
        for (BaseTilemap temp : openMaps.values()) {
            if (temp.getSaveFile().getPath().equals(baseTilemap.getSaveFile().getPath())) {
                new GenericAlert("BaseTilemap with path " + baseTilemap.getSaveFile().getAbsolutePath() + " already loaded");
                return null;
            }
        }
        openMaps.put(baseTilemap.getId(), baseTilemap);
        Main.root.getCenterView().getEditorViewPane().createTab(baseTilemap.getId());
        Logger.info("Finished loading map: " + baseTilemap.getMapName());
        return baseTilemap;
    }

    public void saveMap(UUID id) {
        Logger.info("Starting save for baseTilemap (" + id + ")");

        if (!openMaps.containsKey(id)) {
            Logger.info("Map not loaded so could not be saved id: (" + id + ")");
            return;
        }

        BaseTilemap baseTilemap = openMaps.get(id);
        if (baseTilemap.getSaveFile().exists()) {
            baseTilemap.getSaveFile().delete();
        }

        try {
            baseTilemap.getSaveFile().createNewFile();
            FileWriter writer = new FileWriter(baseTilemap.getSaveFile());
            writer.write(baseTilemap.serializeBase().toString(4));
            Main.root.getCenterView().getEditorViewPane().setChanges(baseTilemap.getId(), false);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        Logger.info("Finished save for baseTilemap (" + id + ") in " + "\"" + baseTilemap.getSaveFile() + "\"");
    }

    public void saveTempAllMaps() {
        Logger.info("Saving map backups");
        for (HashMap.Entry<UUID, BaseTilemap> openMap : openMaps.entrySet()) {
            //TODO
            //saveMap(tempDir, openMap.getKey());
        }
        Logger.info("Finished saving map backups");
    }

    public BaseTilemap getCurrent() {
        return this.getMap(getCurrentLevel());
    }

}
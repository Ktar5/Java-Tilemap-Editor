package com.ktar5.mapeditor;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.IntMap;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ktar5.tilejump.camera.CameraBase;
import com.ktar5.tilejump.camera.CameraMove;
import com.ktar5.tilejump.tools.mapeditor.rendering.GridRenderer;
import com.ktar5.tilejump.tools.mapeditor.serialization.TilemapDeserializer;
import com.ktar5.tilejump.tools.mapeditor.serialization.TilemapSerializer;
import lombok.Getter;
import org.pmw.tinylog.Configurator;
import org.pmw.tinylog.Level;
import org.pmw.tinylog.Logger;
import org.pmw.tinylog.writers.ConsoleWriter;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class TileManager implements Disposable {
    public static final int SIZE = 16;
    public static TileManager instance;

    private File tempDir, saveDir;
    private IntMap<Tilemap> openMaps;
    private final Gson gson;

    @Getter
    private CameraBase camera;
    @Getter
    private int currentLevel = -1;
    @Getter
    private GridRenderer gridRenderer;

    public TileManager(File dir) {
        instance = this;

        openMaps = new IntMap<>();

        //Initialize tinylog
        Configurator.defaultConfig()
                .writer(new ConsoleWriter())
                .level(Level.DEBUG)
                .addWriter(new org.pmw.tinylog.writers.FileWriter("log.txt"))
                .formatPattern("{date:mm:ss:SSS} {class_name}.{method}() [{level}]: {message}")
                .activate();

        //Initialize gson
        gson = new GsonBuilder()
                .registerTypeAdapter(Tilemap.class, new TilemapSerializer())
                .registerTypeAdapter(Tilemap.class, new TilemapDeserializer())
                .setPrettyPrinting()
                .create();


        //Create the save and temp directories
        this.saveDir = dir;
        if (!saveDir.exists() || !saveDir.isDirectory()) {
            saveDir.mkdir();
        }
        File backup = new File(saveDir, "/backup");
        tempDir = backup;
        if (!tempDir.exists() || !tempDir.isDirectory()) {
            tempDir.mkdir();
        }

        camera = new CameraMove(new OrthographicCamera(), new ScreenViewport());

        this.gridRenderer = new GridRenderer();
        //Save maps every 5 minutes
        Timer.schedule(new Timer.Task() {
            @Override
            public void run() {
                saveTempAllMaps();
            }
        }, 5 * 60, 5 * 60);
    }

    public Tilemap getMap(int id) {
        if (!openMaps.containsKey(id)) {
            throw new RuntimeException("Tilemap with id: " + id + " already exists");
        }
        return openMaps.get(id);
    }

    public static TileManager get() {
        if (instance == null) {
            throw new RuntimeException("Please initialize tile manager first.");
        }
        return instance;
    }

    public void createMap(int width, int height, int id) {
        if (openMaps.containsKey(id)) {
            throw new RuntimeException("Tilemap with id: " + id + " already exists");
        }
        File file = new File(saveDir, "tilemap_" + id + ".json");
        if (file.exists()) {
            throw new RuntimeException("File at path already exists: " + file.getPath());
        }
        Tilemap tilemap = Tilemap.create(width, height, id);
        openMaps.put(id, tilemap);
    }

    public void loadMap(int id) {
        this.loadMap(saveDir, id);
    }

    public void saveMap(int id) {
        this.saveMap(saveDir, id);
    }

    public void loadMap(File directory, int id) {
        Logger.info("Began loading map: " + id);
        if (openMaps.containsKey(id)) {
            throw new RuntimeException("Tilemap with id: " + id + " already loaded");
        }
        if (!directory.exists()) {
            throw new RuntimeException("Directory doesn't exist: " + directory.getPath());
        }
        File file = new File(directory, "tilemap_" + id + ".json");
        try {
            if (!file.exists()) {
                throw new RuntimeException("File at path does not exit: " + file.getPath());
            }
            FileReader reader = new FileReader(file);
            Tilemap tilemap = gson.fromJson(reader, Tilemap.class);
            openMaps.put(id, tilemap);
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Logger.info("Finished loading map: " + id);
    }

    public void saveMap(File directory, int id) {
        Logger.info("Starting save for tilemap (" + id + ") in " + "\"" + directory.getPath() + "\"");

        if (!openMaps.containsKey(id)) {
            Logger.info("Map not loaded so could not be saved id: (" + id + ")");
            return;
        }

        Tilemap tilemap = openMaps.get(id);
        File file = new File(directory, "tilemap_" + id + ".json");
        try {
            if (file.exists()) {
                file.delete();
            }
            file.createNewFile();
            FileWriter writer = new FileWriter(file);
            gson.toJson(tilemap, writer);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Logger.info("Finished save for tilemap (" + id + ") in " + "\"" + directory.getPath() + "\"");
    }

    public void saveAllMaps() {
        Logger.info("Saving all maps");
        for (IntMap.Entry<Tilemap> openMap : openMaps) {
            saveMap(saveDir, openMap.key);
        }
        Logger.info("Saved all maps");
    }

    public void saveTempAllMaps() {
        Logger.info("Saving map backups");
        for (IntMap.Entry<Tilemap> openMap : openMaps) {
            saveMap(tempDir, openMap.key);
        }
        Logger.info("Finished saving map backups");
    }


    @Override
    public void dispose() {
        gridRenderer.dispose();
    }
}

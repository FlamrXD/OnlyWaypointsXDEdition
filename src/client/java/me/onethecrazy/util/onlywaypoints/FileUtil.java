package me.onethecrazy.util.onlywaypoints;

import com.google.gson.Gson;
import me.onethecrazy.OnlyWaypointClientOptions;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileUtil {
    public static String readJSONArrayFileContents(Path file) throws IOException{
        createFileIfNotPresent(file, "[]");

        return Files.readString(file);
    }

    public static String readJSONObjectFileContents(Path file) throws IOException{
        createFileIfNotPresent(file, "{}");

        return Files.readString(file);
    }

    public static void writeFile(Path file, String content) throws IOException{
        createFileIfNotPresent(file, "");

        Files.writeString(file, content);
    }

    public static void createFileIfNotPresent(Path file, String emptyFileContent) throws IOException {
        Path parent = file.getParent();
        if (parent != null) Files.createDirectories(parent);

        try {
            Files.createFile(file);
            // Write Empty json, so we don't get an exception when reading the file contents and just parsing them via gson
            Files.writeString(file, emptyFileContent);
        }
        // File already exists
        catch(FileAlreadyExistsException e){ }
    }

    public static void createDefaultPath() throws IOException {
        Files.createDirectories(getDefaultPath());
    }

    public static Path getDefaultPath(){
        return FabricLoader.getInstance().getGameDir().resolve(".onlywaypoints");
    }

    public static Path getSavePath(){
        return getDefaultPath().resolve(".config");
    }

    public static void writeSave(OnlyWaypointClientOptions options) throws IOException {
        Gson gson = new Gson();
        String json = gson.toJson(options);

        writeFile(getSavePath(), json);
    }

    public static OnlyWaypointClientOptions getSave() throws IOException {
        String saveContents = readJSONObjectFileContents(getSavePath());

        Gson gson = new Gson();

        return gson.fromJson(saveContents, OnlyWaypointClientOptions.class);
    }
}

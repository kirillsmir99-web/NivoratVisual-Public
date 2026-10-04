package rtx.nv.api.mods.chatanim.config;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;

public final class ModConfig {
    private static ModConfig instance;
    private static final Gson GSON;
    private transient File configFile;
    public boolean enableMessageAnimation = true;
    public boolean enableTextFieldAnimation = true;
    public boolean removeMessageIndicator = true;
    public int fadeTimeMessage = 150;
    public int fadeTimeTextField = 170;

    static {
        GSON = new GsonBuilder().setPrettyPrinting().create();
    }

    public void load(File file) {
        this.configFile = file;
        if (!file.exists()) {
            this.save();
            return;
        }
        try (FileReader fileReader = new FileReader(file);){
            ModConfig modConfig = (ModConfig)GSON.fromJson((Reader)fileReader, ModConfig.class);
            if (modConfig != null) {
                instance = modConfig;
                ModConfig.instance.configFile = file;
            }
        }
        catch (IOException iOException) {
            rtx.nv.NV.LOGGER.error("Failed to load chat animation config", iOException);
        }
    }

    public void save() {
        if (this.configFile == null) {
            return;
        }
        try (FileWriter fileWriter = new FileWriter(this.configFile);){
            GSON.toJson((Object)this, (Appendable)fileWriter);
        }
        catch (IOException iOException) {
            rtx.nv.NV.LOGGER.error("Failed to save chat animation config", iOException);
        }
    }

    public static ModConfig getConfig() {
        if (instance == null) {
            instance = new ModConfig();
        }
        return instance;
    }
}


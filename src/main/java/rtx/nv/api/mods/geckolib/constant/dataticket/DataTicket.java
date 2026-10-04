package rtx.nv.api.mods.geckolib.constant.dataticket;

import com.google.common.reflect.TypeToken;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Objects;

public class DataTicket<D> {
    static final Map<Pair<Type, String>, DataTicket<?>> IDENTITY_CACHE = new Object2ObjectOpenHashMap();
    private final String id;
    private final Class<? extends D> objectType;
    private final Type dataType;

    DataTicket(String string, Class<? extends D> clazz, Type type) {
        this.id = string;
        this.objectType = clazz;
        this.dataType = type;
    }

    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof DataTicket<?> dataTicket)) {
            return false;
        }
        return this.objectType == dataTicket.objectType && this.id.equals(dataTicket.id);
    }

    public String toString() {
        return "DataTicket{" + this.id + ": " + this.objectType.getName() + "}";
    }

    public int hashCode() {
        return Objects.hash(this.id, this.objectType);
    }

    public String id() {
        return this.id;
    }

    public static <D> DataTicket<D> create(String string, Class<? super D> clazz, TypeToken<D> typeToken) {
        return (DataTicket<D>) IDENTITY_CACHE.computeIfAbsent((Pair<Type, String>)Pair.of(typeToken.getType(), string), pair -> new DataTicket<>(string, (Class<? extends D>) clazz, typeToken.getType()));
    }

    @SuppressWarnings("unchecked")
    public static <D> DataTicket<D> create(String string, Class<? extends D> clazz) {
        return (DataTicket<D>) IDENTITY_CACHE.computeIfAbsent((Pair<Type, String>)(Pair<?, ?>)Pair.of(clazz, string), pair -> new DataTicket<>(string, clazz, clazz));
    }

    public static <D> DataTicket<D> of(String string, TypeToken<D> typeToken) {
        return DataTicket.create(string, (Class<? super D>) typeToken.getRawType(), typeToken);
    }

    public static <D> DataTicket<D> of(String string, Class<? extends D> clazz) {
        return DataTicket.create(string, clazz);
    }

    public Class<? extends D> objectType() {
        return this.objectType;
    }

    public Type dataType() {
        return this.dataType;
    }

    public boolean canExtractFrom(Object object) {
        if (object instanceof rtx.nv.api.mods.geckolib.renderer.base.GeoRenderState renderState) {
            return renderState.hasGeckolibData(this);
        }
        return false;
    }
}

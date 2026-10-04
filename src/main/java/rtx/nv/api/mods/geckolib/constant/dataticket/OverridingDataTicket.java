package rtx.nv.api.mods.geckolib.constant.dataticket;

import com.google.common.reflect.TypeToken;
import it.unimi.dsi.fastutil.Pair;
import java.lang.reflect.Type;
import java.util.function.Function;
import rtx.nv.api.mods.geckolib.renderer.base.GeoRenderState;

public final class OverridingDataTicket<D, C> extends DataTicket<D> {
    private final Class<C> overriddenClass;
    private final Function<C, D> valueExtractor;

    private OverridingDataTicket(String string, Class<? extends D> clazz, Type type, Class<C> clazz2, Function<C, D> function) {
        super(string, clazz, type);
        this.overriddenClass = clazz2;
        this.valueExtractor = function;
    }

    @SuppressWarnings("unchecked")
    public static <D, C> OverridingDataTicket<D, C> create(String string, Class<? extends D> clazz, Class<C> clazz2, Function<C, D> function) {
        return (OverridingDataTicket<D, C>) IDENTITY_CACHE.computeIfAbsent((Pair<Type, String>)(Pair<?, ?>)Pair.of(clazz, string), pair -> new OverridingDataTicket<>(string, clazz, clazz, clazz2, function));
    }

    public static <D, C> OverridingDataTicket<D, C> create(String string, Class<? extends D> clazz, TypeToken<D> typeToken, Class<C> clazz2, Function<C, D> function) {
        return (OverridingDataTicket<D, C>) IDENTITY_CACHE.computeIfAbsent((Pair<Type, String>)Pair.of(typeToken.getType(), string), pair -> new OverridingDataTicket<>(string, clazz, typeToken.getType(), clazz2, function));
    }

    public <R extends GeoRenderState> boolean canExtractFrom(R r) {
        return this.overriddenClass.isAssignableFrom(r.getClass());
    }

    public D extractFrom(C c) {
        return this.valueExtractor.apply(c);
    }

    public D extractFromState(GeoRenderState state) {
        return this.valueExtractor.apply(this.overriddenClass.cast(state));
    }

    public Class<C> getOverriddenClass() {
        return this.overriddenClass;
    }

    public Function<C, D> getValueExtractor() {
        return this.valueExtractor;
    }
}

package rtx.nv.api.mods.accountswitcher.ias.utils;
import java.util.Objects;

public final class Holder<T> {
    private T value;

    public Holder() {
        this.value = null;
    }

    public Holder(T t) {
        this.value = t;
    }

    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof Holder)) {
            return false;
        }
        Holder holder = (Holder)object;
        return Objects.equals(this.value, holder.value);
    }

    public String toString() {
        return "Holder{value=" + String.valueOf(this.value) + "}";
    }

    public int hashCode() {
        return Objects.hashCode(this.value);
    }

    public T get() {
        return this.value;
    }

    public void set(T t) {
        this.value = t;
    }
}


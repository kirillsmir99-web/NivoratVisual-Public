package rtx.nv.api.mods.geckolib.object;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;

public class CompoundException
extends RuntimeException {
    private final List<String> messages = new ObjectArrayList();

    public CompoundException(String string) {
        this.messages.add(string);
    }

    public CompoundException withMessage(String string) {
        this.messages.add(string);
        return this;
    }
}


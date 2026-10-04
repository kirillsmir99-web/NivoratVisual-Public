package rtx.nv.api.invmanager;

public final class InventoryManager {
    private static final InventoryManager INSTANCE = new InventoryManager();

    public static InventoryManager getInstance() {
        return INSTANCE;
    }

    public boolean isActive() {
        return false;
    }
}
package rtx.nv.api.mods.accountswitcher.ias.account;

import java.io.DataOutput;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import rtx.nv.api.mods.accountswitcher.ias.auth.LoginData;
import rtx.nv.api.mods.accountswitcher.ias.auth.handlers.LoginHandler;

public final class OfflineAccount implements Account {
    private final String name;
    private final UUID uuid;

    public OfflineAccount(String name, UUID uuid) {
        this.name = name;
        this.uuid = uuid != null ? uuid : uuid(name);
    }

    public static UUID uuid(String name) {
        if (name == null) return UUID.randomUUID();
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String name() {
        return this.name;
    }

    @Override
    public String type() {
        return "offline";
    }

    @Override
    public void write(DataOutput out) throws IOException {
        out.writeUTF(this.name);
    }

    @Override
    public UUID uuid() {
        return this.uuid;
    }

    @Override
    public UUID skin() {
        return this.uuid;
    }

    @Override
    public boolean canLogin() {
        return true;
    }

    @Override
    public void login(LoginHandler handler) {
        if (handler != null) {
            handler.success(new LoginData(this.name, this.uuid, "ias:offline", false), false);
        }
    }

    @Override
    public boolean insecure() {
        return false;
    }

    @Override
    public String typeTipKey() {
        return "ias.type.offline";
    }
}

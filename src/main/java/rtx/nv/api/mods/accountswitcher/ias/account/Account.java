package rtx.nv.api.mods.accountswitcher.ias.account;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.UUID;
import rtx.nv.api.mods.accountswitcher.ias.auth.handlers.LoginHandler;

public interface Account {
    String name();
    String type();
    void write(DataOutput out) throws IOException;
    UUID uuid();
    UUID skin();
    boolean canLogin();
    void login(LoginHandler handler);
    boolean insecure();
    String typeTipKey();

    static void writeTyped(DataOutput dataOutput, Account account) throws IOException {
        String string = account.type();
        dataOutput.writeUTF(string);
        account.write(dataOutput);
    }

    static Account readTyped(DataInput in) throws IOException {
        String type = in.readUTF();
        String name = in.readUTF();
        return new OfflineAccount(name, null);
    }
}

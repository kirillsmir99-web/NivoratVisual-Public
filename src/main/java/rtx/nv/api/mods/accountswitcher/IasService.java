package rtx.nv.api.mods.accountswitcher;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.loader.api.FabricLoader;
import rtx.nv.api.mods.accountswitcher.ias.IAS;
import rtx.nv.api.mods.accountswitcher.ias.account.Account;
import rtx.nv.api.mods.accountswitcher.ias.account.OfflineAccount;
import rtx.nv.api.mods.accountswitcher.ias.auth.LoginData;
import rtx.nv.api.mods.accountswitcher.ias.auth.handlers.LoginHandler;
import rtx.nv.api.mods.accountswitcher.ias.config.IASConfig;
import rtx.nv.api.mods.accountswitcher.ias.config.IASStorage;
import rtx.nv.utils.session.SessionChanger;

public final class IasService {
    private static boolean initialized;

    private IasService() {
    }

    public static void remove(Account account) {
        IasService.ensureInitialized();
        IASStorage.ACCOUNTS.remove(account);
        IAS.saveStorage();
        if (IASConfig.matchesLastAccount(account)) {
            IASConfig.clearLastAccount();
            IAS.saveConfig();
        }
    }

    public static void apply(LoginData loginData) {
        SessionChanger.applyLoginData(loginData);
    }

    public static synchronized void close() {
        if (!initialized) {
            return;
        }
        IAS.close();
        initialized = false;
    }

    public static synchronized void ensureInitialized() {
        if (initialized) {
            return;
        }
        IAS.init(FabricLoader.getInstance().getGameDir(), FabricLoader.getInstance().getConfigDir());
        IasService.restoreLastAccount();
        initialized = true;
    }

    public static List<Account> accounts() {
        IasService.ensureInitialized();
        return IASStorage.ACCOUNTS;
    }

    public static void replaceRememberedAccount(Account account, Account account2) {
        IasService.ensureInitialized();
        if (!IASConfig.matchesLastAccount(account)) {
            return;
        }
        IASConfig.rememberLastAccount(account2, IASConfig.lastAccountOnline);
        IAS.saveConfig();
    }

    public static void rememberLastAccount(Account account, boolean bl) {
        IasService.ensureInitialized();
        IASConfig.rememberLastAccount(account, bl);
        IAS.saveConfig();
    }

    public static void addOffline(String string) {
        IasService.ensureInitialized();
        IASStorage.ACCOUNTS.removeIf(account -> {
            OfflineAccount offlineAccount;
            return account instanceof OfflineAccount && (offlineAccount = (OfflineAccount)account).name().equalsIgnoreCase(string);
        });
        IASStorage.ACCOUNTS.add((Account)new OfflineAccount(string, null));
        IAS.saveStorage();
    }

    private static void restoreLastAccount() {
        if (!IASConfig.restoreLastAccount || IASConfig.lastAccountName == null || IASConfig.lastAccountUuid == null) {
            return;
        }
        Account account = IASStorage.ACCOUNTS.stream().filter(IASConfig::matchesLastAccount).findFirst().orElse(null);
        if (account == null) {
            return;
        }
        if (!IASConfig.lastAccountOnline || account instanceof OfflineAccount || !account.canLogin()) {
            SessionChanger.applyLoginData(new LoginData(account.name(), OfflineAccount.uuid((String)account.name()), "ias:offline", false));
            return;
        }
        account.login(new LoginHandler() {
            @Override
            public void error(Throwable throwable) {
                // Ignore background error
            }

            @Override
            public boolean cancelled() {
                return false;
            }

            @Override
            public void stage(String str, Object... objects) {
            }

            @Override
            public void success(LoginData loginData, boolean bl) {
                SessionChanger.applyLoginData(loginData);
            }

            @Override
            public CompletableFuture<String> password() {
                return CompletableFuture.completedFuture(null);
            }
        });
    }

    public static boolean isRememberedAccount(Account account) {
        IasService.ensureInitialized();
        return IASConfig.matchesLastAccount(account);
    }

    public static void saveIfChanged(boolean bl) {
        if (bl) {
            IAS.saveStorage();
        }
    }
}


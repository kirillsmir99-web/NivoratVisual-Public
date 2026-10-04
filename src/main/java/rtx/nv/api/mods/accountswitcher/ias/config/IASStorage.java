package rtx.nv.api.mods.accountswitcher.ias.config;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.function.Predicate;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rtx.nv.api.mods.accountswitcher.ias.account.Account;

public final class IASStorage {
    private static final String DISCLAIMER = "> ENGLISH\nNotification about security of accounts stored in the \"In-Game Account Switcher\" mod:\nUNDER NO CIRCUMSTANCES SHOULD YOU SEND THIS FOLDER TO *ANYONE* (INCLUDING DEVELOPERS OF THIS MOD),\nEVEN IF IT APPEARS THAT THIS FOLDER IS FULLY EMPTY.\nIF YOU ACCIDENTALLY SENT THIS FOLDER TO ANYONE, PLEASE, VISIT THE FOLLOWING WEBSITE:\nhttps://account.microsoft.com/security\nAND CHANGE YOUR PASSWORD, THEN VISIT THE FOLLOWING WEBSITE:\nhttps://account.live.com/consent/manage\nAND REVOKE THE PERMISSIONS (ACCESS) TO THE \"In-Game Account Switcher\" APPLICATION,\nAND/OR ANY OTHER THAT YOU DO CAN'T RECOGNIZE OR YOU SUSPECT IT COULD ACCESS YOUR GAME ACCOUNT.\nAFTER REVOKING ACCESS YOU SHOULD *NOT* USE THIS MODIFICATION FOR 31 DAYS.\n(If you suspect someone has got access to your game account, revoke ALL permissions\nfor ALL applications and do *NOT* launch the game for 31 days at all)\n\n> \u0420\u0423\u0421\u0421\u041a\u0418\u0419 (RUSSIAN)\n\u0423\u0432\u0435\u0434\u043e\u043c\u043b\u0435\u043d\u0438\u0435 \u043e \u0431\u0435\u0437\u043e\u043f\u0430\u0441\u043d\u043e\u0441\u0442\u0438 \u0430\u043a\u043a\u0430\u0443\u043d\u0442\u043e\u0432 \u0438\u0437 \u043c\u043e\u0434\u0430 \"In-Game Account Switcher\":\n\u041d\u0418 \u041f\u0420\u0418 \u041a\u0410\u041a\u0418\u0425 \u041e\u0411\u0421\u0422\u041e\u042f\u0422\u0415\u041b\u042c\u0421\u0422\u0412\u0410\u0425 \u041d\u0415 \u041e\u0422\u041f\u0420\u0410\u0412\u041b\u042f\u0419\u0422\u0415 \u042d\u0422\u0423 \u041f\u0410\u041f\u041a\u0423 *\u041a\u041e\u041c\u0423-\u041b\u0418\u0411\u041e* (\u0412 \u0422\u041e\u041c \u0427\u0418\u0421\u041b\u0415 \u0418 \u0420\u0410\u0417\u0420\u0410\u0411\u041e\u0422\u0427\u0418\u041a\u0410\u041c \u042d\u0422\u041e\u0413\u041e \u041c\u041e\u0414\u0410),\n\u0414\u0410\u0416\u0415 \u0415\u0421\u041b\u0418 \u0412\u0410\u041c \u041a\u0410\u0416\u0415\u0422\u0421\u042f, \u0427\u0422\u041e \u042d\u0422\u0410 \u041f\u0410\u041f\u041a\u0410 \u041f\u041e\u041b\u041d\u041e\u0421\u0422\u042c\u042e \u041f\u0423\u0421\u0422\u0410\u042f.\n\u0415\u0421\u041b\u0418 \u0412\u042b \u0421\u041b\u0423\u0427\u0410\u0419\u041d\u041e \u041e\u0422\u041f\u0420\u0410\u0412\u0418\u041b\u0418 \u042d\u0422\u0423 \u041f\u0410\u041f\u041a\u0423 \u041a\u041e\u041c\u0423-\u041b\u0418\u0411\u041e, \u041f\u041e\u0416\u0410\u041b\u0423\u0419\u0421\u0422\u0410, \u0417\u0410\u0419\u0414\u0418\u0422\u0415 \u041d\u0410 \u0421\u041b\u0415\u0414\u0423\u042e\u0429\u0418\u0419 \u0412\u0415\u0411\u0421\u0410\u0419\u0422:\nhttps://account.microsoft.com/security\n\u0418 \u0421\u041c\u0415\u041d\u0418\u0422\u0415 \u0421\u0412\u041e\u0419 \u041f\u0410\u0420\u041e\u041b\u042c, \u041f\u041e\u0422\u041e\u041c \u0417\u0410\u0419\u0414\u0418\u0422\u0415 \u041d\u0410 \u0421\u041b\u0415\u0414\u0423\u042e\u0429\u0418\u0419 \u0412\u0415\u0411\u0421\u0410\u0419\u0422:\nhttps://account.live.com/consent/manage\n\u0418 \u041e\u0422\u0417\u041e\u0412\u0418\u0422\u0415 \u0420\u0410\u0417\u0420\u0415\u0428\u0415\u041d\u0418\u042f (\u0414\u041e\u0421\u0422\u0423\u041f) \u041a \u041f\u0420\u0418\u041b\u041e\u0416\u0415\u041d\u0418\u042e \"In-Game Account Switcher\"\n\u0418/\u0418\u041b\u0418 \u041b\u042e\u0411\u041e\u041c\u0423 \u0414\u0420\u0423\u0413\u041e\u041c\u0423, \u041a\u041e\u0422\u041e\u0420\u041e\u0415 \u0412\u042b \u041d\u0415 \u041c\u041e\u0416\u0415\u0422\u0415 \u041e\u041f\u041e\u0417\u041d\u0410\u0422\u042c \u0418\u041b\u0418 \u041f\u041e\u0414\u041e\u0417\u0420\u0415\u0412\u0410\u0415\u0422\u0415, \u0427\u0422\u041e \u041e\u041d\u041e \u041c\u041e\u0416\u0415\u0422\n\u041f\u041e\u041b\u0423\u0427\u0418\u0422\u042c \u0414\u041e\u0421\u0422\u0423\u041f \u041a \u0412\u0410\u0428\u0415\u041c\u0423 \u0418\u0413\u0420\u041e\u0412\u041e\u041c\u0423 \u0410\u041a\u041a\u0410\u0423\u041d\u0422\u0423.\n\u041f\u041e\u0421\u041b\u0415 \u041e\u0422\u0417\u042b\u0412\u0410 \u0414\u041e\u0421\u0422\u0423\u041f\u0410 \u0412\u042b *\u041d\u0415* \u0414\u041e\u041b\u0416\u041d\u042b \u0418\u0421\u041f\u041e\u041b\u042c\u0417\u041e\u0412\u0410\u0422\u042c \u042d\u0422\u0423 \u041c\u041e\u0414\u0418\u0424\u0418\u041a\u0410\u0426\u0418\u042e \u041a\u0410\u041a \u041c\u0418\u041d\u0418\u041c\u0423\u041c 31 \u0414\u0415\u041d\u042c.\n(\u0415\u0441\u043b\u0438 \u0432\u044b \u043f\u043e\u0434\u043e\u0437\u0440\u0435\u0432\u0430\u0435\u0442\u0435, \u0447\u0442\u043e \u043a\u0442\u043e-\u0442\u043e \u043f\u043e\u043b\u0443\u0447\u0438\u043b \u0434\u043e\u0441\u0442\u0443\u043f \u043a \u0432\u0430\u0448\u0435\u043c\u0443 \u0438\u0433\u0440\u043e\u0432\u043e\u043c\u0443 \u0430\u043a\u043a\u0430\u0443\u043d\u0442\u0443, \u043e\u0442\u0437\u043e\u0432\u0438\u0442\u0435 \u0412\u0421\u0415 \u0440\u0430\u0437\u0440\u0435\u0448\u0435\u043d\u0438\u044f\n\u0434\u043b\u044f \u0412\u0421\u0415\u0425 \u043f\u0440\u0438\u043b\u043e\u0436\u0435\u043d\u0438\u0439 \u0438 *\u041d\u0415* \u0437\u0430\u043f\u0443\u0441\u043a\u0430\u0439\u0442\u0435 \u0438\u0433\u0440\u0443 \u0432\u043e\u043e\u0431\u0449\u0435 \u043a\u0430\u043a \u043c\u0438\u043d\u0438\u043c\u0443\u043c 31 \u0434\u0435\u043d\u044c)\n";
    private static final List<String> DISCLAIMER_FILE_NAMES = List.of("READ_ME_IMPORTANT.txt", "\u041f\u0420\u041e\u0427\u0422\u0418_\u041c\u0415\u041d\u042f_\u0412\u0410\u0416\u041d\u041e.txt");
    public static final Logger LOGGER = LoggerFactory.getLogger((String)"IAS/IASStorage");
    public static final List<Account> ACCOUNTS = new ArrayList<Account>(0);
    public static boolean gameDisclaimerShown = false;

    private IASStorage() {
        throw new AssertionError((Object)"No instances.");
    }

    public static void load(Path path) {
        try {
            LOGGER.debug("IAS: Loading storage for {}...", (Object)path);
            Path path2 = path.resolve("_IAS_ACCOUNTS_DO_NOT_SEND_TO_ANYONE/.hidden");
            Path path3 = path2.resolve("accounts_v1.do_not_send_to_anyone");
            gameDisclaimerShown = Files.isRegularFile(path2.resolve("game_disclaimer_shown"), LinkOption.NOFOLLOW_LINKS);
            if (!Files.isRegularFile(path3, LinkOption.NOFOLLOW_LINKS)) {
                LOGGER.debug("IAS: Storage not found. Saving...");
                IASStorage.save(path);
                return;
            }
            path3 = path3.toRealPath(LinkOption.NOFOLLOW_LINKS);
            byte[] byArray = Files.readAllBytes(path3);
            try (DataInputStream dataInputStream = new DataInputStream(new InflaterInputStream(new ByteArrayInputStream(byArray)));){
                int n = dataInputStream.readUnsignedShort();
                ArrayList<Account> arrayList = new ArrayList<Account>(n);
                for (int i = 0; i < n; ++i) {
                    arrayList.add(Account.readTyped((DataInput)dataInputStream));
                }
                ACCOUNTS.addAll(arrayList);
                HashSet hashSet = new HashSet(ACCOUNTS.size());
                ACCOUNTS.removeIf(Predicate.not(hashSet::add));
                LOGGER.debug("IAS: Loaded {} (currently: {}) accounts from {}.", new Object[]{arrayList.size(), ACCOUNTS.size(), path3});
            }
        }
        catch (Throwable throwable) {
            LOGGER.warn("IAS: Unable to load IAS storage, starting with empty accounts: {}", throwable.toString());
        }
    }

    public static void save(Path path) {
        try {
            byte[] byArray;
            Account[] accountArray;
            LOGGER.debug("IAS: Saving storage into {}...", (Object)path);
            Path path2 = path.resolve("_IAS_ACCOUNTS_DO_NOT_SEND_TO_ANYONE/.hidden/accounts_v1.do_not_send_to_anyone");
            try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                 DeflaterOutputStream deflaterOutputStream = new DeflaterOutputStream(byteArrayOutputStream);
                 DataOutputStream dataOutputStream = new DataOutputStream(deflaterOutputStream);){
                accountArray = (Account[])ACCOUNTS.toArray(Account[]::new);
                dataOutputStream.writeShort(accountArray.length);
                for (Account account : accountArray) {
                    Account.writeTyped(dataOutputStream, account);
                }
                deflaterOutputStream.finish();
                byArray = byteArrayOutputStream.toByteArray();
            }
            Files.createDirectories(path2.getParent(), new FileAttribute[0]);
            try {
                Files.setAttribute(path2.getParent(), "dos:hidden", true, LinkOption.NOFOLLOW_LINKS);
            }
            catch (Throwable throwable) {
                // empty catch block
            }
            try {
                Files.setAttribute(path2.getParent(), "dos:system", true, LinkOption.NOFOLLOW_LINKS);
            }
            catch (Throwable throwable) {
                // empty catch block
            }
            Files.write(path2, byArray, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE, StandardOpenOption.SYNC, StandardOpenOption.DSYNC, LinkOption.NOFOLLOW_LINKS);
            LOGGER.debug("IAS: Saved {} accounts to {}.", (Object)accountArray.length, (Object)path2);
        }
        catch (Throwable throwable) {
            throw new RuntimeException("Unable to save IAS storage.", throwable);
        }
    }

    public static void gameDisclaimerShown(Path path) {
        try {
            LOGGER.debug("IAS: Marking in-game disclaimers as shown into {}...", (Object)path);
            gameDisclaimerShown = true;
            Path path2 = path.resolve("_IAS_ACCOUNTS_DO_NOT_SEND_TO_ANYONE/.hidden/game_disclaimer_shown");
            Files.createDirectories(path2.getParent(), new FileAttribute[0]);
            Files.createFile(path2, new FileAttribute[0]);
            LOGGER.debug("IAS: Marked in-game disclaimers as shown to {}.", (Object)path2);
        }
        catch (Throwable throwable) {
            throw new RuntimeException("Unable to mark game disclaimer as shown.", throwable);
        }
    }

    public static void disclaimers(Path path) {
        try {
            LOGGER.debug("IAS: Writing disclaimers into {}...", (Object)path);
            path = path.resolve("_IAS_ACCOUNTS_DO_NOT_SEND_TO_ANYONE");
            Files.createDirectories(path, new FileAttribute[0]);
            byte[] byArray = DISCLAIMER.getBytes(StandardCharsets.UTF_8);
            for (String string : DISCLAIMER_FILE_NAMES) {
                try {
                    Path path2 = path.resolve(string);
                    if (Files.isRegularFile(path2, LinkOption.NOFOLLOW_LINKS) && Files.size(path2) == (long)byArray.length) continue;
                    Files.write(path2, byArray, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS);
                }
                catch (Throwable throwable) {
                    if (!string.equals("READ_ME_IMPORTANT.txt")) continue;
                    throw throwable;
                }
            }
            LOGGER.debug("IAS: Disclaimers ({}) written to {}.", DISCLAIMER_FILE_NAMES, (Object)path);
        }
        catch (Throwable throwable) {
            LOGGER.error("Unable to write IAS disclaimers.", throwable);
        }
    }
}


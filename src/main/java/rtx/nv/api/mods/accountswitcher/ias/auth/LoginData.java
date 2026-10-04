package rtx.nv.api.mods.accountswitcher.ias.auth;

import java.util.UUID;

public record LoginData(String name, UUID uuid, String token, boolean online) {}

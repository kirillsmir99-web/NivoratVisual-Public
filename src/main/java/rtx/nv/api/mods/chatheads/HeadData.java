package rtx.nv.api.mods.chatheads;
import net.minecraft.client.network.PlayerListEntry;

public final class HeadData {
    public static final HeadData EMPTY = new HeadData(null, -1);
    public final PlayerListEntry playerInfo;
    public final int codePointIndex;

    public HeadData(PlayerListEntry playerListEntry, int n) {
        if (playerListEntry == null && n >= 0) {
            throw new AssertionError();
        }
        this.playerInfo = playerListEntry;
        this.codePointIndex = n;
    }

    public static HeadData of(PlayerListEntry playerListEntry) {
        if (playerListEntry == null) {
            return EMPTY;
        }
        return new HeadData(playerListEntry, -1);
    }

    public boolean hasHeadPosition() {
        return this.codePointIndex >= 0;
    }
}


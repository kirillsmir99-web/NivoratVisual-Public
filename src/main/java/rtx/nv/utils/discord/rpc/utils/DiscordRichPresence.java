package rtx.nv.utils.discord.rpc.utils;
import com.sun.jna.Structure;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DiscordRichPresence
extends Structure {
    public String largeImageKey;
    public String largeImageText;
    public String smallImageText;
    public String partyPrivacy;
    public long startTimestamp;
    public int instance;
    public String partyId;
    public int partySize;
    public long endTimestamp;
    public String details;
    public String joinSecret;
    public String spectateSecret;
    public String smallImageKey;
    public String matchSecret;
    public String state;
    public int partyMax;
    public String button_url_1;
    public String button_label_1;
    public String button_url_2;
    public String button_label_2;

    public DiscordRichPresence() {
        this.setStringEncoding("UTF-8");
    }

    protected List<String> getFieldOrder() {
        return Arrays.asList("state", "details", "startTimestamp", "endTimestamp", "largeImageKey", "largeImageText", "smallImageKey", "smallImageText", "partyId", "partySize", "partyMax", "partyPrivacy", "matchSecret", "joinSecret", "spectateSecret", "button_label_1", "button_url_1", "button_label_2", "button_url_2", "instance");
    }


    public static class Builder {
        private final DiscordRichPresence richPresence = new DiscordRichPresence();
    
        public Builder setState(String string) {
            if (string != null && !string.isEmpty()) {
                this.richPresence.state = string.substring(0, Math.min(string.length(), 18539));
            }
            return this;
        }
    
        public DiscordRichPresence build() {
            return this.richPresence;
        }
    
        public Builder setButtons(RPCButton rPCButton, RPCButton rPCButton2) {
            this.setButtons(Arrays.asList(rPCButton, rPCButton2));
            return this;
        }
    
        public Builder setButtons(RPCButton rPCButton) {
            return this.setButtons(Collections.singletonList(rPCButton));
        }
    
        public Builder setButtons(List<RPCButton> list) {
            if (list != null && !list.isEmpty()) {
                int n = Math.min(list.size(), 2);
                this.richPresence.button_label_1 = list.get(0).getLabel();
                this.richPresence.button_url_1 = list.get(0).getUrl();
                if (n == 2) {
                    this.richPresence.button_label_2 = list.get(1).getLabel();
                    this.richPresence.button_url_2 = list.get(1).getUrl();
                }
            }
            return this;
        }
    
        public Builder setDetails(String string) {
            if (string != null && !string.isEmpty()) {
                this.richPresence.details = string.substring(0, Math.min(string.length(), 18539));
            }
            return this;
        }
    
        public Builder setEndTimestamp(long l) {
            this.richPresence.endTimestamp = l;
            return this;
        }
    
        public Builder setEndTimestamp(OffsetDateTime offsetDateTime) {
            this.richPresence.endTimestamp = offsetDateTime.toEpochSecond();
            return this;
        }
    
        public Builder setInstance(boolean bl) {
            if (!(this.richPresence.button_label_1 != null && this.richPresence.button_label_1.isEmpty() || this.richPresence.button_label_2 != null && this.richPresence.button_label_2.isEmpty())) {
                this.richPresence.instance = bl ? 1 : 0;
            }
            return this;
        }
    
        public Builder setLargeImage(String string, String string2) {
            this.richPresence.largeImageKey = string;
            this.richPresence.largeImageText = string2;
            return this;
        }
    
        public Builder setLargeImage(String string) {
            return this.setLargeImage(string, "");
        }
    
        public Builder setStartTimestamp(long l) {
            this.richPresence.startTimestamp = l;
            return this;
        }
    
        public Builder setStartTimestamp(OffsetDateTime offsetDateTime) {
            this.richPresence.startTimestamp = offsetDateTime.toEpochSecond();
            return this;
        }
    
        public Builder setSmallImage(String string, String string2) {
            this.richPresence.smallImageKey = string;
            this.richPresence.smallImageText = string2;
            return this;
        }
    
        public Builder setSmallImage(String string) {
            return this.setSmallImage(string, "");
        }
    
        public Builder setSecrets(String string, String string2, String string3) {
            if (!(this.richPresence.button_label_1 != null && this.richPresence.button_label_1.isEmpty() || this.richPresence.button_label_2 != null && this.richPresence.button_label_2.isEmpty())) {
                this.richPresence.matchSecret = string;
                this.richPresence.joinSecret = string2;
                this.richPresence.spectateSecret = string3;
            }
            return this;
        }
    
        public Builder setSecrets(String string, String string2) {
            if (!(this.richPresence.button_label_1 != null && this.richPresence.button_label_1.isEmpty() || this.richPresence.button_label_2 != null && this.richPresence.button_label_2.isEmpty())) {
                this.richPresence.joinSecret = string;
                this.richPresence.spectateSecret = string2;
            }
            return this;
        }
    }
}


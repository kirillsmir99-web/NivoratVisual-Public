package rtx.nv.api.mods.chatheads;
import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.util.Pair;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.ObjectTextContent;
import net.minecraft.text.PlainTextContent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextContent;
import net.minecraft.text.TextVisitFactory;
import net.minecraft.text.TranslatableTextContent;
import net.minecraft.text.object.PlayerTextObjectContents;
import net.minecraft.text.object.TextObjectContents;
import net.minecraft.util.Formatting;
import rtx.nv.api.mods.chatheads.ChatHeads;
import rtx.nv.api.mods.chatheads.ChatHeads.PlayerInfoCache;
import rtx.nv.api.mods.chatheads.HeadData;

public class ComponentProcessor {
    public static ArrayList<Text> split(Text text2) {
        ArrayList<Text> arrayList = new ArrayList<Text>();
        ComponentProcessor.walkTree(text2, (text, style2) -> {
            TextContent textContent = text.getContent();
            if (textContent instanceof PlainTextContent plainTextContent) {
                Style[] styleArray = new Style[]{style2};
                StringBuilder stringBuilder = new StringBuilder();
                TextVisitFactory.visitFormatted(plainTextContent.string(), styleArray[0], (n, style, n2) -> {
                    if (!style.equals(styleArray[0])) {
                        if (!stringBuilder.isEmpty()) {
                            arrayList.add(Text.literal(stringBuilder.toString()).setStyle(styleArray[0]));
                        }
                        stringBuilder.setLength(0);
                        styleArray[0] = style;
                    }
                    stringBuilder.appendCodePoint(n2);
                    return true;
                });
                if (!stringBuilder.isEmpty()) {
                    arrayList.add(Text.literal(stringBuilder.toString()).setStyle(styleArray[0]));
                }
            } else {
                MutableText copy = text.copyContentOnly().setStyle(style2);
                arrayList.add(copy);
            }
        });
        return arrayList;
    }

    public static Text join(List<Text> list) {
        if (list.size() == 1) {
            return list.getFirst();
        }
        MutableText mutableText = Text.empty();
        for (Text text : list) {
            mutableText.getSiblings().add(text);
        }
        return mutableText;
    }

    public static void walkTree(Text text, ComponentProcessor.StyledComponentConsumer styledComponentConsumer) {
        ComponentProcessor.walkTree(text, Style.EMPTY, styledComponentConsumer);
    }

    public static void walkTree(Text text, Style style, ComponentProcessor.StyledComponentConsumer styledComponentConsumer) {
        style = text.getStyle().withParent(style);
        styledComponentConsumer.accept(text, style);
        for (Text text2 : text.getSiblings()) {
            ComponentProcessor.walkTree(text2, style, styledComponentConsumer);
        }
    }

    public static PlayerListEntry addChatHeadForClickTellCommand(ArrayList<Text> arrayList2, ChatHeads.PlayerInfoCache playerInfoCache) {
        for (int i = 0; i < arrayList2.size(); ++i) {
            int n;
            MutableText mutableText;
            PlayerListEntry playerListEntry;
            Text text = arrayList2.get(i);
            String string = ComponentProcessor.getTellReceiver(text);
            if (string != null && (playerListEntry = playerInfoCache.get(string)) != null) {
                MutableText mutableText2 = ComponentProcessor.createChatHeadComponent(playerListEntry, text);
                mutableText = Text.empty().append((Text)mutableText2).append(text);
                arrayList2.set(i, (Text)mutableText);
                return playerListEntry;
            }
            TextContent textContent = text.getContent();
            if (textContent instanceof TranslatableTextContent translatableTextContent) {
                final int idx = i;
                PlayerListEntry res = ComponentProcessor.processTranslatableArguments(text, translatableTextContent, list -> ComponentProcessor.addChatHeadForClickTellCommand(list, playerInfoCache), replacement -> arrayList2.set(idx, replacement));
                if (res != null) {
                    return res;
                }
            }
        }
        return null;
    }

    public static void walkLiteralSequencesAndTranslatables(ArrayList<Text> arrayList, Predicate<ComponentProcessor.FoundLiteralSequence> predicate, Predicate<ComponentProcessor.FoundTranslatable> predicate2) {
        StringBuilder stringBuilder = new StringBuilder();
        int n = -1;
        int n2 = -1;
        for (int i = 0; i < arrayList.size(); ++i) {
            TranslatableTextContent translatableTextContent;
            TextContent textContent = arrayList.get(i).getContent();
            if (textContent instanceof PlainTextContent) {
                PlainTextContent plainTextContent = (PlainTextContent)textContent;
                if (n == -1) {
                    n = i;
                }
                n2 = i;
                stringBuilder.append(plainTextContent.string());
                continue;
            }
            if (!stringBuilder.isEmpty() && predicate.test(new ComponentProcessor.FoundLiteralSequence(stringBuilder.toString(), n, n2))) {
                return;
            }
            stringBuilder = new StringBuilder();
            n = -1;
            if (!(textContent instanceof TranslatableTextContent) || !predicate2.test(new ComponentProcessor.FoundTranslatable(translatableTextContent = (TranslatableTextContent)textContent, i))) continue;
            return;
        }
        n2 = arrayList.size() - 1;
        if (!stringBuilder.isEmpty() && predicate.test(new ComponentProcessor.FoundLiteralSequence(stringBuilder.toString(), n, n2))) {
            return;
        }
    }

    private static /* synthetic */ void lambda_addChatHeadForClickTellCommand_3(ArrayList arrayList, int n, Text text) {
        arrayList.set(n, text);
    }

    public static <T> T processTranslatableArguments(Text text, TranslatableTextContent translatableTextContent, Function<ArrayList<Text>, T> function, Consumer<Text> consumer) {
        Object[] objectArray = translatableTextContent.getArgs();
        int n = Objects.equals(translatableTextContent.getKey(), "chat.type.text") ? 1 : objectArray.length;
        for (int i = 0; i < n; ++i) {
            Object rawArg = objectArray[i];
            if (rawArg instanceof String strArg) {
                rawArg = Text.literal(strArg);
                objectArray[i] = rawArg;
            }
            if (!(rawArg instanceof Text textArg)) continue;
            ArrayList<Text> splitList = ComponentProcessor.split(textArg);
            T t = function.apply(splitList);
            if (t == null) continue;
            Text text2 = ComponentProcessor.join(splitList);
            Object[] objectArray2 = Arrays.copyOf(objectArray, objectArray.length);
            objectArray2[i] = text2;
            MutableText mutableText = Text.translatableWithFallback(translatableTextContent.getKey(), translatableTextContent.getFallback(), objectArray2);
            mutableText.setStyle(text.getStyle());
            text.getSiblings().forEach(sibling -> mutableText.append(sibling));
            consumer.accept(mutableText);
            return t;
        }
        return null;
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    public static boolean containsPlayerSprite(ArrayList<Text> arrayList) {
        Iterator<Text> iterator = arrayList.iterator();
        while (true) {
            if (!iterator.hasNext()) {
                return false;
            }
            Text text = iterator.next();
            TextContent textContent = text.getContent();
            if (!(textContent instanceof ObjectTextContent)) continue;
            ObjectTextContent objectTextContent = (ObjectTextContent)textContent;
            try {
                // empty try
            }
            catch (Throwable throwable) {
                throw new MatchException(throwable.toString(), throwable);
            }
            TextObjectContents textObjectContents = objectTextContent.contents();
            TextObjectContents textObjectContents2 = textObjectContents;
            if (textObjectContents2 instanceof PlayerTextObjectContents) break;
        }
        return true;
    }

    public static PlayerListEntry addChatHeadForPlayerName(ArrayList<Text> arrayList, ChatHeads.PlayerInfoCache playerInfoCache) {
        PlayerListEntry[] playerListEntryArray = new PlayerListEntry[1];
        ComponentProcessor.walkLiteralSequencesAndTranslatables(arrayList, foundLiteralSequence -> {
            HeadData headData = ChatHeads.scanForPlayerName(foundLiteralSequence.text, playerInfoCache);
            if (headData == HeadData.EMPTY) {
                return false;
            }
            int n = headData.codePointIndex;
            for (int i = foundLiteralSequence.startIndex; i <= foundLiteralSequence.endIndex; ++i) {
                MutableText mutableText;
                Text text = (Text)arrayList.get(i);
                PlainTextContent plainTextContent = (PlainTextContent)text.getContent();
                int n2 = (int)plainTextContent.string().codePoints().count();
                if (n >= n2) {
                    n -= n2;
                    continue;
                }
                MutableText mutableText2 = ComponentProcessor.createChatHeadComponent(headData.playerInfo, text);
                Pair<MutableText, MutableText> pair = ComponentProcessor.splitLiteral(text, n);
                if (pair == null) {
                    mutableText = Text.empty().append((Text)mutableText2).append(text);
                } else {
                    MutableText mutableText3 = (MutableText)pair.getFirst();
                    MutableText mutableText4 = (MutableText)pair.getSecond();
                    mutableText = Text.empty().append((Text)mutableText3).append((Text)mutableText2).append((Text)mutableText4);
                }
                arrayList.set(i, (Text)mutableText);
                playerListEntryArray[0] = headData.playerInfo;
                return true;
            }
            return false;
        }, foundTranslatable -> {
            TranslatableTextContent translatableTextContent;
            Text text2 = (Text)arrayList.get(foundTranslatable.index);
            PlayerListEntry playerListEntry = ComponentProcessor.processTranslatableArguments(text2, translatableTextContent = foundTranslatable.contents, subList -> ComponentProcessor.addChatHeadForPlayerName(subList, playerInfoCache), text -> arrayList.set(foundTranslatable.index, (Text)text));
            if (playerListEntry != null) {
                playerListEntryArray[0] = playerListEntry;
                return true;
            }
            return false;
        });
        return playerListEntryArray[0];
    }

    public static MutableText createChatHeadComponent(PlayerListEntry playerListEntry) {
        return Text.object((TextObjectContents)new PlayerTextObjectContents(ProfileComponent.ofStatic((GameProfile)playerListEntry.getProfile()), playerListEntry.shouldShowHat())).formatted(Formatting.WHITE);
    }

    public static MutableText createChatHeadComponent(PlayerListEntry playerListEntry, Text text) {
        MutableText mutableText = ComponentProcessor.createChatHeadComponent(playerListEntry);
        if (text.getStyle().isStrikethrough()) {
            return mutableText.formatted(Formatting.STRIKETHROUGH);
        }
        return mutableText;
    }

    /*
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    public static String getTellReceiver(Text text) {
        Object object = text.getStyle().getClickEvent();
        if (!(object instanceof ClickEvent.SuggestCommand)) return null;
        ClickEvent.SuggestCommand suggestCommand = (ClickEvent.SuggestCommand)object;
        try {
            String string;
            String string2 = string = suggestCommand.command();
            if (string2 == null) return null;
            if (!string2.startsWith("/tell ")) return null;
            return string2.substring("/tell ".length()).trim();
        }
        catch (Throwable throwable) {
            throw new MatchException(throwable.toString(), throwable);
        }
    }

    public static String codePointSubstring(String string, int n, int n2) {
        int n3 = string.offsetByCodePoints(0, n);
        int n4 = string.offsetByCodePoints(n3, n2 - n);
        return string.substring(n3, n4);
    }

    public static String codePointSubstring(String string, int n) {
        return string.substring(string.offsetByCodePoints(0, n));
    }

    public static Pair<MutableText, MutableText> splitLiteral(Text text, int n) {
        if (n == 0) {
            return null;
        }
        String string = ((PlainTextContent)text.getContent()).string();
        String string2 = ComponentProcessor.codePointSubstring(string, 0, n);
        String string3 = ComponentProcessor.codePointSubstring(string, n);
        MutableText mutableText = Text.literal((String)string2).setStyle(text.getStyle());
        MutableText mutableText2 = Text.literal((String)string3).setStyle(text.getStyle());
        text.getSiblings().forEach(arg_0 -> ((MutableText)mutableText2).append(arg_0));
        return new Pair((Object)mutableText, (Object)mutableText2);
    }


    public static record FoundLiteralSequence(String text, int startIndex, int endIndex) {
    }
    
        public static record FoundTranslatable(TranslatableTextContent contents, int index) {
    }
    
        public static interface StyledComponentConsumer {
        public void accept(Text var1, Style var2);
    }
}


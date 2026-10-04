package rtx.nv.api.chat.commands.helpers;

import java.util.ArrayList;
import java.util.stream.Stream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;


public class TabCompleteHelper {
    private final List<String> candidates = new ArrayList<>();

    public TabCompleteHelper append(String... items) {
        if (items != null) candidates.addAll(Arrays.asList(items));
        return this;
    }

    public TabCompleteHelper append(Collection<String> items) {
        if (items != null) candidates.addAll(items);
        return this;
    }

    public TabCompleteHelper addCommands(rtx.nv.api.chat.commands.CommandManager cm) {
        if (cm != null) {
            for (rtx.nv.api.chat.commands.Command c : cm.getCommands()) {
                this.candidates.add(c.getName());
            }
        }
        return this;
    }

    public TabCompleteHelper sortAlphabetically() {
        Collections.sort(candidates, String.CASE_INSENSITIVE_ORDER);
        return this;
    }

    public TabCompleteHelper filterPrefix(String prefix) {
        if (prefix == null || prefix.isEmpty()) return this;
        String lower = prefix.toLowerCase(Locale.ROOT);
        candidates.removeIf(s -> !s.toLowerCase(Locale.ROOT).startsWith(lower));
        return this;
    }

    public Stream<String> stream() {
  return candidates.stream();
    }
}
package rtx.nv.api.chat.commands.helpers;

import java.util.List;
import java.util.function.Function;

import net.minecraft.text.Text;
import rtx.nv.utils.chat.ChatMessage;

public class Paginator<T> {
    private final List<T> items;
    private int page = 1;
    private final int itemsPerPage = 8;

    public Paginator(List<T> items) {
        this.items = items;
    }

    public void setPage(int page) {
        this.page = Math.max(1, page);
    }

    public void display(Runnable headerPrinter, Function<T, Text> rowMapper, String cmdPrefix) {
        if (headerPrinter != null) headerPrinter.run();
        if (items == null || items.isEmpty()) return;
        int maxPages = (items.size() + itemsPerPage - 1) / itemsPerPage;
        int curPage = Math.min(page, maxPages);
        int start = (curPage - 1) * itemsPerPage;
        int end = Math.min(start + itemsPerPage, items.size());
        for (int i = start; i < end; i++) {
            ChatMessage.brandmessage(rowMapper.apply(items.get(i)));
        }
    }
}
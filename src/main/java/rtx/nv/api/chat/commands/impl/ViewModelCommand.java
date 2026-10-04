package rtx.nv.api.chat.commands.impl;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.util.Formatting;
import rtx.nv.api.chat.commands.Command;
import rtx.nv.api.modules.impl.Visuals.ViewModel;

public final class ViewModelCommand
extends Command {
    public ViewModelCommand() {
        super("vm", "ViewModel: \u0443\u043f\u0440\u0430\u0432\u043b\u0435\u043d\u0438\u0435 \u0440\u0430\u0441\u043f\u043e\u043b\u043e\u0436\u0435\u043d\u0438\u0435\u043c \u0440\u0443\u043a.", "viewmodel");
    }

    @Override
    public void execute(String string, String[] stringArray) {
        if (stringArray.length == 0) {
            this.usage();
            return;
        }
        if ("reset".equalsIgnoreCase(stringArray[0])) {
            ViewModel viewModel = ViewModel.getInstance();
            if (viewModel == null) {
                this.logDirect("\u041c\u043e\u0434\u0443\u043b\u044c ViewModel \u043d\u0435\u0434\u043e\u0441\u0442\u0443\u043f\u0435\u043d.", Formatting.RED);
                return;
            }
            viewModel.resetLayout();
            this.logDirect("\u0420\u0430\u0441\u043f\u043e\u043b\u043e\u0436\u0435\u043d\u0438\u0435 \u0440\u0443\u043a \u0441\u0431\u0440\u043e\u0448\u0435\u043d\u043e.", Formatting.GREEN);
            return;
        }
        this.usage();
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList("\u0423\u043f\u0440\u0430\u0432\u043b\u0435\u043d\u0438\u0435 ViewModel.", "> vm reset \u2014 \u0441\u0431\u0440\u043e\u0441\u0438\u0442\u044c \u043f\u043e\u043b\u043e\u0436\u0435\u043d\u0438\u0435 \u0438 \u0440\u0430\u0437\u043c\u0435\u0440 \u0440\u0443\u043a");
    }

    @Override
    public Stream<String> tabComplete(String string2, String[] stringArray) {
        if (stringArray.length == 1) {
            return Stream.of("reset").filter(string -> string.startsWith(stringArray[0].toLowerCase()));
        }
        return Stream.empty();
    }
}


package rtx.nv;

public final class ClientEdition {
    /**
     * Флаг сборки бесплатной публичной версии.
     * При true:
     * - Ватермарк зафиксирован по центру сверху, всегда активен и защищен от перемещения.
     * - Материал интерфейса принудительно зафиксирован на Мозаике, переключатель стилей скрыт.
     * - Эксклюзивные киллер-фичи (питомцы, эмоции, эффекты убийств, музыка, шейдерные руки, ESP) отключены.
     * - В меню доступны только базовые категории и проверенные визуалы.
     */
    public static final boolean IS_TRIAL = loadFreeEdition();

    private static boolean loadFreeEdition() {
        java.util.Properties properties = new java.util.Properties();
        try (var input = ClientEdition.class.getResourceAsStream("/nv-edition.properties")) {
            if (input != null) properties.load(input);
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Cannot read client edition", exception);
        }
        return !"pro".equals(properties.getProperty("edition", "free"));
    }

    private ClientEdition() {
    }

    public static boolean isTrial() {
        return IS_TRIAL;
    }

    public static String getEditionName() {
        return IS_TRIAL ? "NivoratFreeVisual" : "NivoratVisual";
    }

    public static String getEditionTag() {
        return IS_TRIAL ? "FREE" : "PRO";
    }
}

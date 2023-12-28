package code.util.translate.base;

public interface TranslateAPI {
    boolean hasAuth();
    TranslateAuth auth();
    String translate(String text, String from, String to);
}

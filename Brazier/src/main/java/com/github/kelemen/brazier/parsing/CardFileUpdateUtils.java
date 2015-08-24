package com.github.kelemen.brazier.parsing;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import org.jtrim.collections.CollectionsEx;
import org.jtrim.utils.ExceptionHelper;

public final class CardFileUpdateUtils {
    private static boolean hasExt(Path path, String ext) {
        String fileName = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return fileName.endsWith(ext);
    }

    public static void processObjects(
            Path dir,
            String extension,
            BiConsumer<? super Path, ? super JsonObject> processor) throws IOException {
        ExceptionHelper.checkNotNullArgument(dir, "dir");
        ExceptionHelper.checkNotNullArgument(extension, "extension");
        ExceptionHelper.checkNotNullArgument(processor, "processor");

        try (DirectoryStream<Path> entityFiles = Files.newDirectoryStream(dir)) {
            for (Path entityFile: entityFiles) {
                if (hasExt(entityFile, extension)) {
                    try {
                        JsonObject entityObj = ParserUtils.fromJsonFile(entityFile);
                        processor.accept(entityFile, entityObj);
                    } catch (Exception ex) {
                        throw new IOException("Failed to parse " + entityFile.getFileName(), ex);
                    }
                }
            }
        }
    }

    public static void updateFiles(
            Path dir,
            String extension,
            Function<? super JsonObject, ? extends JsonObject> processor) throws IOException {
        ExceptionHelper.checkNotNullArgument(dir, "dir");
        ExceptionHelper.checkNotNullArgument(extension, "extension");
        ExceptionHelper.checkNotNullArgument(processor, "processor");

        Gson gson = new Gson();
        Charset charset = Charset.forName("UTF-8");
        processObjects(dir, extension, (file, obj) -> {
            try (Writer writer = Files.newBufferedWriter(file, charset);
                    JsonWriter jsonWriter = new JsonWriter(writer)) {
                jsonWriter.setIndent("    ");

                JsonObject newObj = processor.apply(obj);
                gson.toJson(newObj, jsonWriter);
            } catch (IOException ex) {
                throw ExceptionHelper.throwUnchecked(ex);
            }
        });
    }

    private static JsonElement normalizeArrays(JsonElement obj) {
        if (obj.isJsonObject()) {
            return normalizeArrays(obj.getAsJsonObject());
        }
        if (obj.isJsonArray()) {
            JsonArray array = obj.getAsJsonArray();
            if (array.size() == 1) {
                return array.get(0);
            }
            return array;
        }
        return obj;
    }

    private static JsonObject normalizeArrays(JsonObject obj) {
        JsonObject result = new JsonObject();
        for (Map.Entry<String, JsonElement> field: obj.entrySet()) {
            result.add(field.getKey(), normalizeArrays(field.getValue()));
        }
        return result;
    }

    private static void sortFields(JsonObject obj, Comparator<String> nameCmp, String... path) {
        JsonElement child = getChild(obj, path);
        forObjects(child, (childObj) -> {
            Set<Map.Entry<String, JsonElement>> fields = childObj.entrySet();

            List<Map.Entry<String, JsonElement>> newFields = new ArrayList<>(fields);
            newFields.sort((entry1, entry2) -> {
                return nameCmp.compare(entry1.getKey(), entry2.getKey());
            });

            childObj.entrySet().clear();

            for (Map.Entry<String, JsonElement> field: newFields) {
                childObj.add(field.getKey(), field.getValue());
            }
        });
    }

    @SafeVarargs
    private static <T extends Comparable<T>> Comparator<T> definedOrder(T... objs) {
        Map<T, Integer> order = CollectionsEx.newHashMap(objs.length);
        for (int i = 0; i < objs.length; i++) {
            order.put(objs[i], i);
        }

        return (T o1, T o2) -> {
            Integer order1 = order.get(o1);
            Integer order2 = order.get(o2);

            if (order1 != null) {
                if (order2 != null) {
                    return Integer.compare(order1, order2);
                }
                else {
                    return -1;
                }
            }
            else {
                if (order2 != null) {
                    return 1;
                }

                return o1.compareTo(o2);
            }
        };
    }

    private static JsonElement getChild(JsonObject obj, String... path) {
        if (path.length == 0) {
            return obj;
        }

        JsonObject child = obj;
        for (int i = 0; i < path.length - 1; i++) {
            JsonElement element = child.get(path[i]);
            if (element == null) {
                return null;
            }

            if (!element.isJsonObject()) {
                return null;
            }

            child = element.getAsJsonObject();
        }

        return child.get(path[path.length - 1]);
    }

    private static void forObjects(JsonElement element, Consumer<JsonObject> objProcessor) {
        if (element == null) {
            return;
        }

        if (element.isJsonObject()) {
            objProcessor.accept(element.getAsJsonObject());
        }
        else if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            int size = array.size();
            for (int i = 0; i < size; i++) {
                forObjects(array.get(i), objProcessor);
            }
        }
    }

    public static void normalizeCardFiles(Path dir, String extension) throws IOException {
        ExceptionHelper.checkNotNullArgument(dir, "dir");
        ExceptionHelper.checkNotNullArgument(extension, "extension");

        Comparator<String> topLevelOrder = definedOrder(
                "name",
                "displayName",
                "description",
                "class",
                "type",
                "keywords",
                "rarity",
                "collectible",
                "manaCost",
                "overload",
                "manaCostAdjusters",
                "chooseOne",
                "drawActions",
                "inHandAbility",
                "playActions",
                "secret",
                "minion",
                "weapon"
        );

        Comparator<String> playActionOrder = definedOrder(
                "targets",
                "requires",
                "class",
                "actions"
        );

        Comparator<String> battleCriesOrder = definedOrder(
                "requires",
                "actionCondition",
                "class"
        );

        Comparator<String> minionOrder = definedOrder(
                "displayName",
                "attack",
                "hp",
                "taunt",
                "stealth",
                "charge",
                "divineShield",
                "targetable",
                "maxAttackCount",
                "canAttack",
                "attackWithHp",
                "attackLeft",
                "attackRight",
                "battleCries",
                "ability",
                "deathRattle",
                "triggers"
        );

        updateFiles(dir, extension, (obj) -> {
            JsonObject result = obj;

            sortFields(obj, playActionOrder, "playActions");
            sortFields(obj, minionOrder, "minion");
            sortFields(obj, battleCriesOrder, "minion", "battleCries");

            sortFields(result, topLevelOrder);
            result = normalizeArrays(result);

            return result;
        });
    }

    private CardFileUpdateUtils() {
        throw new AssertionError();
    }
}

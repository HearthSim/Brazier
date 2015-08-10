package com.github.kelemen.hearthstone.emulator.parsing;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jtrim.utils.ExceptionHelper;

public final class UseTrackerJsonTree implements JsonTree {
    private static final UseTrackerJsonTree[] EMPTY_TREE_ARRAY = new UseTrackerJsonTree[0];

    private final JsonElement element;
    private final Map<String, UseTrackerJsonTree> children;
    private final UseTrackerJsonTree[] orderedChildren;
    private boolean used;

    public UseTrackerJsonTree(JsonElement element) {
        ExceptionHelper.checkNotNullArgument(element, "element");

        this.element = element;
        this.children = getChildren(element);
        this.orderedChildren = getIndexChildren(element, children);
        this.used = false;
    }

    private static Map<String, UseTrackerJsonTree> getChildren(JsonElement element) {
        if (!element.isJsonObject()) {
            return Collections.emptyMap();
        }

        JsonObject root = element.getAsJsonObject();
        Set<Map.Entry<String, JsonElement>> children = root.entrySet();
        Map<String, UseTrackerJsonTree> result = new LinkedHashMap<>(children.size() * 4 / 3 + 1);
        for (Map.Entry<String, JsonElement> entry: children) {
            result.put(entry.getKey(), new UseTrackerJsonTree(entry.getValue()));
        }
        return Collections.unmodifiableMap(result);
    }

    private static UseTrackerJsonTree[] getIndexChildren(
            JsonElement element,
            Map<String, UseTrackerJsonTree> children) {

        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            int childCount = array.size();
            List<UseTrackerJsonTree> result = new ArrayList<>(childCount);
            for (int i = 0; i < childCount; i++) {
                result.add(new UseTrackerJsonTree(array.get(i)));
            }
            return result.toArray(new UseTrackerJsonTree[result.size()]);
        }

        if (children.isEmpty()) {
            return EMPTY_TREE_ARRAY;
        }

        List<UseTrackerJsonTree> result = new ArrayList<>(children.size());
        for (UseTrackerJsonTree child: children.values()) {
            result.add(child);
        }
        return result.toArray(new UseTrackerJsonTree[result.size()]);
    }

    @Override
    public JsonElement getElement() {
        used = true;
        return element;
    }

    @Override
    public JsonTree getChild(String childName) {
        used = true;
        return children.get(childName);
    }

    @Override
    public int getChildCount() {
        used = true;
        return orderedChildren.length;
    }

    @Override
    public JsonTree getChild(int index) {
        used = true;
        return orderedChildren[index];
    }

    public boolean isRequestedAllElements() {
        if (!used) {
            return false;
        }

        for (UseTrackerJsonTree child: orderedChildren) {
            if (!child.isRequestedAllElements()) {
                return false;
            }
        }
        return true;
    }

    private static String concat(List<String> values, String separator) {
        StringBuilder result = new StringBuilder();
        for (String value: values) {
            if (result.length() > 0) {
                result.append(separator);
            }
            result.append(value);
        }
        return result.toString();
    }

    private void checkRequestedAllElements(List<String> parents) throws ObjectParsingException {
        if (!used) {
            throw new ObjectParsingException("The entry is unused at " + concat(parents, "/"));
        }

        if (children.isEmpty()) {
            int index = 0;
            for (UseTrackerJsonTree child: orderedChildren) {
                parents.add(Integer.toString(index));
                child.checkRequestedAllElements(parents);
                parents.remove(parents.size() - 1);
                index++;
            }
        }
        else {
            for (Map.Entry<String, UseTrackerJsonTree> entry: children.entrySet()) {
                parents.add(entry.getKey());
                entry.getValue().checkRequestedAllElements(parents);
                parents.remove(parents.size() - 1);
            }
        }
    }

    public void checkRequestedAllElements() throws ObjectParsingException {
        checkRequestedAllElements(new ArrayList<>());
    }

    @Override
    public String toString() {
        return getElement().toString();
    }
}

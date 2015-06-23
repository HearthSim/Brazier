package com.github.kelemen.hearthstone.emulator.parsing;

import com.google.gson.JsonPrimitive;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.jtrim.collections.CollectionsEx;
import org.jtrim.utils.ExceptionHelper;

import static com.github.kelemen.hearthstone.emulator.parsing.ParserUtils.getStringField;

public final class JsonDeserializer {
    public static final class Builder {
        private final CustomClassNameResolver classNameResolver;
        private final Map<Class<?>, List<RawObjectConverter<?>>> typeConverters;
        private final Map<Class<?>, CustomStringParser<?>> customStringParsers;
        private final Map<Class<?>, CollectionMergerWrapper<?>> typeMergers;

        public Builder(CustomClassNameResolver classNameResolver) {
            ExceptionHelper.checkNotNullArgument(classNameResolver, "classNameResolver");
            this.classNameResolver = classNameResolver;
            this.typeConverters = new HashMap<>();
            this.customStringParsers = new HashMap<>();
            this.typeMergers = new HashMap<>();
        }

        public <T> void setTypeMerger(
                Class<T> type,
                CollectionMerger<T> typeMerger) {
            ExceptionHelper.checkNotNullArgument(type, "type");
            ExceptionHelper.checkNotNullArgument(typeMerger, "typeMerger");

            typeMergers.put(type, new CollectionMergerWrapper<>(type, typeMerger));
        }

        public <Src, Dest> void addTypeConversion(
                Class<? extends Src> srcType,
                Class<Dest> destType,
                ObjectMapper<? super Src, ? extends Dest> objectMapper) {

            RawObjectConverter<Src> rawConverter = new RawObjectConverter<>(srcType, objectMapper);
            ExceptionHelper.checkNotNullArgument(destType, "destType");

            typeConverters.computeIfAbsent(destType, (key) -> new LinkedList<>()).add(rawConverter);
        }

        public <T> void setCustomStringParser(
                Class<T> destType,
                CustomStringParser<? extends T> customStringParser) {
            ExceptionHelper.checkNotNullArgument(destType, "destType");
            ExceptionHelper.checkNotNullArgument(customStringParser, "customStringParser");

            customStringParsers.put(destType, customStringParser);
        }

        public JsonDeserializer create() {
            return new JsonDeserializer(this);
        }
    }

    private static final Map<Class<?>, Class<?>> PRIMITIVE_TO_WRAPPER;
    private static final Map<Class<?>, Function<JsonPrimitive, Object>> JSON_PRIMITIVE_CONVERSIONS;

    static {
        Map<Class<?>, Class<?>> primitiveToWrapper = new HashMap<>();
        primitiveToWrapper.put(boolean.class, Boolean.class);
        primitiveToWrapper.put(char.class, Character.class);
        primitiveToWrapper.put(byte.class, Byte.class);
        primitiveToWrapper.put(short.class, Short.class);
        primitiveToWrapper.put(int.class, Integer.class);
        primitiveToWrapper.put(long.class, Long.class);
        primitiveToWrapper.put(float.class, Float.class);
        primitiveToWrapper.put(double.class, Double.class);
        primitiveToWrapper.put(void.class, Void.class);
        PRIMITIVE_TO_WRAPPER = Collections.unmodifiableMap(new HashMap<>(primitiveToWrapper));

        Map<Class<?>, Function<JsonPrimitive, Object>> jsonPrimitiveConversions = new HashMap<>();
        jsonPrimitiveConversions.put(boolean.class, JsonPrimitive::getAsBoolean);
        jsonPrimitiveConversions.put(Boolean.class, JsonPrimitive::getAsBoolean);
        jsonPrimitiveConversions.put(byte.class, JsonPrimitive::getAsByte);
        jsonPrimitiveConversions.put(Byte.class, JsonPrimitive::getAsByte);
        jsonPrimitiveConversions.put(short.class, JsonPrimitive::getAsShort);
        jsonPrimitiveConversions.put(Short.class, JsonPrimitive::getAsShort);
        jsonPrimitiveConversions.put(int.class, JsonPrimitive::getAsInt);
        jsonPrimitiveConversions.put(Integer.class, JsonPrimitive::getAsInt);
        jsonPrimitiveConversions.put(long.class, JsonPrimitive::getAsLong);
        jsonPrimitiveConversions.put(Long.class, JsonPrimitive::getAsLong);
        jsonPrimitiveConversions.put(float.class, JsonPrimitive::getAsFloat);
        jsonPrimitiveConversions.put(Float.class, JsonPrimitive::getAsFloat);
        jsonPrimitiveConversions.put(double.class, JsonPrimitive::getAsDouble);
        jsonPrimitiveConversions.put(Double.class, JsonPrimitive::getAsDouble);
        jsonPrimitiveConversions.put(String.class, JsonPrimitive::getAsString);
        JSON_PRIMITIVE_CONVERSIONS = Collections.unmodifiableMap(new HashMap<>(jsonPrimitiveConversions));
    }

    private final CustomClassNameResolver classNameResolver;
    private final Map<Class<?>, List<RawObjectConverter<?>>> typeConverters;
    private final Map<Class<?>, CustomStringParser<?>> customStringParsers;
    private final Map<Class<?>, CollectionMergerWrapper<?>> typeMergers;

    private JsonDeserializer(Builder builder) {
        this.classNameResolver = builder.classNameResolver;
        this.typeConverters = copyMapOfMap(builder.typeConverters);
        this.customStringParsers = new HashMap<>(builder.customStringParsers);
        this.typeMergers = new HashMap<>(builder.typeMergers);
    }

    private static <K, V> Map<K, List<V>> copyMapOfMap(Map<K, List<V>> src) {
        Map<K, List<V>> result = CollectionsEx.newHashMap(src.size());
        for (Map.Entry<K, List<V>> entry: src.entrySet()) {
            result.put(entry.getKey(), CollectionsEx.readOnlyCopy(entry.getValue()));
        }
        return result;
    }

    public <T> T toJavaObject(JsonTree element, Class<? extends T> expectedType) throws ObjectParsingException {
        return toJavaObject(element, expectedType, (type) -> {});
    }

    public <T> T toJavaObject(
            JsonTree element,
            Class<? extends T> expectedType,
            TypeChecker typeChecker) throws ObjectParsingException {
        ExceptionHelper.checkNotNullArgument(element, "element");
        ExceptionHelper.checkNotNullArgument(expectedType, "expectedType");
        ExceptionHelper.checkNotNullArgument(typeChecker, "typeChecker");

        Object result = toJavaObjectUnsafe(element, expectedType, typeChecker);
        Class<?> wrapperClass = PRIMITIVE_TO_WRAPPER.get(expectedType);
        if (wrapperClass != null) {
            @SuppressWarnings("unchecked")
            T boxedValue = (T)wrapperClass.cast(result);
            return boxedValue;
        }

        if (expectedType.isInstance(result)) {
            return expectedType.cast(result);
        }
        else {
            List<RawObjectConverter<?>> converters = typeConverters.getOrDefault(expectedType, Collections.emptyList());
            for (RawObjectConverter<?> converter: converters) {
                if (converter.appliesTo(result)) {
                    return expectedType.cast(converter.convert(result));
                }
            }
        }

        throw new ObjectParsingException("Unexpected object type: " + result.getClass().getName()
                + ". Expected: " + expectedType.getName());
    }

    private Object fromEnum(String str, Class<?> enumClass) throws ObjectParsingException {
        Object[] enumConstants = enumClass.getEnumConstants();
        Object matchWithCaseError = null;
        for (Object candidate: enumConstants) {
            String name = candidate.toString();
            if (str.equals(name)) {
                return candidate;
            }
            if (str.equalsIgnoreCase(name)) {
                matchWithCaseError = candidate;
            }
        }

        if (matchWithCaseError == null) {
            throw new ObjectParsingException("Invalid enum value for "
                    + enumClass.getSimpleName() + ": " + str
                    + ". Possible names: " + Arrays.toString(enumConstants));
        }
        return matchWithCaseError;
    }

    private Object toJavaObjectUnsafe(
            JsonTree element,
            Class<?> expectedType,
            TypeChecker typeChecker) throws ObjectParsingException {

        if (expectedType.isEnum()) {
            return fromEnum(element.getAsString(), expectedType);
        }
        else if (expectedType.isArray()) {
            Class<?> arrayElementType = expectedType.getComponentType();

            if (element.isJsonArray()) {
                int size = element.getChildCount();
                Object result = Array.newInstance(arrayElementType, size);
                for (int i = 0; i < size; i++) {
                    Array.set(result, i, toJavaObject(element.getChild(i), arrayElementType));
                }
                return result;
            }
            else {
                Object result = Array.newInstance(arrayElementType, 1);
                Array.set(result, 0, toJavaObject(element, arrayElementType));
                return result;
            }
        }
        else if (element.isJsonArray()) {
            CollectionMergerWrapper<?> merger = typeMergers.get(expectedType);
            if (merger != null) {
                return merger.parseFrom(this, element, typeChecker);
            }
        }

        if (element.isJsonObject()) {
            if (element.getChild("class") != null) {
                return toComplexJavaObject(element, typeChecker);
            }
            else {
                return toComplexJavaObject(element, expectedType, typeChecker);
            }
        }
        else {
            if (element.isJsonPrimitive()) {
                return parsePrimitive(element.getAsJsonPrimitive(), expectedType, typeChecker);
            }

            throw new ObjectParsingException("Unexpected JSON element type: " + element);
        }
    }

    private Object parsePrimitive(
            JsonPrimitive element,
            Class<?> expectedType,
            TypeChecker typeChecker) throws ObjectParsingException {

        CustomStringParser<?> customResolver = customStringParsers.get(expectedType);
        if (customResolver != null) {
            Object result = customResolver.parse(element.getAsString());
            if (result != null) {
                return result;
            }
        }

        Function<JsonPrimitive, Object> converter = JSON_PRIMITIVE_CONVERSIONS.get(expectedType);
        return converter != null
                ? converter.apply(element)
                : getFieldObject(element.getAsString(), typeChecker);
    }

    private Object getFieldObject(String fieldDef, TypeChecker typeChecker) throws ObjectParsingException {
        int fieldSepIndex = fieldDef.lastIndexOf('.');
        if (fieldSepIndex < 0) {
            throw new ObjectParsingException("Invalid field definition: " + fieldDef);
        }
        String fieldName = fieldDef.substring(fieldSepIndex + 1);

        Class<?> declaringClass;

        String className = fieldDef.substring(0, fieldSepIndex);
        if (className.indexOf('.') < 0) {
            declaringClass = classNameResolver.toClass(className);
        }
        else {
            try {
                declaringClass = Class.forName(className);
            } catch (ClassNotFoundException ex) {
                throw new ObjectParsingException("Missing class: " + className, ex);
            }
        }

        return getFieldObject(declaringClass, fieldName, typeChecker);
    }

    private static String fieldDef(Class<?> declaringClass, String fieldName) {
        return declaringClass.getName() + '.' + fieldName;
    }

    private Object getFieldObject(Class<?> declaringClass, String fieldName, TypeChecker typeChecker) throws ObjectParsingException {
        if (!Modifier.isPublic(declaringClass.getModifiers())) {
            throw new ObjectParsingException("Class is not public: " + declaringClass.getName());
        }

        Field field;
        try {
            field = declaringClass.getField(fieldName);
        } catch (NoSuchFieldException ex) {
            throw new ObjectParsingException("No such field: " + fieldDef(declaringClass, fieldName), ex);
        }

        if (!Modifier.isPublic(field.getModifiers())) {
            throw new ObjectParsingException("Field is not public: " + fieldDef(declaringClass, fieldName));
        }

        if (!Modifier.isStatic(field.getModifiers())) {
            throw new ObjectParsingException("Field is not static: " + fieldDef(declaringClass, fieldName));
        }

        typeChecker.checkType(field.getGenericType());

        try {
            return field.get(null);
        } catch (IllegalAccessException ex) {
            throw new RuntimeException("Unexpected IllegalAccessException for field: " + fieldDef(declaringClass, fieldName), ex);
        }
    }

    private static String getParameterName(Parameter param) {
        NamedArg nameAnnotation = param.getAnnotation(NamedArg.class);
        return nameAnnotation != null ? nameAnnotation.value() : "?";
    }

    private static boolean hasAllArguments(Executable method, JsonTree obj) {
        for (Parameter param: method.getParameters()) {
            if (obj.getChild(getParameterName(param)) == null) {
                return false;
            }
        }
        return true;
    }

    private <T> T newObject(
            JsonTree root,
            Factory<? extends T> constructor) throws ObjectParsingException {

        Parameter[] parameters = constructor.getParameters();
        Object[] passedArgs = new Object[parameters.length];

        for (int i = 0; i < passedArgs.length; i++) {
            Parameter parameter = parameters[i];
            JsonTree parameterElement = root.getChild(getParameterName(parameter));
            passedArgs[i] = toJavaObject(parameterElement, parameter.getType());
        }

        return constructor.newInstance(passedArgs);
    }

    private <T> T toComplexJavaObject(
            JsonTree root,
            Class<? extends T> actionClass,
            TypeChecker typeChecker) throws ObjectParsingException {
        return actionClass.cast(toComplexJavaObject(root, actionClass, null, typeChecker));
    }

    private static Constructor<?> findConstructor(JsonTree root, Class<?> actionClass) {
        Constructor<?> result = null;
        int argCount = 0;

        Constructor<?>[] constructors = actionClass.getConstructors();
        for (Constructor<?> candidate: constructors) {
            if (!Modifier.isPublic(candidate.getModifiers())) {
                continue;
            }

            if (result == null || argCount < candidate.getParameterCount()) {
                if (hasAllArguments(candidate, root)) {
                    result = candidate;
                    argCount = candidate.getParameterCount();
                }
            }
        }
        return result;
    }

    private static Method findFactoryMethod(
            JsonTree root,
            Class<?> actionClass,
            String methodName) {

        Method result = null;
        int argCount = 0;

        Method[] methods = actionClass.getMethods();
        for (Method method: methods) {
            if (!methodName.equals(method.getName())) {
                continue;
            }

            if (!Modifier.isPublic(method.getModifiers())) {
                continue;
            }

            if (!Modifier.isStatic(method.getModifiers())) {
                continue;
            }

            if (method.getReturnType() == Void.TYPE) {
                continue;
            }

            if (result == null || argCount < method.getParameterCount()) {
                if (hasAllArguments(method, root)) {
                    result = method;
                    argCount = method.getParameterCount();
                }
            }
        }
        return result;
    }

    private static <T> Factory<T> toFactory(Constructor<? extends T> constructor) {
        return new Factory<T>() {
            @Override
            public Parameter[] getParameters() {
                return constructor.getParameters();
            }

            @Override
            public T newInstance(Object... arguments) throws ObjectParsingException {
                try {
                    return constructor.newInstance(arguments);
                } catch (InstantiationException | IllegalAccessException ex) {
                    String className = constructor.getDeclaringClass().getName();
                    throw new RuntimeException("Unexpected constructor error for " + className, ex);
                } catch (InvocationTargetException ex) {
                    String className = constructor.getDeclaringClass().getName();
                    throw new ObjectParsingException("Creating an instance of " + className + " failed.", ex.getCause());
                }
            }
        };
    }

    private static Factory<Object> toFactory(Method factoryMethod) {
        return new Factory<Object>() {
            @Override
            public Parameter[] getParameters() {
                return factoryMethod.getParameters();
            }

            @Override
            public Object newInstance(Object... arguments) throws ObjectParsingException {
                try {
                    return factoryMethod.invoke(null, arguments);
                } catch (IllegalAccessException ex) {
                    String className = factoryMethod.getReturnType().getName();
                    throw new RuntimeException("Unexpected constructor error for " + className, ex);
                } catch (InvocationTargetException ex) {
                    String className = factoryMethod.getReturnType().getName();
                    throw new ObjectParsingException("Creating an instance of " + className + " failed.", ex.getCause());
                }
            }
        };
    }

    private Object toComplexJavaObject(
            JsonTree root,
            Class<?> actionClass,
            String methodName,
            TypeChecker typeChecker) throws ObjectParsingException {

        if (!Modifier.isPublic(actionClass.getModifiers())) {
            throw new ObjectParsingException("The class for " + actionClass.getName() + " is not public.");
        }

        Factory<Object> factory;
        if (methodName == null) {
            if (Modifier.isAbstract(actionClass.getModifiers())) {
                throw new ObjectParsingException("The class for " + actionClass.getName() + " is abstract.");
            }

            Constructor<?> constructor = findConstructor(root, actionClass);
            factory = constructor != null ? toFactory(constructor) : null;
        }
        else {
            Method factoryMethod = findFactoryMethod(root, actionClass, methodName);
            if (factoryMethod != null) {
                typeChecker.checkType(factoryMethod.getGenericReturnType());
                factory = toFactory(factoryMethod);
            }
            else {
                factory = null;
            }
        }

        if (factory == null) {
            return getFieldObject(actionClass, methodName, typeChecker);
        }
        else {
            return newObject(root, factory);
        }
    }

    public Object toJavaObject(JsonTree root) throws ObjectParsingException {
        if (root.isJsonObject()) {
            return toComplexJavaObject(root, (type) -> {});
        }
        else {
            return getFieldObject(root.getAsString(), (type) -> {});
        }
    }

    private Class<?> resolveClass(String className) throws ObjectParsingException {
        if (className.indexOf('.') < 0) {
            return classNameResolver.toClass(className);
        }
        else {
            try {
                return Class.forName(className);
            } catch (ClassNotFoundException ex) {
                throw new ObjectParsingException("Cannot find class: " + className, ex);
            }
        }
    }

    private Object toComplexJavaObject(JsonTree root, TypeChecker typeChecker) throws ObjectParsingException {
        String className = getStringField(root, "class");
        String factoryMethodName = null;

        Class<?> actionClass = null;
        int methodSepCandidateIndex = className.lastIndexOf('.');
        if (methodSepCandidateIndex >= 0) {
            String methodNameCandidate = className.substring(methodSepCandidateIndex + 1);
            boolean methodName = !methodNameCandidate.isEmpty()
                    && Character.isLowerCase(methodNameCandidate.charAt(0));

            if (!methodName) {
                try {
                    actionClass = resolveClass(className);
                } catch (ObjectParsingException ex) {
                    methodName = true;
                    // Maybe it is a field declaration which is treated the same way as a method here.
                }
            }

            if (methodName) {
                factoryMethodName = methodNameCandidate;
                className = className.substring(0, methodSepCandidateIndex);
            }
        }

        if (actionClass == null) {
            actionClass = resolveClass(className);
        }
        return toComplexJavaObject(root, actionClass, factoryMethodName, typeChecker);
    }

    private interface Factory<T> {
        public Parameter[] getParameters();
        public T newInstance(Object... arguments) throws ObjectParsingException;
    }

    private static final class RawObjectConverter<Src> {
        private final Class<? extends Src> srcType;
        private final ObjectMapper<? super Src, ?> objectMapper;

        public RawObjectConverter(
                Class<? extends Src> srcType,
                ObjectMapper<? super Src, ?> objectMapper) {
            ExceptionHelper.checkNotNullArgument(srcType, "srcType");
            ExceptionHelper.checkNotNullArgument(objectMapper, "objectMapper");

            this.srcType = srcType;
            this.objectMapper = objectMapper;
        }

        public boolean appliesTo(Object src) {
            return srcType.isInstance(src);
        }

        public Object convert(Object src) throws ObjectParsingException {
            return objectMapper.convertFrom(srcType.cast(src));
        }
    }

    private static final class CollectionMergerWrapper<T> {
        private final Class<T> type;
        private final CollectionMerger<T> merger;

        public CollectionMergerWrapper(Class<T> type, CollectionMerger<T> merger) {
            this.type = type;
            this.merger = merger;
        }

        public T parseFrom(
                JsonDeserializer deserializer,
                JsonTree array,
                TypeChecker typeChecker) throws ObjectParsingException {

            int elementCount = array.getChildCount();
            List<T> elements = new ArrayList<>(elementCount);
            for (int i = 0; i < elementCount; i++) {
                T element = deserializer.toJavaObject(array.getChild(i), type, typeChecker);
                elements.add(element);
            }
            return merger.merge(elements);
        }
    }

    public interface CollectionMerger<T> {
        public T merge(Collection<? extends T> elements);
    }

    public interface CustomStringParser<T> {
        public T parse(String str) throws ObjectParsingException;
    }

    public interface CustomClassNameResolver {
        public Class<?> toClass(String unqualifiedName) throws ObjectParsingException;
    }

    public interface ObjectMapper<Src, Dest> {
        public Dest convertFrom(Src obj) throws ObjectParsingException;
    }
}

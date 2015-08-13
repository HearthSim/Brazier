package com.github.kelemen.hearthstone.emulator.parsing;

import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.function.BiConsumer;
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

    private CardFileUpdateUtils() {
        throw new AssertionError();
    }
}

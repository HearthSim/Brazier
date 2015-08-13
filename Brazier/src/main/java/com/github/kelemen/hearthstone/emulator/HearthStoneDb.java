package com.github.kelemen.hearthstone.emulator;

import com.github.kelemen.hearthstone.emulator.cards.CardDescr;
import com.github.kelemen.hearthstone.emulator.cards.CardType;
import com.github.kelemen.hearthstone.emulator.minions.MinionDescr;
import com.github.kelemen.hearthstone.emulator.parsing.CardParser;
import com.github.kelemen.hearthstone.emulator.parsing.EntityParser;
import com.github.kelemen.hearthstone.emulator.parsing.JsonDeserializer;
import com.github.kelemen.hearthstone.emulator.parsing.ObjectParsingException;
import com.github.kelemen.hearthstone.emulator.parsing.ParserUtils;
import com.github.kelemen.hearthstone.emulator.parsing.UseTrackerJsonTree;
import com.github.kelemen.hearthstone.emulator.weapons.WeaponDescr;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import org.jtrim.utils.ExceptionHelper;

public final class HearthStoneDb {
    private final HearthStoneEntityDatabase<MinionDescr> minionDb;
    private final HearthStoneEntityDatabase<CardDescr> cardDb;
    private final HearthStoneEntityDatabase<WeaponDescr> weaponDb;
    private final HearthStoneEntityDatabase<CardDescr> heroPowerDb;

    public HearthStoneDb(
            HearthStoneEntityDatabase<CardDescr> cardDb,
            HearthStoneEntityDatabase<CardDescr> heroPowerDb) {
        ExceptionHelper.checkNotNullArgument(cardDb, "cardDb");
        ExceptionHelper.checkNotNullArgument(heroPowerDb, "heroPowerDb");

        this.weaponDb = toWeaponDb(cardDb);
        this.minionDb = toMinionDb(cardDb);
        this.cardDb = cardDb;
        this.heroPowerDb = heroPowerDb;
    }

    private static HearthStoneEntityDatabase<MinionDescr> toMinionDb(HearthStoneEntityDatabase<CardDescr> cardDb) {
        HearthStoneEntityDatabase.Builder<MinionDescr> result = new HearthStoneEntityDatabase.Builder<>();
        for (CardDescr card: cardDb.getAll()) {
            MinionDescr minion = card.getMinion();
            if (minion != null) {
                result.addEntity(minion);
            }
        }
        return result.create();
    }

    private static HearthStoneEntityDatabase<WeaponDescr> toWeaponDb(HearthStoneEntityDatabase<CardDescr> cardDb) {
        HearthStoneEntityDatabase.Builder<WeaponDescr> result = new HearthStoneEntityDatabase.Builder<>();
        for (CardDescr card: cardDb.getAll()) {
            WeaponDescr weapon = card.getWeapon();
            if (weapon != null) {
                result.addEntity(weapon);
            }
        }
        return result.create();
    }

    private static Path tryGetDefaultCardDbPath() {
        URI jarUri;

        try {
            jarUri = HearthStoneDb.class.getProtectionDomain().getCodeSource().getLocation().toURI();
        } catch (URISyntaxException ex) {
            return null;
        }

        Path dir = Paths.get(jarUri).getParent();
        if (dir == null) {
            return null;
        }

        return dir.resolve("card-db.zip");
    }

    private static Path tryGetCardDbPath() {
        String cardDbPath = System.getProperty("CARD_DB");
        if (cardDbPath == null) {
            return tryGetDefaultCardDbPath();
        }
        else {
            return Paths.get(cardDbPath);
        }
    }

    public static HearthStoneDb readDefault() throws IOException, ObjectParsingException {
        Path cardDbPath = tryGetCardDbPath();
        if (cardDbPath == null) {
            throw new IllegalStateException("Missing card database.");
        }

        return fromPath(cardDbPath);
    }

    public static HearthStoneDb fromPath(Path path) throws IOException, ObjectParsingException {
        if (Files.isDirectory(path)) {
            return fromRoot(path);
        }
        else {
            FileSystem zipFS = FileSystems.newFileSystem(path, null);
            Iterator<Path> roots = zipFS.getRootDirectories().iterator();
            if (!roots.hasNext()) {
                throw new IOException("No root dir in " + path);
            }
            return fromRoot(roots.next());
        }
    }

    private static HearthStoneDb fromRoot(Path root) throws IOException, ObjectParsingException {
        Path minionDir = root.resolve("minions");
        Path cardDir = root.resolve("cards");
        Path powerDir = root.resolve("powers");

        AtomicReference<HearthStoneEntityDatabase<CardDescr>> cardDbRef = new AtomicReference<>();
        AtomicReference<HearthStoneDb> resultRef = new AtomicReference<>();

        JsonDeserializer objectParser = ParserUtils.createDefaultDeserializer(resultRef::get);

        HearthStoneEntityDatabase<CardDescr> cardDb
                = createCardDb(cardDir, objectParser);
        HearthStoneEntityDatabase<CardDescr> heroPowerDb
                = createHeroPowerDb(powerDir, objectParser);

        cardDbRef.set(cardDb);

        HearthStoneDb result = new HearthStoneDb(cardDb, heroPowerDb);
        resultRef.set(result);

        return result;
    }

    private static boolean hasExt(Path path, String ext) {
        String fileName = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return fileName.endsWith(ext);
    }

    private static HearthStoneEntityDatabase<CardDescr> createCardDb(
            Path cardDir,
            JsonDeserializer objectParser) throws IOException, ObjectParsingException {

        return createEntityDb(cardDir, ".card", new CardParser(objectParser));
    }

    private static HearthStoneEntityDatabase<CardDescr> createHeroPowerDb(
            Path powerDir,
            JsonDeserializer objectParser) throws IOException, ObjectParsingException {

        CardParser cardParser = new CardParser(objectParser);
        return createEntityDb(powerDir, ".power", (obj) -> {
            return cardParser.fromJson(obj, CardType.HERO_POWER);
        });
    }

    private static <T extends HearthStoneEntity> HearthStoneEntityDatabase<T> createEntityDb(
            Path powerDir,
            String extension,
            EntityParser<T> parser) throws IOException, ObjectParsingException {

        HearthStoneEntityDatabase.Builder<T> result = new HearthStoneEntityDatabase.Builder<>();

        try (DirectoryStream<Path> entityFiles = Files.newDirectoryStream(powerDir)) {
            for (Path entityFile: entityFiles) {
                if (hasExt(entityFile, extension)) {
                    try {
                        JsonObject entityObj = ParserUtils.fromJsonFile(entityFile);
                        UseTrackerJsonTree trackedTree = new UseTrackerJsonTree(entityObj);
                        result.addEntity(parser.fromJson(trackedTree));
                        trackedTree.checkRequestedAllElements();
                    } catch (Exception ex) {
                        throw new ObjectParsingException("Failed to parse " + entityFile.getFileName(), ex);
                    }
                }
            }
        }

        return result.create();
    }

    public HearthStoneEntityDatabase<CardDescr> getHeroPowerDb() {
        return heroPowerDb;
    }

    public HearthStoneEntityDatabase<WeaponDescr> getWeaponDb() {
        return weaponDb;
    }

    public HearthStoneEntityDatabase<MinionDescr> getMinionDb() {
        return minionDb;
    }

    public HearthStoneEntityDatabase<CardDescr> getCardDb() {
        return cardDb;
    }
}

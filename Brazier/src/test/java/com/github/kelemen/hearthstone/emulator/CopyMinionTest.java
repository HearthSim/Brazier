package com.github.kelemen.hearthstone.emulator;

import org.junit.Test;

import static com.github.kelemen.hearthstone.emulator.TestCards.*;

public final class CopyMinionTest {
    @Test
    public void testFacelessMinionKeptAliveByHpAura() {
        PlayScript.testScript((script) -> {
            script.setMana("p1", 10);

            script.playMinionCard("p1", WISP, 0);
            script.playMinionCard("p1", STORMWIND_CHAMPION, 1);
            script.playCard("p1", MOONFIRE, "p1:0");


            script.expectBoard("p1",
                    expectedMinion(WISP, 2, 1),
                    expectedMinion(STORMWIND_CHAMPION, 6, 6));
            script.expectBoard("p2");

            script.setMana("p2", 10);
            script.playMinionCard("p2", FACELESS_MANIPULATOR, 0, "p1:0");

            script.expectBoard("p1",
                    expectedMinion(WISP, 2, 1),
                    expectedMinion(STORMWIND_CHAMPION, 6, 6));
            script.expectBoard("p2");
        });
    }

    @Test
    public void testFacelessMinionKeptAliveByHpAuraThenMirrorEntity() {
        PlayScript.testScript((script) -> {
            script.setMana("p1", 10);

            script.playMinionCard("p1", WISP, 0);
            script.playMinionCard("p1", STORMWIND_CHAMPION, 1);
            script.playCard("p1", MOONFIRE, "p1:0");
            script.playCard("p1", MIRROR_ENTITY);

            script.expectBoard("p1",
                    expectedMinion(WISP, 2, 1),
                    expectedMinion(STORMWIND_CHAMPION, 6, 6));
            script.expectBoard("p2");

            script.setCurrentPlayer("p2");
            script.setMana("p2", 10);
            script.playMinionCard("p2", FACELESS_MANIPULATOR, 0, "p1:0");

            script.expectBoard("p1",
                    expectedMinion(WISP, 2, 1),
                    expectedMinion(STORMWIND_CHAMPION, 6, 6),
                    expectedMinion(WISP, 2, 1));
            script.expectBoard("p2");
        });
    }

    @Test
    public void testFacelessDamageMinionWithHpAura() {
        PlayScript.testScript((script) -> {
            script.setMana("p1", 10);

            script.playMinionCard("p1", EMPEROR_COBRA, 0);
            script.playMinionCard("p1", STORMWIND_CHAMPION, 1);
            script.playCard("p1", MOONFIRE, "p1:0");


            script.expectBoard("p1",
                    expectedMinion(EMPEROR_COBRA, 3, 3),
                    expectedMinion(STORMWIND_CHAMPION, 6, 6));
            script.expectBoard("p2");

            script.setMana("p2", 10);
            script.playMinionCard("p2", FACELESS_MANIPULATOR, 0, "p1:0");

            script.expectBoard("p1",
                    expectedMinion(EMPEROR_COBRA, 3, 3),
                    expectedMinion(STORMWIND_CHAMPION, 6, 6));
            script.expectBoard("p2",
                    expectedMinion(EMPEROR_COBRA, 2, 2));
        });
    }

    @Test
    public void testBasicFaceless() {
        PlayScript.testScript((script) -> {
            script.setMana("p1", 10);
            script.playMinionCard("p1", YETI, 0);

            script.expectBoard("p1",
                    expectedMinion(YETI, 4, 5));
            script.expectBoard("p2");

            script.setMana("p2", 10);
            script.playMinionCard("p2", FACELESS_MANIPULATOR, 0, "p1:0");

            script.expectBoard("p1",
                    expectedMinion(YETI, 4, 5));
            script.expectBoard("p2",
                    expectedMinion(YETI, 4, 5));
        });
    }

    private static void setupAuraProviderCopy(PlayScript script) {
        script.setMana("p1", 10);
        script.playMinionCard("p1", BLUEGILL_WARRIOR, 0);
        script.playMinionCard("p1", STORMWIND_CHAMPION, 1);

        script.setMana("p2", 10);
        script.playMinionCard("p2", YETI, 0);

        script.expectBoard("p1",
                expectedMinion(BLUEGILL_WARRIOR, 3, 2),
                expectedMinion(STORMWIND_CHAMPION, 6, 6));
        script.expectBoard("p2",
                expectedMinion(YETI, 4, 5));

        script.setMana("p2", 10);
        script.playMinionCard("p2", FACELESS_MANIPULATOR, 1, "p1:1");

        script.expectBoard("p1",
                expectedMinion(BLUEGILL_WARRIOR, 3, 2),
                expectedMinion(STORMWIND_CHAMPION, 6, 6));
        script.expectBoard("p2",
                expectedMinion(YETI, 5, 6),
                expectedMinion(STORMWIND_CHAMPION, 6, 6));
    }

    @Test
    public void testCopyAuraProvider() {
        PlayScript.testScript((script) -> {
            setupAuraProviderCopy(script);
        });
    }

    @Test
    public void testCopyAuraProviderKillOriginalProvider() {
        PlayScript.testScript((script) -> {
            setupAuraProviderCopy(script);

            script.setMana("p2", 10);
            script.playCard("p2", PYROBLAST, "p1:1");

            script.expectBoard("p1",
                    expectedMinion(BLUEGILL_WARRIOR, 2, 1));
            script.expectBoard("p2",
                    expectedMinion(YETI, 5, 6),
                    expectedMinion(STORMWIND_CHAMPION, 6, 6));
        });
    }

    @Test
    public void testCopyAuraProviderKillNewProvider() {
        PlayScript.testScript((script) -> {
            setupAuraProviderCopy(script);

            script.setMana("p2", 10);
            script.playCard("p2", PYROBLAST, "p2:1");

            script.expectBoard("p1",
                expectedMinion(BLUEGILL_WARRIOR, 3, 2),
                expectedMinion(STORMWIND_CHAMPION, 6, 6));
            script.expectBoard("p2",
                expectedMinion(YETI, 4, 5));
        });
    }

    @Test
    public void testAuraTargetCopy() {
        PlayScript.testScript((script) -> {
            script.setMana("p1", 10);
            script.playMinionCard("p1", BLUEGILL_WARRIOR, 0);
            script.playMinionCard("p1", STORMWIND_CHAMPION, 1);

            script.setMana("p2", 10);
            script.playMinionCard("p2", YETI, 0);

            script.expectBoard("p1",
                    expectedMinion(BLUEGILL_WARRIOR, 3, 2),
                    expectedMinion(STORMWIND_CHAMPION, 6, 6));
            script.expectBoard("p2",
                    expectedMinion(YETI, 4, 5));

            script.setMana("p2", 10);
            script.playMinionCard("p2", FACELESS_MANIPULATOR, 1, "p1:0");

            script.expectBoard("p1",
                    expectedMinion(BLUEGILL_WARRIOR, 3, 2),
                    expectedMinion(STORMWIND_CHAMPION, 6, 6));
            script.expectBoard("p2",
                    expectedMinion(YETI, 4, 5),
                    expectedMinion(BLUEGILL_WARRIOR, 2, 1));
        });
    }
}

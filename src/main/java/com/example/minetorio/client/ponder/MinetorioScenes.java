package com.example.minetorio.client.ponder;

import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.Direction;

/**
 * Сцены размышлений Minetorio. Ключи текстов: minetorio.ponder.<id>.header и .text_1, .text_2 ...
 * (нумерация идёт по порядку вызовов showText внутри сцены - см. lang-файлы).
 */
public final class MinetorioScenes {

    // ---------- форма головки по слоям ----------

    public static void head3x3(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("head_3x3", "Quarry Drill Head 3x3");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.idle(10);
        layers(scene, util, 5, 2, 1, 2, "The tip is a column of Drill Head blocks: 1x1, two blocks tall.");
        layers(scene, util, 5, 2, 3, 4, "Above it goes a cross of 5 blocks, two layers high.");
        layers(scene, util, 5, 2, 5, 5, "The top layer is a solid 3x3 square.");
        finish(scene, util, 2, 3, 2, "Build the head exactly in this shape with the tip down, and glue all its blocks together with Super Glue.");
    }

    public static void head5x5(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("head_5x5", "Quarry Drill Head 5x5");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();
        scene.idle(10);
        layers(scene, util, 7, 3, 1, 2, "The tip is a column of Drill Head blocks: 1x1, two blocks tall.");
        layers(scene, util, 7, 3, 3, 4, "Above it goes a cross of 5 blocks, two layers high.");
        layers(scene, util, 7, 3, 5, 5, "Then a solid 3x3 layer.");
        layers(scene, util, 7, 3, 6, 7, "Two layers of a 13-block diamond follow.");
        layers(scene, util, 7, 3, 8, 8, "The top layer is a 5x5 square with its corners cut off: 21 blocks.");
        finish(scene, util, 3, 4, 3, "Build the head exactly in this shape with the tip down, and glue all its blocks together with Super Glue.");
    }

    private static void layers(SceneBuilder scene, SceneBuildingUtil util, int plate, int center, int y1, int y2, String text) {
        scene.world().showSection(util.select().fromTo(0, y1, 0, plate - 1, y2, plate - 1), Direction.DOWN);
        scene.idle(10);
        scene.overlay().showText(70).text(text).pointAt(util.vector().centerOf(center, y2, center)).placeNearTarget();
        scene.idle(80);
    }

    private static void finish(SceneBuilder scene, SceneBuildingUtil util, int c, int y, int c2, String text) {
        scene.overlay().showText(90).text(text).pointAt(util.vector().centerOf(c, y, c2)).placeNearTarget();
        scene.idle(100);
        scene.markAsFinished();
    }

    // ---------- сборка и работа бура ----------

    public static void drill3x3(SceneBuilder scene, SceneBuildingUtil util) {
        operation(scene, util, "drill_3x3", "Quarry Drill 3x3", 7, 1, 5,
                "The drive needs 1024 SU at 32 RPM. At 32 RPM it yields 1 ore block and 1 raw ore every 5 minutes; every extra 32 RPM multiplies that.");
    }

    public static void drill5x5(SceneBuilder scene, SceneBuildingUtil util) {
        operation(scene, util, "drill_5x5", "Quarry Drill 5x5", 9, 2, 8,
                "The drive needs 1536 SU at 32 RPM. At 32 RPM it yields 2 ore blocks and 3 raw ore every 5 minutes; every extra 32 RPM multiplies that.");
    }

    /** plate - размер площадки, r - радиус головки/платформы (1 для 3x3, 2 для 5x5), shapeH - высота формы. */
    private static void operation(SceneBuilder scene, SceneBuildingUtil util, String id, String title,
                                  int plate, int r, int shapeH, String driveText) {
        int c = plate / 2;
        int tip = 6;
        int headTop = tip + shapeH - 1;
        int bearing = headTop + 1;
        int platform = bearing + 1;
        int piston = platform + 1;
        int cx = c + r + 1;

        scene.title(id, title);
        scene.configureBasePlate(0, 0, plate);
        scene.scaleSceneView(0.5f);
        scene.showBasePlate();
        scene.idle(5);

        // 1. пласт руды
        scene.world().showSection(util.select().fromTo(c - r - 1, 1, c - r - 1, c + r + 1, 1, c + r + 1), Direction.DOWN);
        scene.idle(10);
        scene.overlay().showText(70).text("Quarry drills work on ore patches. Build the drill right above the ore.")
                .pointAt(util.vector().centerOf(c, 1, c)).placeNearTarget();
        scene.idle(80);

        // 2. поршень
        scene.world().showSection(util.select().fromTo(c, piston, c, c, piston + 2, c), Direction.DOWN);
        scene.idle(10);
        scene.overlay().showText(80).text("A Mechanical Piston facing down lowers the drill. Every Piston Extension Pole behind it adds one block of reach.")
                .pointAt(util.vector().centerOf(c, piston, c)).placeNearTarget();
        scene.idle(90);

        // 3. платформа и подшипник
        var platformSel = util.select().fromTo(c - r, platform, c - r, c + r, platform, c + r)
                .add(util.select().position(c, bearing, c));
        var platformLink = scene.world().showIndependentSection(platformSel, Direction.DOWN);
        scene.idle(10);
        scene.overlay().showText(80).text("Under the piston goes a platform of Drill Platform blocks, with a Mechanical Bearing facing down in its centre.")
                .pointAt(util.vector().centerOf(c, platform, c)).placeNearTarget();
        scene.idle(90);

        // 4. головка
        var headLink = scene.world().showIndependentSection(
                util.select().fromTo(c - r, tip, c - r, c + r, headTop, c + r), Direction.DOWN);
        scene.idle(10);
        scene.overlay().showText(80).text("The Drill Head hangs under the bearing. Glue all its blocks together so the bearing carries them as one piece.")
                .pointAt(util.vector().centerOf(c, tip + 1, c)).placeNearTarget();
        scene.idle(90);

        // 5. спуск и вращение
        scene.overlay().showText(80).text("Power the bearing and the piston. The head spins and sinks until it stops just above the ore. It never breaks any blocks.")
                .pointAt(util.vector().centerOf(c, tip + 1, c)).placeNearTarget();
        scene.world().moveSection(platformLink, util.vector().of(0, -4, 0), 60);
        scene.world().moveSection(headLink, util.vector().of(0, -4, 0), 60);
        scene.world().rotateSection(headLink, 0, 360, 0, 60);
        scene.idle(90);

        // 6. привод
        scene.world().showSection(util.select().position(cx, piston, c)
                .add(util.select().fromTo(cx, piston + 1, c, cx, piston + 2, c)), Direction.DOWN);
        scene.idle(10);
        scene.overlay().showText(100).text(driveText)
                .pointAt(util.vector().centerOf(cx, piston, c)).placeNearTarget();
        scene.idle(110);

        // 7. выход руды
        scene.world().showSection(util.select().position(cx + 1, piston, c), Direction.DOWN);
        scene.idle(10);
        scene.overlay().showText(100).text("Place the drive with right-click to make the red-dot output face you, or while sneaking to face away. Ore is pushed into an adjacent chest or belt.")
                .pointAt(util.vector().centerOf(cx + 1, piston, c)).placeNearTarget();
        scene.idle(110);
        scene.markAsFinished();
    }

    private MinetorioScenes() {}
}

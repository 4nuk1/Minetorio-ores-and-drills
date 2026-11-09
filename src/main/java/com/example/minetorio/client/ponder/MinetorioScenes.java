package com.example.minetorio.client.ponder;

import com.example.minetorio.DrillShapes;
import com.example.minetorio.DrillTier;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * Сцены размышлений Minetorio. Ключи текстов: minetorio.ponder.<id>.header и .text_1, .text_2 ...
 * (нумерация идёт по порядку вызовов showText внутри сцены - см. lang-файлы).
 * Вращающаяся головка - сущность, в сцене её заменяют кубы-образец (железо / красная шерсть).
 */
public final class MinetorioScenes {

    public static void head3x3(SceneBuilder scene, SceneBuildingUtil util) {
        stack(scene, util, "head_3x3", "Quarry Drill Head 3x3", 5, DrillTier.SMALL_3X3, 1.0f);
    }

    public static void head5x5(SceneBuilder scene, SceneBuildingUtil util) {
        stack(scene, util, "head_5x5", "Quarry Drill Head 5x5", 7, DrillTier.LARGE_5X5, 0.8f);
    }

    public static void drill3x3(SceneBuilder scene, SceneBuildingUtil util) {
        operation(scene, util, "drill_3x3", "Quarry Drill 3x3", 7, DrillTier.SMALL_3X3, 0.75f,
                "The drive needs 1024 SU at 32 RPM. At 32 RPM it yields 1 ore block and 1 raw ore every 5 minutes; every extra 32 RPM multiplies that. Each cycle wears the ore block down by 1.");
    }

    public static void drill5x5(SceneBuilder scene, SceneBuildingUtil util) {
        operation(scene, util, "drill_5x5", "Quarry Drill 5x5", 9, DrillTier.LARGE_5X5, 0.65f,
                "The drive needs 1536 SU at 32 RPM. At 32 RPM it yields 2 ore blocks and 3 raw ore every 5 minutes; every extra 32 RPM multiplies that. Each cycle wears the ore block down by 1.");
    }

    /** Кубы вращающейся головки (без центральной колонны) вокруг нижнего блока колонны (tx, ty, tz). */
    private static Selection bit(SceneBuildingUtil util, DrillTier tier, int tx, int ty, int tz) {
        Selection sel = null;
        for (BlockPos rel : DrillShapes.visualCells(tier)) {
            Selection one = util.select().position(tx + rel.getX(), ty + rel.getY(), tz + rel.getZ());
            sel = sel == null ? one : sel.add(one);
        }
        return sel;
    }

    // ---------- из чего собрать ----------

    private static void stack(SceneBuilder scene, SceneBuildingUtil util, String id, String title,
                              int plate, DrillTier tier, float scale) {
        int c = plate / 2;
        int h = tier.rods();
        int baseY = h + 1;

        scene.title(id, title);
        scene.configureBasePlate(0, 0, plate);
        scene.scaleSceneView(scale);
        scene.showBasePlate();
        scene.idle(10);

        scene.world().showSection(util.select().position(c, baseY, c), Direction.DOWN);
        scene.idle(10);
        scene.overlay().showText(70).text("Start with a Drill Head Base block. It is the carriage that the Mechanical Piston moves.")
                .pointAt(util.vector().centerOf(c, baseY, c)).placeNearTarget();
        scene.idle(80);

        scene.world().showSection(util.select().fromTo(c, 1, c, c, h, c), Direction.DOWN);
        scene.idle(10);
        scene.overlay().showText(80).text("Under it, stack " + h + " Drill Head blocks in one column and glue everything together with Super Glue.")
                .pointAt(util.vector().centerOf(c, 2, c)).placeNearTarget();
        scene.idle(90);

        var spinning = scene.world().showIndependentSection(bit(util, tier, c, 1, c), Direction.DOWN);
        scene.world().rotateSection(spinning, 0, 360, 0, 80);
        scene.idle(10);
        scene.overlay().showText(90).text("When the drive is turning, a rotating bit shaped like this appears around the lower blocks. It never breaks blocks.")
                .pointAt(util.vector().centerOf(c, 2, c)).placeNearTarget();
        scene.idle(100);
        scene.markAsFinished();
    }

    // ---------- сборка и работа бура ----------

    private static void operation(SceneBuilder scene, SceneBuildingUtil util, String id, String title,
                                  int plate, DrillTier tier, float scale, String driveText) {
        int r = tier == DrillTier.SMALL_3X3 ? 1 : 2;
        int c = plate / 2;
        int h = tier.rods();
        int tip = 4;                 // старт нижнего блока; спуск на 2 блока ставит его прямо на руду (y=2)
        int baseY = tip + h;
        int piston = baseY + 1;
        int cx = c + r + 1;

        scene.title(id, title);
        scene.configureBasePlate(0, 0, plate);
        scene.scaleSceneView(scale);
        scene.showBasePlate();
        scene.idle(5);

        // 1. пласт руды
        scene.world().showSection(util.select().fromTo(c - r - 1, 1, c - r - 1, c + r + 1, 1, c + r + 1), Direction.DOWN);
        scene.idle(10);
        scene.overlay().showText(70).text("Quarry drills work on ore patches. Build the drill right above the ore.")
                .pointAt(util.vector().centerOf(c, 1, c)).placeNearTarget();
        scene.idle(80);

        // 2. поршень
        scene.world().showSection(util.select().fromTo(c, piston, c, c, piston + 1, c), Direction.DOWN);
        scene.idle(10);
        scene.overlay().showText(80).text("A Mechanical Piston facing down lowers the drill. Every Piston Extension Pole behind it adds one block of reach.")
                .pointAt(util.vector().centerOf(c, baseY, c)).placeNearTarget();
        scene.idle(90);

        // 3. основание
        var baseLink = scene.world().showIndependentSection(util.select().position(c, baseY, c), Direction.DOWN);
        scene.idle(10);
        scene.overlay().showText(70).text("Put the Drill Head Base block right under the piston.")
                .pointAt(util.vector().centerOf(c, baseY, c)).placeNearTarget();
        scene.idle(80);

        // 4. колонна
        var rodsLink = scene.world().showIndependentSection(util.select().fromTo(c, tip, c, c, tip + h - 1, c), Direction.DOWN);
        scene.idle(10);
        scene.overlay().showText(80).text("Stack " + h + " Drill Head blocks under it and glue them all together.")
                .pointAt(util.vector().centerOf(c, tip + 1, c)).placeNearTarget();
        scene.idle(90);

        // 5. спуск
        scene.overlay().showText(80).text("Power the piston: it lowers the drill until the lowest block touches the ore.")
                .pointAt(util.vector().centerOf(c, tip + 1, c)).placeNearTarget();
        scene.world().moveSection(baseLink, util.vector().of(0, -2, 0), 60);
        scene.world().moveSection(rodsLink, util.vector().of(0, -2, 0), 60);
        scene.idle(90);

        // 6. вращающаяся головка
        var spinning = scene.world().showIndependentSection(bit(util, tier, c, tip - 2, c), Direction.DOWN);
        scene.world().rotateSection(spinning, 0, 360, 0, 80);
        scene.idle(10);
        scene.overlay().showText(80).text("A rotating bit appears around the blocks and spins with the drive. It never breaks blocks.")
                .pointAt(util.vector().centerOf(c, tip, c)).placeNearTarget();
        scene.idle(90);

        // 7. привод
        scene.world().showSection(util.select().position(cx, piston, c)
                .add(util.select().position(cx, piston + 1, c)), Direction.DOWN);
        scene.idle(10);
        scene.overlay().showText(110).text(driveText)
                .pointAt(util.vector().centerOf(cx, piston, c)).placeNearTarget();
        scene.idle(120);

        // 8. выход
        scene.world().showSection(util.select().position(cx + 1, piston, c), Direction.DOWN);
        scene.idle(10);
        scene.overlay().showText(100).text("Place the drive with right-click to make the red-dot output face you, or while sneaking to face away. Ore is pushed into an adjacent chest or belt.")
                .pointAt(util.vector().centerOf(cx + 1, piston, c)).placeNearTarget();
        scene.idle(110);
        scene.markAsFinished();
    }

    private MinetorioScenes() {}
}

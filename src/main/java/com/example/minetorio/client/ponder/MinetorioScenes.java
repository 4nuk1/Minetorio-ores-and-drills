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
 * Стержень (сущность) в сцене показан скрытыми блоками drill_rod, головка в движении - блоками головки.
 */
public final class MinetorioScenes {
    private static final int DESCENT = 2; // на сколько блоков сцена опускает головку

    public static void head3x3(SceneBuilder scene, SceneBuildingUtil util) {
        shape(scene, util, "head_3x3", "Quarry Drill Head 3x3", 5, DrillTier.SMALL_3X3, 0.55f, new int[][]{{0, 1}, {2, 3}, {4, 4}});
    }

    public static void head5x5(SceneBuilder scene, SceneBuildingUtil util) {
        shape(scene, util, "head_5x5", "Quarry Drill Head 5x5", 7, DrillTier.LARGE_5X5, 0.42f, new int[][]{{0, 1}, {2, 3}, {4, 4}, {5, 6}, {7, 7}});
    }

    public static void drill3x3(SceneBuilder scene, SceneBuildingUtil util) {
        operation(scene, util, "drill_3x3", "Quarry Drill 3x3", 9, 1, DrillTier.SMALL_3X3, 0.45f,
                "Place the Quarry Drill Drive next to the base and give it rotation from any side except its output. It needs 1024 SU at 16 RPM. A cycle always takes 1 minute; the faster it spins, the more each cycle gives: from 1 ore block and 1 raw ore at 16 RPM up to 4 and 4 at 256 RPM. When it starts, it pairs with the base until one of them is broken.");
    }

    public static void drill5x5(SceneBuilder scene, SceneBuildingUtil util) {
        operation(scene, util, "drill_5x5", "Quarry Drill 5x5", 11, 2, DrillTier.LARGE_5X5, 0.36f,
                "Place the Quarry Drill Drive next to the base and give it rotation from any side except its output. It needs 1536 SU at 16 RPM. A cycle always takes 1 minute; the faster it spins, the more each cycle gives: from 2 ore blocks and 3 raw ore at 16 RPM up to 4 and 12 at 256 RPM. When it starts, it pairs with the base until one of them is broken.");
    }

    /** Блоки головки с относительной высотой от yFrom до yTo вокруг нижней точки (tx, ty, tz). */
    private static Selection layers(SceneBuildingUtil util, DrillTier tier, int tx, int ty, int tz, int yFrom, int yTo) {
        Selection sel = null;
        for (BlockPos rel : DrillShapes.cells(tier)) {
            if (rel.getY() < yFrom || rel.getY() > yTo) continue;
            Selection one = util.select().position(tx + rel.getX(), ty + rel.getY(), tz + rel.getZ());
            sel = sel == null ? one : sel.add(one);
        }
        return sel;
    }

    // ---------- форма головки ----------

    private static void shape(SceneBuilder scene, SceneBuildingUtil util, String id, String title,
                              int plate, DrillTier tier, float scale, int[][] groups) {
        int c = plate / 2;
        int baseY = tier.shapeHeight() + 1;

        scene.title(id, title);
        scene.configureBasePlate(0, 0, plate);
        scene.scaleSceneView(scale);
        scene.showBasePlate();
        scene.idle(10);

        for (int[] g : groups) {
            scene.world().showSection(layers(util, tier, c, 1, c, g[0], g[1]), Direction.DOWN);
            scene.idle(15);
        }
        scene.overlay().showText(90).text("Build the head from Drill Head blocks exactly in this shape, with the tip pointing down.")
                .pointAt(util.vector().centerOf(c + 1, 3, c)).placeNearTarget();
        scene.idle(100);

        scene.world().showSection(util.select().position(c, baseY, c), Direction.DOWN);
        scene.world().showSection(util.select().position(c, baseY + 1, c), Direction.DOWN);
        scene.idle(10);
        scene.overlay().showText(90).text("Put a Drill Head Base block right on top of the head and give it rotation from above.")
                .pointAt(util.vector().centerOf(c, baseY, c)).placeNearTarget();
        scene.idle(100);
        scene.markAsFinished();
    }

    // ---------- сборка и работа бура ----------

    private static void operation(SceneBuilder scene, SceneBuildingUtil util, String id, String title,
                                  int plate, int r, DrillTier tier, float scale, String driveText) {
        int c = plate / 2;
        int h = tier.shapeHeight();
        int tip = 2;                          // остриё стоит на руде (руда на y=1) в конечном положении
        int gapTop = tip + h + DESCENT - 1;   // верх промежутка между головкой и основанием
        int baseY = gapTop + 1;
        int cx = c + r + 2;

        scene.title(id, title);
        scene.configureBasePlate(0, 0, plate);
        scene.scaleSceneView(scale);
        scene.showBasePlate();
        scene.idle(5);

        // 1. пласт руды
        scene.world().showSection(util.select().fromTo(c - r - 1, 1, c - r - 1, c + r + 1, 1, c + r + 1), Direction.DOWN);
        scene.idle(10);
        scene.overlay().showText(80).text("Quarry drills work on ore patches. Build the head above the ore with free space under it.")
                .pointAt(util.vector().centerOf(c, 1, c)).placeNearTarget();
        scene.idle(90);

        // 2. головка: показываем в конечном положении и сразу поднимаем к основанию
        var head = scene.world().showIndependentSection(layers(util, tier, c, tip, c, 0, h), Direction.DOWN);
        scene.idle(10);
        scene.world().moveSection(head, util.vector().of(0, DESCENT, 0), 1);
        scene.idle(5);
        scene.overlay().showText(80).text("Build the head from Drill Head blocks exactly in this shape, tip down.")
                .pointAt(util.vector().centerOf(c + 1, tip + DESCENT + 2, c)).placeNearTarget();
        scene.idle(90);

        // 3. основание
        scene.world().showSection(util.select().position(c, baseY, c), Direction.DOWN);
        scene.world().showSection(util.select().position(c, baseY + 1, c), Direction.DOWN);
        scene.idle(10);
        scene.overlay().showText(80).text("Put a Drill Head Base right on top of the head and give it rotation from above.")
                .pointAt(util.vector().centerOf(c, baseY, c)).placeNearTarget();
        scene.idle(90);

        // 4. стержень толкает и вращает головку вниз
        scene.overlay().showText(110).text("A rotating rod slides out of the base and pushes the whole head down, spinning it, until it touches the ore. It never breaks blocks.")
                .pointAt(util.vector().centerOf(c, gapTop, c)).placeNearTarget();
        scene.world().rotateSection(head, 0, 720, 0, DESCENT * 25 + 40);
        for (int k = 0; k < DESCENT; k++) {
            var piece = scene.world().showIndependentSection(util.select().position(c, gapTop - k, c), Direction.DOWN);
            scene.world().rotateSection(piece, 0, 720, 0, 120);
            scene.world().moveSection(head, util.vector().of(0, -1, 0), 25);
            scene.idle(25);
        }
        scene.idle(60);

        // 5. привод
        scene.world().showSection(util.select().position(cx, baseY, c), Direction.DOWN);
        scene.world().showSection(util.select().position(cx, baseY + 1, c), Direction.DOWN);
        scene.idle(10);
        scene.overlay().showText(130).text(driveText)
                .pointAt(util.vector().centerOf(cx, baseY, c)).placeNearTarget();
        scene.idle(140);

        // 6. выход
        scene.world().showSection(util.select().position(cx + 1, baseY, c), Direction.DOWN);
        scene.idle(10);
        scene.overlay().showText(110).text("Place the drive with right-click to make the red-dot output face you, or while sneaking to face away. Ore goes into an adjacent chest or belt. Right-click the drive to see how much ore is left.")
                .pointAt(util.vector().centerOf(cx + 1, baseY, c)).placeNearTarget();
        scene.idle(120);
        scene.markAsFinished();
    }

    private MinetorioScenes() {}
}

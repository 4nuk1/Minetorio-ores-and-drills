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
 * Стержень (сущность) в сцене показан скрытыми блоками drill_rod.
 */
public final class MinetorioScenes {

    public static void head3x3(SceneBuilder scene, SceneBuildingUtil util) {
        shape(scene, util, "head_3x3", "Quarry Drill Head 3x3", 5, DrillTier.SMALL_3X3, 1.0f, new int[][]{{2, 3}, {4, 4}});
    }

    public static void head5x5(SceneBuilder scene, SceneBuildingUtil util) {
        shape(scene, util, "head_5x5", "Quarry Drill Head 5x5", 7, DrillTier.LARGE_5X5, 0.8f, new int[][]{{2, 3}, {4, 4}, {5, 6}, {7, 7}});
    }

    public static void drill3x3(SceneBuilder scene, SceneBuildingUtil util) {
        operation(scene, util, "drill_3x3", "Quarry Drill 3x3", 9, 1, DrillTier.SMALL_3X3, 0.7f,
                "Place the Quarry Drill Drive nearby and give it rotation from any side except its output. It needs 1024 SU at 32 RPM and yields 1 ore block and 1 raw ore every 5 minutes at 32 RPM; every extra 32 RPM multiplies that. When it starts, it pairs with the base until one of them is broken.");
    }

    public static void drill5x5(SceneBuilder scene, SceneBuildingUtil util) {
        operation(scene, util, "drill_5x5", "Quarry Drill 5x5", 11, 2, DrillTier.LARGE_5X5, 0.6f,
                "Place the Quarry Drill Drive nearby and give it rotation from any side except its output. It needs 1536 SU at 32 RPM and yields 2 ore blocks and 3 raw ore every 5 minutes at 32 RPM; every extra 32 RPM multiplies that. When it starts, it pairs with the base until one of them is broken.");
    }

    /** Блоки головки с относительной высотой от yFrom до yTo вокруг нижней точки стержня (tx, ty, tz). */
    private static Selection layers(SceneBuildingUtil util, DrillTier tier, int tx, int ty, int tz, int yFrom, int yTo) {
        Selection sel = null;
        for (BlockPos rel : DrillShapes.visualCells(tier)) {
            if (rel.getY() < yFrom || rel.getY() > yTo) continue;
            Selection one = util.select().position(tx + rel.getX(), ty + rel.getY(), tz + rel.getZ());
            sel = sel == null ? one : sel.add(one);
        }
        return sel;
    }

    /** Стержень выдвигается по блоку сверху вниз; каждый кусок вращается. */
    private static void extendRod(SceneBuilder scene, SceneBuildingUtil util, int c, int topY, int bottomY) {
        for (int y = topY; y >= bottomY; y--) {
            var piece = scene.world().showIndependentSection(util.select().position(c, y, c), Direction.DOWN);
            scene.world().rotateSection(piece, 0, 720, 0, 120);
            scene.idle(6);
        }
    }

    // ---------- форма головки ----------

    private static void shape(SceneBuilder scene, SceneBuildingUtil util, String id, String title,
                              int plate, DrillTier tier, float scale, int[][] groups) {
        int c = plate / 2;
        int h = tier.shapeHeight();
        int baseY = h + 1;

        scene.title(id, title);
        scene.configureBasePlate(0, 0, plate);
        scene.scaleSceneView(scale);
        scene.showBasePlate();
        scene.idle(10);

        for (int[] g : groups) {
            scene.world().showSection(layers(util, tier, c, 1, c, g[0], g[1]), Direction.DOWN);
            scene.idle(15);
        }
        scene.overlay().showText(90).text("Build the head ring by ring from Drill Head blocks. The middle column stays empty: the rod will run there.")
                .pointAt(util.vector().centerOf(c + 1, 2, c)).placeNearTarget();
        scene.idle(100);

        scene.world().showSection(util.select().position(c, baseY, c), Direction.DOWN);
        scene.world().showSection(util.select().position(c, baseY + 1, c), Direction.DOWN);
        scene.idle(10);
        scene.overlay().showText(80).text("Put a Drill Head Base block on top of the empty middle column and give it rotation from above.")
                .pointAt(util.vector().centerOf(c, baseY, c)).placeNearTarget();
        scene.idle(90);

        scene.overlay().showText(90).text("When the base spins, a rotating rod slides out of it down the middle column to the bottom of the head, right above the ore.")
                .pointAt(util.vector().centerOf(c, 3, c)).placeNearTarget();
        extendRod(scene, util, c, baseY - 1, 1);
        scene.idle(80);
        scene.markAsFinished();
    }

    // ---------- сборка и работа бура ----------

    private static void operation(SceneBuilder scene, SceneBuildingUtil util, String id, String title,
                                  int plate, int r, DrillTier tier, float scale, String driveText) {
        int c = plate / 2;
        int h = tier.shapeHeight();
        int tip = 2;                 // нижний блок стержня стоит прямо над рудой (руда на y=1)
        int baseY = tip + h;
        int cx = c + r + 2;

        scene.title(id, title);
        scene.configureBasePlate(0, 0, plate);
        scene.scaleSceneView(scale);
        scene.showBasePlate();
        scene.idle(5);

        // 1. пласт руды
        scene.world().showSection(util.select().fromTo(c - r - 1, 1, c - r - 1, c + r + 1, 1, c + r + 1), Direction.DOWN);
        scene.idle(10);
        scene.overlay().showText(80).text("Quarry drills work on ore patches. Build the head directly above the ore: the bottom of the middle column must touch the ore.")
                .pointAt(util.vector().centerOf(c, 1, c)).placeNearTarget();
        scene.idle(90);

        // 2. головка
        scene.world().showSection(layers(util, tier, c, tip, c, 0, h), Direction.DOWN);
        scene.idle(10);
        scene.overlay().showText(80).text("Build the head from Drill Head blocks in exactly this shape and leave the middle column empty.")
                .pointAt(util.vector().centerOf(c + 1, tip + 2, c)).placeNearTarget();
        scene.idle(90);

        // 3. основание
        scene.world().showSection(util.select().position(c, baseY, c), Direction.DOWN);
        scene.world().showSection(util.select().position(c, baseY + 1, c), Direction.DOWN);
        scene.idle(10);
        scene.overlay().showText(70).text("Put a Drill Head Base on top of the head and give it rotation from above.")
                .pointAt(util.vector().centerOf(c, baseY, c)).placeNearTarget();
        scene.idle(80);

        // 4. стержень
        scene.overlay().showText(90).text("A rotating rod slides out of the base all the way down to the ore. It never breaks blocks.")
                .pointAt(util.vector().centerOf(c, tip + 2, c)).placeNearTarget();
        extendRod(scene, util, c, baseY - 1, tip);
        scene.idle(80);

        // 5. привод
        scene.world().showSection(util.select().position(cx, baseY, c), Direction.DOWN);
        scene.world().showSection(util.select().position(cx, baseY + 1, c), Direction.DOWN);
        scene.idle(10);
        scene.overlay().showText(120).text(driveText)
                .pointAt(util.vector().centerOf(cx, baseY, c)).placeNearTarget();
        scene.idle(130);

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

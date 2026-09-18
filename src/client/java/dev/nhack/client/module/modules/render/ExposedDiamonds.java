package dev.nhack.client.module.modules.render;

import dev.nhack.client.event.Subscribe;
import dev.nhack.client.event.events.HudRenderEvent;
import dev.nhack.client.event.events.TickEvent;
import dev.nhack.client.module.Category;
import dev.nhack.client.module.Module;
import dev.nhack.client.setting.ColorSetting;
import dev.nhack.client.setting.NumberSetting;
import dev.nhack.client.util.WorldToScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class ExposedDiamonds extends Module {

    private final NumberSetting radius = addSetting(new NumberSetting("Radius", "Радиус поиска в чанках", 3.0, 1.0, 6.0, 1.0));
    private final ColorSetting diamondColor = addSetting(new ColorSetting("DiamondColor", "Цвет алмаза", 0xFF00FFFF));

    // Потокобезопасный список для рендера
    private final List<BlockPos> foundDiamonds = new CopyOnWriteArrayList<>();
    private int scanIndex = 0;

    public ExposedDiamonds() {
        super("CaveXRay", "Подсвечивает открытые алмазы на 2D экране", Category.RENDER);
    }

    @Override
    protected void onEnable() {
        foundDiamonds.clear();
        scanIndex = 0;
    }

    @Override
    protected void onDisable() {
        foundDiamonds.clear();
    }

    @Subscribe
    public void onTick(TickEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        int r = radius.getInt();
        int playerChunkX = mc.player.getBlockX() >> 4;
        int playerChunkZ = mc.player.getBlockZ() >> 4;

        List<int[]> chunkOffsets = getChunkOffsets(r);
        if (scanIndex >= chunkOffsets.size()) {
            scanIndex = 0;
            foundDiamonds.removeIf(pos -> {
                int cx = pos.getX() >> 4;
                int cz = pos.getZ() >> 4;
                return Math.abs(cx - playerChunkX) > r || Math.abs(cz - playerChunkZ) > r;
            });
        }

        int[] offset = chunkOffsets.get(scanIndex);
        int targetX = playerChunkX + offset[0];
        int targetZ = playerChunkZ + offset[1];

        scanChunk(mc, targetX, targetZ);
        scanIndex++;
    }

    @Subscribe
    public void onRenderHud(HudRenderEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.options.hideGui) return;

        GuiGraphics graphics = event.graphics();
        Font font = mc.font;

        for (BlockPos pos : foundDiamonds) {
            // Проецируем центр блока на экран
            float[] screen = WorldToScreen.project(Vec3.atCenterOf(pos));
            if (screen == null) continue;

            int x = (int) screen[0];
            int y = (int) screen[1];

            // Дистанция до алмаза
            double dist = mc.player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            String distText = String.format("%.0fm", Math.sqrt(dist));

            // Рисуем квадрат и текст как в твоем ESP
            graphics.fill(x - 4, y - 4, x + 4, y + 4, diamondColor.argb() & 0x66FFFFFF | 0x44000000);
            graphics.fill(x - 1, y - 1, x + 1, y + 1, diamondColor.argb());
            graphics.drawString(font, "DIAMOND", x - font.width("DIAMOND") / 2, y - 14, diamondColor.argb());
            graphics.drawString(font, distText, x - font.width(distText) / 2, y + 6, 0xFFFFFFFF);
        }
    }

    private void scanChunk(Minecraft mc, int chunkX, int chunkZ) {
        if (!mc.level.hasChunk(chunkX, chunkZ)) return;

        LevelChunk chunk = mc.level.getChunk(chunkX, chunkZ);
        if (chunk.isEmpty()) return;

        int minX = chunkX << 4;
        int minZ = chunkZ << 4;
        int minY = -64;
        int maxY = 16;

        List<BlockPos> newFound = new ArrayList<>();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos adjPos = new BlockPos.MutableBlockPos();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = minY; y <= maxY; y++) {
                    pos.set(minX + x, y, minZ + z);
                    BlockState state = chunk.getBlockState(pos);

                    if (state != null && (state.is(Blocks.DIAMOND_ORE) || state.is(Blocks.DEEPSLATE_DIAMOND_ORE))) {
                        if (isExposed(mc, pos, adjPos)) {
                            newFound.add(pos.immutable());
                        }
                    }
                }
            }
        }

        foundDiamonds.removeIf(p -> (p.getX() >> 4) == chunkX && (p.getZ() >> 4) == chunkZ);
        foundDiamonds.addAll(newFound);
    }

    private boolean isExposed(Minecraft mc, BlockPos pos, BlockPos.MutableBlockPos adjPos) {
        for (Direction dir : Direction.values()) {
            adjPos.setWithOffset(pos, dir);
            BlockState adjState = mc.level.getBlockState(adjPos);
            if (adjState != null && (adjState.isAir() || !adjState.getFluidState().isEmpty() || !adjState.isSolidRender())) {
                return true;
            }
        }
        return false;
    }

    private List<int[]> getChunkOffsets(int radius) {
        List<int[]> offsets = new ArrayList<>();
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                offsets.add(new int[]{x, z});
            }
        }
        return offsets;
    }
}
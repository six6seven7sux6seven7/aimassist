package com.aimassist;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;
import java.util.function.DoubleConsumer;

public class AimConfigScreen extends Screen {
    public AimConfigScreen() { super(Text.literal("Aim Assist")); }

    @Override
    protected void init() {
        int w = 200, x = width / 2 - w / 2, y = height / 2 - 122, s = 24;
        addDrawableChild(new Slider(x, y, w, "Speed", 1, 25, AimConfig.speed, v -> AimConfig.speed = v));
        addDrawableChild(new Slider(x, y + s, w, "Max deg/sec", 50, 720, AimConfig.maxSpeed, v -> AimConfig.maxSpeed = v));
        addDrawableChild(new Slider(x, y + s * 2, w, "Range", 3, 12, AimConfig.range, v -> AimConfig.range = v));
        addDrawableChild(new Slider(x, y + s * 3, w, "FOV", 10, 360, AimConfig.fov, v -> AimConfig.fov = v));
        addDrawableChild(new Slider(x, y + s * 4, w, "Center zone", 0.1, 0.8, AimConfig.zone, v -> AimConfig.zone = v));
        addDrawableChild(new Slider(x, y + s * 5, w, "Aim height", 0.3, 0.95, AimConfig.height, v -> AimConfig.height = v));
        addDrawableChild(new Slider(x, y + s * 6, w, "Aim offset", 0, 0.8, AimConfig.offset, v -> AimConfig.offset = v));

        addDrawableChild(ButtonWidget.builder(label("Players only", AimConfig.playersOnly), b -> {
            AimConfig.playersOnly = !AimConfig.playersOnly; b.setMessage(label("Players only", AimConfig.playersOnly));
        }).dimensions(x, y + s * 7, w, 20).build());
        addDrawableChild(ButtonWidget.builder(label("Only when attacking", AimConfig.onlyWhenAttack), b -> {
            AimConfig.onlyWhenAttack = !AimConfig.onlyWhenAttack; b.setMessage(label("Only when attacking", AimConfig.onlyWhenAttack));
        }).dimensions(x, y + s * 8, w, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Done"), b -> close()).dimensions(x, y + s * 9 + 4, w, 20).build());
    }

    private static Text label(String n, boolean v) { return Text.literal(n + ": " + (v ? "ON" : "OFF")); }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        super.render(ctx, mx, my, delta);
        ctx.drawCenteredTextWithShadow(textRenderer, title, width / 2, height / 2 - 142, 0xFFFFFF);
    }

    @Override public void removed() { AimConfig.save(); }
    @Override public boolean shouldPause() { return false; }

    private static class Slider extends SliderWidget {
        private final String label; private final double min, max; private final DoubleConsumer setter;
        Slider(int x, int y, int w, String label, double min, double max, double cur, DoubleConsumer setter) {
            super(x, y, w, 20, Text.empty(), (cur - min) / (max - min));
            this.label = label; this.min = min; this.max = max; this.setter = setter;
            updateMessage();
        }
        private double get() { return min + (max - min) * value; }
        @Override protected void updateMessage() { setMessage(Text.literal(label + ": " + String.format("%.2f", get()))); }
        @Override protected void applyValue() { setter.accept(get()); }
    }
}

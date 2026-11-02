package com.aimassist;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

public class AimAssistClient implements ClientModInitializer {
    private static KeyBinding toggleKey, menuKey;
    private long lastNanos = System.nanoTime();
    private final java.util.Random rnd = new java.util.Random();
    private Entity offTarget;
    private double offX, offY, offZ, tgtX, tgtY, tgtZ, nextChange;

    @Override
    public void onInitializeClient() {
        AimConfig.load();
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.aimassist.toggle", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_G, "category.aimassist"));
        menuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.aimassist.menu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_H, "category.aimassist"));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (toggleKey.wasPressed()) {
                AimConfig.enabled = !AimConfig.enabled;
                if (mc.player != null)
                    mc.player.sendMessage(Text.literal("Aim Assist: " + (AimConfig.enabled ? "§aON" : "§cOFF")), true);
            }
            while (menuKey.wasPressed()) mc.setScreen(new AimConfigScreen());
        });

        WorldRenderEvents.START.register(ctx -> frame(MinecraftClient.getInstance()));
    }

    private void frame(MinecraftClient mc) {
        long now = System.nanoTime();
        double dt = Math.min((now - lastNanos) / 1e9, 0.1);
        lastNanos = now;

        if (!AimConfig.enabled || mc.player == null || mc.world == null || mc.currentScreen != null) return;
        if (AimConfig.onlyWhenAttack && !mc.options.attackKey.isPressed()) return;

        PlayerEntity me = mc.player;
        Vec3d eye = me.getEyePos();
        float curYaw = me.getYaw(), curPitch = me.getPitch();

        Entity best = null; Vec3d bestAim = null; double bestAng = Double.MAX_VALUE;
        for (Entity e : mc.world.getEntities()) {
            if (e == me || !(e instanceof LivingEntity le) || !le.isAlive() || le.isSpectator()) continue;
            if (AimConfig.playersOnly && !(e instanceof PlayerEntity)) continue;
            Box b = e.getBoundingBox();
            Vec3d aim = new Vec3d(b.getCenter().x, b.minY + (b.maxY - b.minY) * AimConfig.height, b.getCenter().z);
            if (eye.distanceTo(aim) > AimConfig.range) continue;
            double ey = MathHelper.wrapDegrees(yawTo(eye, aim) - curYaw);
            double ep = pitchTo(eye, aim) - curPitch;
            double ang = Math.hypot(ey, ep);
            if (ang > AimConfig.fov / 2.0 || ang >= bestAng) continue;
            best = e; bestAim = aim; bestAng = ang;
        }
        if (best == null) return;

        Box hb = best.getBoundingBox();
        if (best != offTarget) {
            offTarget = best;
            offX = tgtX = rnd.nextDouble() * 2 - 1;
            offY = tgtY = rnd.nextDouble() * 2 - 1;
            offZ = tgtZ = rnd.nextDouble() * 2 - 1;
            nextChange = now / 1e9 + 1.0;
        }
        if (now / 1e9 > nextChange) {
            tgtX = rnd.nextDouble() * 2 - 1; tgtY = rnd.nextDouble() * 2 - 1; tgtZ = rnd.nextDouble() * 2 - 1;
            nextChange = now / 1e9 + 1.0 + rnd.nextDouble() * 1.5;
        }
        double f = 1.0 - Math.exp(-1.5 * dt);
        offX += (tgtX - offX) * f; offY += (tgtY - offY) * f; offZ += (tgtZ - offZ) * f;
        bestAim = bestAim.add(offX * hb.getLengthX() / 2 * AimConfig.offset,
                              offY * hb.getLengthY() / 2 * AimConfig.offset * 0.5,
                              offZ * hb.getLengthZ() / 2 * AimConfig.offset);

        double errYaw = MathHelper.wrapDegrees(yawTo(eye, bestAim) - curYaw);
        double errPitch = pitchTo(eye, bestAim) - curPitch;

        Box box = best.getBoundingBox();
        Vec3d look = me.getRotationVec(1.0f);
        boolean inside = box.raycast(eye, eye.add(look.multiply(AimConfig.range + 1.0))).isPresent();
                if (inside && AimConfig.stopAtEdge) return;

        double speed = AimConfig.speed;
        if (inside) {
            double dist = Math.max(0.5, eye.distanceTo(box.getCenter()));
            double yawZone = Math.toDegrees(Math.atan((box.getLengthX() + box.getLengthZ()) / 4.0 / dist)) * AimConfig.zone;
            double pitchZone = Math.toDegrees(Math.atan(box.getLengthY() / 2.0 / dist)) * AimConfig.zone;
            if (Math.abs(errYaw) < yawZone) errYaw = 0;
            if (Math.abs(errPitch) < pitchZone) errPitch = 0;
            speed *= 0.5;
        }
        if (errYaw == 0 && errPitch == 0) return;

        double k = 1.0 - Math.exp(-speed * dt);
        double stepYaw = errYaw * k, stepPitch = errPitch * k;
        double maxStep = AimConfig.maxSpeed * dt, len = Math.hypot(stepYaw, stepPitch);
        if (len > maxStep) { stepYaw *= maxStep / len; stepPitch *= maxStep / len; }

        me.setYaw((float) (curYaw + stepYaw));
        me.setPitch(MathHelper.clamp((float) (curPitch + stepPitch), -90f, 90f));
    }

    private static float yawTo(Vec3d from, Vec3d to) {
        return (float) (Math.toDegrees(Math.atan2(to.z - from.z, to.x - from.x)) - 90.0);
    }

    private static float pitchTo(Vec3d from, Vec3d to) {
        double dx = to.x - from.x, dz = to.z - from.z;
        return (float) -Math.toDegrees(Math.atan2(to.y - from.y, Math.sqrt(dx * dx + dz * dz)));
    }
              }

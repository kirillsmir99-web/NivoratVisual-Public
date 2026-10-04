package rtx.nv.api.modules.impl.Visuals;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.entity.projectile.thrown.EggEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.entity.projectile.thrown.SnowballEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.TridentItem;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import rtx.nv.api.drags.Position;
import rtx.nv.api.events.EventHandler;
import rtx.nv.api.events.impl.render.HudRenderEvent;
import rtx.nv.api.events.impl.render.WorldRenderEvent;
import rtx.nv.api.modules.Category;
import rtx.nv.api.modules.Module;
import rtx.nv.api.modules.settings.impl.BooleanSetting;
import rtx.nv.api.modules.settings.impl.ColorSetting;
import rtx.nv.api.modules.settings.impl.ModeSetting;
import rtx.nv.api.modules.settings.impl.MultiSelectSetting;
import rtx.nv.api.modules.settings.impl.SeparatorSetting;
import rtx.nv.api.modules.settings.impl.SliderSetting;
import rtx.nv.api.ui.theme.ClientAccent;
import rtx.nv.utils.color.ColorUtil;
import rtx.nv.utils.render.pipeline.ClientPipelines;
import rtx.nv.utils.render.render2d.Render2D;

public final class ProjectileHelper extends Module {
    private static final int MAX_STEPS = 240;
    private static final String FONT = "montserrat-semibold";
    private static final float FONT_SIZE = 6.5f;

    private static final String PEARL = "Жемчуг эндера";
    private static final String ARROW = "Стрелы";
    private static final String TRIDENT = "Трезубец";
    private static final String POTION = "Зелья";
    private static final String SNOWBALL = "Снежки";
    private static final String ITEM = "Предметы";

    private static final String COLOR_CLIENT = "Клиент";
    private static final String COLOR_CUSTOM = "Свой";

    private final SeparatorSetting targetsSeparator = this.register(new SeparatorSetting("Снаряды"));
    private final MultiSelectSetting itemsToPredict = this.register(new MultiSelectSetting(
        "Цели",
        "Снаряды для визуального отслеживания."
    ).value(PEARL, ARROW, TRIDENT, POTION, SNOWBALL, ITEM).selected(PEARL, ARROW, TRIDENT, POTION, SNOWBALL));
    private final BooleanSetting fromHand = this.register(new BooleanSetting(
        "Предсказание из рук",
        "Отображать траекторию при удержании снаряда в руках.",
        true
    ));
    private final SliderSetting maxRange = this.register(new SliderSetting(
        "Дальность",
        "Максимальная дистанция отслеживания снарядов в блоках."
    ).range(16.0f, 256.0f).increment(4.0f).setValue(128.0f));

    private final SeparatorSetting geometrySeparator = this.register(new SeparatorSetting("Геометрия"));
    private final SliderSetting lineWidth = this.register(new SliderSetting(
        "Толщина линии",
        "Мгновенная толщина линии траектории."
    ).range(0.04f, 0.40f).increment(0.02f).setValue(0.12f));
    private final BooleanSetting lightNodes = this.register(new BooleanSetting(
        "Световые узлы",
        "Редкие светящиеся точки вдоль траектории полета.",
        true
    ));
    private final SliderSetting nodeInterval = this.register(new SliderSetting(
        "Интервал узлов",
        "Дистанция в блоках между световыми узлами на линии."
    ).range(1.0f, 8.0f).increment(0.5f).setValue(3.5f).visible(this.lightNodes::getValue));
    private final SliderSetting indicatorSize = this.register(new SliderSetting(
        "Размер маркера",
        "Радиус ориентированного маркера приземления на поверхности."
    ).range(0.20f, 1.20f).increment(0.05f).setValue(0.45f));
    private final BooleanSetting fadeTail = this.register(new BooleanSetting(
        "Затухание траектории",
        "Мягкое растворение начального отрезка траектории.",
        true
    ));
    private final BooleanSetting showTime = this.register(new BooleanSetting(
        "Индикатор времени",
        "Показывать расчетное время подлета до точки приземления.",
        true
    ));

    private final SeparatorSetting styleSeparator = this.register(new SeparatorSetting("Оформление"));
    private final ModeSetting colorMode = this.register(new ModeSetting(
        "Режим цвета",
        "Источник цветовой палитры для линии и маркера.",
        COLOR_CLIENT,
        COLOR_CLIENT,
        COLOR_CUSTOM
    ));
    private final ColorSetting customColor = this.register(new ColorSetting(
        "Свой цвет",
        "Пользовательский оттенок линии и маркера.",
        new Color(110, 225, 255, 230)
    ).visibleWhen(() -> this.colorMode.is(COLOR_CUSTOM)));
    private final SliderSetting opacity = this.register(new SliderSetting(
        "Прозрачность",
        "Общая прозрачность визуальных элементов."
    ).range(0.20f, 1.0f).increment(0.05f).setValue(0.85f));

    private final List<TrajectoryData> trajectories = new ArrayList<>();
    private final List<LandingData> landings = new ArrayList<>();
    private final List<Label> labels = new ArrayList<>();
    private final List<LivingEntity> livingCache = new ArrayList<>();
    private final Vector4f projectionScratch = new Vector4f();

    public ProjectileHelper() {
        super("ProjectileHelper", "Показывает траектории полета снарядов и точку их приземления", Category.VISUALS);
    }

    @Override
    protected void onDisable() {
        this.trajectories.clear();
        this.landings.clear();
        this.labels.clear();
        this.livingCache.clear();
    }

    private int resolveBaseColor() {
        int color;
        if (this.colorMode.is(COLOR_CLIENT)) {
            color = ClientAccent.accentOpaque();
        } else {
            color = this.customColor.getValue();
        }
        float alphaFactor = this.opacity.getFloat();
        return ColorUtil.multAlpha(color, alphaFactor);
    }

    private Profile profileFor(Entity entity) {
        if (entity instanceof EnderPearlEntity) {
            return Profile.PEARL;
        }
        if (entity instanceof SnowballEntity || entity instanceof EggEntity) {
            return Profile.SNOWBALL;
        }
        if (entity instanceof TridentEntity) {
            return Profile.TRIDENT;
        }
        if (entity instanceof PersistentProjectileEntity) {
            return Profile.ARROW;
        }
        if (entity instanceof PotionEntity) {
            return Profile.POTION;
        }
        if (entity instanceof ItemEntity) {
            return Profile.ITEM;
        }
        return null;
    }

    private Vec3d step(Vec3d pos, Vec3d velocity, Profile profile) {
        boolean inWater = this.mc.world != null && this.mc.world.getFluidState(BlockPos.ofFloored(pos)).isIn(FluidTags.WATER);
        double drag = inWater ? profile.waterDrag() : profile.drag();
        return velocity.multiply(drag).subtract(0.0, profile.gravity(), 0.0);
    }

    private void predict(Entity entity, Vec3d pos, Vec3d velocity, Profile profile) {
        ArrayList<Vec3d> points = new ArrayList<>();
        Vec3d currPos = pos;
        Vec3d currVel = velocity;
        int ticks = 0;
        Vec3d finalPos = currPos;
        Vec3d normal = new Vec3d(0.0, 1.0, 0.0);
        boolean isEntity = false;
        boolean finished = false;

        for (int i = 0; i < MAX_STEPS; ++i) {
            points.add(currPos);
            Vec3d prevPos = currPos;
            currPos = currPos.add(currVel);
            ++ticks;

            EntityHit entityHit = this.firstEntityInPath(prevPos, currPos, entity);
            if (entityHit != null) {
                finalPos = entityHit.hitPos();
                isEntity = true;
                normal = currVel.lengthSquared() > 1.0E-9 ? currVel.normalize().multiply(-1.0) : normal;
                finished = true;
                break;
            }

            BlockHitResult blockHitResult = this.mc.world != null ? this.mc.world.raycast(
                new RaycastContext(prevPos, currPos, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, entity)
            ) : null;

            if (blockHitResult != null && blockHitResult.getType() != HitResult.Type.MISS) {
                finalPos = blockHitResult.getPos();
                normal = faceToNormal(blockHitResult.getSide());
                finished = true;
                break;
            }

            if (currPos.y < -128.0) {
                finalPos = currPos;
                finished = true;
                break;
            }

            currVel = this.step(prevPos, currVel, profile);
        }

        if (!finished) {
            finalPos = currPos;
        }

        this.trajectories.add(new TrajectoryData(points, finalPos));
        this.landings.add(new LandingData(finalPos, ticks, isEntity, normal));
    }

    private HandShot handShot(PlayerEntity player, ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        Item item = stack.getItem();
        if (item == Items.ENDER_PEARL) {
            return new HandShot(Profile.PEARL, 1.5);
        }
        if (item == Items.SNOWBALL || item == Items.EGG) {
            return new HandShot(Profile.SNOWBALL, 1.5);
        }
        if (item == Items.SPLASH_POTION || item == Items.LINGERING_POTION) {
            return new HandShot(Profile.POTION, 0.5);
        }
        if (item instanceof CrossbowItem) {
            if (CrossbowItem.isCharged(stack)) {
                return new HandShot(Profile.ARROW, 3.15);
            }
            return null;
        }
        if (item instanceof BowItem) {
            if (!this.isUsing(player, stack)) {
                return null;
            }
            float pull = Math.max(0.05f, BowItem.getPullProgress(player.getItemUseTime()));
            return new HandShot(Profile.ARROW, 3.0 * (double)pull);
        }
        if (item instanceof TridentItem) {
            if (!this.isUsing(player, stack)) {
                return null;
            }
            float pull = MathHelper.clamp((float)player.getItemUseTime() / 10.0f, 0.1f, 1.0f);
            return new HandShot(Profile.TRIDENT, 2.5 * (double)pull);
        }
        if (item == Items.WIND_CHARGE) {
            return new HandShot(Profile.PEARL, 1.5);
        }
        return null;
    }

    private boolean isUsing(PlayerEntity player, ItemStack stack) {
        return player.isUsingItem() && !player.getActiveItem().isEmpty() && player.getActiveItem().getItem() == stack.getItem();
    }

    private void predictFromHand(float partialTicks) {
        ClientPlayerEntity player = this.mc.player;
        if (player == null) {
            return;
        }
        ItemStack stack = player.getMainHandStack();
        HandShot shot = this.handShot(player, stack);
        if (shot == null) {
            stack = player.getOffHandStack();
            shot = this.handShot(player, stack);
        }
        if (shot == null) {
            return;
        }

        Vec3d dir = this.directionFromRotation(player.getPitch(), player.getYaw());
        Vec3d startPos = player.getCameraPosVec(partialTicks).add(dir.multiply(0.2));
        Vec3d velocity = dir.multiply(shot.speed()).add(0.0, player.getVelocity().y * 0.5, 0.0);
        this.predict(player, startPos, velocity, shot.profile());
    }

    private Vec3d directionFromRotation(float pitch, float yaw) {
        float fPitch = pitch * ((float)Math.PI / 180.0f);
        float fYaw = -yaw * ((float)Math.PI / 180.0f);
        float cosPitch = MathHelper.cos(fPitch);
        return new Vec3d((double)(MathHelper.sin(fYaw) * cosPitch), (double)(-MathHelper.sin(fPitch)), (double)(MathHelper.cos(fYaw) * cosPitch)).normalize();
    }

    private void gatherLiving() {
        this.livingCache.clear();
        if (this.mc.world == null) {
            return;
        }
        for (Entity entity : this.mc.world.getEntities()) {
            if (!(entity instanceof LivingEntity living) || entity == this.mc.player) continue;
            this.livingCache.add(living);
        }
    }

    private EntityHit firstEntityInPath(Vec3d start, Vec3d end, Entity sourceEntity) {
        double closestDist = Double.MAX_VALUE;
        LivingEntity hitEntity = null;
        Vec3d hitPoint = null;
        for (LivingEntity target : this.livingCache) {
            if (target == sourceEntity) continue;
            Optional<Vec3d> rayHit = target.getBoundingBox().expand(0.12).raycast(start, end);
            if (rayHit.isEmpty()) continue;
            double dist = start.squaredDistanceTo(rayHit.get());
            if (dist < closestDist) {
                closestDist = dist;
                hitEntity = target;
                hitPoint = rayHit.get();
            }
        }
        return hitEntity != null ? new EntityHit(hitEntity, hitPoint) : null;
    }

    private static Vec3d faceToNormal(Direction direction) {
        if (direction == null) {
            return new Vec3d(0.0, 1.0, 0.0);
        }
        return new Vec3d(direction.getOffsetX(), direction.getOffsetY(), direction.getOffsetZ());
    }

    private static Vec3d anyPerpendicular(Vec3d v) {
        Vec3d perp = Math.abs(v.y) < 0.9 ? new Vec3d(0.0, 1.0, 0.0) : new Vec3d(1.0, 0.0, 0.0);
        return perp.crossProduct(v);
    }

    @EventHandler
    public void onWorldRender(WorldRenderEvent event) {
        this.trajectories.clear();
        this.landings.clear();
        this.labels.clear();

        if (this.mc.player == null || this.mc.world == null) {
            return;
        }

        float partialTicks = MathHelper.clamp(event.getPartialTicks(), 0.0f, 1.0f);
        this.gatherLiving();

        float maxDist = this.maxRange.getFloat();
        double maxDistSq = (double)(maxDist * maxDist);

        for (Entity entity : this.mc.world.getEntities()) {
            if (entity == this.mc.player) continue;
            if (this.mc.player.squaredDistanceTo(entity) > maxDistSq) continue;

            Profile profile = this.profileFor(entity);
            if (profile == null || !this.itemsToPredict.isSelected(profile.setting())) continue;

            Vec3d vel = entity.getVelocity();
            if (vel.lengthSquared() <= 1.0E-6) continue;

            this.predict(entity, entity.getLerpedPos(partialTicks), vel, profile);
        }

        if (this.fromHand.getValue()) {
            this.predictFromHand(partialTicks);
        }

        Camera camera = event.getCamera() == null ? this.mc.gameRenderer.getCamera() : event.getCamera();
        Vec3d camPos = camera.getCameraPos();
        VertexConsumerProvider.Immediate consumers = this.mc.getBufferBuilders().getEntityVertexConsumers();
        MatrixStack.Entry entry = event.getStack().peek();

        int baseColor = this.resolveBaseColor();
        double width = (double)this.lineWidth.getFloat() * 0.12;

        VertexConsumer lineConsumer = consumers.getBuffer(ClientPipelines.TARGET_CIRCLE_NODEPTH);

        for (TrajectoryData traj : this.trajectories) {
            this.renderRibbonLine(lineConsumer, entry, camPos, traj, width, baseColor);
        }

        VertexConsumer markerConsumer = consumers.getBuffer(ClientPipelines.PROJECTILE_TRIS);

        for (LandingData landing : this.landings) {
            this.renderSegmentedMarker(markerConsumer, entry, camPos, landing, baseColor);
        }

        if (this.lightNodes.getValue()) {
            VertexConsumer nodeConsumer = consumers.getBuffer(ClientPipelines.WORLD_PARTICLES_COLOR);
            double interval = (double)this.nodeInterval.getFloat();
            for (TrajectoryData traj : this.trajectories) {
                this.renderLightNodes(nodeConsumer, entry, camPos, traj, interval, baseColor, width * 1.8);
            }
            consumers.draw(ClientPipelines.WORLD_PARTICLES_COLOR);
        }

        consumers.draw(ClientPipelines.TARGET_CIRCLE_NODEPTH);
        consumers.draw(ClientPipelines.PROJECTILE_TRIS);

        if (this.showTime.getValue()) {
            this.collectLabels(event, camPos);
        }
    }

    private void renderRibbonLine(VertexConsumer consumer, MatrixStack.Entry entry, Vec3d camPos, TrajectoryData traj, double width, int baseColor) {
        List<Vec3d> pts = traj.points();
        int count = pts.size();
        if (count < 2) {
            return;
        }

        boolean fade = this.fadeTail.getValue();
        int rgb = baseColor & 0x00FFFFFF;
        int maxAlpha = (baseColor >> 24) & 0xFF;

        for (int i = 0; i < count - 1; ++i) {
            Vec3d p1 = pts.get(i);
            Vec3d p2 = pts.get(i + 1);

            Vec3d segDir = p2.subtract(p1);
            if (segDir.lengthSquared() < 1.0E-9) continue;

            Vec3d toCam = camPos.subtract(p1);
            Vec3d norm = segDir.crossProduct(toCam);
            if (norm.lengthSquared() < 1.0E-9) {
                norm = anyPerpendicular(segDir);
            }
            Vec3d perp = norm.normalize().multiply(width);

            float t1 = (float)i / (float)(count - 1);
            float t2 = (float)(i + 1) / (float)(count - 1);

            int a1 = maxAlpha;
            int a2 = maxAlpha;
            if (fade) {
                float f1 = MathHelper.clamp(t1 * 2.5f, 0.15f, 1.0f);
                float f2 = MathHelper.clamp(t2 * 2.5f, 0.15f, 1.0f);
                a1 = Math.round(maxAlpha * f1);
                a2 = Math.round(maxAlpha * f2);
            }

            int c1 = rgb | (a1 << 24);
            int c2 = rgb | (a2 << 24);

            Vec3d v1 = p1.subtract(perp);
            Vec3d v2 = p1.add(perp);
            Vec3d v3 = p2.add(perp);
            Vec3d v4 = p2.subtract(perp);

            this.vertex(consumer, entry, v1, camPos, c1);
            this.vertex(consumer, entry, v2, camPos, c1);
            this.vertex(consumer, entry, v3, camPos, c2);
            this.vertex(consumer, entry, v4, camPos, c2);
        }
    }

    private void renderLightNodes(VertexConsumer consumer, MatrixStack.Entry entry, Vec3d camPos, TrajectoryData traj, double interval, int baseColor, double nodeSize) {
        List<Vec3d> pts = traj.points();
        if (pts.size() < 2) return;

        double accumulatedDist = 0.0;
        int nodeColor = ColorUtil.multAlpha(baseColor, 0.95f);

        for (int i = 1; i < pts.size(); ++i) {
            Vec3d pPrev = pts.get(i - 1);
            Vec3d pCurr = pts.get(i);
            accumulatedDist += pPrev.distanceTo(pCurr);

            if (accumulatedDist >= interval) {
                accumulatedDist = 0.0;
                this.renderBillboardDiamond(consumer, entry, camPos, pCurr, nodeSize, nodeColor);
            }
        }
    }

    private void renderBillboardDiamond(VertexConsumer consumer, MatrixStack.Entry entry, Vec3d camPos, Vec3d pos, double size, int color) {
        Vec3d toCam = camPos.subtract(pos);
        if (toCam.lengthSquared() < 1.0E-9) return;
        Vec3d dir = toCam.normalize();
        Vec3d up = Math.abs(dir.y) < 0.95 ? new Vec3d(0.0, 1.0, 0.0) : new Vec3d(1.0, 0.0, 0.0);
        Vec3d right = dir.crossProduct(up).normalize().multiply(size);
        Vec3d perpUp = right.crossProduct(dir).normalize().multiply(size);

        Vec3d pTop = pos.add(perpUp);
        Vec3d pRight = pos.add(right);
        Vec3d pBottom = pos.subtract(perpUp);
        Vec3d pLeft = pos.subtract(right);

        this.vertex(consumer, entry, pTop, camPos, color);
        this.vertex(consumer, entry, pRight, camPos, color);
        this.vertex(consumer, entry, pBottom, camPos, color);
        this.vertex(consumer, entry, pLeft, camPos, color);
    }

    private void renderSegmentedMarker(VertexConsumer consumer, MatrixStack.Entry entry, Vec3d camPos, LandingData landing, int baseColor) {
        Vec3d rawPos = landing.pos();
        Vec3d normal = landing.normal().normalize();
        Vec3d surfacePos = rawPos.add(normal.multiply(0.015));

        double outerR = (double)this.indicatorSize.getFloat();
        double innerR = outerR * 0.72;
        double centerR = outerR * 0.28;

        Vec3d perp1 = anyPerpendicular(normal).normalize();
        Vec3d perp2 = normal.crossProduct(perp1).normalize();

        int segColor = ColorUtil.multAlpha(baseColor, 0.85f);
        int centerColor = ColorUtil.multAlpha(baseColor, 0.45f);

        int numSectors = 4;
        int stepsPerSector = 6;
        double gap = 0.14;

        for (int s = 0; s < numSectors; ++s) {
            double startAngle = (double)s * (Math.PI * 2.0 / numSectors) + gap;
            double endAngle = (double)(s + 1) * (Math.PI * 2.0 / numSectors) - gap;

            for (int step = 0; step < stepsPerSector; ++step) {
                double a1 = startAngle + (endAngle - startAngle) * ((double)step / stepsPerSector);
                double a2 = startAngle + (endAngle - startAngle) * ((double)(step + 1) / stepsPerSector);

                Vec3d o1 = surfacePos.add(perp1.multiply(Math.cos(a1) * outerR)).add(perp2.multiply(Math.sin(a1) * outerR));
                Vec3d o2 = surfacePos.add(perp1.multiply(Math.cos(a2) * outerR)).add(perp2.multiply(Math.sin(a2) * outerR));
                Vec3d i1 = surfacePos.add(perp1.multiply(Math.cos(a1) * innerR)).add(perp2.multiply(Math.sin(a1) * innerR));
                Vec3d i2 = surfacePos.add(perp1.multiply(Math.cos(a2) * innerR)).add(perp2.multiply(Math.sin(a2) * innerR));

                this.vertex(consumer, entry, i1, camPos, segColor);
                this.vertex(consumer, entry, o1, camPos, segColor);
                this.vertex(consumer, entry, o2, camPos, segColor);
                this.vertex(consumer, entry, i2, camPos, segColor);
            }
        }

        int centerSteps = 12;
        for (int i = 0; i < centerSteps; ++i) {
            double a1 = Math.PI * 2.0 * (double)i / centerSteps;
            double a2 = Math.PI * 2.0 * (double)(i + 1) / centerSteps;

            Vec3d c1 = surfacePos.add(perp1.multiply(Math.cos(a1) * centerR)).add(perp2.multiply(Math.sin(a1) * centerR));
            Vec3d c2 = surfacePos.add(perp1.multiply(Math.cos(a2) * centerR)).add(perp2.multiply(Math.sin(a2) * centerR));

            this.vertex(consumer, entry, surfacePos, camPos, centerColor);
            this.vertex(consumer, entry, c1, camPos, centerColor);
            this.vertex(consumer, entry, c2, camPos, centerColor);
            this.vertex(consumer, entry, surfacePos, camPos, centerColor);
        }
    }

    private void vertex(VertexConsumer consumer, MatrixStack.Entry entry, Vec3d pos, Vec3d camPos, int color) {
        consumer.vertex(entry, (float)(pos.x - camPos.x), (float)(pos.y - camPos.y), (float)(pos.z - camPos.z)).color(color);
    }

    private void collectLabels(WorldRenderEvent event, Vec3d camPos) {
        Matrix4f projMatrix = event.getProjectionMatrix();
        Matrix4f viewMatrix = event.getStack().peek().getPositionMatrix();
        float screenW = Position.screenWidth();
        float screenH = Position.screenHeight();

        for (LandingData landing : this.landings) {
            Vector4f projected = this.project(viewMatrix, projMatrix, camPos, landing.pos());
            if (projected == null) continue;

            float screenX = (projected.x / projected.w * 0.5f + 0.5f) * screenW;
            float screenY = (1.0f - (projected.y / projected.w * 0.5f + 0.5f)) * screenH;

            if (Float.isNaN(screenX) || Float.isNaN(screenY)) continue;
            this.labels.add(new Label(landing.ticks(), screenX, screenY));
        }
    }

    private Vector4f project(Matrix4f viewMatrix, Matrix4f projMatrix, Vec3d camPos, Vec3d targetPos) {
        Vector4f vec = this.projectionScratch.set((float)(targetPos.x - camPos.x), (float)(targetPos.y - camPos.y), (float)(targetPos.z - camPos.z), 1.0f);
        viewMatrix.transform(vec);
        projMatrix.transform(vec);
        if (vec.w <= 1.0E-4f) {
            return null;
        }
        return vec;
    }

    @EventHandler
    public void onHud(HudRenderEvent event) {
        if (!this.showTime.getValue() || this.labels.isEmpty()) {
            return;
        }
        DrawContext context = event.getGraphics();
        Render2D.beginFrame(context);
        for (Label label : this.labels) {
            double seconds = (double)(label.ticks() * 50) / 1000.0;
            String text = String.format(Locale.US, "%.1fs", seconds);
            float textW = Render2D.msdfWidth(FONT, text, FONT_SIZE);
            float pillW = textW + 8.0f;
            float pillH = 9.0f;
            float x = label.x() - pillW / 2.0f;
            float y = label.y() - pillH - 4.0f;

            Render2D.rect(x, y, pillW, pillH, ColorUtil.rgba(10, 14, 20, 190));
            Render2D.outline(x, y, pillW, pillH, 0.0f, 0.75f, ColorUtil.multAlpha(ClientAccent.accentOpaque(), 0.6f));
            Render2D.msdfText(FONT, text, x + (pillW - textW) / 2.0f, y + 2.0f, FONT_SIZE, -1);
        }
        Render2D.flush();
    }

    public static record TrajectoryData(List<Vec3d> points, Vec3d finalPos) {}
    public static record LandingData(Vec3d pos, int ticks, boolean isEntity, Vec3d normal) {}
    public static record Label(int ticks, float x, float y) {}
    public static record HandShot(Profile profile, double speed) {}
    public static record EntityHit(LivingEntity entity, Vec3d hitPos) {}

    public enum Profile {
        ARROW(0.05, 0.99, 0.6, ProjectileHelper.ARROW),
        PEARL(0.03, 0.99, 0.8, ProjectileHelper.PEARL),
        SNOWBALL(0.03, 0.99, 0.8, ProjectileHelper.SNOWBALL),
        TRIDENT(0.05, 0.99, 0.6, ProjectileHelper.TRIDENT),
        POTION(0.05, 0.99, 0.8, ProjectileHelper.POTION),
        ITEM(0.04, 0.98, 0.8, ProjectileHelper.ITEM);

        private final double gravity;
        private final double drag;
        private final double waterDrag;
        private final String setting;

        Profile(double gravity, double drag, double waterDrag, String setting) {
            this.gravity = gravity;
            this.drag = drag;
            this.waterDrag = waterDrag;
            this.setting = setting;
        }

        public double gravity() { return gravity; }
        public double drag() { return drag; }
        public double waterDrag() { return waterDrag; }
        public String setting() { return setting; }
    }
}

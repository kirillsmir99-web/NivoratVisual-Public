package rtx.nv.api.modules.impl.Visuals.particles;

import net.minecraft.util.Identifier;

public final class ParticleConstants {
    public static final ParticleTexture POINT = new ParticleTexture("Точка", Identifier.of("nv", "textures/features/particles/point.png"));
    public static final ParticleTexture STAR = new ParticleTexture("Звезда", Identifier.of("nv", "textures/features/particles/star.png"));
    public static final ParticleTexture HEART = new ParticleTexture("Сердце", Identifier.of("nv", "textures/features/particles/heart.png"));
    public static final ParticleTexture TRIANGLE = new ParticleTexture("Треугольник", Identifier.of("nv", "textures/features/particles/triangle.png"));

    public static final ParticleTexture SPARKS = new ParticleTexture("Искры", Identifier.of("nv", "textures/features/particles/sparks.png"));
    public static final ParticleTexture LIGHT_STREAKS = new ParticleTexture("Световые штрихи", Identifier.of("nv", "textures/features/particles/light_streaks.png"));
    public static final ParticleTexture ORBITAL_POINTS = new ParticleTexture("Орбитальные точки", Identifier.of("nv", "textures/features/particles/orbital_points.png"));

    public static final ParticleTexture ARCS = new ParticleTexture("Дуги", Identifier.of("nv", "textures/features/particles/arcs.png"));
    public static final ParticleTexture LIGHT_PETALS = new ParticleTexture("Лепестки света", Identifier.of("nv", "textures/features/particles/light_petals.png"));

    public static final ParticleTexture[] WORLD_TEXTURES = new ParticleTexture[]{
        POINT, STAR, HEART, TRIANGLE, SPARKS, LIGHT_STREAKS, ORBITAL_POINTS
    };

    public static final ParticleTexture[] HIT_TEXTURES = new ParticleTexture[]{
        POINT, STAR, HEART, SPARKS, ARCS, LIGHT_PETALS
    };

    public static final ParticleTexture[] TEXTURES = WORLD_TEXTURES;

    private ParticleConstants() {}
}

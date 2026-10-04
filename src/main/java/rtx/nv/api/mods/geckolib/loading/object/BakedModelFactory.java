package rtx.nv.api.mods.geckolib.loading.object;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.Map;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import rtx.nv.api.mods.geckolib.cache.model.BakedGeoModel;
import rtx.nv.api.mods.geckolib.cache.model.GeoBone;
import rtx.nv.api.mods.geckolib.cache.model.GeoQuad;
import rtx.nv.api.mods.geckolib.cache.model.GeoVertex;
import rtx.nv.api.mods.geckolib.cache.model.cuboid.GeoCube;
import rtx.nv.api.mods.geckolib.loading.definition.geometry.GeometryDescription;
import rtx.nv.api.mods.geckolib.loading.json.raw.Cube;
import rtx.nv.api.mods.geckolib.loading.json.raw.FaceUV;
import rtx.nv.api.mods.geckolib.loading.json.raw.FaceUV.Rotation;
import rtx.nv.api.mods.geckolib.loading.json.raw.UVUnion;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import rtx.nv.api.mods.geckolib.cache.model.cuboid.CuboidGeoBone;
import rtx.nv.api.mods.geckolib.loading.json.raw.ModelProperties;
import rtx.nv.api.mods.geckolib.loading.json.raw.Bone;
import rtx.nv.api.mods.geckolib.util.JsonUtil;
import rtx.nv.api.mods.geckolib.loading.object.BoneStructure;
import rtx.nv.api.mods.geckolib.loading.object.GeometryTree;

public interface BakedModelFactory {
    public static final Map<String, BakedModelFactory> FACTORIES = new Object2ObjectOpenHashMap(1);
    public static final BakedModelFactory DEFAULT_FACTORY = new BakedModelFactory.Builtin();

    public static void register(String string, BakedModelFactory bakedModelFactory) {
        FACTORIES.put(string, bakedModelFactory);
    }

    public BakedGeoModel constructGeoModel(GeometryTree var1);

    public static BakedModelFactory getForNamespace(String string) {
        return FACTORIES.getOrDefault(string, DEFAULT_FACTORY);
    }

    default public GeoQuad[] buildQuads(UVUnion uVUnion, BakedModelFactory.VertexSet vertexSet, Cube cube, float f, float f2, boolean bl) {
        GeoQuad[] geoQuadArray = new GeoQuad[]{this.buildQuad(vertexSet, cube, uVUnion, f, f2, bl, Direction.WEST), this.buildQuad(vertexSet, cube, uVUnion, f, f2, bl, Direction.EAST), this.buildQuad(vertexSet, cube, uVUnion, f, f2, bl, Direction.NORTH), this.buildQuad(vertexSet, cube, uVUnion, f, f2, bl, Direction.SOUTH), this.buildQuad(vertexSet, cube, uVUnion, f, f2, bl, Direction.UP), this.buildQuad(vertexSet, cube, uVUnion, f, f2, bl, Direction.DOWN)};
        return geoQuadArray;
    }

    default public GeoQuad buildQuad(BakedModelFactory.VertexSet vertexSet, Cube cube, UVUnion uVUnion, float f, float f2, boolean bl, Direction direction) {
        return (GeoQuad)uVUnion.uvData().map(dArray -> {
            double[][] dArrayArray;
            double[] dArray2 = cube.size();
            Vec3d vec3d = new Vec3d(Math.floor(dArray2[0]), Math.floor(dArray2[1]), Math.floor(dArray2[2]));
            switch (direction) {
                default: {
                    throw new MatchException(null, null);
                }
                case WEST: {
                    double[][] dArrayArray2 = new double[2][];
                    dArrayArray2[0] = new double[]{dArray[0] + vec3d.z + vec3d.x, dArray[1] + vec3d.z};
                    dArrayArray = dArrayArray2;
                    dArrayArray2[1] = new double[]{vec3d.z, vec3d.y};
                    break;
                }
                case EAST: {
                    double[][] dArrayArray3 = new double[2][];
                    dArrayArray3[0] = new double[]{dArray[0], dArray[1] + vec3d.z};
                    dArrayArray = dArrayArray3;
                    dArrayArray3[1] = new double[]{vec3d.z, vec3d.y};
                    break;
                }
                case NORTH: {
                    double[][] dArrayArray4 = new double[2][];
                    dArrayArray4[0] = new double[]{dArray[0] + vec3d.z, dArray[1] + vec3d.z};
                    dArrayArray = dArrayArray4;
                    dArrayArray4[1] = new double[]{vec3d.x, vec3d.y};
                    break;
                }
                case SOUTH: {
                    double[][] dArrayArray5 = new double[2][];
                    dArrayArray5[0] = new double[]{dArray[0] + vec3d.z + vec3d.x + vec3d.z, dArray[1] + vec3d.z};
                    dArrayArray = dArrayArray5;
                    dArrayArray5[1] = new double[]{vec3d.x, vec3d.y};
                    break;
                }
                case UP: {
                    double[][] dArrayArray6 = new double[2][];
                    dArrayArray6[0] = new double[]{dArray[0] + vec3d.z, dArray[1]};
                    dArrayArray = dArrayArray6;
                    dArrayArray6[1] = new double[]{vec3d.x, vec3d.z};
                    break;
                }
                case DOWN: {
                    double[][] dArrayArray7 = new double[2][];
                    dArrayArray7[0] = new double[]{dArray[0] + vec3d.z + vec3d.x, dArray[1] + vec3d.z};
                    dArrayArray = dArrayArray7;
                    dArrayArray7[1] = new double[]{vec3d.x, -vec3d.z};
                }
            }
            double[][] dArrayArray8 = dArrayArray;
            return GeoQuad.build(vertexSet.verticesForQuad(direction, true, bl || cube.mirror() == Boolean.TRUE), dArrayArray8[0], dArrayArray8[1], FaceUV.Rotation.NONE, f, f2, bl, direction);
        }, uVFaces -> {
            FaceUV faceUV = uVFaces.fromDirection(direction);
            if (faceUV == null) {
                return null;
            }
            return GeoQuad.build(vertexSet.verticesForQuad(direction, false, bl || cube.mirror() == Boolean.TRUE), faceUV.uv(), faceUV.uvSize(), faceUV.uvRotation(), f, f2, bl, direction);
        });
    }

    public GeoBone constructBone(BoneStructure var1, GeometryDescription var2, GeoBone var3);

    public GeoCube constructCube(Cube var1, GeometryDescription var2, float var3);


    public static final class Builtin
    implements BakedModelFactory {
        @Override
        public BakedGeoModel constructGeoModel(GeometryTree geometryTree) {
            java.util.List<GeoBone> objectArrayList = new ObjectArrayList<>();
            for (BoneStructure boneStructure : geometryTree.topLevelBones().values()) {
                objectArrayList.add(this.constructBone(boneStructure, geometryTree.properties(), null));
            }
            GeometryDescription geometryDescription = geometryTree.properties();
            return new BakedGeoModel(objectArrayList.toArray(new GeoBone[0]), new ModelProperties(geometryDescription.identifier(), geometryDescription.visibleBoundsWidth(), geometryDescription.visibleBoundsHeight(), geometryDescription.visibleBoundsOffset(), geometryDescription.textureWidth(), geometryDescription.textureHeight()));
        }
    
        @Override
        public GeoBone constructBone(BoneStructure boneStructure, GeometryDescription geometryDescription, GeoBone geoBone) {
            int n;
            Bone bone = boneStructure.self();
            Vec3d vec3d = JsonUtil.arrayToVec(bone.pivot());
            Vec3d vec3d2 = JsonUtil.arrayToVec(bone.rotation());
            GeoBone[] geoBoneArray = new GeoBone[boneStructure.children().size()];
            GeoCube[] geoCubeArray = new GeoCube[bone.cubes().length];
            CuboidGeoBone cuboidGeoBone = new CuboidGeoBone(geoBone, bone.name(), geoBoneArray, geoCubeArray, (float)(-vec3d.x), (float)vec3d.y, (float)vec3d.z, (float)Math.toRadians(-vec3d2.x), (float)Math.toRadians(-vec3d2.y), (float)Math.toRadians(vec3d2.z));
            for (n = 0; n < bone.cubes().length; ++n) {
                geoCubeArray[n] = this.constructCube(bone.cubes()[n], geometryDescription, bone.inflate() == null ? 0.0f : bone.inflate().floatValue());
            }
            n = 0;
            for (BoneStructure boneStructure2 : boneStructure.children().values()) {
                geoBoneArray[n++] = this.constructBone(boneStructure2, geometryDescription, cuboidGeoBone);
            }
            return cuboidGeoBone;
        }
    
        @Override
        public GeoCube constructCube(Cube cube, GeometryDescription geometryDescription, float f) {
            boolean bl = cube.mirror() == Boolean.TRUE;
            double d = cube.inflate() != null ? cube.inflate() / 16.0 : (double)(f / 16.0f);
            Vec3d vec3d = JsonUtil.arrayToVec(cube.size());
            Vec3d vec3d2 = JsonUtil.arrayToVec(cube.origin());
            Vec3d vec3d3 = JsonUtil.arrayToVec(cube.rotation());
            Vec3d vec3d4 = JsonUtil.arrayToVec(cube.pivot());
            vec3d2 = new Vec3d(-(vec3d2.x + vec3d.x) / 16.0, vec3d2.y / 16.0, vec3d2.z / 16.0);
            Vec3d vec3d5 = vec3d.multiply(0.0625, 0.0625, 0.0625);
            vec3d4 = vec3d4.multiply(-1.0, 1.0, 1.0);
            vec3d3 = new Vec3d(Math.toRadians(-vec3d3.x), Math.toRadians(-vec3d3.y), Math.toRadians(-vec3d3.z));
            GeoQuad[] geoQuadArray = this.buildQuads(cube.uv(), new BakedModelFactory.VertexSet(vec3d2, vec3d5, d), cube, geometryDescription.textureWidth(), geometryDescription.textureHeight(), bl);
            return new GeoCube(geoQuadArray, vec3d4, vec3d3, vec3d);
        }
    }
    
        public static record VertexSet(GeoVertex bottomLeftBack, GeoVertex bottomRightBack, GeoVertex topLeftBack, GeoVertex topRightBack, GeoVertex topLeftFront, GeoVertex topRightFront, GeoVertex bottomLeftFront, GeoVertex bottomRightFront) {
        public VertexSet(Vec3d vec3d, Vec3d vec3d2, double d) {
            this(new GeoVertex(vec3d.x - d, vec3d.y - d, vec3d.z - d), new GeoVertex(vec3d.x - d, vec3d.y - d, vec3d.z + vec3d2.z + d), new GeoVertex(vec3d.x - d, vec3d.y + vec3d2.y + d, vec3d.z - d), new GeoVertex(vec3d.x - d, vec3d.y + vec3d2.y + d, vec3d.z + vec3d2.z + d), new GeoVertex(vec3d.x + vec3d2.x + d, vec3d.y + vec3d2.y + d, vec3d.z - d), new GeoVertex(vec3d.x + vec3d2.x + d, vec3d.y + vec3d2.y + d, vec3d.z + vec3d2.z + d), new GeoVertex(vec3d.x + vec3d2.x + d, vec3d.y - d, vec3d.z - d), new GeoVertex(vec3d.x + vec3d2.x + d, vec3d.y - d, vec3d.z + vec3d2.z + d));
        }
    
        public GeoVertex[] quadWest() {
            return new GeoVertex[]{this.topRightBack, this.topLeftBack, this.bottomLeftBack, this.bottomRightBack};
        }
    
        public GeoVertex[] quadEast() {
            return new GeoVertex[]{this.topLeftFront, this.topRightFront, this.bottomRightFront, this.bottomLeftFront};
        }
    
        public GeoVertex[] quadNorth() {
            return new GeoVertex[]{this.topLeftBack, this.topLeftFront, this.bottomLeftFront, this.bottomLeftBack};
        }
    
        public GeoVertex[] quadSouth() {
            return new GeoVertex[]{this.topRightFront, this.topRightBack, this.bottomRightBack, this.bottomRightFront};
        }
    
        public GeoVertex[] quadDown() {
            return new GeoVertex[]{this.bottomLeftBack, this.bottomLeftFront, this.bottomRightFront, this.bottomRightBack};
        }
    
        public GeoVertex[] quadUp() {
            return new GeoVertex[]{this.topRightBack, this.topRightFront, this.topLeftFront, this.topLeftBack};
        }
    
        public GeoVertex[] verticesForQuad(Direction direction, boolean bl, boolean bl2) {
            return switch (direction) {
                default -> throw new MatchException(null, null);
                case Direction.WEST -> {
                    if (bl2) {
                        yield this.quadEast();
                    }
                    yield this.quadWest();
                }
                case Direction.EAST -> {
                    if (bl2) {
                        yield this.quadWest();
                    }
                    yield this.quadEast();
                }
                case Direction.NORTH -> this.quadNorth();
                case Direction.SOUTH -> this.quadSouth();
                case Direction.UP -> {
                    if (bl2 && !bl) {
                        yield this.quadDown();
                    }
                    yield this.quadUp();
                }
                case Direction.DOWN -> bl2 && !bl ? this.quadUp() : this.quadDown();
            };
        }
    }
}


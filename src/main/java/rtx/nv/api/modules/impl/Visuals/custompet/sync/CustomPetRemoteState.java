package rtx.nv.api.modules.impl.Visuals.custompet.sync;
import net.minecraft.util.math.Vec3d;
import rtx.nv.api.modules.impl.Visuals.custompet.CustomPetVariant;

public record CustomPetRemoteState(String identityKey, String profileUsername, String minecraftUsername, CustomPetVariant variant, int robotType, boolean active, double x, double y, double z, float yaw, boolean moving, boolean umbrella, boolean airborne, double animationSpeed, String petKind) {
    public CustomPetRemoteState {
        identityKey = identityKey == null ? "" : identityKey.trim();
        profileUsername = profileUsername == null ? "" : profileUsername.trim();
        minecraftUsername = minecraftUsername == null ? "" : minecraftUsername.trim();
        variant = variant == null ? CustomPetVariant.NITWIT : variant;
        animationSpeed = animationSpeed <= 0.0 ? 1.0 : animationSpeed;
        petKind = petKind == null || petKind.isBlank() ? "frog" : petKind.trim();
    }

    public Vec3d position() {
        return new Vec3d(this.x, this.y, this.z);
    }

    public boolean isOwl() {
        return "owl".equals(this.petKind);
    }
}


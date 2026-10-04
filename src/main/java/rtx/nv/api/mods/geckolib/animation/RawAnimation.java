package rtx.nv.api.mods.geckolib.animation;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import rtx.nv.api.mods.geckolib.animation.object.LoopType;

public final class RawAnimation {
    public static final PacketCodec<ByteBuf, RawAnimation> STREAM_CODEC = PacketCodec.tuple(RawAnimation.Stage.STREAM_CODEC.collect(PacketCodecs.toList()), (RawAnimation rawAnimation) -> rawAnimation.animationList, RawAnimation::new);
    private final List<RawAnimation.Stage> animationList;

    private RawAnimation() {
        this((List<RawAnimation.Stage>)new ObjectArrayList());
    }

    private RawAnimation(List<RawAnimation.Stage> list) {
        this.animationList = list;
    }

    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof RawAnimation)) {
            return false;
        }
        RawAnimation rawAnimation = (RawAnimation)object;
        if (this.animationList.size() != rawAnimation.animationList.size()) {
            return false;
        }
        return this.hashCode() == object.hashCode();
    }

    public String toString() {
        return "RawAnimation{" + this.animationList.stream().map(RawAnimation.Stage::toString).collect(Collectors.joining(" -> ")) + "}";
    }

    public int hashCode() {
        return Objects.hash(this.animationList);
    }

    public static RawAnimation copyOf(RawAnimation rawAnimation) {
        RawAnimation rawAnimation2 = RawAnimation.begin();
        rawAnimation2.animationList.addAll(rawAnimation.animationList);
        return rawAnimation2;
    }

    public static RawAnimation begin() {
        return new RawAnimation();
    }

    public RawAnimation thenWait(int n) {
        this.animationList.add(new RawAnimation.Stage("internal.wait", LoopType.PLAY_ONCE, n));
        return this;
    }

    public RawAnimation thenLoop(String string) {
        return this.then(string, LoopType.LOOP);
    }

    public RawAnimation thenPlay(String string) {
        return this.then(string, LoopType.DEFAULT);
    }

    public RawAnimation thenPlayAndHold(String string) {
        return this.then(string, LoopType.HOLD_ON_LAST_FRAME);
    }

    public RawAnimation thenPlayXTimes(String string, int n) {
        for (int i = 0; i < n; ++i) {
            this.then(string, i == n - 1 ? LoopType.DEFAULT : LoopType.PLAY_ONCE);
        }
        return this;
    }

    public List<RawAnimation.Stage> getAnimationStages() {
        return this.animationList;
    }

    public int getStageCount() {
        return this.animationList.size();
    }

    public RawAnimation then(String string, LoopType loopType) {
        this.animationList.add(new RawAnimation.Stage(string, loopType));
        return this;
    }


    public static record Stage(String animationName, LoopType loopType, int waitTicks) {
        public static final PacketCodec<ByteBuf, Stage> STREAM_CODEC = PacketCodec.tuple((PacketCodec)PacketCodecs.STRING, Stage::animationName, (PacketCodec)PacketCodecs.STRING.xmap(LoopType::fromString, LoopType::getId), Stage::loopType, (PacketCodec)PacketCodecs.VAR_INT, Stage::waitTicks, Stage::new);
        public static final String WAIT = "internal.wait";
    
        public Stage(String string, LoopType loopType) {
            this(string, loopType, 0);
        }
    
        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (object == null || this.getClass() != object.getClass()) {
                return false;
            }
            return this.hashCode() == object.hashCode();
        }
    
        @Override
        public String toString() {
            return this.animationName;
        }
    
        @Override
        public int hashCode() {
            return Objects.hash(this.animationName, this.loopType);
        }
    }
}


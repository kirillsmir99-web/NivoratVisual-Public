package rtx.nv.utils.render.targetesp;

@FunctionalInterface
public interface TargetEspColorProvider {
    int getColor(int index, float alpha);
}

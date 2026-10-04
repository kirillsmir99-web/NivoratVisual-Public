package rtx.nv.api.mods.geckolib.service;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.block.entity.state.BlockEntityRenderState;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.state.BipedEntityRenderState;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import rtx.nv.api.mods.geckolib.animatable.GeoAnimatable;
import rtx.nv.api.mods.geckolib.animatable.GeoBlockEntity;
import rtx.nv.api.mods.geckolib.animatable.GeoItem;
import rtx.nv.api.mods.geckolib.renderer.GeoArmorRenderer;
import rtx.nv.api.mods.geckolib.renderer.GeoBlockRenderer;
import rtx.nv.api.mods.geckolib.renderer.GeoEntityRenderer;
import rtx.nv.api.mods.geckolib.renderer.GeoItemRenderer;
import rtx.nv.api.mods.geckolib.renderer.GeoObjectRenderer;
import rtx.nv.api.mods.geckolib.renderer.GeoReplacedEntityRenderer;
import rtx.nv.api.mods.geckolib.renderer.base.GeoRenderState;
import rtx.nv.api.mods.geckolib.renderer.base.RenderPassInfo;

public interface GeckoLibEvents {
    <T extends BlockEntity & GeoBlockEntity, R extends BlockEntityRenderState & GeoRenderState> void fireCompileBlockRenderLayers(GeoBlockRenderer<T, R> r);
    <T extends BlockEntity & GeoBlockEntity, R extends BlockEntityRenderState & GeoRenderState> void fireCompileBlockRenderState(GeoBlockRenderer<T, R> r, R state, T animatable);
    <T extends Item & GeoItem, R extends BipedEntityRenderState & GeoRenderState> void fireCompileArmorRenderLayers(GeoArmorRenderer<T, R> r);
    <T extends Entity & GeoAnimatable, R extends EntityRenderState & GeoRenderState> void fireCompileEntityRenderLayers(GeoEntityRenderer<T, R> r);
    <T extends Entity & GeoAnimatable, R extends EntityRenderState & GeoRenderState> void fireCompileEntityRenderState(GeoEntityRenderer<T, R> r, R state, T animatable);
    <T extends Entity & GeoAnimatable, R extends EntityRenderState & GeoRenderState> boolean fireEntityPreRender(GeoEntityRenderer<T, R> r, RenderPassInfo<R> info, OrderedRenderCommandQueue queue);
    <T extends GeoAnimatable, E extends Entity, R extends EntityRenderState & GeoRenderState> void fireCompileReplacedEntityRenderLayers(GeoReplacedEntityRenderer<T, E, R> r);
    <T extends Item & GeoItem, O extends GeoArmorRenderer.RenderData, R extends BipedEntityRenderState & GeoRenderState> void fireCompileArmorRenderState(GeoArmorRenderer<T, R> r, R state, T animatable, O data);
    <T extends GeoAnimatable, E extends Entity, R extends EntityRenderState & GeoRenderState> boolean fireReplacedEntityPreRender(GeoReplacedEntityRenderer<T, E, R> r, RenderPassInfo<R> info, OrderedRenderCommandQueue queue);
    <T extends GeoAnimatable, E extends Entity, R extends EntityRenderState & GeoRenderState> void fireCompileReplacedEntityRenderState(GeoReplacedEntityRenderer<T, E, R> r, R state, T animatable, E entity);
    <T extends Item & GeoItem> void fireCompileItemRenderLayers(GeoItemRenderer<T> r);
    <T extends GeoAnimatable, E, R extends GeoRenderState> void fireCompileObjectRenderLayers(GeoObjectRenderer<T, E, R> r);
    <T extends GeoAnimatable, E, R extends GeoRenderState> void fireCompileObjectRenderState(GeoObjectRenderer<T, E, R> r, R state, T animatable, E entity);
    <T extends Item & GeoItem, O extends GeoItemRenderer.RenderData, R extends GeoRenderState> void fireCompileItemRenderState(GeoItemRenderer<T> r, R state, T animatable, O data);
    <T extends Item & GeoItem> boolean fireItemPreRender(GeoItemRenderer<T> r, RenderPassInfo<GeoRenderState> info, OrderedRenderCommandQueue queue);
    <T extends GeoAnimatable, E, R extends GeoRenderState> boolean fireObjectPreRender(GeoObjectRenderer<T, E, R> r, RenderPassInfo<R> info, OrderedRenderCommandQueue queue);
    <T extends BlockEntity & GeoBlockEntity, R extends BlockEntityRenderState & GeoRenderState> boolean fireBlockPreRender(GeoBlockRenderer<T, R> r, RenderPassInfo<R> info, OrderedRenderCommandQueue queue);
    <T extends Item & GeoItem, R extends BipedEntityRenderState & GeoRenderState> boolean fireArmorPreRender(GeoArmorRenderer<T, R> r, RenderPassInfo<R> info, OrderedRenderCommandQueue queue);
}

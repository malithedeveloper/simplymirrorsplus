package com.malidev.simplymirrorsplus.client;

import java.util.function.IntConsumer;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public final class MirrorSettingsScreen extends Screen {
    public static final KeyMapping KEY=new KeyMapping("key.simplymirrorsplus.settings",GLFW.GLFW_KEY_M,KeyMapping.Category.MISC);
    private static final int WIDTH=200,HEIGHT=20,ROW=24;

    public MirrorSettingsScreen() { super(Component.translatable("simplymirrorsplus.settings.title")); }

    @Override protected void init() {
        int x=width/2-WIDTH/2,y=height/2-ROW*3;
        addRenderableWidget(CycleButton.onOffBuilder(MirrorConfig.enabled).create(x,y,WIDTH,HEIGHT,
            Component.translatable("simplymirrorsplus.settings.enabled"),(button,value)->MirrorConfig.enabled=value));
        addRenderableWidget(new StepSlider(x,y+ROW,"simplymirrorsplus.settings.resolution",resolutionSteps(),MirrorConfig.resolutionPercent,value->MirrorConfig.resolutionPercent=value));
        addRenderableWidget(new StepSlider(x,y+ROW*2,"simplymirrorsplus.settings.refresh_rate",MirrorConfig.REFRESH_RATES,MirrorConfig.refreshRate,value->MirrorConfig.refreshRate=value));
        addRenderableWidget(CycleButton.onOffBuilder(MirrorConfig.playersOnly).create(x,y+ROW*3,WIDTH,HEIGHT,
            Component.translatable("simplymirrorsplus.settings.players_only"),(button,value)->MirrorConfig.playersOnly=value));
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE,button->onClose()).bounds(x,y+ROW*4+8,WIDTH,HEIGHT).build());
    }

    private static int[] resolutionSteps() {
        int[] steps=new int[(MirrorConfig.MAX_RESOLUTION-MirrorConfig.MIN_RESOLUTION)/5+1];
        for(int i=0;i<steps.length;i++)steps[i]=MirrorConfig.MIN_RESOLUTION+i*5;
        return steps;
    }

    @Override public void extractRenderState(GuiGraphicsExtractor graphics,int mouseX,int mouseY,float partial) {
        super.extractRenderState(graphics,mouseX,mouseY,partial);
        graphics.centeredText(font,title,width/2,height/2-ROW*3-16,0xFFFFFFFF);
    }

    /** No blur or dimming: changes must be visible on the mirror while adjusting them. */
    @Override public void extractBackground(GuiGraphicsExtractor graphics,int mouseX,int mouseY,float partial) { }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void removed() { MirrorConfig.save(); }

    private static final class StepSlider extends AbstractSliderButton {
        private final String label;
        private final int[] steps;
        private final IntConsumer onChange;

        StepSlider(int x,int y,String label,int[] steps,int current,IntConsumer onChange) {
            super(x,y,WIDTH,HEIGHT,CommonComponents.EMPTY,position(steps,current));
            this.label=label;
            this.steps=steps;
            this.onChange=onChange;
            updateMessage();
        }

        private static double position(int[] steps,int current) {
            int nearest=0;
            for(int i=0;i<steps.length;i++)if(Math.abs(steps[i]-current)<Math.abs(steps[nearest]-current))nearest=i;
            return nearest/(steps.length-1d);
        }

        private int index() { return (int)Math.round(value*(steps.length-1)); }

        @Override protected void updateMessage() {
            setMessage(Component.translatable(label,steps[index()]));
        }

        @Override protected void applyValue() {
            int index=index();
            value=index/(steps.length-1d);
            onChange.accept(steps[index]);
        }
    }
}

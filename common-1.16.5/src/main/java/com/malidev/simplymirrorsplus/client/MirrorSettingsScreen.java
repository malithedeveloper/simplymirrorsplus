package com.malidev.simplymirrorsplus.client;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.function.IntConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import org.lwjgl.glfw.GLFW;

public final class MirrorSettingsScreen extends Screen {
    public static final KeyMapping KEY=new KeyMapping("key.simplymirrorsplus.settings",GLFW.GLFW_KEY_M,"key.categories.misc");
    private static final int WIDTH=200,HEIGHT=20,ROW=24;

    public MirrorSettingsScreen() { super(new TranslatableComponent("simplymirrorsplus.settings.title")); }

    @Override protected void init() {
        int x=width/2-WIDTH/2,y=height/2-ROW*3;
        toggle(x,y,"simplymirrorsplus.settings.enabled",()->MirrorConfig.enabled,value->MirrorConfig.enabled=value);
        addButton(new StepSlider(x,y+ROW,"simplymirrorsplus.settings.resolution",resolutionSteps(),MirrorConfig.resolutionPercent,value->MirrorConfig.resolutionPercent=value));
        addButton(new StepSlider(x,y+ROW*2,"simplymirrorsplus.settings.refresh_rate",MirrorConfig.REFRESH_RATES,MirrorConfig.refreshRate,value->MirrorConfig.refreshRate=value));
        toggle(x,y+ROW*3,"simplymirrorsplus.settings.players_only",()->MirrorConfig.playersOnly,value->MirrorConfig.playersOnly=value);
        addButton(new Button(x,y+ROW*4+8,WIDTH,HEIGHT,CommonComponents.GUI_DONE,button->onClose()));
    }

    private void toggle(int x,int y,String label,Supplier<Boolean> getter,Consumer<Boolean> setter) {
        Component name=new TranslatableComponent(label);
        addButton(new Button(x,y,WIDTH,HEIGHT,CommonComponents.optionStatus(name,getter.get()),button->{
            setter.accept(!getter.get());
            button.setMessage(CommonComponents.optionStatus(name,getter.get()));
        }));
    }

    private static int[] resolutionSteps() {
        int[] steps=new int[(MirrorConfig.MAX_RESOLUTION-MirrorConfig.MIN_RESOLUTION)/5+1];
        for(int i=0;i<steps.length;i++)steps[i]=MirrorConfig.MIN_RESOLUTION+i*5;
        return steps;
    }

    @Override public void render(PoseStack pose,int mouseX,int mouseY,float partial) {
        super.render(pose,mouseX,mouseY,partial);
        drawCenteredString(pose,font,title,width/2,height/2-ROW*3-16,0xFFFFFF);
    }

    @Override public boolean isPauseScreen() { return false; }
    @Override public void removed() { MirrorConfig.save(); }

    private static final class StepSlider extends AbstractSliderButton {
        private final String label;
        private final int[] steps;
        private final IntConsumer onChange;

        StepSlider(int x,int y,String label,int[] steps,int current,IntConsumer onChange) {
            super(x,y,WIDTH,HEIGHT,TextComponent.EMPTY,position(steps,current));
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
            setMessage(new TranslatableComponent(label,steps[index()]));
        }

        @Override protected void applyValue() {
            int index=index();
            value=index/(steps.length-1d);
            onChange.accept(steps[index]);
        }
    }
}

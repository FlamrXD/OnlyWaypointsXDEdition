package me.onethecrazy.mixin.client;

import me.onethecrazy.OnlyWaypoints;
import me.onethecrazy.screen.WaypointMenuScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.StatsScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Supplier;

@Mixin(GameMenuScreen.class)
public abstract class PauseScreenMixin extends Screen{

    protected PauseScreenMixin(Text title) {
        super(title);
    }

    @Shadow @Nullable private ButtonWidget exitButton;

    @Unique
    private ButtonWidget addWaypointButton;

    @Inject(method="initWidgets", at=@At("TAIL"))
    private void onInit(CallbackInfo ci){

        // If the Exit Button is null, something is really wrong
        assert exitButton != null;
        int exitWidth = exitButton.getWidth();

        int x = exitButton.getX() + exitWidth + 4;
        int y = exitButton.getY();

        addWaypointButton = createCustomButton(Text.translatable("gui.onlywaypoints.open_menu"), () -> new WaypointMenuScreen(Text.empty()), x, y, 120);

        this.addDrawableChild(addWaypointButton);
    }

    private ButtonWidget createCustomButton(Text text, Supplier<Screen> supplier, int xPos, int yPos, int width){
        var btn = ButtonWidget.builder(text, button -> this.client.setScreen(supplier.get())).width(width).build();
        btn.setPosition(xPos, yPos);

        return btn;
    }
}

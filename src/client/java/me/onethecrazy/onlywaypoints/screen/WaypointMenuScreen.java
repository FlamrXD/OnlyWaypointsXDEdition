package me.onethecrazy.onlywaypoints.screen;

import me.onethecrazy.onlywaypoints.waypoints.WaypointManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.GridWidget;
import net.minecraft.client.gui.widget.ThreePartsLayoutWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class WaypointMenuScreen extends Screen {
    private ScrollListWidget listWidget;
    public final ThreePartsLayoutWidget layout = new ThreePartsLayoutWidget(this);

    public static void open(){
        MinecraftClient.getInstance().setScreen(
                new WaypointMenuScreen(Text.empty())
        );
    }

    public WaypointMenuScreen(Text title){
        super(title);
    }

    @Override
    protected void init() {
        listWidget = new ScrollListWidget(this.client, this);

        initHeader();
        initBody();
        initFooter();

        this.layout.forEachChild(this::addDrawableChild);
        this.layout.refreshPositions();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        boolean handled = super.keyPressed(keyCode, scanCode, modifiers);
        boolean isEntryFocused = listWidget.getFocused() != null;

        if(!handled && !isEntryFocused && keyCode == GLFW.GLFW_KEY_M){
            this.close();
            return true;
        }

        return handled;
    }

    private void initFooter(){
        var gridWidget = new GridWidget();
        var nestedGridWidget = new GridWidget();

        nestedGridWidget.getMainPositioner().margin(2)
                                            .marginLeft(0);

        nestedGridWidget.add(ButtonWidget.builder(Text.translatable("key.onlywaypoints.remove_waypoint"), button -> this.listWidget.removeFocusedEntry()).width(99).build(), 0 , 0);
        nestedGridWidget.add(ButtonWidget.builder(Text.translatable("key.onlywaypoints.add_waypoint"), button -> this.listWidget.addEntry()).width(99).build(), 0 , 1);

        gridWidget.add(nestedGridWidget, 0, 0);
        gridWidget.add(ButtonWidget.builder(ScreenTexts.DONE, button -> { WaypointManager.saveWaypoints(); this.close(); }).width(200).build(), 1, 0);

        this.layout.addFooter(gridWidget);
        this.layout.setFooterHeight(20 * 2 + 4 * 4);
        this.layout.refreshPositions();
    }

    private void initHeader(){
        // Add empty text
        // Maybe fix the bug that the positions are calculated wrongly when a text is present
        this.layout.addHeader(Text.of(""), client.textRenderer);
    }

    private void initBody(){
        this.layout.addBody(listWidget);
        this.layout.refreshPositions();
    }
}

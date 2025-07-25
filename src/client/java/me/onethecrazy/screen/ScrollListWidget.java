package me.onethecrazy.screen;

import me.onethecrazy.waypoints.WaypointManager;
import me.onethecrazy.waypoints.objects.Waypoint;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.widget.*;
import net.minecraft.text.Text;
import net.minecraft.util.math.ColorHelper;
import org.joml.Vector2i;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Objects;

public class ScrollListWidget extends ElementListWidget<ScrollListWidget.WaypointEntry> {
    private final WaypointMenuScreen screen;
    private static final int ROW_HEIGHT = 26;
    private float shouldFocusedBeNull = -1;
    private float deltaTicks;

    public ScrollListWidget(MinecraftClient client, WaypointMenuScreen screen) {
        super(client, client.currentScreen.width, screen.layout.getContentHeight(), screen.layout.getHeaderHeight(), ROW_HEIGHT);

        this.screen = screen;

        refreshEntries();
    }

    public int addEntry(WaypointEntry entry){
        return super.addEntry(entry);
    }

    public void addEntry(){
        Waypoint intermediate = new Waypoint(MinecraftClient.getInstance());

        WaypointManager.addWaypoint(intermediate);

        this.setFocused(null);

        refreshEntries();
    }

    public void removeFocusedEntry(){
        WaypointEntry focused = this.getFocused();

        if(focused == null) return;

        WaypointManager.removeWaypoint(focused.wp);

        this.setFocused(null);

        refreshEntries();
    }

    public void refreshEntries(){
        // clear all
        this.children().clear();

        // Populate with waypoints
        for (Waypoint wp : WaypointManager.waypoints) {
                addEntry(new ScrollListWidget.WaypointEntry(wp, this));
        }
    }

    @Override
    protected void renderList(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        this.deltaTicks = deltaTicks;

        updateFocusedEntry();
        drawSelectionBox(context);

        super.renderList(context, mouseX, mouseY, deltaTicks);
    }

    private void updateFocusedEntry(){
        if(shouldFocusedBeNull != -1 && this.deltaTicks != shouldFocusedBeNull){
            this.setFocused(null);
            shouldFocusedBeNull = -1;
        }
    }

    private void drawSelectionBox(DrawContext ctx){
        // Draw selected border
        WaypointEntry focused = this.getFocused();
        if (focused != null) {
            int borderColor = 0xFFFFFFFF;
            int fillColor = ColorHelper.withAlpha(0.4f, 0x000000);

            // Draw the vanilla‑style selection box
            Vector2i topRightCorner = focused.getTopLeftCorner();
            int x = topRightCorner.x;
            int y = topRightCorner.y;

            // Draw Fill
            ctx.fill(x - 2, y - 2, x + getRowWidth() - 2, y + ROW_HEIGHT - 2, fillColor);

            // Draw Border
            ctx.drawHorizontalLine(x - 2, x + getRowWidth() - 2, y - 2, borderColor);
            ctx.drawHorizontalLine(x - 2, x + getRowWidth() - 2, y + ROW_HEIGHT - 2, borderColor);
            ctx.drawVerticalLine(x - 2, y - 2, y + ROW_HEIGHT - 2, borderColor);
            ctx.drawVerticalLine(x + getRowWidth() - 2, y - 2, y + ROW_HEIGHT - 2, borderColor);
        }
    }

    @Override
    public int getRowWidth() {
        return 500;
    }

    public static class WaypointEntry extends ElementListWidget.Entry<WaypointEntry>{
        private final Waypoint wp;
        private final ScrollListWidget screenContainer;

        private final ButtonWidget deleteButton;
        private final ButtonWidget visibilityButton;
        private final TextFieldWidget nameWidget;
        private final TextFieldWidget xCoordWidget;
        private final TextFieldWidget yCoordWidget;
        private final TextFieldWidget zCoordWidget;

        private int x;
        private int y;

        private static final Text DO_RENDER_EMOJI_TEXT = Text.of("\uD83D\uDC41");
        private static final Text DONT_RENDER_EMOJI_TEXT = Text.of("§m\uD83D\uDC41");
        private static final int BUTTON_SIZE = 20;
        private static final int MARGIN = 4;
        private static final int NAME_TEXT_BOX_WIDTH = 200;
        private static final int COORDINATES_TEXT_BOX_WIDTH = 40;
        private static final String X_COORD_LABEL_TEXT = "X:";
        private static final String Y_COORD_LABEL_TEXT = "Y:";
        private static final String Z_COORD_LABEL_TEXT = "Z:";

        public WaypointEntry(Waypoint wp, ScrollListWidget parentScreen){
            this.wp = wp;
            this.screenContainer = parentScreen;

            var client = MinecraftClient.getInstance();

            // Build Widgets
            this.deleteButton = ButtonWidget.builder(Text.of("\uD83D\uDDD1"), btn -> {
                WaypointManager.removeWaypoint(wp);

                // Delay setting focus to null to next frame (deltaTick) (because otherwise it would be overwritten by button)
                screenContainer.shouldFocusedBeNull = screenContainer.deltaTicks + 1;

                screenContainer.refreshEntries();
            }).dimensions(0, 0, BUTTON_SIZE, BUTTON_SIZE).build();

            this.visibilityButton = ButtonWidget.builder(Text.of(wp.shouldRender ? DO_RENDER_EMOJI_TEXT : DONT_RENDER_EMOJI_TEXT), btn -> {
                wp.shouldRender = !wp.shouldRender;

                btn.setMessage(wp.shouldRender ? DO_RENDER_EMOJI_TEXT : DONT_RENDER_EMOJI_TEXT);
            }).dimensions(0, 0, BUTTON_SIZE, BUTTON_SIZE).build();

            this.nameWidget = new TextFieldWidget(client.textRenderer, NAME_TEXT_BOX_WIDTH, client.textRenderer.fontHeight + 4, Text.empty());
            this.nameWidget.setText(wp.name);
            this.nameWidget.setChangedListener((newName) -> wp.name = newName);

            this.xCoordWidget = new TextFieldWidget(client.textRenderer, COORDINATES_TEXT_BOX_WIDTH, client.textRenderer.fontHeight + 4, Text.empty());
            this.xCoordWidget.setText(String.valueOf(wp.coordinates.x));
            this.xCoordWidget.setChangedListener((newXCoord) -> {
                try{
                    wp.coordinates.x = Integer.parseInt(newXCoord);
                }
                // Invalid User Input (NaN)
                catch(Exception e){
                    if(!isValidCoordStringButNotANumber(newXCoord))
                        xCoordWidget.setText(String.valueOf(wp.coordinates.x));
                    else wp.coordinates.x = 0;
                }
            });

            this.yCoordWidget = new TextFieldWidget(client.textRenderer, COORDINATES_TEXT_BOX_WIDTH, client.textRenderer.fontHeight + 4, Text.empty());
            this.yCoordWidget.setText(String.valueOf(wp.coordinates.y));
            this.yCoordWidget.setChangedListener((newYCoord) -> {
                try{
                    wp.coordinates.y = Integer.parseInt(newYCoord);
                }
                // Invalid User Input (NaN)
                catch(Exception e){
                    if(!isValidCoordStringButNotANumber(newYCoord))
                        yCoordWidget.setText(String.valueOf(wp.coordinates.y));
                    else wp.coordinates.y = 0;
                }
            });

            this.zCoordWidget = new TextFieldWidget(client.textRenderer, COORDINATES_TEXT_BOX_WIDTH, client.textRenderer.fontHeight + 4, Text.empty());
            this.zCoordWidget.setText(String.valueOf(wp.coordinates.z));
            this.zCoordWidget.setChangedListener((newZCoord) -> {
                try{
                    wp.coordinates.z = Integer.parseInt(newZCoord);
                }
                // Invalid User Input (NaN)
                catch(Exception e){
                    if(!isValidCoordStringButNotANumber(newZCoord))
                        zCoordWidget.setText(String.valueOf(wp.coordinates.z));
                    else wp.coordinates.z = 0;
                }
            });
        }

        @Override
        public List<? extends Element> children() { return List.of(this.deleteButton, this.visibilityButton, this.nameWidget, this.xCoordWidget, this.yCoordWidget, this.zCoordWidget); }

        @Override
        public List<? extends Selectable> selectableChildren() { return List.of(this.deleteButton, this.visibilityButton, this.nameWidget, this.xCoordWidget, this.yCoordWidget, this.zCoordWidget); }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            screenContainer.setFocused(this);

            return super.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public void render(DrawContext ctx, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            var client = MinecraftClient.getInstance();
            var tr = client.textRenderer;

            // Update positions
            this.x = x;
            this.y = y;

            // Position Widgets
            int centeredTextY = y + (entryHeight - client.textRenderer.fontHeight) / 2;
            int centeredTextYWithMargin = y + (entryHeight - client.textRenderer.fontHeight - 4) / 2;
            int centeredButtonY = y + (entryHeight - BUTTON_SIZE) / 2;
            int visibilityX = x + MARGIN;
            int deleteX = visibilityX + MARGIN + BUTTON_SIZE;
            int nameX = deleteX + MARGIN + BUTTON_SIZE;
            int xLabelX = nameX + MARGIN * 3 + NAME_TEXT_BOX_WIDTH;
            int xCoordX = xLabelX + MARGIN + tr.getWidth(X_COORD_LABEL_TEXT);
            int yLabelX = xCoordX + MARGIN + COORDINATES_TEXT_BOX_WIDTH;
            int yCoordX = yLabelX + MARGIN + tr.getWidth(Y_COORD_LABEL_TEXT);
            int zLabelX = yCoordX + MARGIN + COORDINATES_TEXT_BOX_WIDTH;
            int zCoordX = zLabelX + MARGIN + tr.getWidth(Z_COORD_LABEL_TEXT);
            int uuidX = zCoordX + MARGIN + COORDINATES_TEXT_BOX_WIDTH;

            deleteButton.setPosition(deleteX, centeredButtonY);
            visibilityButton.setPosition(visibilityX, centeredButtonY);
            nameWidget.setPosition(nameX, centeredTextYWithMargin);
            xCoordWidget.setPosition(xCoordX, centeredTextYWithMargin);
            yCoordWidget.setPosition(yCoordX, centeredTextYWithMargin);
            zCoordWidget.setPosition(zCoordX, centeredTextYWithMargin);

            deleteButton.render(ctx, mouseX, mouseY, tickDelta);
            visibilityButton.render(ctx, mouseX, mouseY, tickDelta);
            nameWidget.render(ctx, mouseX, mouseY, tickDelta);
            ctx.drawText(tr, Text.of(X_COORD_LABEL_TEXT), xLabelX, centeredTextY, 0xFFFFFFFF,false);
            xCoordWidget.render(ctx, mouseX, mouseY, tickDelta);
            ctx.drawText(tr, Text.of(Y_COORD_LABEL_TEXT), yLabelX, centeredTextY, 0xFFFFFFFF,false);
            yCoordWidget.render(ctx, mouseX, mouseY, tickDelta);
            ctx.drawText(tr, Text.of(Z_COORD_LABEL_TEXT), zLabelX, centeredTextY, 0xFFFFFFFF,false);
            zCoordWidget.render(ctx, mouseX, mouseY, tickDelta);
            ctx.drawText(tr, Text.of(wp.uuid), uuidX, centeredTextY, 0x40C0C0FF,false);
        }

        public Vector2i getTopLeftCorner(){
            return new Vector2i(this.x, this.y);
        }

        private boolean isValidCoordStringButNotANumber(String input){
            return Objects.equals(input, "") || Objects.equals(input, "-");
        }
    }
}

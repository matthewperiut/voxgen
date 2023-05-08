package com.matthewperiut.voxgen;

import com.mojang.brigadier.context.CommandContext;
import me.x150.MessageSubscription;
import me.x150.renderer.event.Events;
import me.x150.renderer.event.RenderEvent;
import me.x150.renderer.render.Renderer3d;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static java.lang.Math.ceil;
import static java.lang.Math.sqrt;
import static net.minecraft.util.math.MathHelper.floor;

@Environment(EnvType.CLIENT)

public class VoxGenClient implements ClientModInitializer {

    boolean lines = false;
    BlockPos p1 = new BlockPos(Vec3i.ZERO);
    BlockPos p2 = new BlockPos(Vec3i.ZERO);

    public static String generateTimestampedName() {
        // Get the current time in the local time zone
        LocalDateTime now = LocalDateTime.now();

        // Format the time as YYYY-MM-DD_HH.MM.SS
        String formattedTime = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH.mm.ss"));

        // Construct the screenshot file name as YYYY-MM-DD_HH.MM.SS-username.png
        String screenshotName = formattedTime + ".png";

        return screenshotName;
    }

    public static void renderWireframeCube(MatrixStack matrixStack, Color color, Vec3d min, Vec3d max) {
        Vec3d distance = new Vec3d(max.getX() - min.getX(), max.getY() - min.getY(), max.getZ() - min.getZ());
        Renderer3d.renderOutline(matrixStack, color, min, distance);
    }

    @MessageSubscription
    void onWorldRendered(RenderEvent.World world) {
        if (lines)
        {
            renderWireframeCube(world.getMatrixStack(), Color.red, min, max.add(1,1,1));
        }
    }

    Vec3d min = Vec3d.ZERO;
    Vec3d max = Vec3d.ZERO;
    private void CalcMinMax()
    {
        min = new Vec3d(Math.min(p1.getX(), p2.getX()), Math.min(p1.getY(), p2.getY()), Math.min(p1.getZ(), p2.getZ()));
        max = new Vec3d(Math.max(p1.getX(), p2.getX()), Math.max(p1.getY(), p2.getY()), Math.max(p1.getZ(), p2.getZ()));
    }

    @Override
    public void onInitializeClient() {

        Events.manager.registerSubscribers(this);

        //CommandDispatcher<FabricClientCommandSource> dispatcher = ClientCommandManager.getActiveDispatcher();
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(
                    ClientCommandManager.literal("lines").executes(context -> {
                        lines = !lines;
                        context.getSource().sendFeedback(Text.literal("Lines " + (lines ? "enabled" : "disabled")));
                        return 1;
                    })
            );
        });
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(
                    ClientCommandManager.literal("p1").executes(context -> {
                        p1 = context.getSource().getPlayer().getBlockPos();
                        context.getSource().sendFeedback(Text.literal("First pos is " + p1));
                        CalcMinMax();
                        return 1;
                    })
            );
        });
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(
                    ClientCommandManager.literal("p2").executes(context -> {
                        p2 = context.getSource().getPlayer().getBlockPos();
                        context.getSource().sendFeedback(Text.literal("Second pos is " + p2));
                        CalcMinMax();
                        return 1;
                    })
            );
        });
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(
                    ClientCommandManager.literal("vox").executes(context -> executeVoxCommand(context, min, max))
            );
        });
    }

    private static int executeVoxCommand(CommandContext<FabricClientCommandSource> context, Vec3d min, Vec3d max) {
        new Thread(() -> {
            String client_directory = MinecraftClient.getInstance().runDirectory.toString();

            {
                File vox = new File("vox");
                vox.mkdir();
            }

            File file = new File("vox/" + generateTimestampedName());

            ClientWorld world = context.getSource().getWorld();

            int xSize = (int) (max.getX() - min.getX() + 1);
            int ySize = (int) (max.getY() - min.getY() + 1);
            int zSize = (int) (max.getZ() - min.getZ() + 1);

            int horizontal_layers = floor(sqrt(ySize));
            int vertical_layers = (int) (ceil(sqrt(ySize)) + 1);

            int horizontal_pixels = xSize * horizontal_layers;
            int vertical_pixels = zSize * vertical_layers;

            BufferedImage image = new BufferedImage(horizontal_pixels, vertical_pixels, BufferedImage.TYPE_INT_ARGB);

            for (int y = 0; y < ySize; y++)
            {
                int horizontal_layer_offset = y % horizontal_layers;
                int hpo = horizontal_layer_offset * xSize;
                int vertical_layer_offset = floor((double)y / horizontal_layers);
                int vpo = vertical_layer_offset * zSize;

                for (int x = 0; x < xSize; x++) {
                    for (int z = 0; z < zSize; z++) {
                        //context.getSource().sendFeedback(Text.of("x y z " + x + " " + y + " " + z));

                        BlockPos pos = new BlockPos((int) (min.getX() + x), (int) (min.getY() + y), (int) (min.getZ() + z));

                        try {
                            BlockState state = world.getBlockState(pos);
                            int color = TextureUtils.getPixelColor(state);
                            image.setRGB(hpo + x, vpo + z, TextureUtils.rgbToRgba(color));
                        }
                        catch (Exception e)
                        {
                            System.out.println(e.getMessage());
                        }

                    }
                }
            }

            try {
                ImageIO.write(image, "png", file);

                Text text = Text.literal(file.getName()).formatted(Formatting.UNDERLINE).styled((style) -> {
                    return style.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, file.getAbsolutePath()));
                });

                context.getSource().sendFeedback(Text.translatable("voxgen.success", text));

                FileHandler.writeSizeMetadata(file.getAbsolutePath(),xSize,ySize,zSize);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).start();

        return 0;
    }
}
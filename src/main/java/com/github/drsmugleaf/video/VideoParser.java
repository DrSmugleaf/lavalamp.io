package com.github.drsmugleaf.video;

import org.bytedeco.javacv.*;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * Created by DrSmugleaf on 08/03/2020
 */
public class VideoParser {

    private final File FILE;
    private final FFmpegFrameGrabber GRABBER;
    private final Java2DFrameConverter CONVERTER;
    private final FFmpegFrameFilter FILTER;

    public VideoParser(File file, int height, int width) {
        FILE = file;
        GRABBER = new FFmpegFrameGrabber(FILE);
        CONVERTER = new Java2DFrameConverter();
        String scale = String.format("scale=%d:%d", width, height);
        FILTER = new FFmpegFrameFilter(scale, width, height);
        start();
    }

    public VideoParser(String file, int height, int width) {
        this(new File(file), height, width);
    }

    public File getFile() {
        return FILE;
    }

    public FFmpegFrameGrabber getGrabber() {
        return GRABBER;
    }

    public Java2DFrameConverter getConverter() {
        return CONVERTER;
    }

    public FFmpegFrameFilter getFilter() {
        return FILTER;
    }

    protected void start() {
        try {
            getGrabber().start();
            getFilter().start();
        } catch (FrameGrabber.Exception e) {
            throw new IllegalStateException("Error starting grabber", e);
        } catch (FrameFilter.Exception e) {
            throw new IllegalStateException("Error starting filter", e);
        }
    }

    public Stream<Frame> getFrames() {
        return Stream.generate(() -> {
            try {
                return getGrabber().grabFrame(false, true, true, false);
            } catch (FrameGrabber.Exception e) {
                throw new IllegalStateException("Error grabbing frame");
            }
        }).takeWhile(Objects::nonNull).map(frame -> {
            try {
                getFilter().push(frame);
                return getFilter().pull();
            } catch (FrameFilter.Exception e) {
                throw new IllegalStateException("Error resizing frame",  e);
            }
        });
    }

    public Stream<BufferedImage> getImages() {
        return getFrames().map(frame -> getConverter().convert(frame));
    }

    public Stream<int[][][]> getRGB() {
        return getImages()
                .map(image -> {
                    int width = image.getWidth();
                    int height = image.getHeight();
                    int[][][] pixels = new int[width][height][3];

                    for (int x = 0; x < width; x++) {
                        for (int y = 0; y < height; y++) {
                            int rgb = image.getRGB(x, y);
                            int red = (rgb & 0xff0000) >> 16;
                            int green = (rgb & 0xff00) >> 8;
                            int blue = rgb & 0xff;
                            pixels[x][y] = new int[]{red, green, blue};
                        }
                    }

                    return pixels;
                });
    }

    public void show(long maxAmount) {
        CanvasFrame canvas = new CanvasFrame("Lava Lamp", CanvasFrame.getDefaultGamma() / getGrabber().getGamma());
        getFrames().limit(maxAmount).forEachOrdered(frame -> {
            canvas.showImage(frame);

            try {
                TimeUnit.MICROSECONDS.sleep(33333);
            } catch (InterruptedException e) {
                throw new IllegalStateException("Error sleeping thread", e);
            }
        });
    }

    public int[][][][][] segment(int length) {
        int[][][][] frames = getRGB().toArray(int[][][][]::new);
        int[][][][][] segments = new int[frames.length][length][][][];

        for (int i = 0; i < frames.length - 1; i++) {
            int[][][] frame = frames[i];
            int j = i;
            for (int k = 0; i - j < length && j >= 0; k++) {
                segments[j][k] = frame;
                j--;
            }
        }

        return segments;
    }

}

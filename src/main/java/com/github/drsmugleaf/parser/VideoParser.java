package com.github.drsmugleaf.parser;

import org.bytedeco.ffmpeg.global.avutil;
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

    private final File PATH;
    private final FFmpegFrameGrabber GRABBER;
    private final Java2DFrameConverter CONVERTER;
    private final FFmpegFrameFilter FILTER;

    public VideoParser(File path, int width, int height) {
        PATH = path;
        GRABBER = new FFmpegFrameGrabber(PATH);
        CONVERTER = new Java2DFrameConverter();
        String scale = String.format("scale=%d:%d", width, height);
        FILTER = new FFmpegFrameFilter(scale, width, height);
        start();
    }

    public VideoParser(String path, int width, int height) {
        this(new File(path), width, height);
    }

    public File getPath() {
        return PATH;
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

    public Stream<int[]> getRGB() {
        return getImages().map(image -> {
            int width = image.getWidth();
            int height = image.getHeight();
            return image.getRGB(0, 0, width, height, null, 0, width);
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

}

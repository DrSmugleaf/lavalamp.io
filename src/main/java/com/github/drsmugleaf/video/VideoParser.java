package com.github.drsmugleaf.video;

import org.bytedeco.javacv.*;
import org.datavec.image.loader.NativeImageLoader;
import org.nd4j.linalg.api.ndarray.INDArray;

import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * Created by DrSmugleaf on 08/03/2020
 */
public class VideoParser {

    private final File FILE;
    private final int HEIGHT;
    private final int WIDTH;
    private final NativeImageLoader LOADER;

    public VideoParser(File file, int height, int width) {
        FILE = file;
        HEIGHT = height;
        WIDTH = width;
        LOADER = new NativeImageLoader(height, width, 3);
    }

    public VideoParser(String file, int height, int width) {
        this(new File(file), height, width);
    }

    public File getFile() {
        return FILE;
    }

    public int getHeight() {
        return HEIGHT;
    }

    public int getWidth() {
        return WIDTH;
    }

    protected FFmpegFrameGrabber newGrabber() {
        return new FFmpegFrameGrabber(FILE);
    }

    public FFmpegFrameFilter newFilter() {
        String scale = String.format("scale=%d:%d", getWidth(), getHeight());
        return new FFmpegFrameFilter(scale, getWidth(), getHeight());
    }

    public NativeImageLoader getLoader() {
        return LOADER;
    }

    public Stream<Frame> getFrames() {
        FFmpegFrameGrabber grabber = newGrabber();
        FFmpegFrameFilter filter = newFilter();

        try {
            grabber.start();
            filter.start();
        } catch (FrameGrabber.Exception e) {
            throw new IllegalStateException("Error starting frame grabber", e);
        } catch (FrameFilter.Exception e) {
            throw new IllegalStateException("Error starting filter grabber", e);
        }

        return Stream.generate(() -> {
            try {
                return grabber.grabFrame(false, true, true, false);
            } catch (FrameGrabber.Exception e) {
                throw new IllegalStateException("Error grabbing frame", e);
            }
        }).onClose(() -> {
            try {
                grabber.close();
                filter.close();
            } catch (FrameGrabber.Exception e) {
                throw new IllegalStateException("Error closing frame grabber", e);
            } catch (FrameFilter.Exception e) {
                throw new IllegalStateException("Error closing filter grabber", e);
            }
        }).takeWhile(Objects::nonNull).map(frame -> {
            try {
                filter.push(frame);
                return filter.pull();
            } catch (FrameFilter.Exception e) {
                throw new IllegalStateException("Error resizing frame",  e);
            }
        });
    }

    public Stream<INDArray> getMatrices() {
        return getFrames().map(frame -> {
            try {
                return getLoader().asMatrix(frame);
            } catch (IOException e) {
                throw new IllegalStateException("Error converting frame to matrix", e);
            }
        });
    }

    public void show() {
        double gamma;
        try (FFmpegFrameGrabber grabber = newGrabber()) {
            gamma = CanvasFrame.getDefaultGamma() / grabber.getGamma();
        } catch (FrameGrabber.Exception e) {
            throw new IllegalStateException("Error closing frame grabber", e);
        }

        CanvasFrame canvas = new CanvasFrame("Lava Lamp", gamma);

        try(Stream<Frame> frames = getFrames()) {
            frames.forEachOrdered(frame -> {
                canvas.showImage(frame);

                try {
                    TimeUnit.MICROSECONDS.sleep(33333);
                } catch (InterruptedException e) {
                    throw new IllegalStateException("Error sleeping thread", e);
                }
            });
        }

        canvas.dispatchEvent(new WindowEvent(canvas, WindowEvent.WINDOW_CLOSING));
    }

}

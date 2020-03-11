package com.github.drsmugleaf.video;

import org.nd4j.shade.guava.collect.ImmutableList;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by DrSmugleaf on 11/03/2020
 */
public class VideoDirectory {

    private final ImmutableList<VideoParser> PARSERS;

    public VideoDirectory(File directory, int height, int width) {
        List<VideoParser> parsers = new ArrayList<>();
        if (!directory.isDirectory()) {
            throw new IllegalArgumentException("Path isn't a directory: " + directory);
        }

        File[] files = directory.listFiles();
        if (files == null) {
            throw new IllegalArgumentException("Error reading files from directory " + directory);
        }

        for (File file : files) {
            VideoParser parser = new VideoParser(file, height, width);
            parsers.add(parser);
        }

        PARSERS = ImmutableList.copyOf(parsers);
    }

    public VideoDirectory(String directory, int height, int width) {
        this(new File(directory), height, width);
    }

    public ImmutableList<VideoParser> getParsers() {
        return PARSERS;
    }

}

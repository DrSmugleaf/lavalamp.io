package com.github.drsmugleaf;

import com.github.drsmugleaf.parser.VideoParser;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.ComputationGraphConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.graph.rnn.LastTimeStepVertex;
import org.deeplearning4j.nn.conf.layers.LSTM;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.nd4j.linalg.activations.Activation;

import java.util.Objects;

/**
 * Created by DrSmugleaf on 08/03/2020
 */
public class Lavalamp {

    private static final String VIDEO = Objects.requireNonNull(Lavalamp.class.getClassLoader().getResource("video/1.webm")).getFile();
    private static final int NB_INPUTS = 86;

    public static void main(String[] args) {
        ComputationGraphConfiguration conf = new NeuralNetConfiguration.Builder()
                .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                .l2(0.01)
                .graphBuilder()
                .addInputs("in")
                .addLayer("lstm", new LSTM.Builder().nIn(NB_INPUTS).nOut(30).build(), "in")
                .addVertex("lastStep", new LastTimeStepVertex("in"), "lstm")
                .addLayer("out", new OutputLayer.Builder().activation(Activation.SOFTMAX).nIn(30).nOut(2).build(), "lastStep")
                .setOutputs("out")
                .build();

        ComputationGraph model = new ComputationGraph(conf);
        model.init();

        VideoParser parser = new VideoParser(VIDEO, 100, 100);
        parser.show(300);
    }

}

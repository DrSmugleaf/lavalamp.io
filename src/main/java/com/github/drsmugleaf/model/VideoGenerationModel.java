package com.github.drsmugleaf.model;

import org.deeplearning4j.nn.conf.BackpropType;
import org.deeplearning4j.nn.conf.GradientNormalization;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.*;
import org.deeplearning4j.nn.conf.preprocessor.CnnToFeedForwardPreProcessor;
import org.deeplearning4j.nn.conf.preprocessor.CnnToRnnPreProcessor;
import org.deeplearning4j.nn.conf.preprocessor.FeedForwardToRnnPreProcessor;
import org.deeplearning4j.nn.conf.preprocessor.RnnToCnnPreProcessor;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.learning.config.AdaGrad;
import org.nd4j.linalg.lossfunctions.LossFunctions;

/**
 * Created by DrSmugleaf on 11/03/2020
 */
public class VideoGenerationModel {

    private final MultiLayerNetwork NETWORK;

    public VideoGenerationModel(int seed, int height, int width) {
        MultiLayerConfiguration configuration = new NeuralNetConfiguration.Builder()
                .seed(seed)
                .l2(0.001)
                .updater(new AdaGrad(0.04))
                .list()
                .layer(new ConvolutionLayer
                        .Builder(10, 10)
                        .nIn(3)
                        .nOut(30)
                        .stride(4, 4)
                        .activation(Activation.RELU)
                        .weightInit(WeightInit.RELU)
                        .build()
                )
                .layer(new SubsamplingLayer
                        .Builder(SubsamplingLayer.PoolingType.MAX)
                        .kernelSize(3, 3)
                        .stride(2, 2)
                        .build()
                )
                .layer(new ConvolutionLayer
                        .Builder(3, 3)
                        .nIn(30)
                        .nOut(10)
                        .stride(2, 2)
                        .activation(Activation.RELU)
                        .weightInit(WeightInit.RELU)
                        .build()
                )
                .layer(new DenseLayer
                        .Builder()
                        .activation(Activation.RELU)
                        .nIn(490)
                        .nOut(50)
                        .weightInit(WeightInit.RELU)
                        .gradientNormalization(GradientNormalization.ClipElementWiseAbsoluteValue)
                        .gradientNormalizationThreshold(10)
                        .updater(new AdaGrad(0.01))
                        .build()
                )
                .layer(new LSTM
                        .Builder()
                        .activation(Activation.TANH)
                        .nIn(50)
                        .nOut(50)
                        .weightInit(WeightInit.XAVIER)
                        .updater(new AdaGrad(0.008))
                        .gradientNormalization(GradientNormalization.ClipElementWiseAbsoluteValue)
                        .gradientNormalizationThreshold(10)
                        .build()
                )
                .layer(new RnnOutputLayer
                        .Builder(LossFunctions.LossFunction.MCXENT)
                        .activation(Activation.SOFTMAX)
                        .nIn(50)
                        .nOut(50)
                        .weightInit(WeightInit.XAVIER)
                        .gradientNormalization(GradientNormalization.ClipElementWiseAbsoluteValue)
                        .gradientNormalizationThreshold(10)
                        .build()
                )
                .layer(new ConvolutionLayer
                        .Builder(2, 2)
                        .nIn(50)
                        .nOut(30)
                        .stride(3, 3)
                        .activation(Activation.RELU)
                        .weightInit(WeightInit.RELU)
                        .build()
                )
                .layer(new SubsamplingLayer
                        .Builder(SubsamplingLayer.PoolingType.MAX)
                        .kernelSize(2, 2)
                        .stride(3, 3)
                        .build()
                )
                .layer(new ConvolutionLayer
                        .Builder(4, 4)
                        .nIn(30)
                        .nOut(3)
                        .stride(10, 10)
                        .activation(Activation.RELU)
                        .weightInit(WeightInit.RELU)
                        .build()
                )
                .inputPreProcessor(0, new RnnToCnnPreProcessor(height, width, 3))
                .inputPreProcessor(3, new CnnToFeedForwardPreProcessor(7, 7, 10))
                .inputPreProcessor(4, new FeedForwardToRnnPreProcessor())
                .inputPreProcessor(5, new RnnToCnnPreProcessor(7, 7, 10))
                .inputPreProcessor(8, new CnnToRnnPreProcessor(height, width, 3))
                .backpropType(BackpropType.TruncatedBPTT)
                .tBPTTForwardLength(50)
                .tBPTTBackwardLength(50)
                .build();

        NETWORK = new MultiLayerNetwork(configuration);
    }

    public MultiLayerNetwork getNetwork() {
        return NETWORK;
    }

}

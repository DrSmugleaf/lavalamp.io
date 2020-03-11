package com.github.drsmugleaf.model;

import com.github.drsmugleaf.parser.VideoParser;
import org.deeplearning4j.nn.conf.BackpropType;
import org.deeplearning4j.nn.conf.GradientNormalization;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.*;
import org.deeplearning4j.nn.conf.preprocessor.CnnToFeedForwardPreProcessor;
import org.deeplearning4j.nn.conf.preprocessor.FeedForwardToRnnPreProcessor;
import org.deeplearning4j.nn.conf.preprocessor.RnnToCnnPreProcessor;
import org.deeplearning4j.nn.weights.WeightInit;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.learning.config.AdaGrad;
import org.nd4j.linalg.lossfunctions.LossFunctions;

/**
 * Created by DrSmugleaf on 11/03/2020
 */
public class ClassificationModel {

    private final VideoParser PARSER;
    private final MultiLayerConfiguration CONFIGURATION;

    public ClassificationModel(VideoParser parser, int frames) {
        PARSER = parser;
        CONFIGURATION = new NeuralNetConfiguration.Builder()
                .seed(42)
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
                        .nOut(4)
                        .weightInit(WeightInit.XAVIER)
                        .gradientNormalization(GradientNormalization.ClipElementWiseAbsoluteValue)
                        .gradientNormalizationThreshold(10)
                        .build()
                )
                .inputPreProcessor(0, new RnnToCnnPreProcessor(PARSER.getGrabber().getImageHeight(), PARSER.getGrabber().getImageWidth(), 3))
                .inputPreProcessor(3, new CnnToFeedForwardPreProcessor(7, 7, 10))
                .inputPreProcessor(4, new FeedForwardToRnnPreProcessor())
                .backpropType(BackpropType.TruncatedBPTT)
                .tBPTTForwardLength(frames / 5)
                .tBPTTBackwardLength(frames / 5)
                .build();
    }

    public VideoParser getParser() {
        return PARSER;
    }

    public MultiLayerConfiguration getConfiguration() {
        return CONFIGURATION;
    }

}

/*
 * File:                OnlinePerceptron.java
 * Authors:             Justin Basilico
 * Company:             Sandia National Laboratories
 * Project:             Cognitive Foundry Learning Core
 * 
 * Copyright October 19, 2010, Sandia Corporation.
 * Under the terms of Contract DE-AC04-94AL85000, there is a non-exclusive 
 * license for use of this work by or on behalf of the U.S. Government. Export 
 * of this program may require a license from the United States Government. 
 */

package gov.sandia.cognition.learning.algorithm.perceptron;

import gov.sandia.cognition.annotation.PublicationReference;
import gov.sandia.cognition.annotation.PublicationType;
import gov.sandia.cognition.learning.algorithm.AbstractBatchAndOnlineLearner;
import gov.sandia.cognition.learning.data.InputOutputPair;
import gov.sandia.cognition.learning.function.categorization.LinearBinaryCategorizer;
import gov.sandia.cognition.math.matrix.Vector;
import gov.sandia.cognition.math.matrix.VectorFactory;
import gov.sandia.cognition.math.matrix.Vectorizable;

/**
 * An online version of the classic Perceptron algorithm.
 * 
 * @author  Justin Basilico
 * @since   3.1
 */
@PublicationReference(
    author="Wikipedia",
    title="Perceptron Learning algorithm",
    type=PublicationType.WebPage,
    year=2008,
    url="http://en.wikipedia.org/wiki/Perceptron#Learning_algorithm"
)
public class OnlinePerceptron
    extends AbstractBatchAndOnlineLearner<InputOutputPair<? extends Vectorizable, Boolean>, LinearBinaryCategorizer>
{

    /** The factory to create weight vectors. */
    protected VectorFactory<?> vectorFactory;

    /**
     * Creates a new {@code OnlinePerceptron}.
     */
    public OnlinePerceptron()
    {
        this(VectorFactory.getDenseDefault());
    }

    /**
     * Creates a new {@code OnlinePerceptron} with the given vector factory.
     *
     * @param   vectorFactory
     *      The vector factory to use to create the weight vectors.
     */
    public OnlinePerceptron(
        final VectorFactory<?> vectorFactory)
    {
        super();

        this.setVectorFactory(vectorFactory);
    }

    @Override
    public LinearBinaryCategorizer createInitialLearnedObject()
    {
        // Start with a null weight vector and zero bias.
        return new LinearBinaryCategorizer();
    }

    @Override
    public void update(
        final LinearBinaryCategorizer target,
        final InputOutputPair<? extends Vectorizable, Boolean> example)
    {
        // Get the information about the example.
        final Vector input = example.getInput().convertToVector();
        final boolean actual = example.getOutput();

        Vector weights = target.getWeights();
        if (weights == null)
        {
            // This is the first example, so initialize the weight vector.
            weights = this.getVectorFactory().createVector(
                input.getDimensionality());
            target.setWeights(weights);
        }
        // else - Use the existing weights.

        // Predict the output as a double (negative values are false, positive
        // are true).
        final double prediction = target.evaluateAsDouble(input);

        // Make an update if there was an error.
        if (actual && prediction <= 0.0)
        {
            // An error with the true (positive) category.
            weights.plusEquals(input);
            target.setBias(target.getBias() + 1.0);
        }
        else if (!actual && prediction >= 0.0)
        {
            // An error with the false (negative) category.
            weights.minusEquals(input);
            target.setBias(target.getBias() - 1.0);
        }
        // else - There was no error made, so nothing to update.
    }

    /**
     * Gets the VectorFactory used to create the weight vector.
     *
     * @return The VectorFactory used to create the weight vector.
     */
    public VectorFactory<?> getVectorFactory()
    {
        return this.vectorFactory;
    }

    /**
     * Sets the VectorFactory used to create the weight vector.
     *
     * @param  vectorFactory The VectorFactory used to create the weight vector.
     */
    public void setVectorFactory(
        final VectorFactory<?> vectorFactory)
    {
        this.vectorFactory = vectorFactory;
    }

}

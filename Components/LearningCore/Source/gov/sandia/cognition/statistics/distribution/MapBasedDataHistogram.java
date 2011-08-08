/*
 * File:                MapBasedDataHistogram.java
 * Authors:             Kevin R. Dixon
 * Company:             Sandia National Laboratories
 * Project:             Cognitive Foundry
 * 
 * Copyright Jan 23, 2009, Sandia Corporation.
 * Under the terms of Contract DE-AC04-94AL85000, there is a non-exclusive
 * license for use of this work by or on behalf of the U.S. Government. 
 * Export of this program may require a license from the United States
 * Government. See CopyrightHistory.txt for complete details.
 * 
 */

package gov.sandia.cognition.statistics.distribution;

import gov.sandia.cognition.factory.Factory;
import gov.sandia.cognition.learning.algorithm.OnlineLearner;
import gov.sandia.cognition.math.MathUtil;
import gov.sandia.cognition.statistics.AbstractDataHistogram;
import gov.sandia.cognition.statistics.DataHistogram;
import gov.sandia.cognition.statistics.DistributionEstimator;
import gov.sandia.cognition.util.AbstractCloneableSerializable;
import gov.sandia.cognition.util.ArgumentChecker;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * The {@code MapBasedDataHistogramTest} class implements a
 * {@code DataDistribution} with an underlying data structure of a 
 * {@code LinkedHashMap} that maps the values to the integer counts.
 *
 * @param  <DataType> Value for the domain (x-axis, independent variable), may 
 *      be something like an Integer, etc.
 * @author Justin Basilico
 * @since  2.0
 */
public class MapBasedDataHistogram<DataType>
    extends AbstractDataHistogram<DataType>
{

    // Note: This class does not use of setters/getters internally for
    // performance reasons.
    
    /**
     * Default initial capacity of the Map, {@value}.
     */
    public static final int DEFAULT_INITIAL_CAPACITY = 16;
    
    /** The total number of values. */
    protected int totalCount;

    /** The map of values to counts. */
    protected Map<DataType, Entry> countMap;

    /**
     * Creates a new instance of DataCountMapHistogram.
     */
    public MapBasedDataHistogram()
    {
        this(DEFAULT_INITIAL_CAPACITY);
    }

    /**
     * Creates a map-based data histogram with the given expected domain size.
     *
     * @param   initialDomainCapacity
     *      The expected domain size. Must be positive.
     */
    public MapBasedDataHistogram(
        final int initialDomainCapacity)
    {
        this(new LinkedHashMap<DataType, Entry>(initialDomainCapacity));
    }

    /**
     * Creates a new instance of DataCountMapHistogram from the given data.
     * 
     * @param data
     *      Data to add
     */
    public MapBasedDataHistogram(
        Collection<DataType> data)
    {
        this((data != null) ? data.size() : DEFAULT_INITIAL_CAPACITY);
        if (data != null)
        {
            for (DataType x : data)
            {
                this.add(x);
            }
        }
    }

    /**
     * Copy constructor.
     *
     * @param   other
     *      MapBasedDataHistogram to copy
     */
    public MapBasedDataHistogram(
        DataHistogram<DataType> other)
    {
        this(other.getDomain().size());
        for (DataType input : other.getDomain())
        {
            this.add(input, other.getCount(input));
        }

    }

    /**
     * Creates a new instance of MapBasedDataHistogram.
     *
     * @param countMap
     *      The map to backing the counts.
     */
    protected MapBasedDataHistogram(
        Map<DataType, Entry> countMap)
    {
        super();
        this.setCountMap(countMap);
    }

    @Override
    public MapBasedDataHistogram<DataType> clone()
    {
        @SuppressWarnings("unchecked")
        MapBasedDataHistogram<DataType> clone =
            (MapBasedDataHistogram<DataType>) super.clone();

        // Make a new backing map. Initialize it to an approprite size.
        clone.countMap = new LinkedHashMap<DataType, Entry>(
            this.countMap.size());

        // The totalCount is copied directly.
        
        // Copy all the values into the map.
        for (DataType value : this.getDomain())
        {
            final int count = this.getCount(value);
            clone.countMap.put(value, new Entry(count));
        }

        return clone;
    }

    public void add(
        final DataType value,
        final int number)
    {
        if (number < 0)
        {
            throw new IllegalArgumentException("number cannot be negative");
        }
        else if (number == 0)
        {
            // Adding zeros does nothing.
            return;
        }

        // Get hte existing entry.
        Entry entry = this.countMap.get(value);
        if (entry == null)
        {
            // Entry doesn't exist yet. Add it.
            entry = new Entry(number);
            this.countMap.put(value, entry);
        }
        else
        {
            // Increment the count.
            entry.count += number;
        }

        // Put the count back into the map and increment the total count.
        this.totalCount += number;
    }

    public void remove(
        final DataType value,
        final int number)
    {
        if (number < 0)
        {
            throw new IllegalArgumentException("number cannot be negative");
        }
        else if (number == 0)
        {
            // No need to remove zeros.
            return;
        }

        // Get the current count.
        final Entry entry = this.countMap.get(value);
        if (entry != null)
        {
            final int oldCount = entry.count;
            final int newCount = oldCount - number;

            if (newCount <= 0)
            {
                // There was a request to remove more than was there so only
                // remove what was there from the total.
                this.countMap.remove(value);
                this.totalCount -= oldCount;
            }
            else
            {
                // Remove only the amount that was requested.
                entry.count = newCount;
                this.totalCount -= number;
            }
        }
        // else - the count doesn't exist in the Map
    }

    public Set<DataType> getDomain()
    {
        return this.countMap.keySet();
    }

    public int getDomainSize()
    {
        return this.countMap.size();
    }

    public int getCount(
        final DataType input)
    {
        // See if there is a count in the count map.
        final Entry entry = this.countMap.get(input);

        if (entry == null)
        {
            // No count, so the count is zero.
            return 0;
        }
        else
        {
            // Return the count.
            return entry.count;
        }
    }

    public int getMaximumCount()
    {
        // Go through all the counts to find the maximum.
        int max = 0;
        for (Entry entry : this.countMap.values())
        {
            final int count = entry.count;
            if (count > max)
            {
                max = count;
            }
        }
        return max;
    }

    public DataType getMaximumValue()
    {
        // Go through all the entries to find the (first) count with the
        // maximum count.
        DataType value = null;
        int max = 0;
        for (Map.Entry<DataType, Entry> entry : this.countMap.entrySet())
        {
            final int count = entry.getValue().count;
            if (count > max)
            {
                value = entry.getKey();
                max = count;
            }
        }
        return value;
    }

    public LinkedList<DataType> getMaximumValues()
    {
        // Get the maximum count.
        final int max = this.getMaximumCount();

        // Go through all the values and get the ones whose counts equal the
        // maximum.
        final LinkedList<DataType> result = new LinkedList<DataType>();
        for (Map.Entry<DataType, Entry> entry : this.countMap.entrySet())
        {
            final int count = entry.getValue().count;
            if (count == max)
            {
                result.add(entry.getKey());
            }
        }

        return result;
    }

    public int getTotalCount()
    {
        return this.totalCount;
    }

    /**
     * Sets the total count.
     *
     * @param  totalCount The total count.
     */
    protected void setTotalCount(
        final int totalCount )
    {
        this.totalCount = totalCount;
    }

    /**
     * Gets the count map.
     *
     * @return The count map.
     */
    protected Map<DataType, Entry> getCountMap()
    {
        return this.countMap;
    }

    /**
     * Sets the count map.
     *
     * @param  countMap The count map.
     */
    protected void setCountMap(
        final Map<DataType, Entry> countMap )
    {
        this.countMap = countMap;
    }

    /**
     * Getting the mean is not supported by MapBasedDataHistogram since it does
     * not constrain DataType to know how to compute the mean. However,
     * subclasses can feel free to implement this.
     *
     * Note: You may be looking for getMeanCount instead, which returns the
     * mean count over the values.
     *
     * @return
     *      Nothing. An exception will be thrown.
     */
    public DataType getMean()
    {
        // We do not support the mean over the count type since we do not
        // know how to compute the mean from an arbitrary set of values.
        throw new UnsupportedOperationException("mean not supported");
    }

    /**
     * Gets the mean count. Note that if you add a count to the domain and
     * then reduce it to zero, it will no longer be included in the mean.
     *
     * @return
     *      The mean count.
     */
    public double getMeanCount()
    {
        final int valueCount = this.getDomain().size();

        if (valueCount == 0)
        {
            // The data is empty and we don't want to divide-by-zero.
            return 0.0;
        }
        else
        {
            // Compute the mean.
            return (double) this.getTotalCount() / valueCount;
        }
    }
    
    @Override
    public String toString()
    {
        final int domainSize = this.getDomain().size();
        StringBuilder result = new StringBuilder(domainSize * 100);
        result.append("Histogram has " + domainSize + " domain objects and " +
            this.getTotalCount() + " total count:\n");
        for (DataType value : this.getDomain())
        {
            result.append(value.toString());
            result.append(": ");
            result.append(this.getCount(value));
            result.append(" (");
            result.append(this.getFraction(value));
            result.append(")");
            result.append("\n");
        }

        return result.toString();

    }

    @Override
    public ArrayList<? extends DataType> sample(
        Random random,
        int numSamples)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public DataHistogram.PMF<DataType> getProbabilityFunction()
    {
        return new MapBasedDataHistogram.PMF<DataType>(this);
    }

    @Override
    public double getEntropy()
    {

        // Compute the entropy by looping over the values in the map.
        double entropy = 0.0;
        
        if (this.totalCount > 0)
        {
            for (Entry entry : this.countMap.values())
            {
                if (entry.count > 0)
                {
                    final double p = (double) entry.count / this.totalCount;
                    entropy -= p * MathUtil.log2(p);
                }
            }
        }

        // Return the computed entropy.
        return entropy;
    }

    /**
     * PMF of the DataHistogram
     * @param <DataType>
     * Type of data in the domain
     */
    public static class PMF<DataType>
        extends MapBasedDataHistogram<DataType>
        implements DataHistogram.PMF<DataType>
    {

        /**
         * Creates a new instance of DataCountMapHistogram.
         */
        public PMF()
        {
            super();
        }

        /**
         * Creates a map-based data histogram with the given expected domain size.
         *
         * @param   initialDomainCapacity
         *      The expected domain size. Must be positive.
         */
        public PMF(
            final int initialDomainCapacity)
        {
            super(initialDomainCapacity);
        }

        /**
         * Creates a new instance of DataCountMapHistogram.
         *
         * @param   data
         *      Data to add.
         */
        public PMF(
            final Collection<DataType> data)
        {
            super(data);
        }

        /**
         * Copy constructor
         * @param other
         * MapBasedDataHistogram to copy
         */
        public PMF(
            final DataHistogram<DataType> other)
        {
            super(other);
        }

        /**
         * Creates a new instance of MapBasedDataHistogram.
         *
         * @param   countMap
         *      The map backing the histogram.
         */
        protected PMF(
            final Map<DataType, Entry> countMap)
        {
            super(countMap);
        }

        @Override
        public double logEvaluate(
            DataType input)
        {
            return Math.log(this.getFraction(input));
        }

        @Override
        public Double evaluate(
            DataType input)
        {
            return this.getFraction(input);
        }

        @Override
        public MapBasedDataHistogram.PMF<DataType> getProbabilityFunction()
        {
            return this;
        }

    }

    /**
     * An entry in the histogram. This class is used instead of an Integer
     * to avoid having to create lots of new Integer objects for each
     * arithmetic operation. Instead, the contained int value is updated.
     */
    protected static class Entry
        extends AbstractCloneableSerializable
    {
// TODO: If we create a generic mutable integer object. Use that here instead.
// -- jdbasil (2010-01-28)
        /**
         * Count of the Entry.
         */
        protected int count;

        /**
         * Creates a new entry with zero count.
         */
        public Entry()
        {
            this(0);
        }

        /**
         * Creates an entry with the given count.
         *
         * @param   count
         *      The count.
         */
        public Entry(
            final int count)
        {
            super();

            this.count = count;
        }

        /**
         * Gets the count.
         *
         * @return
         *      The count.
         */
        public int getCount()
        {
            return this.count;
        }

        /**
         * Sets the count.
         *
         * @param   count
         *      The count.
         */
        public void setCount(
            final int count)
        {
            this.count = count;
        }
        
    }

    
    /**
     * Creates a batch learner for a map-based data histogram.
     *
     * @param   <DataType>
     *      The data type to learn over.
     */
    public static class Learner<DataType>
        extends AbstractCloneableSerializable
        implements DistributionEstimator<DataType, MapBasedDataHistogram.PMF<DataType>>,
            OnlineLearner<DataType, MapBasedDataHistogram.PMF<DataType>>
    {

        /**
         * Creates a new {@code Learner}.
         */
        public Learner()
        {
            super();
        }

        @Override
        public PMF<DataType> learn(
            final Collection<? extends DataType> data)
        {
            final PMF<DataType> result = new PMF<DataType>();
            result.addAll(data);
            return result;
        }

        @Override
        public PMF<DataType> createInitialLearnedObject()
        {
            return new PMF<DataType>();
        }

        @Override
        public void update(
            final PMF<DataType> target,
            final DataType data)
        {
            target.add(data);
        }

        @Override
        public void update(
            final PMF<DataType> target,
            final Iterable<? extends DataType> data)
        {
            target.addAll(data);
        }

    }

    /**
     * A factory for {@code MapBasedDataHistogram} objects using some given
     * initial capacity for them.
     *
     * @param   <DataType>
     *      The type of data for the factory.
     */
    public static class DefaultFactory<DataType>
        extends AbstractCloneableSerializable
        implements Factory<MapBasedDataHistogram<DataType>>
    {

        /** The initial domain capacity. */
        protected int initialDomainCapacity;

        /**
         * Creates a new {@code DefaultFactory} with a default
         * initial domain capacity.
         */
        public DefaultFactory()
        {
            this(DEFAULT_INITIAL_CAPACITY);
        }

        /**
         * Creates a new {@code DefaultFactory} with a given
         * initial domain capacity.
         *
         * @param   initialDomainCapacity
         *      The initial capacity for the domain. Must be positive.
         */
        public DefaultFactory(
            final int initialDomainCapacity)
        {
            super();

            this.setInitialDomainCapacity(initialDomainCapacity);
        }
        
        @Override
        public MapBasedDataHistogram<DataType> create()
        {
            // Create the histogram.
            return new MapBasedDataHistogram<DataType>(
                this.getInitialDomainCapacity());
        }

        /**
         * Gets the initial domain capacity.
         *
         * @return
         *      The initial domain capacity. Must be positive.
         */
        public int getInitialDomainCapacity()
        {
            return this.initialDomainCapacity;
        }

        /**
         * Sets the initial domain capacity.
         *
         * @param   initialDomainCapacity
         *      The initial domain capacity. Must be positive.
         */
        public void setInitialDomainCapacity(
            final int initialDomainCapacity)
        {
            ArgumentChecker.assertIsPositive("initialDomainCapacity",
                initialDomainCapacity);
            this.initialDomainCapacity = initialDomainCapacity;
        }

    }

}

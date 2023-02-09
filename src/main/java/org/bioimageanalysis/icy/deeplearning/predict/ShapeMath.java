package org.bioimageanalysis.icy.deeplearning.predict;

import java.io.FileNotFoundException;
import java.util.Arrays;

import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.util.Intervals;

public class ShapeMath
{
	// For the model parameters see:
	// https://github.com/bioimage-io/spec-bioimage-io/blob/gh-pages/model_spec_latest.md
	// https://github.com/bioimage-io/core-bioimage-io-python/blob/main/bioimageio/core/build_spec/build_model.py

	private final int n;
	private final long[] inputMinDimensions;
	private final long[] inputSteps;
	private final long[] outputHalos;
	private final long[] offsets;
	private final double[] scales;

	public ShapeMath( final ModelSpec modelSpec )
	{
		n = modelSpec.inputShapeMin.length;

		offsets = Arrays.stream( modelSpec.outputShapeOffset ).mapToLong( x -> ( long ) (2 * x) ).toArray();

		scales = modelSpec.outputShapeScale;

		inputMinDimensions = Arrays.stream( modelSpec.inputShapeMin ).mapToLong( x -> x ).toArray();

		outputHalos = Arrays.stream( modelSpec.outputHalo ).mapToLong( x -> x ).toArray();

		inputSteps =  Arrays.stream( modelSpec.inputShapeStep ).mapToLong( x -> x ).toArray();
	}

	public long[] getOutputDimensions( final long[] inputDimensions )
	{
		final long[] outputDimensions = new long[ n ];
		for ( int d = 0; d < n; d++ )
			outputDimensions[ d ] = ( long ) Math.ceil( offsets[ d ] + ( scales[ d ] * inputDimensions[ d ] ) );

		return outputDimensions;
	}

	public Interval getOutputInterval( final Interval inputInterval )
	{
		final long[] inputDimensions = inputInterval.dimensionsAsLongArray();
		final long[] outputDimensions = getOutputDimensions( inputDimensions );

		final long[] min = new long[ n ];
		for ( int d = 0; d < n; d++ )
			min[ d ] = ( long ) Math.ceil( inputInterval.min( d ) * scales[ d ] );

		return FinalInterval.createMinSize( min, outputDimensions );
	}

	public Interval addOutputHalo( final Interval outputInterval )
	{
		return Intervals.expand( outputInterval, outputHalos );
	}

	public Interval removeOutputHalo( final Interval outputInterval )
	{
		final long[] negativeHalos = Arrays.stream( outputHalos )
				.map( halo -> -halo ).toArray();
		return Intervals.expand( outputInterval, negativeHalos );
	}

	/**
	 * Computes the interval in the input image that is needed to
	 * produce the model output for the given {@code outputCellInterval}.
	 *
	 * Note that this may not be a valid cell size for the model.
	 * Use {@code expandToValidCellSize} to make it a valid interval.
	 *
	 * @param outputInterval
	 * @return
	 */
	public FinalInterval getInputInterval( final Interval outputInterval )
	{
		final long[] outputCellDimensions = outputInterval.dimensionsAsLongArray();

		final long[] min = new long[ n ];
		final long[] dimensions = new long[ n ];
		for ( int d = 0; d < n; d++ )
		{
			if ( scales[ d ] > 0 )
			{
				min[ d ] = ( long ) Math.ceil( outputInterval.min( d ) / scales[ d ] );
				dimensions[ d ] = ( long ) Math.ceil( ( outputCellDimensions[ d ] - offsets[ d ] ) / scales[ d ] );
			}
			else
			{
				min[ d ] = 0;
				dimensions[ d ] = 1;
			}
		}

		return FinalInterval.createMinSize( min, dimensions );
	}

	public Interval expandToValidInputInterval( final Interval inputInterval )
	{
		final long[] inputCellDimensions = inputInterval.dimensionsAsLongArray();

		final long[] min = inputInterval.minAsLongArray();
		final long[] dimensions = new long[ n ];
		for ( int d = 0; d < n; d++ )
		{
			if ( inputCellDimensions[ d ] < inputMinDimensions[ d ] )
			{
				dimensions[ d ] = inputMinDimensions[ d ];
			}
			else if ( inputSteps[ d ] == 0 )
			{
				dimensions[ d ] = inputCellDimensions[ d ];
			}
			else
			{
				final int k = ( int ) Math.ceil( (inputCellDimensions[ d ] - inputMinDimensions[ d ]) / inputSteps[ d ] );
				dimensions[ d ] = inputMinDimensions[ d ] + k * inputSteps[ d ];
			}
		}

		return FinalInterval.createMinSize( min, dimensions );
	}

	/**
	 * Creates an interval that the model can consume.
	 * Note that the output that the model will produce
	 * may be larger than the given {@code outputInterval}.
	 *
	 * @param outputInterval
	 * @return
	 * 			interval that the model can consume, such that the model output will include the given {@code outputInterval}
	 */
	public Interval getValidInputInterval( final Interval outputInterval )
	{
		return expandToValidInputInterval( getInputInterval( outputInterval ) );
	}

	public static void main( final String[] args ) throws FileNotFoundException
	{
		final String rdfPath = "/Users/tinevez/Desktop/platynereisemnucleisegmentationboundarymodel_torchscript/rdf.yaml";
		final ModelSpec info = ModelSpec.from( rdfPath );
		System.out.println( info ); // DEBUG
	}

}

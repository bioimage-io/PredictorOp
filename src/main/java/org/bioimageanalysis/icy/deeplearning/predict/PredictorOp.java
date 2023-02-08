package org.bioimageanalysis.icy.deeplearning.predict;

import java.util.function.Consumer;

import org.bioimageanalysis.icy.deeplearning.model.Model;

import net.imglib2.Interval;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;

/**
 * 
 * @param <O>
 *            the type of the pixel in the output.
 * @param <I>
 *            the type of the pixels in the input.
 */
public class PredictorOp< O, I > implements Consumer< RandomAccessibleInterval< O > >
{

	private final Model model;

	private final RandomAccessible< I > input;

	public PredictorOp( final Model model, final RandomAccessible< I > input )
	{
		this.model = model;
		this.input = input;
	}

	@Override
	public void accept( final RandomAccessibleInterval< O > outputCell )
	{
		predict( getInputInterval( outputCell ), outputCell );

	}

	/**
	 * Determines the size of an interval in the input source, that the model
	 * needs to fill a buffer of size given the specified output interval.
	 * 
	 * @param outputInterval
	 * @return
	 */
	public Interval getInputInterval( final Interval outputInterval )
	{
		return null; // TODO
	}

	/**
	 * Runs the model on the input, in the specified input interval, and writes
	 * the results in the specified output cell.
	 * <p>
	 * It's the caller responsibility to ensure that the input interval and the
	 * output cell are of adequate size and origin for the model given at
	 * construction.
	 * 
	 * @param inputInterval
	 * @param outputCell
	 */
	public void predict( final Interval inputInterval, final RandomAccessible< O > outputCell )
	{
		// TODO
	}

	public static void main( final String[] args )
	{

	}

}

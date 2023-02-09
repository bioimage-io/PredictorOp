package org.bioimageanalysis.icy.deeplearning.predict;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.bioimageanalysis.icy.deeplearning.model.Model;
import org.bioimageanalysis.icy.deeplearning.tensor.Tensor;

import net.imglib2.Interval;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.RealTypeConverters;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

/**
 * 
 * @param <O>
 *            the type of the pixel in the output.
 * @param <I>
 *            the type of the pixels in the input. // TODO: Change to FloatType
 */
public class PredictorOp< I extends RealType< I > & NativeType< I >, O extends RealType< O > & NativeType< O > > implements Consumer< RandomAccessibleInterval< O > >
{

	private final Model model;

	private final RandomAccessible< I > input;

	private final ShapeMath shapeMath;

	private final ModelSpec spec;

	public PredictorOp( final Model model, final RandomAccessible< I > input, final ModelSpec spec)
	{
		this.model = model;
		this.input = input;
		this.spec = spec;
		this.shapeMath = new ShapeMath( spec );
	}

	@Override
	public void accept( final RandomAccessibleInterval< O > cell )
	{
		// Inputs.
		final Interval addOutputHalo = shapeMath.addOutputHalo( cell );
		final Interval validInputInterval = shapeMath.getValidInputInterval( addOutputHalo );

		final IntervalView< I > rai = Views.interval( input, validInputInterval );
		final RandomAccessibleInterval< FloatType > raiFloat = Tensor.createCopyOfRaiInWantedDataType( rai, new FloatType() );
		// TODO: Carlos: It actually wants an Img, which it should not?!
		final Tensor< FloatType > inputTensor = Tensor.build( "input0", spec.inputAxes, raiFloat );
		final List< Tensor< ? > > inputs = new ArrayList<>();
		inputs.add( inputTensor );

		// Outputs.
		// TODO: Carlos: Passing in the cell does not really work!
//		final Tensor< O > outputTensor = Tensor.build( "output0", spec.outputAxes, cell );
		final Tensor< O > outputTensor = Tensor.buildEmptyTensor( "output0", spec.outputAxes );
		final List< Tensor< ? > > outputs = new ArrayList<>();
		outputs.add( outputTensor );

		// Run the model.
		try
		{
			model.runModel( inputs, outputs );
			@SuppressWarnings( "unchecked" )
			final
			RandomAccessibleInterval< FloatType > output = ( RandomAccessibleInterval< FloatType > ) outputs.get( 0 ).getData();
			// TODO Do dimension massaging with the matcher?

			RealTypeConverters.copyFromTo( output, cell );
		}
		catch ( final Exception e )
		{
			e.printStackTrace();
			throw new RuntimeException( e );
		}
	}
}

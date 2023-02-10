package org.bioimageanalysis.icy.deeplearning.predict;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.bioimage.modelrunner.model.Model;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.cache.img.ReadOnlyCachedCellImgFactory;
import net.imglib2.cache.img.ReadOnlyCachedCellImgOptions;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

public class PredictionCachedCellImgCreator
{
	public static List< RandomAccessibleInterval< FloatType > > createLazyXYZOutputImages( final RandomAccessibleInterval< FloatType > xyzInput, final Model model, final ModelSpec modelSpec )
	{
		// create model input image
		final RandomAccessibleInterval< FloatType > modelInput = AxesMatcher.matchAxes( modelSpec.inputAxes, "xyz", xyzInput );

		// instantiate predictor with the input image
		final PredictorOp< FloatType, FloatType > predictorOp = new PredictorOp<>( model, Views.extendMirrorSingle( modelInput ), modelSpec );

		// create model output image
		//
		final String outputDataType = modelSpec.outputDataType;
		// TODO: Use the outputDataType
		final FloatType type = new FloatType();

		final ShapeMath shapeMath = new ShapeMath( modelSpec );

		// TODO remove the long mapping if we change it in the modelSpec
		final int[] outputCellDimensions =	Arrays.stream(
						shapeMath.getOutputDimensions(
								Arrays.stream( modelSpec.inputShapeMin )
										.mapToLong( x -> x ).toArray() )
				).mapToInt( x -> ( int ) x ).toArray();

		final long[] outputInterval = shapeMath.getOutputDimensions( modelInput.dimensionsAsLongArray() );

		System.out.println("Output image dimensions: " + Arrays.toString( outputInterval ) );
		System.out.println("Output cell dimensions: " + Arrays.toString( outputCellDimensions ) );

		final RandomAccessibleInterval< FloatType > modelOutput =
				new ReadOnlyCachedCellImgFactory().create(
						outputInterval,
						type,
						predictorOp::accept,
						ReadOnlyCachedCellImgOptions.options().cellDimensions( outputCellDimensions )
				);

		final RandomAccessibleInterval< FloatType > cxyzOutput = AxesMatcher.matchAxes( "cxyz", modelSpec.outputAxes, modelOutput );

		final ArrayList< RandomAccessibleInterval< FloatType > > xyzOutputs = new ArrayList<>();
		for ( int c = 0; c < cxyzOutput.dimension( 0 ); c++ )
		{
			xyzOutputs.add( Views.hyperSlice( cxyzOutput , 0, c ) );
		}

		return xyzOutputs;
	}
}

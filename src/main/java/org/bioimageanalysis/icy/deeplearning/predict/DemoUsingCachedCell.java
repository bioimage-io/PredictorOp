package org.bioimageanalysis.icy.deeplearning.predict;

import java.io.FileNotFoundException;
import java.util.List;

import org.bioimageanalysis.icy.deeplearning.model.Model;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.RealTypeConverters;
import net.imglib2.img.ImagePlusAdapter;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;

public class DemoUsingCachedCell
{
	// J-Y
	public static final String MODEL_FOLDER = "/Users/tinevez/Desktop/DemoModelRunner/platynereisemnucleisegmentationboundarymodel_torchscript";
	public static final String ENGINES_FOLDER = "/Users/tinevez/Development/Mastodon/model-runner-java/engines";
	public static final String EXAMPLE_IMAGE = "/Users/tinevez/Desktop/DemoModelRunner/sample_input_0.tif";
	public static final boolean USE_CPU = true;
	public static final boolean USE_GPU = false;

	// TISCHI


	// TOBY


	public static < I extends RealType< I > & NativeType< I >, O extends RealType< O > & NativeType< O > > void main( final String[] args ) throws Exception
	{
		try
		{
			ImageJ.main( args );

			/*
			 * Load the input image.
			 */
			final String imgPath = EXAMPLE_IMAGE;
			final ImagePlus imp = IJ.openImage( imgPath );
			final Img< I > img = ImagePlusAdapter.wrap( imp );
			// Could be determined from the input file.
			final String inputAxes = "xyz";
			System.out.println( "Image loaded: " + img + " with axes: " + inputAxes + " and type: " + img.firstElement().getClass().getSimpleName() );
			imp.show();

			/*
			 * Load the model specs.
			 */
			final ModelSpec spec = ModelSpec.fromFolder( MODEL_FOLDER );
			System.out.println( "Model specs loaded. Output axes: " + spec.outputAxes + " - Output data type: " + spec.outputType().getClass().getSimpleName() );

			/*
			 * Create the model.
			 */
			final Model model = ModelCreator.fromFiles(
					MODEL_FOLDER,
					spec.weightSource,
					ENGINES_FOLDER,
					USE_CPU,
					USE_GPU,
					spec.weightType.getFormat(),
					"1.9.1" // FUCKIT
			);
			System.out.println( "Model loaded: " + model );

			/*
			 * Use the cached cell creator.
			 */
			System.out.println( "Running the model." ); 
			final List< RandomAccessibleInterval< FloatType > > outputs = PredictionCachedCellImgCreator.createLazyXYZOutputImages(
					RealTypeConverters.convert( img, new FloatType() ), model, spec );

			/*
			 * Reshape the output. It is model dependent and is the
			 * responsibility of the consumer.
			 */
			
			for ( final RandomAccessibleInterval< FloatType > output : outputs )
			{
				final Img< FloatType > target = ArrayImgs.floats( output.dimensionsAsLongArray() );
				RealTypeConverters.copyFromTo( output, target );
				final ImagePlus impOut = ImageJFunctions.wrap( target, target.toString() );
				impOut.setDimensions( 1, impOut.getNChannels(), 1 );
				impOut.show();
			}
		}
		catch ( final FileNotFoundException e )
		{
			e.printStackTrace();
		}
	}
}

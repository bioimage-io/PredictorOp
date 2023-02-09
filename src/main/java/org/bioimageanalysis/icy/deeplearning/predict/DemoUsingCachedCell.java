package org.bioimageanalysis.icy.deeplearning.predict;

import java.io.FileNotFoundException;
import java.util.List;

import bdv.util.AxisOrder;
import bdv.util.BdvFunctions;
import bdv.util.BdvHandle;
import bdv.util.BdvOptions;
import bdv.util.BdvStackSource;
import bdv.util.volatiles.VolatileViews;
import net.imglib2.Volatile;
import net.imglib2.type.numeric.ARGBType;
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

import static org.bioimageanalysis.icy.deeplearning.predict.Resources.ENGINES_FOLDER;
import static org.bioimageanalysis.icy.deeplearning.predict.Resources.EXAMPLE_IMAGE;
import static org.bioimageanalysis.icy.deeplearning.predict.Resources.MODEL_FOLDER;
import static org.bioimageanalysis.icy.deeplearning.predict.Resources.USE_CPU;
import static org.bioimageanalysis.icy.deeplearning.predict.Resources.USE_GPU;

public class DemoUsingCachedCell
{
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

			final BdvStackSource< I > bdvStackSource = BdvFunctions.show( img, "input", BdvOptions.options().axisOrder( AxisOrder.XYZ ) );
			final BdvHandle bdvHandle = bdvStackSource.getBdvHandle();


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
			int channel = 0 ;
			for ( final RandomAccessibleInterval< FloatType > output : outputs )
			{
				// show in BDV
				final RandomAccessibleInterval< Volatile< FloatType > > volatileRandomAccessibleInterval = VolatileViews.wrapAsVolatile( output );
				final BdvStackSource< Volatile< FloatType > > stackSource = BdvFunctions.show( volatileRandomAccessibleInterval, "ch_" + ( channel++ ), BdvOptions.options().axisOrder( AxisOrder.XYZ ).addTo( bdvHandle ) );
				stackSource.setDisplayRange( 0, 1 );
				stackSource.setColor( new ARGBType( ARGBType.rgba( 255, 0, 255, 255 ) ) );
			}

			// show in ImageJ
			// doing this at the same time as showing in BDV screws things up
//			channel = 0 ;
//			for ( final RandomAccessibleInterval< FloatType > output : outputs )
//			{
//				final ImagePlus impOut = ImageJFunctions.wrap( output, "ch_" + channel++ );
//				impOut.setDimensions( 1, impOut.getNChannels(), 1 );
//				impOut.show();
//			}
		}
		catch ( final FileNotFoundException e )
		{
			e.printStackTrace();
		}
	}
}

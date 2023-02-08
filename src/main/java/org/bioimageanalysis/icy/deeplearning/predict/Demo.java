package org.bioimageanalysis.icy.deeplearning.predict;

import java.io.File;
import java.io.FileNotFoundException;

import org.bioimageanalysis.icy.deeplearning.engine.EngineInfo;
import org.bioimageanalysis.icy.deeplearning.model.Model;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import net.imglib2.Interval;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.ImagePlusAdapter;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Util;
import net.imglib2.view.Views;

public class Demo
{

	public static < I extends RealType< I > & NativeType< I >, O extends RealType< O > & NativeType< O > > void main( final String[] args )
	{
		try
		{
			ImageJ.main( args );

			/*
			 * Load the input image.
			 */
			final String imgPath = "/Users/tinevez/Desktop/DemoModelRunner/sample_input_0.tif";
			final ImagePlus imp = IJ.openImage( imgPath );
			final Img< I > img = ImagePlusAdapter.wrap( imp );
			// Could be determined from the input file.
			final String inputAxes = "xyz";
			System.out.println( "Image loaded: " + img + " with axes: " + inputAxes + " and type: " + img.firstElement().getClass().getSimpleName() );
			imp.show();

			/*
			 * Load the model.
			 */
			final Model model = loadModel();
			System.out.println( "Model loaded: " + model );

			/*
			 * Load the specs.
			 */
			final ModelSpec spec = ModelSpec.from( model );
			System.out.println( "Model specs loaded. Output axes: " + spec.outputAxes + " - Output data type: " + spec.outputType().getClass().getSimpleName() );

			/*
			 * Reshape the input to match the specs.
			 */
			final RandomAccessibleInterval< I > input = AxesMatcher.matchAxes( spec.inputAxes, inputAxes, img );
			System.out.println( "Input reshaped: " + input );

			/*
			 * Prepare holder for results.
			 */
			final ShapeMath shapeMath = new ShapeMath( spec );
			final Interval outputInterval = shapeMath.getOutputInterval( input );
			@SuppressWarnings( "unchecked" )
			final O outputType = ( O ) spec.outputType();
			final ImgFactory< O > factory = Util.getArrayOrCellImgFactory( outputInterval, outputType );
			RandomAccessibleInterval< O > output = factory.create( outputInterval );
			System.out.println( "Output holder prepared: " + output );

			/*
			 * Run the model.
			 */
			final RandomAccessible<I> extendedInput = Views.extendMirrorSingle( input );
			final PredictorOp< I, O > op = new PredictorOp<>( model, extendedInput, spec );
			System.out.println( "Running the model." ); 
			final long start = System.currentTimeMillis();
			op.accept( output );
			final long end = System.currentTimeMillis();
			System.out.println( String.format( "Done in %.1f s.", ( end - start ) / 1000. ) );

			/*
			 * Reshape the output. This should be done in a generic manner,
			 * guessing automatically the desired shape of outputs based on on
			 * the output axes string.
			 */
			output = Views.permute( output, 2, 4 );
			output = Views.hyperSlice( output, 0, 0 );
			final RandomAccessibleInterval< O > ch1 = Views.hyperSlice( output, 0, 0 );
			final RandomAccessibleInterval< O > ch2 = Views.hyperSlice( output, 0, 1 );
			
			final ImagePlus imp1 = ImageJFunctions.wrap( ch1, "Output 1" );
			imp1.setDimensions( 1, imp1.getNChannels(), 1 );
			imp1.show();
			final ImagePlus imp2 = ImageJFunctions.wrap( ch2, "Output 2" );
			imp2.setDimensions( 1, imp2.getNChannels(), 1 );
			imp2.show();
		}
		catch ( final FileNotFoundException e )
		{
			e.printStackTrace();
		}
	}

	private static Model loadModel()
	{
		try
		{
			final String rootFolder = "/Users/tinevez/Desktop/DemoModelRunner";
			final String engine = "torchscript";
			final String engineVersion = "1.9.1";
			final String enginesDir = "/Users/tinevez/Development/Mastodon/model-runner-java/engines";
			final String modelFolder = new File( rootFolder, "platynereisemnucleisegmentationboundarymodel_torchscript" ).getAbsolutePath();
			final String modelSource = new File( modelFolder, "/weights-torchscript.pt" ).getAbsolutePath();
			final boolean cpu = true;
			final boolean gpu = false;
			final EngineInfo engineInfo = EngineInfo.defineDLEngine( engine, engineVersion, enginesDir, cpu, gpu );
			final Model model = Model.createDeepLearningModel( modelFolder, modelSource, engineInfo );
			model.loadModel();
			return model;
		}
		catch ( final Exception e )
		{
			e.printStackTrace();
			throw new RuntimeException( e );
		}
	}
}


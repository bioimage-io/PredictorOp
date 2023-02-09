package org.bioimageanalysis.icy.deeplearning.predict;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.Roi;
import net.imglib2.Interval;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.ImagePlusAdapter;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Intervals;
import net.imglib2.util.Util;
import net.imglib2.view.Views;
import org.bioimageanalysis.icy.deeplearning.engine.EngineInfo;
import org.bioimageanalysis.icy.deeplearning.model.Model;

import java.io.File;
import java.io.FileNotFoundException;

public class DemoUsingModelCreator
{
	// J-Y
	public static final String MODEL_FOLDER = "/Users/tinevez/Desktop/DemoModelRunner/platynereisemnucleisegmentationboundarymodel_torchscript";
	public static final String ENGINES_FOLDER = "/Users/tinevez/Development/Mastodon/model-runner-java/engines";
	public static final String EXAMPLE_IMAGE = "/Users/tinevez/Desktop/DemoModelRunner/sample_input_0.tif";

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
					true,
					false,
					spec.weightType.getFormat(),
					"1.9.1" // FUCKIT
			);
			System.out.println( "Model loaded: " + model );


			/*
			 * Specify a ROI in the input.
			 */
//			final Interval roi = img;
			final Interval roi = Intervals.createMinMax( 50, 50, 10, 150, 150, 20 );
			imp.setRoi( new Roi( roi.min( 0 ), roi.min( 1 ), roi.dimension( 0 ), roi.dimension( 1 ) ) );
			System.out.println( "Using a ROI specified in the input coordinate system: " + roi );
			// Convert it to an output ROI.
			final Interval reshapedRoi = AxesMatcher.matchInterval( spec.inputAxes, inputAxes, roi );
			System.out.println( "ROI in output coordinate system: " + reshapedRoi );

			/*
			 * Show the input cropped by the ROI.
			 */
			final ImagePlus crop = ImageJFunctions.wrap( Views.interval( img, roi ), "Cropped" );
			crop.setDimensions( 1, crop.getNChannels(), 0 );
			crop.show();

			/*
			 * Reshape the input to match the specs.
			 */
			final RandomAccessibleInterval< I > reshapedInput = AxesMatcher.matchAxes( spec.inputAxes, inputAxes, img );
			System.out.println( "Input reshaped: " + reshapedInput );

			/*
			 * Prepare holder for results.
			 */
			final ShapeMath shapeMath = new ShapeMath( spec );
//			final Interval outputInterval = shapeMath.getOutputInterval( reshapedInput );
			final Interval outputInterval = shapeMath.getOutputInterval( reshapedRoi );
			System.out.println( "Output corresponding to ROI in output coordinate system: " + outputInterval );
			@SuppressWarnings( "unchecked" )
			final O outputType = ( O ) spec.outputType();
			final ImgFactory< O > factory = Util.getArrayOrCellImgFactory( outputInterval, outputType );
			RandomAccessibleInterval< O > output = factory.create( outputInterval );
			output = Views.translate( output, outputInterval.minAsLongArray() );
			System.out.println( "Output holder prepared: " + output );

			/*
			 * Instantiates the op on an extended version of the input,
			 * specifying the model.
			 */
			final RandomAccessible< I > extendedInput = Views.extendMirrorSingle( reshapedInput );
			final PredictorOp< I, O > op = new PredictorOp<>( model, extendedInput, spec );
			System.out.println( "Op created: " + op );

			/*
			 * Run the model.
			 */
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
			final String enginesDir = ENGINES_FOLDER;
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


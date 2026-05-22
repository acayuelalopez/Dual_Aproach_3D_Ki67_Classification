import ij.IJ
import ij.ImagePlus
import ij.WindowManager
import ij.gui.Roi
import ij.gui.ShapeRoi
import ij.measure.ResultsTable
import ij.plugin.ChannelSplitter
import ij.plugin.Duplicator
import ij.plugin.RGBStackMerge
import ij.plugin.ZProjector
import ij.plugin.frame.RoiManager
import inra.ijpb.binary.BinaryImages
import inra.ijpb.color.CommonColors
import inra.ijpb.data.image.ColorImages
import inra.ijpb.morphology.Strel
import loci.plugins.BF
import loci.plugins.in.ImporterOptions
import mcib3d.geom.Object3D
import net.imglib2.converter.ChannelARGBConverter
import org.apache.commons.compress.utils.FileNameUtils
import mcib3d.geom.Objects3DPopulation
import mcib3d.image3d.ImageInt

import java.io.File;

// INPUT UI
//
//#@File(label = "Input File Directory", style = "directory") inputFilesDir
//#@File(label = "Output directory", style = "directory") outputDir
//#@Integer(label = "Dapi Channel", value = 0) dapiChannel
//#@Integer(label = "CD74 Channel", value = 1) greenChannel
//#@Integer(label = "Microglia Channel", value = 2) cyanChannel
//#@Integer(label = "MDK Channel", value = 3) redChannel
//#@Integer(label = "LUC Channel", value = 4) grayChannel
//#@Boolean(label = "Apply DAPI?") applyDAPI
def inputFilesRawDir = new File("/mnt/imgserver/IA/Projects/2026/2026_4_24_npriego/output/images")
def outputDir = new File("/mnt/imgserver/IA/Projects/2026/2026_4_24_npriego/output/csv")
def inputFilesNucleiDir = new File("/mnt/imgserver/IA/Projects/2026/2026_4_24_npriego/output/nuclei")
def inputFilesKi67Dir = new File("/mnt/imgserver/IA/Projects/2026/2026_4_24_npriego/output/ki67")


// IDE
//
//
//def headless = true;
//new ImageJ().setVisible(true);

IJ.log("-Parameters selected: ")
IJ.log("    -inputFileDir: " + inputFilesRawDir)
IJ.log("    -outputDir: " + outputDir)
IJ.log("                                                           ");
/** Get files (images) from input directory */
def listOfFiles = inputFilesRawDir.listFiles();
def tablePerImage = new ResultsTable()
def tablePerNucleus = new ResultsTable()
def ki67Mean = 0.doubleValue()
def ki67Std = 0.doubleValue()
def counter = 0.intValue()

def positiveKi67, negativeKi67 = null
def counterNuc = 0.intValue()
def imp = null
/** Begin of calculating distribution */
def ki67MeanPerNuc = new ArrayList<Double>()
def ki67Index = null
def dapiIndex = null


for (def i = 0; i < listOfFiles.length; i++) {
    if (!listOfFiles[i].getName().contains("_seg.npy")) {
        ki67Index = 0.intValue()
        dapiIndex = 2.intValue()

        imp = new ImagePlus(inputFilesRawDir.getAbsolutePath() + File.separator + listOfFiles[i].getName())
        IJ.log(inputFilesRawDir.getAbsolutePath() + File.separator + listOfFiles[i].getName())
        def labelDapi = new ImagePlus(inputFilesNucleiDir.getAbsolutePath() + File.separator + listOfFiles[i].getName().replaceAll(".tif", "_cp_masks.tif"))
        def channels = ChannelSplitter.split(imp)
        def chKi67 = channels[ki67Index]

        // Get Ki67 signal
        def signalKi67 = ImageInt.wrap(extractCurrentStack(chKi67));


        // Get Dapi objects population
        def imgDapi = ImageInt.wrap(extractCurrentStack(labelDapi));
        def populationDapi = new Objects3DPopulation(imgDapi);

        for (int j = 0.intValue(); j < populationDapi.getNbObjects(); j++)
            ki67MeanPerNuc.add(populationDapi.getObject(j).getPixMeanValue(signalKi67))
    }

}


ki67Mean = ki67MeanPerNuc.stream()
        .mapToDouble(d -> d)
        .average()
        .orElse(0.0)

ki67Std = std(ki67MeanPerNuc, ki67Mean)


for (def i = 0; i < listOfFiles.length; i++) {
    if (!listOfFiles[i].getName().contains("_seg.npy")) {


        ki67Index = 0.intValue()
        dapiIndex = 2.intValue()

        def colocCounter = new ArrayList<Double>()
        IJ.log("-Analyzing image: " + listOfFiles[i].getName())
        imp = new ImagePlus(inputFilesRawDir.getAbsolutePath() + File.separator + listOfFiles[i].getName())
        def labelDapi = new ImagePlus(inputFilesNucleiDir.getAbsolutePath() + File.separator + listOfFiles[i].getName().replaceAll(".tif", "_cp_masks.tif"))
        def labelKi67 = new ImagePlus(inputFilesKi67Dir.getAbsolutePath() + File.separator + listOfFiles[i].getName().replaceAll(".tif", "_cp_masks.tif"))
        def channels = ChannelSplitter.split(imp)
        def chKi67 = channels[ki67Index]
        def chDapi = channels[dapiIndex]

// Get Ki67 signal
        def signalKi67 = ImageInt.wrap(extractCurrentStack(chKi67));


// Get Dapi objects population
        def imgDapi = ImageInt.wrap(extractCurrentStack(labelDapi));
        def populationDapi = new Objects3DPopulation(imgDapi);


// Get Ki67 objects population
        def imgKi67 = ImageInt.wrap(extractCurrentStack(labelKi67));
        def populationKi67 = new Objects3DPopulation(imgKi67);

        for (int j = 0.intValue(); j < populationDapi.getNbObjects(); j++)
            ki67MeanPerNuc.add(populationDapi.getObject(j).getPixMeanValue(signalKi67))

// overlap analysis

        for (def j = 0.intValue(); j < populationDapi.getNbObjects(); j++) {
            def colocCounterInt = 0.doubleValue()
            for (def k = 0.intValue(); k < populationKi67.getNbObjects(); k++)
                if (populationDapi.getObject(j).pcColoc(populationKi67.getObject(k)) > 20)
                    colocCounterInt++

            colocCounter.add(colocCounterInt.doubleValue())


        }

        IJ.log(listOfFiles[i].getName() + "-----" + colocCounter.stream()
                .mapToDouble(d -> d)
                .sum()
        )
        def merge = RGBStackMerge.mergeChannels(new ImagePlus[]{labelDapi, chDapi, chKi67}, false)
        IJ.saveAs(merge, "Tiff", outputDir.getAbsolutePath().replace("csv", "merge") + File.separator + listOfFiles[i].getName())


//Get ki67+
        def positiveKi67Th = new ArrayList<Object3D>();
        def negativeKi67Th = new ArrayList<Object3D>();
        def positiveKi67Ov = new ArrayList<Object3D>();
        def negativeKi67Ov = new ArrayList<Object3D>();

        for (def j = 0.intValue(); j < populationDapi.getNbObjects(); j++) {
            counterNuc++
            tablePerNucleus.incrementCounter();
            tablePerNucleus.setValue("Image Serie Title", counterNuc, imp.getTitle())
            tablePerNucleus.setValue("DAPI Label ID", counterNuc, populationDapi.getObject(j).getValue())
            tablePerNucleus.setValue("Ki67 Mean Intensity", counterNuc, populationDapi.getObject(j).getPixMeanValue(signalKi67))
            tablePerNucleus.setValue("Ki67 Sum Intensity", counterNuc, populationDapi.getObject(j).getIntegratedDensity(signalKi67))
            tablePerNucleus.setValue("Ki67 Positivity Threshold", counterNuc, (ki67Mean).toString())
            if (populationDapi.getObject(j).getPixMeanValue(signalKi67) > (ki67Mean + (0.5) * ki67Std)) {
                positiveKi67Th.add(populationDapi.getObject(j))
                tablePerNucleus.setValue("Ki67 Status Threshold", counterNuc, "Positive")

            } else {
                negativeKi67Th.add(populationDapi.getObject(j))
                tablePerNucleus.setValue("Ki67 Status Threshold", counterNuc, "Negative")
            }

            if (colocCounter.get(j) > 0.doubleValue()) {
                positiveKi67Ov.add(populationDapi.getObject(j))
                tablePerNucleus.setValue("Ki67 Status Overlap", counterNuc, "Positive")

            } else {
                negativeKi67Ov.add(populationDapi.getObject(j))
                tablePerNucleus.setValue("Ki67 Status Overlap", counterNuc, "Negative")
            }
        }


        counter++
        tablePerImage.incrementCounter();
        tablePerImage.setValue("Image Serie Title", counter, imp.getTitle())
        tablePerImage.setValue("N of DAPI+ Cells", counter, populationDapi.getNbObjects())
        tablePerImage.setValue("N of DAPI+Ki67+ Cells Threshold", counter, positiveKi67Th.size())
        tablePerImage.setValue("N of DAPI+Ki67- Threshold", counter, negativeKi67Th.size())
        tablePerImage.setValue("N of DAPI+Ki67+ Cells Overlap", counter, positiveKi67Ov.size())
        tablePerImage.setValue("N of DAPI+Ki67- Overlap", counter, negativeKi67Ov.size())

    }
}
tablePerImage.saveAs(outputDir.getAbsolutePath() + File.separator + "3DAnalysis_perImage_new" + ".csv")
tablePerNucleus.saveAs(outputDir.getAbsolutePath() + File.separator + "3DAnalysis_perNucleus_new" + ".csv")
IJ.log("Done!!!")

static double std(ArrayList<Double> table, double mean) {
    // Step 1:
    double meanDef = mean
    double temp = 0;

    for (int i = 0; i < table.size(); i++) {
        int val = table.get(i);

        // Step 2:
        double squrDiffToMean = Math.pow(val - meanDef, 2);

        // Step 3:
        temp += squrDiffToMean;
    }

    // Step 4:
    double meanOfDiffs = (double) temp / (double) (table.size());

    // Step 5:
    return Math.sqrt(meanOfDiffs);
}

ImagePlus extractCurrentStack(ImagePlus plus) {
    // check dimensionsnegativeGrayObjs
    int[] dims = plus.getDimensions();//XYCZT
    int channel = plus.getChannel();
    int frame = plus.getFrame();
    ImagePlus stack;
    // crop actual frame
    if ((dims[2] > 1) || (dims[4] > 1)) {
        IJ.log("hyperstack found, extracting current channel " + channel + " and frame " + frame);
        def duplicator = new Duplicator();
        stack = duplicator.run(plus, channel, channel, 1, dims[3], frame, frame);
    } else stack = plus.duplicate();

    return stack;
}


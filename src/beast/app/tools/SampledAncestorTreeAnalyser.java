package beast.app.tools;

import beast.app.BEASTVersion2;
import beast.app.util.Application;
import beast.core.Description;
import beast.core.Param;
import beast.evolution.tree.Tree;
import beast.util.AddOnManager;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * @author Alexandra Gavryushkina
 */

@Description("utility for creating a report of ancestral nodes in a tree set")
/*
 * Usage: /path/to/beast/appstore SampledAncestorTreeAnalyser -file <treefile.trees>
 * Opens a report in a webbrowser containing statistics on how much support there is
 * for an internal node to be ancestral.
 *
 */
public class SampledAncestorTreeAnalyser extends beast.core.Runnable {
	
	private File file;
	private Boolean printFrequencies = true;
	private Boolean printPairs = false;
	private Boolean printCladeFrequencies = false;
	
	public SampledAncestorTreeAnalyser() {}
	public SampledAncestorTreeAnalyser(@Param(name="file", description="tree file containing set of ancestral ancestor trees") File file,
			@Param(name="printFrequencies", description="show sampled ancestor frequencies in output table", defaultValue="true") Boolean printFrequencies,
			@Param(name="printPairs", description="show ancestor-descendant pair frequencies in output table", defaultValue="false") Boolean printPairs,
			@Param(name="printCladeFrequencies", description="show sampled ancesor calde frequencies in output table", defaultValue="false") Boolean printCladeFrequencies) {
		this.file = file;
		this.printFrequencies = printFrequencies;
		this.printPairs = printPairs;
		this.printCladeFrequencies = printCladeFrequencies;
	}

	public File getFile() {
		return file;
	}
	public void setFile(File file) {
		this.file = file;
	}
    public Boolean getPrintFrequencies() {
		return printFrequencies;
	}
	public void setPrintFrequencies(Boolean printFrequencies) {
		this.printFrequencies = printFrequencies;
	}
	public Boolean getPrintPairs() {
		return printPairs;
	}
	public void setPrintPairs(Boolean printPairs) {
		this.printPairs = printPairs;
	}
	public Boolean getPrintCladeFrequencies() {
		return printCladeFrequencies;
	}
	public void setPrintCladeFrequencies(Boolean printCladeFrequencies) {
		this.printCladeFrequencies = printCladeFrequencies;
	}
	
	public void run() throws Exception {
 
        FileReader reader = null;

        try {
            System.out.println("Reading file " + file.getName());
            reader = new FileReader(file);
            List<Tree> trees = SATreeTraceAnalysis.Utils.getTrees(file);
            SATreeTraceAnalysis analysis = new SATreeTraceAnalysis(trees, 0.1);
            String result = analysis.toHTML(printCladeFrequencies, printPairs, printFrequencies);
            
    		// create HTML file with results
    		String html = "<html>\n" + 
    		"<title>BEAST " + new BEASTVersion2().getVersionString() + ": Sampled Ancestor Tree Analyser</title>\n" +
    		"<head>  \n" +
    		"<link rel='stylesheet' type='text/css' href='css/style.css'>\n" +
    		"</head>\n" +
    		"<body>\n" +
    		"<h2>Sampled Ancestor Tree Trace Analysis from " + file.getPath() +"</h2>\n" +
    		result +
    		"</body>\n" +
    		"</html>";		
    		
    		
    		// write html file in package dir + "/js/minitracer.html"
    		String jsPath = Application.getPackagePath("SA.addon.jar") + "js";
            FileWriter outfile = new FileWriter(jsPath + "/SATreeAnalysis.html");
            outfile.write(html);
            outfile.close();
    		
            // open html file in browser
            Application.openUrl("file://" + jsPath + "/SATreeAnalysis.html");
//            analysis.report(System.out);
        }
        catch (IOException e) {
            //
        }
        finally {
            if (reader != null) {
                reader.close();
            }
        }
    }

	@Override
	public void initAndValidate() {
	}

	
	public static void main(String[] args) throws Exception {
		AddOnManager.loadExternalJars();	
		new Application(new SampledAncestorTreeAnalyser(), "SampledAncestorTreeAnalyser", args);
	}
}

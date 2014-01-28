package beast.evolution.speciation;

import beast.core.Input;
import beast.core.parameter.RealParameter;
import beast.evolution.tree.TreeInterface;
import beast.evolution.tree.ZeroBranchSANode;
import beast.evolution.tree.ZeroBranchSATree;

/**
 * Created by Gavra on 28/01/14.
 */
public class FBDModel extends SpeciesTreeDistribution {

    public Input<RealParameter> originInput =
            new Input<RealParameter>("origin", "The origin of infection", Input.Validate.REQUIRED);

    //'direct' parameters
    public Input<RealParameter> birthRateInput =
            new Input<RealParameter>("birthRate", "Birth rate");
    public Input<RealParameter> deathRateInput =
            new Input<RealParameter>("deathRate", "Death rate");
    public Input<RealParameter> samplingRateInput =
            new Input<RealParameter>("samplingRate", "Sampling rate per individual");

    //transformed parameters:
    public Input<RealParameter> diversificationRateInput =
            new Input<RealParameter>("diversificationRate", "Net diversification rate. Birth rate - death rate", Input.Validate.XOR, birthRateInput);
    public Input<RealParameter> turnoverInput =
            new Input<RealParameter>("turnover", "Turnover. Death rate/birth rate", Input.Validate.XOR, deathRateInput);
    public Input<RealParameter> samplingProportionInput =
            new Input<RealParameter>("samplingProportion", "The probability of sampling prior to death. Sampling rate/(sampling rate + death rate)", Input.Validate.XOR, samplingRateInput);


    // r parameter
    public Input<RealParameter> becomeNoninfectiousAfterSamplingProbability =
            new Input<RealParameter>("becomeNoninfectiousAfterSamplingProbability", "The probability of an individual to become noninfectious immediately after the sampling", Input.Validate.REQUIRED);

    public Input<RealParameter> rhoProbability =
            new Input<RealParameter>("rho", "Probability of an individual to be sampled at present", (RealParameter)null);


    protected double r;
    protected double lambda;
    protected double mu;
    protected double psi;
    protected double c1;
    protected double c2;
    protected double origin;
    protected double rho;
    protected boolean transform; //is true if the model is parametrised through transformed parameters

    public void initAndValidate() throws Exception {

        if (birthRateInput.get() != null && deathRateInput.get() != null && samplingRateInput.get() != null) {

            transform = false;
            //mu = deathRateInput.get().getValue();
            //psi = samplingRateInput.get().getValue();
            //lambda = birthRateInput.get().getValue();

        } else if (diversificationRateInput.get() != null && turnoverInput.get() != null && samplingProportionInput.get() != null) {

            transform = true;

        } else {
            throw new RuntimeException("Either specify birthRate, deathRate and samplingRate OR specify diversificationRate, turnover and samplingProportion!");
        }

    }

    private double p0s(double t) {
        double p0 = (lambda + mu + psi - c1 * ((1 + c2) - Math.exp(-c1 * t) * (1 - c2)) / ((1 + c2) + Math.exp(-c1 * t) * (1 - c2))) / (2 * lambda);
        return r + (1 - r) * p0;
    }

    private double oneMinusP0Hat(double t) {
        return rho*(lambda-mu)/(lambda*rho + (lambda*(1-rho) - mu)* Math.exp((mu-lambda) * t)) ;
    }

    private double q(double t) {
        return Math.exp(c1 * t) * (1 + c2) * (1 + c2) + Math.exp(-c1 * t) * (1 - c2) * (1 - c2) + 2 * (1 - c2 * c2);
    }

    private void transformParameters() {
        double d = diversificationRateInput.get().getValue();
        double r_turnover = turnoverInput.get().getValue();
        double s = samplingProportionInput.get().getValue();
        lambda = d/(1-r_turnover);
        mu = r_turnover*lambda;
        psi = mu*s/(1-s);
    }

    private void updateParameters() {

        if (transform) {
            transformParameters();
        } else {
            lambda = birthRateInput.get().getValue();
            mu = deathRateInput.get().getValue();
            psi = samplingRateInput.get().getValue();
        }

        r = becomeNoninfectiousAfterSamplingProbability.get().getValue();
        if (rhoProbability.get() != null ) {
            rho = rhoProbability.get().getValue();
        } else {
            rho = 0.;
        }
        c1 = Math.sqrt((lambda - mu - psi) * (lambda - mu - psi) + 4 * lambda * psi);
        c2 = -(lambda - mu - 2*lambda*rho - psi) / c1;
        origin = originInput.get().getValue();
    }

    @Override
    public double calculateTreeLogLikelihood(TreeInterface tree)
    {
        int nodeCount = tree.getNodeCount();
        updateParameters();
        //double x0 = tree.getRoot().getHeight() + origToRootDistance;
        if (origin < tree.getRoot().getHeight()) {
            return Double.NEGATIVE_INFINITY;
        }

        double logPost = -Math.log(q(origin)) - Math.log(oneMinusP0Hat(origin));


        int internalNodeCount = tree.getLeafNodeCount() - ((ZeroBranchSATree)tree).getDirectAncestorNodeCount() - 1;

        logPost += Math.log(Math.pow(2, internalNodeCount));

        for (int i = 0; i < nodeCount; i++) {
            if (tree.getNode(i).isLeaf()) {
                if  (!((ZeroBranchSANode)tree.getNode(i)).isDirectAncestor())  {
                    if (tree.getNode(i).getHeight() > 0.000000000005 || rho == 0.) {
                        logPost += Math.log(psi) + Math.log(q(tree.getNode(i).getHeight())) + Math.log(p0s(tree.getNode(i).getHeight()));
                    } else {
                        logPost += Math.log(4*rho);
                    }
                }
            } else {
                if (((ZeroBranchSANode)tree.getNode(i)).isFake()) {
                    if (r == 1) {
                        System.out.println("r = 1 but there are sampled ancestors in the tree");
                        System.exit(0);
                    }
                    logPost += Math.log(psi) + Math.log(1 - r);
                } else {
                    logPost += Math.log(lambda) - Math.log(q(tree.getNode(i).getHeight()));
                }
            }
        }

        //System.out.println("logpost = " + logPost + " 2 to the power = " + Math.pow(2, internalNodeCount));

        return logPost;
    }

    @Override
    protected boolean requiresRecalculation() {
        return true;
    }

}
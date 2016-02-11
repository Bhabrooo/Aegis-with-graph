package com.jamal.aegistest;

import java.util.Vector;

/**
 * Created by Jamal on 22-Jan-16.
 */
public class ECG {
    //constants
    private static final int fs = 360;// same as sample_rate. Shortened for ease
    private static final int sample_rate = 360;
    private static final int rawL = 1800;
    private static final int sqrd_dL = 1804;
    private static final int integralL = 1857;

    //variables
    private double mean_RR, m_selected_RR, test_m;
    private double raw[], sqrd_d[], integral[];
    private int delay, not_nois, skip, ser_back;
    private Vector<Double> qrs_c ;
    private Vector<Integer> qrs_i;
    public Vector<Double> qrs_amp_raw;
    public Vector<Integer>qrs_i_raw;
    private  Vector<Double> nois_c;
    private Vector<Integer> nois_i;

    //default constructor
    ECG(){
        this.delay = 0;
        this.mean_RR = 0;
        this.m_selected_RR = 0;
        this.test_m = 0;
        this.not_nois = 0;
        this.skip = 0;
        this.ser_back = 0;

        raw = new double[rawL];
        sqrd_d = new double[sqrd_dL];
        integral = new double[integralL];

        qrs_c = new Vector<Double>(0);
        qrs_i = new Vector<Integer>(0);
        qrs_amp_raw = new Vector<Double>(0);
        qrs_i_raw = new Vector<Integer>(0);
        nois_c = new Vector<Double>(0);
        nois_i = new Vector<Integer>(0);

        for (int i=0; i<integralL; i++){
            this.integral[i]=0;
            if(i<sqrd_dL)
            {
                this.sqrd_d[i]=0;
            }
        }
    }

    //parameterised constructor
    ECG(Vector<Double> ecg_ble){
        this.delay = 0;
        this.mean_RR = 0;
        this.m_selected_RR = 0;
        this.test_m = 0;
        this.not_nois = 0;
        this.skip = 0;
        this.ser_back = 0;

        raw = new double[rawL];
        sqrd_d = new double[sqrd_dL];
        integral = new double[integralL];

        qrs_c = new Vector<Double>(0);
        qrs_i = new Vector<Integer>(0);
        qrs_amp_raw = new Vector<Double>(0);
        qrs_i_raw = new Vector<Integer>(0);
        nois_c = new Vector<Double>(0);
        nois_i = new Vector<Integer>(0);

        for (int i=0; i<integralL; i++){
            this.integral[i]=0;
            if(i<sqrd_dL)
            {
                this.sqrd_d[i]=0;
            }
            if(i<rawL){
                this.raw[i]= ecg_ble.elementAt(i);
            }
        }
    }

    // fucntions
    // convolves array in[] with array filt[]. This is used for filtering
    void conv(double[] out, double[] in, double[] filt, int lin, int lfilt){
        int t1, totalL;
        double t2;
        totalL=lin+lfilt-1;
        for (int i=0; i<totalL; i++){
            out[i]=0;
        }
        for (int i=0; i<totalL; i++){
            t1=i;
            t2=0;
            for (int j=0; j<lfilt; j++){
                if (!(t1<0)){
                    if (t1<=(lin-1)){
                        t2=t2+(in[t1]*filt[j]);
                    }
                }
                t1=t1-1;
            }
            out[i]=t2;
        }
    }

    // returns MAX amplitude of the array sig[]
    double findmax1(double[] sig, int ind1, int ind2){
        double max=sig[ind1-1];
        for (int i=ind1; i<ind2; i++){
            if(sig[i]>=max){
                max=sig[i];
            }
        }
        return max;
    }

    // returns INDEX OF MAX amplitude of the array sig[]
    int findmax2(double[] sig, int ind1, int ind2){
        double max=sig[ind1-1];
        int loc=ind1-1;
        for (int i=ind1; i<ind2; i++){
            if(sig[i]>=max){
                max=sig[i];
                loc=i-ind1+1;
            }
        }
        return loc;
    }

    // returns the MEAN VALUE of the array sig[]
    double findmean(double[] sig, int ind1, int ind2){
        double mean=0;
        for(int i=ind1-1; i<ind2; i++)
        {
            mean=mean+sig[i];
        }
        mean= (mean/(ind2-ind1+1));
        return mean;
    }

    // returns the MEAN VALUE of the vector sig
    double findmean_v(Vector<Integer> sig, int ind1, int ind2){
        double mean=0;
        for (int i=ind1-1; i<ind2; i++)
        {
            mean=mean+sig.elementAt(i);
        }
        mean= (mean/(ind2-ind1+1));
        return mean;
    }

    // returns a vector diffRR containing DIFFERENCE OF CONSECUTIVE ELEMENTS of vector sig
    void finddiff(Vector<Integer> diffRR, Vector<Integer> sig, int ind1, int ind2){
        int temp;
        for(int i=ind1-1; i<ind2-1; i++){
            temp=sig.elementAt(i+1) - sig.elementAt(i);
            diffRR.addElement(temp);
        }
    }

    // returns a vector diffRR containing DIFFERENCE OF CONSECUTIVE ELEMENTS of array sig
    void finddiff2(Vector<Integer> diffRR, double[] sig, int ind1, int ind2){
        int temp;
        for(int i=ind1-1; i<ind2-1; i++){
            temp=(int)(sig[i+1] - sig[i]);
            diffRR.addElement(temp);
        }
    }

    // Returns location of the peaks found in the array sig
    Vector<Integer> peakfind_loc(double[] sig, float min_dis){
        Vector<Integer> loc = new Vector<Integer>(0);
        double norm_sig[] = new double[integralL];
        double max=findmax1(sig,1,integralL);
        for (int i=0; i<integralL; i++){
            norm_sig[i]=sig[i]/max;
        }
        int prev_loc=0, distance=0, count=0;
        double current, old1=norm_sig[1], old2=norm_sig[0];
        for (int i=2; i<integralL; i++){
            current=norm_sig[i];
            if(count==0){
                if((current>0.25) && (current<old1) && (old1>=old2)){
                    count=count+1;
                    loc.addElement(i-1);
                    prev_loc=i-1;
                }
            }
            else{
                if((current>0.25) && (current<old1) && (old1>=old2)){
                    distance=(i-1)-prev_loc;
                    if(distance>min_dis){
                        count=count+1;
                        loc.addElement(i-1);
                        prev_loc=i-1;
                    }
                }
            }
            old2=old1;
            old1=current;
        }
        return loc;
    }

    // Returns amplitude of the peaks found in the array sig
    Vector<Double> peakfind_amp(double[] sig, float min_dis){
        Vector<Double> ampl = new Vector<Double>(0);
        double norm_sig[] = new double[integralL];
        double max=findmax1(sig,1,integralL);
        for (int i=0; i<integralL; i++){
            norm_sig[i]=sig[i]/max;
        }
        int prev_loc=0, distance=0, count=0;
        double current, old1=norm_sig[1], old2=norm_sig[0];
        for (int i=2; i<integralL; i++){
            current=norm_sig[i];
            if(count==0){
                if((current>0.25) && (current<old1) && (old1>=old2)){
                    count=count+1;
                    ampl.addElement(sig[i - 1]);
                    prev_loc=i-1;
                }
            }
            else{
                if((current>0.25) && (current<old1) && (old1>=old2)){
                    distance=(i-1)-prev_loc;
                    if(distance>min_dis){
                        count=count+1;
                        ampl.addElement(sig[i-1]);
                        prev_loc=i-1;
                    }
                }
            }
            old2=old1;
            old1=current;
        }
        return ampl;
    }

    // calculates the SQUARED DERIVATIVE from raw array
    void derivative(){
        delay=delay+2;
        double max;
        double filt_deri[]= new double[]{-0.125, -.25, 0, .25, .125};
        int l_filt=5;
        int lengthR=rawL+l_filt-1;
        conv(sqrd_d, raw, filt_deri, rawL, l_filt);
        max=findmax1(sqrd_d, 1, lengthR);
        for(int i=0; i<lengthR; i++){
            sqrd_d[i]=sqrd_d[i]/max;
            sqrd_d[i]=sqrd_d[i]*sqrd_d[i];
        }
    }

    // calculates the INTEGRAL from SQRD_D array
    void integrate(){
        delay = delay + 15;
        double filt_inti[]= new double[54];
        for (int i=0;i<54;i++){
            filt_inti[i]=0.0185;
        }
        int l_filt=54;
        //int lengthR=sqrd_dL+l_filt-1;
        conv(integral, sqrd_d, filt_inti, sqrd_dL, l_filt);
    }

    // main algorithm which uses all other functions. gives location of R peaks and thier amplitudes
    void r_peaks(){
        derivative();
        integrate();
        Vector<Integer> locs = new Vector<Integer>(0);
        Vector<Double> pks = new Vector<Double>(0);
        locs = peakfind_loc(integral,72);
        pks = peakfind_amp(integral,72);

        // --------------------------------------------
        // initialize the training phase of 2 secs
        double THR_SIG, THR_NOISE, SIG_LEV, NOISE_LEV;
        THR_SIG = findmax1(integral,1,720);
        THR_SIG = THR_SIG/3;
        THR_NOISE = findmean(integral,1,720);
        THR_NOISE=THR_NOISE/2;
        SIG_LEV= THR_SIG;
        NOISE_LEV = THR_NOISE;

        // --------------------------------------------
        // Initialize raw signal threshold
        double THR_SIG1, THR_NOISE1, SIG_LEV1, NOISE_LEV1;
        THR_SIG1 = findmax1(raw,1,720);
        THR_SIG1 = THR_SIG1/3;
        THR_NOISE1 = findmean(raw,1,720);
        THR_NOISE1 = THR_NOISE1/2;
        SIG_LEV1 = THR_SIG1;
        NOISE_LEV1 = THR_NOISE1;

        // --------------------------------------------
        // decision rule and thresholding
        int x_i=0;
        double y_i=0;
        for(int i=0; i<pks.size();i++){
            //locate the corresponding peak in the filtered signal
            //y_i is the peak value and x_i is the peak index
            if( ((locs.elementAt(i)-54)>= 1) && (locs.elementAt(i)<= 2000) ){
                x_i=findmax2(raw, (locs.elementAt(i)-54), locs.elementAt(i));
                y_i=findmax1(raw, (locs.elementAt(i)-54), locs.elementAt(i));
            }
            else if(i==0){
                x_i=findmax2(raw, 1, locs.elementAt(i));
                y_i=findmax1(raw, 1, locs.elementAt(i));
                ser_back = 1;
            }
            else if(locs.elementAt(i)>=2000){
                x_i=findmax2(raw, (locs.elementAt(i)-54), 2000);
                y_i=findmax1(raw, (locs.elementAt(i)-54), 2000);
            }

            //update the heart rate
            if(qrs_c.size() >= 9){
                Vector<Integer> diffRR= new Vector<Integer>(0);
                int comp;
                finddiff(diffRR, qrs_i, 1, qrs_i.size()); //find RR intervals in qrs_i
                mean_RR = findmean_v(diffRR, 1, diffRR.size()); //calculate the mean of 8 previous R waves interval
                comp = qrs_i.elementAt(qrs_i.size()-1) - qrs_i.elementAt(qrs_i.size()-2); //latest RR
                if( (comp <= (0.92*mean_RR)) || (comp >= (1.16*mean_RR)) ){
                    // lower down thresholds to detect better
                    THR_SIG = 0.5*(THR_SIG);
                    // lower down thresholds to detect better filtered
                    THR_SIG1 = 0.5*(THR_SIG1);
                }
                else{
                    //the latest regular beats mean
                    m_selected_RR = mean_RR;
                }
            }

            // calculate the mean of the last 8 R waves to make sure that QRS is not
            // missing(If no R detected , trigger a search back) 1.66*mean
            if(m_selected_RR!=0){
                test_m = m_selected_RR; //if the regular RR availabe use it
            }
            else if( (mean_RR==0) && (m_selected_RR==0) ){
                test_m = mean_RR;
            }
            else{
                test_m=0;
            }

            if(test_m!=0){
                //if a QRS is missed
                if( (locs.elementAt(i)-qrs_i.elementAt(qrs_i.size()-1)) >= (1.66*test_m ) ){
                    // search back and locate the max in the interval
                    // [prev QRS+200ms refractory, current peak - 200 ms];
                    double pks_temp;
                    pks_temp = findmax1(integral, (72 + qrs_i.elementAt(qrs_i.size() - 1)), (locs.elementAt(i) - 72));
                    int locs_temp;
                    locs_temp = findmax2(integral, (72+qrs_i.elementAt(qrs_i.size()-1)), (locs.elementAt(i)-72));
                    locs_temp = qrs_i.elementAt(qrs_i.size()-1) + 72 + locs_temp -1;
                    if(pks_temp > THR_NOISE){
                        qrs_c.addElement(pks_temp);
                        qrs_i.addElement(locs_temp);
                        // find the location in filtered sig i.e actual ECG waveform
                        int x_i_t;
                        double y_i_t;
                        if(locs_temp <= 2000){
                            x_i_t=findmax2(raw, (locs_temp-54), locs_temp);
                            y_i_t=findmax1(raw, (locs_temp-54), locs_temp);
                        }
                        else{
                            x_i_t=findmax2(raw, (locs_temp-54), 2000);
                            y_i_t=findmax1(raw, (locs_temp-54), 2000);
                        }
                        //take care of bandpass signal threshold
                        if(y_i_t > THR_NOISE1){
                            int pushed = locs_temp-54+(x_i_t-1);
                            qrs_i_raw.addElement(pushed); // save index of raw ecg
                            qrs_amp_raw.addElement(y_i_t); // save amplitude of raw ecg
                            SIG_LEV1 = 0.25*y_i_t + 0.75*SIG_LEV1; // when found with the second thres set
                        }
                        not_nois = 1;
                        SIG_LEV = 0.25*pks_temp + 0.75*SIG_LEV; // when found with the second threshold
                    }
                }
                else{
                    not_nois = 0;
                }
            }
            // find noise and QRS peaks
            if(pks.elementAt(i) >= THR_SIG){
                // if a QRS candidate occurs within 360ms of the previous QRS
                // the algorithm determines if its T wave or QRS
                if(qrs_c.size()>=3){
                    if((locs.elementAt(i)-qrs_i.elementAt(qrs_i.size()-1)) <= 130){
                        Vector<Integer> temp1 = new Vector<Integer>(0);
                        Vector<Integer> temp2 = new Vector<Integer>(0);
                        double slope1, slope2;

                        finddiff2( temp1, integral, (locs.elementAt(i)-27), locs.elementAt(i) );
                        slope1=findmean_v(temp1, 1, temp1.size()); // mean slope of the waveform at that position

                        finddiff2(temp2, integral, (qrs_i.elementAt(qrs_i.size()-1) -27), qrs_i.size());
                        slope2=findmean_v(temp2, 1, temp2.size()); // mean slope of previous R wave

                        // slope less then 0.5 of previous R
                        if(Math.abs(slope1) <= Math.abs(0.5 * (slope2))){
                            nois_c.addElement(pks.elementAt(i));
                            nois_i.addElement(locs.elementAt(i));
                            skip = 1;                         // T wave identification
                            // adjust noise level in both filtered and MVI
                            NOISE_LEV1 = 0.125*y_i + 0.875*NOISE_LEV1;
                            NOISE_LEV = 0.125*pks.elementAt(i) + 0.875*NOISE_LEV;
                        }
                        else{
                            skip=0;
                        }
                    }
                }

                // skip is 1 when a T wave is detected
                if(skip==0){
                    qrs_c.addElement(pks.elementAt(i));
                    qrs_i.addElement(locs.elementAt(i));
                    //bandpass filter check threshold
                    if(y_i >= THR_SIG1){
                        if(ser_back!=0){
                            qrs_i_raw.addElement(x_i); // save index of raw
                        }
                        else{
                            int pushed2;
                            pushed2= locs.elementAt(i)-54+(x_i-1);
                            qrs_i_raw.addElement(pushed2); // save index of raw
                        }
                        qrs_amp_raw.addElement(y_i); // save index of raw
                        SIG_LEV1 = 0.125*y_i + 0.875*SIG_LEV1; // adjust threshold for raw
                    }
                    // adjust Signal level
                    SIG_LEV = 0.125*pks.elementAt(i) + 0.875*SIG_LEV;
                }
            }
            else if(pks.elementAt(i) < THR_NOISE){
                nois_c.addElement(pks.elementAt(i));
                nois_i.addElement(locs.elementAt(i));
                // noise level in filtered signal
                NOISE_LEV1 = 0.125*y_i + 0.875*NOISE_LEV1;
                // adjust Noise level in MVI
                NOISE_LEV = 0.125*pks.elementAt(i) + 0.875*NOISE_LEV;
            }

            // adjust the threshold with SNR
            if((NOISE_LEV != 0) || (SIG_LEV != 0)){
                THR_SIG = NOISE_LEV + 0.25*(Math.abs(SIG_LEV - NOISE_LEV));
                THR_NOISE = 0.5*(THR_SIG);
            }

            // adjust the threshold with SNR for bandpassed signal
            if((NOISE_LEV1 != 0) || (SIG_LEV1 != 0)){
                THR_SIG1 = NOISE_LEV1 + 0.25*(Math.abs(SIG_LEV1 - NOISE_LEV1));
                THR_NOISE1 = 0.5*(THR_SIG1);
            }
            skip = 0; // reset parameters
            not_nois = 0; // reset parameters
            ser_back = 0;  // reset raw param
        }
    }
// CLASS END
}

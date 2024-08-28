/*
 *  21.04.2004 Original verion. davagin@udm.ru.
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package davaguine.jmac.tools;

/**
 * @author Dmitry Vaguine
 * @version 04.03.2004 14:51:31
 */
public class ProgressHelper {

    public ProgressHelper(int totalSteps, ProgressCallback progressCallback) {
        callbackFunction = progressCallback;

        this.totalSteps = totalSteps;
        currentStep = 0;
        lastCallbackFiredPercentageDone = 0;

        updateProgress(0);
    }

    public void updateStatus(String msg) {
        callbackFunction.updateStatus(msg);
    }

    public void updateProgress() {
        updateProgress(-1, false);
    }

    public void updateProgress(int currentStep) {
        updateProgress(currentStep, false);
    }

    public void updateProgress(int currentStep, boolean forceUpdate) {
        // update the step
        if (currentStep == -1)
            this.currentStep++;
        else
            this.currentStep = currentStep;

        // figure the percentage done
        float percentageDone = ((float) (currentStep)) / ((float) (Math.max(totalSteps, 1)));
        int percentageDone_ = (int) (percentageDone * 1000 * 100);
        if (percentageDone_ > 100000) percentageDone_ = 100000;

        // fire the callback
        if (callbackFunction != null) {
            callbackFunction.percentageDone = percentageDone_;
            if (forceUpdate || (percentageDone_ - lastCallbackFiredPercentageDone) >= 1000) {
                callbackFunction.callback(percentageDone_);
                lastCallbackFiredPercentageDone = percentageDone_;
            }
        }
    }

    public void updateProgressComplete() {
        updateProgress(totalSteps, true);
    }

    public boolean isKillFlag() {
        return callbackFunction != null ? callbackFunction.killFlag : false;
    }

    private ProgressCallback callbackFunction = null;

    private final int totalSteps;
    private int currentStep;
    private int lastCallbackFiredPercentageDone;
}

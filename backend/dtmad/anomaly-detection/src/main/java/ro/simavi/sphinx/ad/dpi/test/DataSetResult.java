package ro.simavi.sphinx.ad.dpi.test;

public class DataSetResult {

    private String input;

    private String malwareInput;

    private String output;

    private String malwareOutput;

    private String mergeOutput;

    public DataSetResult(String input, String malwareInput, String output, String malwareOutput, String mergeOutput){
        this.input = input;
        this.malwareInput = malwareInput;
        this.output = output;
        this.malwareOutput = malwareOutput;
        this.mergeOutput = mergeOutput;
    }

    public String getInput() {
        return input;
    }

    public void setInput(String input) {
        this.input = input;
    }

    public String getMalwareInput() {
        return malwareInput;
    }

    public void setMalwareInput(String malwereInput) {
        this.malwareInput = malwereInput;
    }

    public String getOutput() {
        return output;
    }

    public void setOutput(String output) {
        this.output = output;
    }

    public String getMalwareOutput() {
        return malwareOutput;
    }

    public void setMalwareOutput(String malwareOutput) {
        this.malwareOutput = malwareOutput;
    }

    public String getMergeOutput() {
        return mergeOutput;
    }

    public void setMergeOutput(String mergeOutput) {
        this.mergeOutput = mergeOutput;
    }

}

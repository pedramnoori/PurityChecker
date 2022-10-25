package purity;

public class Purity {

    private String purityValue;
    private String purityValidation;
    private String purityComment;
    private String validationComment;
    private int mappingState;



    public String getValidationComment() {
        return validationComment;
    }

    public void setValidationComment(String validationComment) {
        this.validationComment = validationComment;
    }


    public int getMappingState() {
        return mappingState;
    }

    public void setMappingState(int mappingState) {
        this.mappingState = mappingState;
    }


    public String getPurityValue() {
        return purityValue;
    }

    public void setPurityValue(String purityValue) {
        this.purityValue = purityValue;
    }

    public String getPurityValidation() {
        return purityValidation;
    }

    public void setPurityValidation(String purityValidation) {
        this.purityValidation = purityValidation;
    }

    public String getPurityComment() {
        return purityComment;
    }

    public void setPurityComment(String purityComment) {
        this.purityComment = purityComment;
    }
}

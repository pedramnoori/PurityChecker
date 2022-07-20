package purity;

public class RefactoringJson {

    private String type;
    private String description;
    private String comment;
    private String validation;
    private String detectionTools;
    private String validators;
    private Purity purity;


    public Purity getPurity() {
        return purity;
    }

    public void setPurity(Purity purity) {
        this.purity = purity;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getValidation() {
        return validation;
    }

    public void setValidation(String validation) {
        this.validation = validation;
    }

    public String getDetectionTools() {
        return detectionTools;
    }

    public void setDetectionTools(String detectionTools) {
        this.detectionTools = detectionTools;
    }

    public String getValidators() {
        return validators;
    }

    public void setValidators(String validators) {
        this.validators = validators;
    }
}

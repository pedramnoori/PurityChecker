package purity;

public class ReplacementJustificationResult {

    private boolean justificationState;
    private String justificationComment;



    public boolean isJustificationState() {
        return justificationState;
    }

    public void setJustificationState(boolean justificationState) {
        this.justificationState = justificationState;
    }

    public String getJustificationComment() {
        return justificationComment;
    }

    public void setJustificationComment(String justificationComment) {
        this.justificationComment = justificationComment;
    }

    public void appendJustificationComment(String comment) {
        this.justificationComment += "\n" + comment;
    }
}
